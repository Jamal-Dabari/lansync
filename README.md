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

- Java 25+. If no JDK 25 is installed, Gradle provisions one via the foojay
  resolver, so `./gradlew` works on a fresh clone with no local setup.
- A LAN that allows mDNS
- For native builds only: Oracle GraalVM for JDK 25. The `graalvmNative` block
  in `app/build.gradle.kts` selects it by vendor; change that vendor if you use
  a different GraalVM distribution. Nothing else in the build needs it.

## Build

```bash
./gradlew :app:installDist           # launcher at app/build/install/lansync/bin/lansync
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
lansync sync 192.168.1.42            # or address a peer directly, skipping mDNS
lansync status                       # last sync time + manifest stats
```

## Commands

| Command                      | Description                                              |
| ---------------------------- | -------------------------------------------------------- |
| `init --dir <path>`          | Initialize sync directory and generate device ID         |
| `serve`                      | Start the TCP listener and register the mDNS service     |
| `list-peers [--timeout N]`   | Show peers discovered on the LAN                         |
| `interfaces`                 | List network interfaces mDNS can bind to                 |
| `config [--dir\|--port\|--bind\|--name]` | Show or change stored settings              |
| `sync <peer> [flags]`        | Perform a delta sync with a peer                         |
| `status`                     | Show device ID, sync dir, last sync time, file counts    |
| `discover` / `listen` / `send` | Low-level debugging commands from earlier milestones   |

`<peer>` accepts a full device ID, a device-ID prefix (4+ chars), a hostname, or a
literal IP address. An IP is dialed directly and skips mDNS entirely, which is the
reliable path on networks that filter multicast.

## Choosing a network interface

A typical laptop has several interfaces: the real LAN adapter plus VPN tunnels,
container bridges and, on macOS, Apple's `awdl0` / `llw0` peer-to-peer links.
Multicast sent on the wrong one never reaches the LAN, so peers silently fail to
find each other.

lansync auto-selects, preferring a non-loopback, multicast-capable, site-local
interface and ranking known-virtual ones last. When the guess is wrong, pin it:

```bash
lansync interfaces                   # see what is available and what was chosen
lansync config --bind en0            # pin by interface name
lansync config --bind 192.168.1.10   # or by address
lansync config --bind auto           # back to auto-selection
lansync serve --bind en0             # or override for a single run
```

Resolution order is `--bind` flag, then `$LANSYNC_BIND`, then the stored
`bindAddress`, then auto-selection. Naming an interface that does not exist fails
immediately with the list of valid options rather than at the next sync.

