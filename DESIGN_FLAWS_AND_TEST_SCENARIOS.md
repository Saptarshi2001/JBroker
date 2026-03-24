# Design Flaws and Required Test Coverage

This document summarises the main architectural shortcomings of the current **jbroker** codebase and lists the test scenarios that should be added (or extended) to ensure reliable behaviour.

---

## 1. Design Flaws

| **Area** | **Issue** | **Impact** | **Suggested Remedy** |
|---|---|---|---|
| **ThreadPool** | `stop()` is called after each client connection in `Broker.connect()` (line 46 of [`src/main/java/com/jbroker/Broker.java`](src/main/java/com/jbroker/Broker.java:46)). | The pool shuts down prematurely, so subsequent client connections are never processed. | Remove the call or move it to a proper shutdown hook after the server stops. |
| **Worker** | `run()` consumes **only one** task (`queue.take()` then `task.run()`) and then exits (lines 27‑33 of [`src/main/java/com/jbroker/Worker.java`](src/main/java/com/jbroker/Worker.java:27)). | Worker threads terminate after the first task, causing the pool to lose workers and leading to task loss. | Wrap the take‑run block in a `while (!Thread.currentThread().isInterrupted())` loop and honour the `isstopped` flag. |
| **Synchronization & State** | `Broker.publish()` stores the topic in a **method‑local** variable (`topic = input[1];`) which does not modify the instance field used elsewhere. The shared `turn` flag is never cleared after publishing. | The broker may never send messages to the correct topic and `turn` remains true, causing unintended behaviour on subsequent parses. | Store the topic in an instance field (e.g., `this.currentTopic`) and reset `turn` after `publishmsg`. |
| **Lock Granularity** | `subscribe()` and `publishmsg()` acquire the same `ReentrantLock` but also hold the lock while performing I/O (`PrintWriter`). | Potential dead‑locks or reduced concurrency because I/O blocks while the lock is held. | Separate map‑locking from socket I/O, or use finer‑grained synchronization. |
| **Null Handling in Tests** | `BrokerHandleClientSubscribeTest` creates a mock `Socket` that is `null`, leading to NPEs. Production code does not guard against `null` sockets. | Tests cannot run; production code may crash on unexpected null arguments. | Add null‑checks or use proper Mockito stubbing (`when(mockSocket.getInputStream()).thenReturn(...)`). |
| **Parse Robustness** | `parse()` assumes the input string always contains at least two tokens and accesses `input[0]` without validation (lines 61‑63). Empty or whitespace‑only strings cause `ArrayIndexOutOfBoundsException`. | Failing tests `BrokerParseTest.testParseWithEmptyString` and `testParseWithOnlySpaces`. | Validate the input length and throw a clear `IllegalArgumentException` for malformed commands. |
| **Spring Boot Test Configuration** | `JbrokerApplicationTests` relies on a `@SpringBootTest` but the project lacks a `@SpringBootConfiguration` class, causing `IllegalStateException`. | The test suite cannot start the Spring context. | Add a minimal `@SpringBootApplication` class or annotate `JbrokerApplication` appropriately, or replace the test with a plain unit test. |
| **Unsubscribe Logic** | `unsub()` iterates over all topics and removes the subscriber ID but never removes empty entries from the map. | Memory leakage and stale topic entries. | Clean up empty `ArrayList`s and remove topic entries when no subscribers remain. |
| **Message Length Validation** | In `parse()`, the length check compares `input[0].length()` with `message.getlength()`, which is unrelated (should compare payload length). | Incorrect error handling when publishing. | Compare the actual message payload length (`input[2]` or similar) with the stored length. |

---

## 2. Test Scenarios to Add / Extend

Below is a checklist of concrete test cases that should be present in the `src/test/java/com/jbroker` package.

| **Class** | **Scenario** | **Description** |
|---|---|---|
| **BrokerParseTest** | *Empty command* | Verify that `Broker.parse("\n")` throws a defined exception (e.g., `IllegalArgumentException`). |
| | *Whitespace‑only command* | Ensure no `ArrayIndexOutOfBoundsException` is thrown and the method returns gracefully. |
| | *Invalid command token* | Pass an unknown token (e.g., `"Foo bar"`) and assert that the broker logs an error or throws `UnsupportedOperationException`. |
| **BrokerHandleClientSubscribeTest** | *Mock socket with InputStream* | Properly stub `mockSocket.getInputStream()` to return a `ByteArrayInputStream` containing a command byte and verify `handleclient` forwards the correct string. |
| | *Null socket handling* | Call `handleclient(null, …)` and assert that a `NullPointerException` is thrown with a clear message (or that the method guards against null). |
| | *Subscription map integrity* | After calling `subscribe()` ensure the internal `mp` contains the expected topic → socket → subscriber‑id mapping. |
| | *Duplicate subscription* | Attempt to subscribe the same `subId` twice and verify the method prints "Not possible" and does not duplicate the entry. |
| **BrokerPublishTest** | *Publish flow* | Simulate a full publish sequence (`Pub` followed by payload) and assert that all subscribed sockets receive the `+OK` and message placeholder. |
| | *Turn flag reset* | Verify that after `publishmsg` the `turn` flag is cleared, preventing accidental re‑publishing. |
| **ThreadPoolTest** (new) | *Multiple tasks* | Submit several `Runnable` tasks and confirm each is executed. |
| | *Worker lifecycle* | Ensure workers keep processing tasks until `stop()` is invoked. |
| **WorkerTest** (new) | *Loop behaviour* | Validate that a worker processes tasks in a loop (once the bug is fixed). |
| **JbrokerApplicationTests** | *Spring context startup* | Add a minimal `@SpringBootApplication` class (e.g., `JbrokerApplication`) and verify the context loads without errors. |
| | *Integration test* | Start the broker on an available port, connect a client socket, send a `Connect {}` command and verify `+OK` response. |
| **BrokerTest** | *Server socket binding* | Ensure `Broker.connect()` correctly binds to port **4222** and handles `IOException` when the port is already in use. |
| | *Graceful shutdown* | Add a test that invokes a shutdown hook (or stops the `ThreadPool`) and verifies resources are released. |

---

## 3. Checklist for Implementation

- [ ] Refactor **ThreadPool** to avoid stopping after each connection.
- [ ] Update **Worker.run()** so it processes tasks in a loop.
- [ ] Add proper null checks and Mockito stubs in *BrokerHandleClientSubscribeTest*.
- [ ] Harden **Broker.parse()** with input validation and meaningful exceptions.
- [ ] Provide a minimal Spring Boot configuration class for *JbrokerApplicationTests*.
- [ ] Extend the test suite with the scenarios listed above.
- [ ] Document all changes in this markdown file.

By addressing the design flaws and enriching the test coverage, the broker will become more reliable and maintainable.
