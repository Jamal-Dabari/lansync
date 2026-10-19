# TODO: AI-era roadmap

lansync's edge is private, local-only file movement. The goal is to make it a
tool AI agents can drive and a reliable way to move large AI artifacts, without
embedding an LLM in the binary.

Suggested order: 1 → 2 → 3 → 4 → 5. Items 1–2 make agents usable; 3–5 make
the data safe to trust.

## 1. Machine-readable output

- [ ] Add `--json` to `list-peers`, `status`, `sync --dry-run`, `interfaces`
- [ ] Define stable, documented exit codes (no peer found, auth failed, conflict, partial transfer, …)
- [ ] Make every command fully non-interactive when `LANSYNC_PASSPHRASE` is set

## 2. MCP server

- [ ] Add a `lansync mcp` command that speaks MCP over stdio
- [ ] Tools: `list_peers`, `status`, `diff` (dry run), `sync`, `fetch_file`
- [ ] Document setup for Claude Code and other MCP clients in the README

## 3. Large-file support (models, datasets, checkpoints)

- [ ] Resumable transfers: keep partial tmp files and resume from the last verified chunk
- [ ] Content-defined chunking so a small change doesn't resend a whole multi-GB file
      (`Differ` currently compares whole-file SHA-256)
- [ ] `lansync init --preset models` for Ollama / HuggingFace / LM Studio cache dirs

## 4. Safe concurrent edits

- [ ] Conflict detection: when both sides changed a path since the last sync, stop
      overwriting silently (today the initiator wins, see `Differ.java`). Keep both
      copies as `file.conflict-<peer>` or track a common ancestor
- [ ] Sync journal + `lansync undo` to roll back the last sync
- [ ] `.lansyncignore` and protected read-only paths (`.git`, `.env`, keys)

## 5. Later: share AI work across machines

- [ ] Peers advertise which models/datasets they hold; others fetch on demand
- [ ] Send a job's inputs to the GPU machine and sync the outputs back

## Non-goals

- Built-in AI features ("summarize changes", "AI picks the winner"). These need a
  cloud API (breaks the privacy pitch) or a bundled local model (breaks the
  single-binary goal). Agents get this for free via `--json` and MCP.