Only mDNS is bound to a specific interface. The TCP listener always accepts on all
interfaces, so a peer can reach you by direct IP even when discovery is misconfigured
— and if mDNS registration fails, `serve` says so and keeps listening rather than
exiting.

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
├── net/          framed wire protocol (Frame, FrameIO, MessageType, SecureChannel)
│                 plus the message bodies (HelloMessage, AuthMessage, …)
├── crypto/       PBKDF2 key derivation, AES-256-GCM, HMAC-SHA256
├── manifest/     directory walker, SHA-256 hasher, content differ
├── sync/         SyncSession (orchestration), TransferEngine (chunked I/O)
├── util/         Config, passphrase prompt, interface picker, path guard
└── App.java      entry point
```

### Wire protocol

Every frame: `[4-byte length][1-byte type][payload]`. The four handshake frames are plaintext; every frame after `AUTH_OK` is sealed as `[12-byte IV][ciphertext][16-byte GCM tag]`, with the frame's type byte bound in as AAD so a frame cannot be replayed under a different type.

The client drives the conversation. It computes the diff and issues pushes (`FILE_HEADER` + chunks), pulls (`FILE_REQUEST`, answered with `FILE_HEADER` + chunks) and deletes; the server is a pure responder that loops until it receives `BYE`.

| Code | Type           | Purpose                                  |
| ---- | -------------- | ---------------------------------------- |
| 0x01 | `HELLO`        | device ID, protocol version, nonce       |
| 0x02 | `CHALLENGE`    | HMAC challenge                           |
| 0x03 | `AUTH`         | HMAC response                            |
| 0x04 | `AUTH_OK`      | session established                      |
| 0x10 | `MANIFEST`     | JSON manifest payload                    |
| 0x20 | `FILE_REQUEST` | request a file by relative path          |
| 0x21 | `FILE_HEADER`  | path, size, expected SHA-256             |
| 0x22 | `FILE_CHUNK`   | up to 64 KiB of file data                |
| 0x23 | `FILE_END`     | end of chunk stream for the current file |
| 0x24 | `FILE_DELETE`  | remove a path (mirror mode)              |
| 0xF0 | `ERROR`        | error code + message                     |
| 0xFF | `BYE`          | graceful close                           |

## Security model

- **Key derivation**: PBKDF2-HMAC-SHA256, 600k iterations (OWASP 2023+). The salt is derived deterministically from the sorted device-ID pair, so peers arrive at the same key without transmitting salt.
- **Encryption**: AES-256-GCM with a fresh 96-bit random IV per chunk. The 128-bit GCM tag authenticates every chunk; tampering aborts the transfer.
- **Replay protection**: Handshake nonces prevent replaying stale auth sessions.
- **Passphrase**: Never persisted or transmitted. It is read from the terminal (or `LANSYNC_PASSPHRASE` for headless runs), held in a `char[]`, and zeroed by the key derivation that consumes it.
- **Path traversal**: Every peer-supplied path is normalized and checked to stay inside the sync root, so a hostile peer cannot write outside it.
- **Trust boundary**: Assumes a semi-trusted LAN. Provides confidentiality and authenticity against passive and active attackers on the network, but does not defend against a compromised peer device.
- **No TLS**: A custom AEAD layer is used instead of TLS to avoid certificate management on a LAN. See [Future work](#future-work) for pinned-TLS plans.

## Config

`~/.lansync/config.json`:

```json
{
  "deviceId": "a7f3-…-uuid",
  "hostname": "laptop",
  "syncDir":  "/home/you/LANSync",
  "port": 5176,
  "bindAddress": "en0",
  "lastSyncAt": "2026-04-18T14:30:00Z"
}
```

Edit it with `lansync config` rather than by hand. `bindAddress` is optional; when
absent, the interface is auto-selected.

### Environment variables

| Variable              | Purpose                                                     |
| --------------------- | ----------------------------------------------------------- |
| `LANSYNC_PASSPHRASE`  | Supplies the passphrase for headless runs (daemons, CI)      |
| `LANSYNC_BIND`        | Overrides the mDNS interface without editing the config file |

## Development

```bash
./gradlew :app:compileJava           # compile
./gradlew :app:test                  # unit + integration tests
./gradlew :app:javadoc               # API docs at app/build/docs/javadoc (builds warning-free)
./gradlew :app:run --args="status"   # run a command on the JVM
```

Tests:
- **Unit**: `CryptoTest` (GCM round-trip, tamper and AAD-mismatch detection), `DifferTest` (diff edge cases), `NetworkPickerTest` (interface ranking and bind resolution).
- **Integration**: `SyncSessionIT` runs two in-process peers over loopback with separate sync dirs and asserts convergence, multi-chunk transfer, mirror deletes, and handshake rejection on a passphrase mismatch.
- **Cross-platform**: manual smoke tests on macOS, Linux, Windows at each milestone.

### Conflict semantics

The diff is content-based (SHA-256), not timestamp-based. When both peers hold the
same path with different content, **the initiating side wins** — `sync` pushes its
copy over the peer's. Files present only on one side are pulled or pushed; with
`--mirror`, remote-only files are deleted instead of pulled. Run `--dry-run` first
if you are not sure which way a file will move.

## Roadmap

- [x] mDNS device discovery
- [x] TCP transfer + length-prefixed framing
- [x] PBKDF2 + AES-256-GCM handshake
- [x] Manifest exchange + diff
- [x] Full encrypted sync with integrity verification
- [x] `--mirror` delete semantics
- [x] Configurable mDNS interface selection
- [ ] Resume / retry on partial transfer
- [ ] File-watcher mode (`lansync watch` is still a stub)

## Future work

- File-watcher mode for continuous sync (`java.nio.file.WatchService`)
- Bandwidth throttling and Zstd compression
- Multi-peer topologies (hub, mesh)
- Trust-on-first-use (TOFU) public-key pinning to replace the shared passphrase
- Optional TLS 1.3 transport with pinned self-signed certificates

## Stack

Java 25 · Gradle · picocli · jmDNS · Gson · JCE (AES-GCM, PBKDF2, HMAC-SHA256) · GraalVM native-image · JUnit 5
