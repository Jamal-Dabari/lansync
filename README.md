# LANSync

A peer-to-peer CLI file sync tool for your local network. No cloud, no accounts, no manual IP configuration — devices find each other over mDNS, authenticate with a shared passphrase, and transfer only the files that have changed.

## Why

Dropbox / Drive / OneDrive route your files through someone else's servers. `rsync` works locally but needs manual IPs, SSH keys, and has rough Windows support. LANSync fills the gap: zero-config discovery, end-to-end encryption, cross-platform, and single-binary.

## Features

- **Zero-config discovery** — mDNS/Bonjour (`_lansync._tcp.local.`) finds peers automatically
- **End-to-end encryption** — AES-256-GCM with PBKDF2-derived keys (600k iterations)
- **Delta sync** — SHA-256 manifest diff; only changed files cross the wire
- **Integrity verified** — every file re-hashed after transfer; tmp-file + atomic rename
- **Cross-platform** — JVM-based; optional GraalVM native image for single-binary distribution
- **LAN-only** — no internet dependency, no cloud exposure

## Requirements

- Java 25+ 
- A LAN that allows mDNS 
- For native builds: GraalVM for JDK 25

## Build

```bash
./gradlew :app:installDist           # JVM distribution at app/build/install/app
./gradlew :app:nativeCompile         # native binary at app/build/native/nativeCompile/app
```

## Quickstart

On both devices:

```bash
lansync init --dir ~/LANSync         # register device, set sync directory
lansync serve                        # start listener (foreground)
```

On one device, in another shell:

```bash
lansync list-peers                   # discover peers on the LAN
lansync sync <peer-id>               # pull/push deltas
lansync sync <peer-id> --dry-run     # preview changes
lansync sync <peer-id> --mirror      # delete remote files missing locally
lansync status                       # last sync time + manifest stats
```

## Commands

| Command                      | Description                                              |
| ---------------------------- | -------------------------------------------------------- |
| `init --dir <path>`          | Initialize sync directory and generate device ID         |
| `serve`                      | Start the TCP listener and register the mDNS service     |
| `list-peers [--timeout N]`   | Show peers discovered on the LAN                         |
| `sync <peer-id> [flags]`     | Perform a bidirectional delta sync with a peer           |
| `status`                     | Show device ID, sync dir, last sync time, file counts    |
| `discover` / `listen` / `send` | Low-level debugging commands from earlier milestones   |

## Architecture

```
┌────────┐  mDNS advertise  ┌────────┐
│ Peer A │ ◀──────────────▶ │ Peer B │
└────┬───┘                  └────┬───┘
     │    1. HELLO + nonce       │
     │───────────────────────────▶
     │    2. CHALLENGE (HMAC)    │
     │◀───────────────────────────
     │    3. AUTH response       │
     │───────────────────────────▶
     │    4. AUTH_OK             │
     │◀───────────────────────────
     │    5. MANIFEST (both)     │
     │◀─────────────────────────▶│
     │    6. FILE_CHUNK (GCM)    │
     │◀─────────────────────────▶│
     │    7. BYE                 │
     └───────────────────────────┘
```

Every node runs both a server (accepting inbound sync sessions) and a client (initiating outbound ones). There is no central coordinator.

### Package layout

```
app/src/main/java/org/example/app/
├── cli/          picocli commands (init, serve, list-peers, sync, status, …)
├── discovery/    jmDNS service registration + peer registry
├── net/          length-prefixed framed wire protocol (Frame, FrameIO, MessageType)
├── crypto/       PBKDF2 key derivation, AES-256-GCM, HMAC-SHA256
├── manifest/     directory walker, SHA-256 hasher, three-way differ
├── sync/         SyncSession (orchestration), TransferEngine (chunked I/O)
├── util/         Config (~/.lansync/config.json)
└── App.java      entry point
```

### Wire protocol

Every frame: `[4-byte length][1-byte type][payload]`. After the handshake completes, all payloads are AES-256-GCM encrypted.

| Code | Type           | Purpose                                  |
| ---- | -------------- | ---------------------------------------- |
| 0x01 | `HELLO`        | device ID, protocol version, nonce       |
| 0x02 | `CHALLENGE`    | HMAC challenge                           |
| 0x03 | `AUTH`         | HMAC response                            |
| 0x04 | `AUTH_OK`      | session established                      |
| 0x10 | `MANIFEST`     | JSON manifest payload                    |
| 0x20 | `FILE_REQUEST` | request a file by relative path          |
| 0x21 | `FILE_HEADER`  | path, size, IV                           |
| 0x22 | `FILE_CHUNK`   | encrypted chunk (IV ‖ ciphertext ‖ tag)  |
| 0x23 | `FILE_END`     | SHA-256 of plaintext                     |
| 0xF0 | `ERROR`        | error code + message                     |
| 0xFF | `BYE`          | graceful close                           |

## Security model

- **Key derivation**: PBKDF2-HMAC-SHA256, 600k iterations (OWASP 2023+). The salt is derived deterministically from the sorted device-ID pair, so peers arrive at the same key without transmitting salt.
- **Encryption**: AES-256-GCM with a fresh 96-bit random IV per chunk. The 128-bit GCM tag authenticates every chunk; tampering aborts the transfer.
- **Replay protection**: Handshake nonces prevent replaying stale auth sessions.
- **Passphrase**: Never persisted or transmitted. The derived key lives in memory only for the session and is zeroed afterward.
- **Trust boundary**: Assumes a semi-trusted LAN. Provides confidentiality and authenticity against passive and active attackers on the network, but does not defend against a compromised peer device.
- **No TLS**: A custom AEAD layer is used instead of TLS to avoid certificate management on a LAN. See [Future work](#future-work) for pinned-TLS plans.

## Config

`~/.lansync/config.json`:

```json
{
  "deviceId": "a7f3-…-uuid",
  "hostname": "jamals-mac",
  "syncDir":  "/Users/jamal/LANSync",
  "port": 5176,
  "lastSyncAt": "2026-04-18T14:30:00Z"
}
```

## Development

```bash
./gradlew :app:compileJava           # compile
./gradlew :app:test                  # unit + integration tests
./gradlew :app:run --args="status"   # run a command on the JVM
```

Tests:
- **Unit**: crypto round-trip + tamper detection, manifest diff edge cases, frame parser.
- **Integration**: two in-process peers on localhost with separate sync dirs, assert convergence.
- **Cross-platform**: manual smoke tests on macOS, Linux, Windows at each milestone.

## Roadmap

- [x] mDNS device discovery
- [x] TCP transfer + length-prefixed framing
- [ ] PBKDF2 + AES-256-GCM handshake
- [ ] Manifest exchange + three-way diff
- [ ] Full encrypted sync with integrity verification
- [ ] `--mirror` delete semantics
- [ ] Resume / retry on partial transfer

## Future work

- File-watcher mode for continuous sync (`java.nio.file.WatchService`)
- Bandwidth throttling and Zstd compression
- Multi-peer topologies (hub, mesh)
- Trust-on-first-use (TOFU) public-key pinning to replace the shared passphrase
- Optional TLS 1.3 transport with pinned self-signed certificates

## Stack

Java 25 · Gradle · picocli · jmDNS · Gson · JCE (AES-GCM, PBKDF2, HMAC-SHA256) · GraalVM native-image · JUnit 5
