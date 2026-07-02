"""End-to-end tests against the actual Java JBroker process."""

import asyncio
import contextlib
import io
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import types
import unittest
from pathlib import Path

SDK_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
SRC_DIR = SDK_ROOT / "src"
JAVA_SOURCE_DIR = REPOSITORY_ROOT / "src" / "main" / "java"

package = types.ModuleType("jbroker_client")
package.__path__ = [str(SRC_DIR)]
sys.modules["jbroker_client"] = package

from jbroker_client.client import PyClient
from jbroker_client.exceptions import ConnectionError_


def find_available_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


async def wait_for_output(
    output: io.StringIO,
    expected: str,
    timeout: float = 3.0,
) -> None:
    deadline = asyncio.get_running_loop().time() + timeout
    while expected not in output.getvalue():
        if asyncio.get_running_loop().time() >= deadline:
            raise AssertionError(
                f"Timed out waiting for {expected!r}. Output: {output.getvalue()!r}"
            )
        await asyncio.sleep(0.01)


class RealJBrokerProcess:
    """Compile and run the repository's Java broker for Python e2e tests."""

    def __init__(self):
        self.host = "127.0.0.1"
        self.port = find_available_port()
        self._temporary_directory = tempfile.TemporaryDirectory()
        self._classes = Path(self._temporary_directory.name) / "classes"
        self._process: subprocess.Popen | None = None

    def start(self) -> None:
        java = shutil.which("java")
        javac = shutil.which("javac")
        if not java or not javac:
            raise unittest.SkipTest("Java and javac are required for JBroker e2e tests")

        self._classes.mkdir(parents=True)
        sources = [str(path) for path in JAVA_SOURCE_DIR.rglob("*.java")]
        compile_result = subprocess.run(
            [javac, "-d", str(self._classes), *sources],
            capture_output=True,
            text=True,
            timeout=60,
            check=False,
        )
        if compile_result.returncode != 0:
            raise RuntimeError(
                "Failed to compile JBroker:\n"
                f"{compile_result.stdout}\n{compile_result.stderr}"
            )

        environment = os.environ.copy()
        environment["SERVER_ADDRESS"] = self.host
        environment["SERVER_PORT"] = str(self.port)
        self._process = subprocess.Popen(
            [java, "-cp", str(self._classes), "com.jbroker.JbrokerApplication"],
            cwd=REPOSITORY_ROOT,
            env=environment,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        self._wait_until_ready()

    def _wait_until_ready(self, timeout: float = 10.0) -> None:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self._process and self._process.poll() is not None:
                output = self._process.stdout.read() if self._process.stdout else ""
                raise RuntimeError(f"JBroker exited during startup:\n{output}")
            try:
                with socket.create_connection((self.host, self.port), timeout=0.2):
                    return
            except OSError:
                time.sleep(0.05)
        raise RuntimeError("JBroker did not start before the timeout")

    def stop(self) -> None:
        if self._process and self._process.poll() is None:
            self._process.terminate()
            try:
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._process.kill()
                self._process.wait(timeout=5)
        if self._process and self._process.stdout:
            self._process.stdout.close()
        self._temporary_directory.cleanup()


class TestRealJBrokerE2E(unittest.TestCase):
    """Exercise the Python SDK against the repository's Java broker."""

    broker: RealJBrokerProcess

    @classmethod
    def setUpClass(cls):
        cls.broker = RealJBrokerProcess()
        cls.broker.start()

    @classmethod
    def tearDownClass(cls):
        cls.broker.stop()

    def make_client(self) -> PyClient:
        return PyClient(
            host=self.broker.host,
            port=self.broker.port,
            max_retries=1,
            backoff_base=0.01,
        )

    def test_connect_and_disconnect(self):
        async def run_test():
            client = self.make_client()
            await client.connect(client.host, client.port)
            self.assertTrue(client.connected)
            await client.disconnect()
            self.assertFalse(client.connected)

        asyncio.run(run_test())

    def test_subscribe_and_unsubscribe(self):
        async def run_test():
            client = self.make_client()
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                await client.connect(client.host, client.port)
                await client.subscribe("sports.football", 42)
                await wait_for_output(output, "Subscribed to 'sports.football'")
                await client.unsubscribe(42)
                await wait_for_output(output, "Unsubscribed subscriber 42")
                await client.disconnect()

        asyncio.run(run_test())

    def test_publish_delivers_to_another_python_client(self):
        async def run_test():
            subscriber = self.make_client()
            publisher = self.make_client()
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                await subscriber.connect(subscriber.host, subscriber.port)
                await publisher.connect(publisher.host, publisher.port)
                await subscriber.subscribe("python.e2e", 101)
                await wait_for_output(output, "Subscribed to 'python.e2e'")

                await publisher.publish("python.e2e", "hello-from-python")
                await wait_for_output(
                    output,
                    "Received message: hello-from-python",
                )

                await subscriber.disconnect()
                await publisher.disconnect()

        asyncio.run(run_test())

    def test_connect_failure(self):
        client = PyClient(
            host="127.0.0.1",
            port=find_available_port(),
            max_retries=1,
            backoff_base=0.01,
        )
        with self.assertRaises(ConnectionError_):
            asyncio.run(client.connect(client.host, client.port))

    def test_subscribe_before_connect(self):
        client = self.make_client()
        with self.assertRaises(ConnectionError_):
            asyncio.run(client.subscribe("not-connected", 1))


if __name__ == "__main__":
    unittest.main()
