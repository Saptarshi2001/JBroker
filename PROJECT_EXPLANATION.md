# Project Overview

This repository implements a very simple publish‑subscribe (pub/sub) broker using Java sockets and a custom thread pool. Below is a concise description of each class and the purpose of every public (or otherwise notable) method.

---

## `com.jbroker.Broker`

| Method | Visibility | Description |
|--------|------------|-------------|
| `connect()` | public | Starts a `ServerSocket` on port **4222** and continuously accepts client connections. For each accepted socket a task is submitted to the `ThreadPool`. The task invokes `handleclient` with a shared `topic` string and a reusable `Message` instance. After submitting the task the pool is stopped (which stops all workers – this appears to be a bug, but it is the current logic). |

| `handleclient(Socket socket, String topic, Message message)` | public synchronized | Reads a single byte from the client socket, converts it to a string, and forwards it to `parse`. This method assumes the client sends a complete command in one byte, which is unrealistic but reflects the simplified protocol used in the tests. |
| `parse(String msg, Socket socket, String topic, Message message)` | public synchronized | Parses a space‑separated command string.
  * `Connect {}` – acknowledges a connection with `+OK`.
  * If the internal flag `turn` is `true`, the method treats the incoming data as the payload of a publish operation, validates the length against the stored `Message`, and then calls `publishmsg` to forward the message to subscribers.
  * `Sub <topic> <subId>` – registers a subscription via `subscribe`.
  * `Pub <topic> <msg> <subId> <len>` – switches `turn` to `true`, stores the message data via `publish`, and will later be sent when `turn` is processed.
  * `unsub <subId>` – removes a subscription for the given subscriber ID via `unsub`.
|
| `subscribe(String[] msg, Socket socket)` | public synchronized | Adds a subscription for a client socket. The method uses a `ReentrantLock` to protect the shared map `mp` (structure: `topic -> (socket -> list of subIds)`). If the topic already exists, the subscriber ID is appended; otherwise a new map entry is created. |
| `publish(String[] input, Socket socket, String topic)` | public synchronized | Constructs a new `Message` object from the publish command (`input[0]` is the command token, `input[1]` is the topic, `input[2]` is the subscriber ID, `input[3]` is the length) and stores it in the instance variable `message`. It also updates the local `topic` variable (which is a method parameter, so the change does **not** affect the caller’s variable). |
| `publishmsg(Socket socket, String topic)` | public synchronized | Sends `+OK` to the publishing client, then iterates over all subscriber sockets registered for the given `topic` and writes an empty line (the real payload is omitted in this simplified implementation). The method locks `mp` during the iteration to avoid concurrent modification. |
| `unsub(int msg, Socket socket)` | public | Walks through every topic in `mp` and removes the subscriber ID `msg` from the list associated with `socket`. If the ID is found it is removed; the method does not clean up empty maps. |

---

## `com.jbroker.JbrokerApplication`

| Method | Visibility | Description |
|--------|------------|-------------|
| `main(String[] args)` | public static | Instantiates a `Broker` and calls its `connect()` method, thereby launching the broker server. |

---

## `com.jbroker.Message`

A simple data holder for a published message.

| Method | Visibility | Description |
|--------|------------|-------------|
| `Message(String message, String topic, int subid, int length)` | public | Constructor that stores the provided values.
| `getmessage()` / `setmessage(String)` | public | Getter and setter for the message payload.
| `gettopic()` / `settopic(String)` | public | Getter and setter for the topic name.
| `getsubid()` / `setsubid(int)` | public | Getter and setter for the subscriber identifier associated with the message.
| `getlength()` / `setlength(int)` | public | Getter and setter for the expected length of the message payload. |

---

## `com.jbroker.ThreadPool`

A very small custom thread pool that creates a fixed number of `Worker` threads backed by a bounded `ArrayBlockingQueue`.

| Method | Visibility | Description |
|--------|------------|-------------|
| `ThreadPool(int numberofthreads, int maxnooftasks)` | public | Initializes the blocking queue with capacity **maxnooftasks** and creates **numberofthreads** `Worker` objects. Each worker is started in its own `Thread`. |
| `execute(Runnable task)` | public synchronized | Adds a task to the queue (`offer`). Workers will retrieve tasks via `queue.take()`.
| `stop()` | public synchronized | Sets a flag `isstopped` (currently unused) and invokes `stop()` on each `Worker`. The `Worker.stop()` method is empty, so workers keep running, but the flag indicates an intended shutdown mechanism. |

---

## `com.jbroker.Worker`

Implements `Runnable` and continuously consumes tasks from the shared queue.

| Method | Visibility | Description |
|--------|------------|-------------|
| `run()` | public | Calls `queue.take()` (blocking) to obtain a `Runnable` and immediately executes `task.run()`. Any `InterruptedException` is logged. The current implementation only processes **one** task then exits; a typical worker would loop, but here the loop is missing, meaning each worker handles a single task before terminating. |
| `stop()` | public | Empty placeholder intended for graceful shutdown. |

---

## Summary

The code demonstrates a minimal, educational implementation of a broker that can accept connections, register subscriptions, and forward messages to subscribers. Several design flaws (e.g., stopping the thread pool after each connection, a worker that processes only one task, mutable state shared without proper synchronization) are present, but the overall flow is straightforward and useful for learning purposes.
