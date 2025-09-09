# BitBuddies

**BitBuddies** is a peer-to-peer (P2P) networking and distributed file storage system
It provides a lightweight TCP transport, customizable peer management, and persistent storage per node

---

## Table of Contents
1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Getting Started](#getting-started)
5. [Configuration](#configuration)
6. [Example Usage](#example)
7. [Testing](#testing)
8. [Notes](#notes)

---

## Features
- TCP-based P2P transport layer
- Peer discovery through bootstrap nodes
- Handshake and peer connection hooks
- Persistent storage per node
- Concurrent handling of multiple peers using virtual threads (Java 21)
- Logging with SLF4J + Logback
- RPC-based message passing between peers

---

## Tech Stack
- **Java 21**
- **Gradle** for dependency and build management
- **SLF4J + Logback** for logging
- **JUnit 5** for testing

---

## Architecture

      +-------------------+
      |     App.java      |
      +-------------------+
                |
                v
      +---------------------+
      |      Server         |
      |---------------------|
      | Storage             |
      | Concurrent Peers    |
      +---------------------+
                |
                v
      +---------------------+
      |    TCPTransport     |
      |---------------------|
      | TCPPeer             |
      | RPC Queue           |
      +---------------------+


- **App.java**: Starts one or more servers on specified ports, optionally bootstrapping other nodes
- **Server**: Maintains peers, storage, and handles incoming RPC messages
- **TCPTransport**: Handles socket-level connections, peer handshake, and RPC queueing
- **TCPPeer**: Represents a single peer connection with send/receive methods
- **Storage**: Reads/writes files, organizes them with **hashed paths** for consistency
- **StorageOptions**: Configures storage paths and hashing logic


## Getting Started

### Prerequisites
- Java 21 installed (`java -version`)
- Gradle wrapper available (`./gradlew`)

### Clone and Build
```bash
git clone <repo-url>
cd BitBuddies
./gradlew build
```

### Configuration

Ports: Defined in `App.makeServer(listenAddr, storageDir, bootstrapNodes)`

Storage: Each server writes to its **own** storage directory

Hooks:

- handshake(peer): called when a peer connects

- onPeer(peer): called after handshake is complete

### Example Usage

```java
// Start a node
makeServer(":5000", "5000_storage", List.of());

// Start a second node and connect it to a bootstrap node
makeServer(":5001", "5001_storage", List.of(":5000"));
```

### Testing

Run unit tests with Maven:
```bash
./gradlew test
```

### Notes

- Uses one virtual thread per socket connection for concurrency

- TCP connections are managed via TCPTransport and represented by TCPPeer, allowing for future UDP/gRPC implementations

- Peers are stored in a thread-safe ConcurrentHashMap<SocketAddress, Peer>

- Storage paths are hashed with SHA-256 for file organization

- Supports dynamic network formation via bootstrap nodes
