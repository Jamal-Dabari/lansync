# LANSync: Uses and potential

This document describes what LANSync is useful for today, how it could be extended in general, and how it could be used by AI agents working across several machines.

## 1. What it's for today

### What it does

- **Finds your other machines on its own.** Run `lansync serve` on each machine and `lansync list-peers` shows them. No IP addresses to configure. On networks that block discovery, give it an IP directly (`lansync sync 192.168.1.42`).
- **Only sends changed files.** `lansync sync <peer>` compares the two folders by content and moves only new or modified files. Each file is re-checked after it arrives.
- **Private transfers.** Both machines need the same passphrase. Everything sent is encrypted. Peers cannot write outside the sync folder.
- **Runs on Mac, Windows and Linux.** Use it through Java, or as a single native binary built with GraalVM.

### Good uses

- Keeping a laptop and desktop in step (documents, notes, project folders) without Dropbox or iCloud.
- Moving large files (photos, video, datasets) between home machines at LAN speed.
- Backing up to a NAS, Raspberry Pi or home server.
  - `LANSYNC_PASSPHRASE` allows unattended runs from cron.
  - `--dry-run` previews what would change.
  - `--mirror` makes the other side an exact copy.
- Syncing between Windows and Mac/Linux, where rsync is awkward.
- Learning and portfolio work: service discovery, a framed wire protocol, an encrypted channel and content diffing.

### Current limits

- **No automatic syncing.** `lansync watch` only prints "not yet implemented".
- **Edit conflicts are resolved silently.** The machine you sync from wins and overwrites the other (`manifest/Differ.java`).
- **Deletions don't carry over without `--mirror`.** A deleted file gets copied back.
- **Changed files are sent whole.** There is no rsync-style partial transfer.
- **Same network only.** It also assumes peers aren't compromised.

### Summary

LANSync is good for one person keeping their own machines in sync at home. It is not ready for several people editing the same files.

## 2. General extensions (most useful first)

### 2.1 Sync correctness (do first)

- **Track deletions and detect conflicts.** Keep a record of each machine's state after the last sync. That lets the tool tell "deleted here" from "never had it", and "changed on one side" from "changed on both". On a two-sided change, keep both copies (e.g. `report (conflict from laptop).md`). This mostly touches `manifest/Differ.java` plus a small state file under `~/.lansync/`. It is what makes the tool trustworthy.
- **Stop rehashing every file on every sync.** `ManifestBuilder.build` hashes every file on each run. Cache each file's hash and reuse it when size and modified time (mtime) are unchanged. The mtime is already recorded but unused. This makes large-folder syncs near-instant, and watch mode needs it.
- **Resume interrupted transfers** (on the roadmap). Add an offset to the existing file-request message and resume from the partial temp file.

### 2.2 Day-to-day usability

- **Finish `lansync watch`.** Watch the folder, wait for bursts of edits to settle, then sync. `serve` notifies peers on change. This depends on the hash cache.
- **`.lansyncignore` with gitignore syntax** (`node_modules/`, `*.tmp`, `.git/`). Today only temp files and `.DS_Store` are skipped (`isTransient`).
- **Run as a background service** with launchd, systemd and Windows service configs.
- **Progress display and parallel transfers** over a few connections, for folders with many small files.

### 2.3 Performance

- **Send only changed parts of big files, like rsync.** Only `TransferEngine`'s block-transfer code changes.
- **Zstd compression** for text-heavy folders (skipping already-compressed files), and **a bandwidth limit option**. Both are on the roadmap.

### 2.4 Security

- **Replace the shared passphrase with device pairing.** Each machine has its own key pair. You pair once by confirming a short code on both screens. This lets you revoke one device without changing the passphrase everywhere.
- **Use standard TLS 1.3 with those pinned keys** instead of the custom encryption layer. This means less hand-written crypto.

### 2.5 Bigger directions

- **More than two machines**, either as a hub (sync through a NAS) or a mesh. Conflict tracking is a prerequisite.
- **Multiple named sync folders**, each shared with a different set of machines (e.g. `photos` to the NAS only).
- **File version history.** Keep the last N versions of overwritten or deleted files in `.lansync/trash/`.
- **Desktop UI.** A tray app or local web dashboard showing peers, status and conflicts.
- **Android client**, since the code is Java.

### Suggested order

1. Hash cache
2. Deletion and conflict tracking
3. `.lansyncignore`
4. `watch`
5. Pairing

## 3. Agent use across machines

There are three ways agents could use LANSync, from the smallest change to the largest:

1. Agents using it as a tool.
2. Agents sharing a workspace through it.
3. Agents talking to each other over it.

### 3.1 Let an agent run LANSync (start here)

- Add a `--json` flag to `status`, `list-peers` and `sync --dry-run`. This is a small change in `cli/`.
- Add a `lansync mcp` server mode. MCP (Model Context Protocol) is the standard way agents such as Claude Code discover and call outside tools. The tools would be `list_peers`, `status`, `preview_sync` and `sync`. They wrap existing code (`SyncSession`, `DeviceDiscovery`).
- Example: tell a laptop agent to send a build to the desktop, or to fetch yesterday's results from the server.

### 3.2 Agents on several machines sharing one folder

- **Conflicts can't be silent.** This needs the deletion and conflict tracking from section 2. Both copies are kept and the conflict is reported to the agent, which merges them.
- **File claims.** An agent claims a path for a set time (e.g. `src/api/` for 10 minutes) and others avoid it. The claim expires automatically if the agent crashes. This needs a small new message type on the existing encrypted channel.
- **Change notifications instead of polling.** Use an extended `watch` in which `serve` tells peers which files changed. An agent can then wait for "the other machine finished writing `results/`".
- **Ignore rules are required** (caches, logs, `node_modules/`).

### 3.3 Agents sending work and messages to each other

Example: a laptop agent hands a heavy job to the GPU machine and collects the output.

- **Message channel.** Add message types for sending a task, reporting progress and returning a result, reusing the framing and encryption in `net/`. Files go over sync; messages say what to do with them.
- **Remote work is the risky part.** Today anyone with the passphrase has full access. Required first:
  - Per-device identity (pairing) with per-device permissions ("laptop may send tasks, NAS may only store files") and individual revocation.
  - A fixed allowlist of tasks on the receiving machine (e.g. `make test` in a folder), never arbitrary commands.
  - A log of who requested what and when.
- **Treat synced files as untrusted input.** A file from another machine may contain text meant to trick the reading agent (prompt injection). Treat it as data, not instructions, especially if it can lead to running something.

### 3.4 Syncing agent settings between machines

Keep skills, project instructions and memory files consistent across machines. Credential and token files must be excluded, so this depends on ignore rules.

### Recommended order

1. `--json`, then the MCP server. Useful immediately, with no protocol changes.
2. Conflict detection and ignore rules.
3. Change notifications and file claims.
4. Per-device identity, and only then remote tasks. Doing remote tasks without per-device identity would let any device holding the passphrase run tasks on all your machines.

## Combined roadmap

This merges the two recommended orders above into one list.

1. Hash cache (section 2).
2. `--json` output, then the `lansync mcp` server (section 3).
3. Deletion and conflict tracking (shared by both sections).
4. `.lansyncignore` (shared by both sections).
5. `watch`, with change notifications and file claims for agents.
6. Device pairing, i.e. per-device identity.
7. Remote tasks for agents, only after per-device identity is in place.
