# Sakura LSL — IntelliJ Platform Plugin

[![CI](https://github.com/Sakura-Studios-IKE/sakura-intellij-lsl/actions/workflows/ci.yml/badge.svg)](https://github.com/Sakura-Studios-IKE/sakura-intellij-lsl/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/Sakura-Studios-IKE/sakura-intellij-lsl)](https://github.com/Sakura-Studios-IKE/sakura-intellij-lsl/releases)

A one-stop IDE experience for **Linden Scripting Language** (LSL) developers
using the Sakura toolchain (`sakura-lslc`, `sakura-slemu`, `sakura-lsltest`).

## Features

### Editor

- Recognises `.lsl` and `.lslh` files with a sakura icon.
- Hand-rolled lexer with keyword, builtin, constant, comment and string
  highlighting.
- **Smart completion** of LSL built-in functions, constants, types,
  keywords and event handlers, populated from the same builtin table
  the compiler uses.
- **Parameter info popups** (Ctrl+P) that show the function signature
  and highlight the current argument as you type.
- **Hover documentation** for every built-in function, constant, and
  event — including Mono-only badges where applicable.
- **Live error checking** via an `ExternalAnnotator` that runs
  `sakura-lslc` in the background and surfaces gcc-style diagnostics
  inline.
- Brace matcher, line/block commenter (`//` and `/* */`).

### Run, debug, test

- **Run current file** (`Ctrl+Alt+L`) — no run configuration needed.
- **LSL: run in slemu** rich run configuration UI:
  script picker, volume directory, owner/avatar fixtures, HTTP mode
  (real vs fixture), tracing, max steps, wall-clock timeout, scripted
  commands file or inline events, extra slemu args.
- **LSL: lsltest run** configuration for running a directory of
  `test_*.py` files through the `lsltest` harness.
- **Sakura LSL tool window** with two tabs:
  - **Emulator** — keep an interactive `slemu` session attached and
    send commands to it from a panel.
  - **Debugger** — graphical front-end to `lsldb`: breakpoints,
    step/continue, current-line view, locals, catchpoints.
- Status-bar compile light (green / yellow / red) — click to jump to
  the first diagnostic.

### Project wizards

- **Just LSL** — scripts/, tests/, ready to compile and emulate.
- **Full Stack LSL** — same plus a FastAPI + Alembic + PostgreSQL
  backend, a React (Vite) frontend, and a `docker-compose.yml` to spin
  them up locally.

### Second Life integration

- **Hot Reload to Second Life** — write the current script to a
  Firestorm external-editor watch directory.
- **Copy LSL Script to Clipboard** — fallback for pasting into the
  in-world script editor.

See [`CHANGELOG.md`](./CHANGELOG.md) for the full version history.

## Building

You need JDK 17 and Gradle 9.0 (the wrapper in this repository will
fetch it on first run). The plugin builds against the IntelliJ Platform
2025.3.5 SDK by default; set `SAKURA_LSL_IDE=/path/to/your/IDEA` to
build against your actually-installed IDE instead.

```bash
./gradlew buildPlugin
```

The plugin distribution is written to:

```
build/distributions/sakura-lsl-1.0.0.zip
```

## Installing

### From the JetBrains Marketplace (recommended)

1. In IntelliJ: **Settings → Plugins → Marketplace**.
2. Search for **Sakura LSL**.
3. Click **Install** and restart the IDE.

### From a zip

1. Open IntelliJ IDEA (2024.1 or later — verified through 2026.x).
2. **Settings → Plugins → ⚙ (gear icon) → Install Plugin from Disk…**
3. Pick `build/distributions/sakura-lsl-1.0.0.zip`.
4. Restart the IDE when prompted.

> If you previously installed `io.github.riej.lsl`, you can safely
> uninstall it — Sakura LSL is a fully standalone plugin with its own
> file type, lexer, completions, inspections, run configs, debugger
> tool window, and project wizards.

## Configuring

Open **Settings → Tools → Sakura LSL** and point the plugin at your binaries:

| Setting | Default | What it is |
| --- | --- | --- |
| `lslc path` | `lslc` | The Sakura LSL compiler |
| `slemu path` | `slemu` | The Sakura SL emulator |
| `lsltest path` | `lsltest` | The Sakura LSL test runner |
| `Firestorm watch directory` | *(empty)* | Where Firestorm's external editor watches for changes |
| `Extra lslc flags` | *(empty)* | Additional CLI flags passed to `lslc` on every run |

## Hot reload to Second Life

Second Life **does not expose a public upload API**, so the plugin cannot
push scripts directly into a region. Instead, the **Hot Reload to Second
Life** action uses the *Firestorm external editor watch* pattern:

1. In Firestorm: **Preferences → Network & Files → External editor → enable
   the external-editor watch**, and tell Firestorm to watch the directory
   you chose in Sakura LSL's settings.
2. In Second Life: open the in-world script you want to live-edit. Firestorm
   creates a temp file in the watch directory and reloads the script
   whenever the file changes.
3. In IntelliJ: invoke **Tools → Hot Reload to Second Life**
   (or `Ctrl+Alt+R`). The plugin copies your `.lsl` file into the watch
   directory; Firestorm picks it up and pushes the change in-world.

If you don't run Firestorm, use **Tools → Copy LSL Script to Clipboard** and
paste manually into the SL script editor.

## Caveats

- The plugin requires that `sakura-lslc` be on your `PATH` or that you set
  the full path in settings.
- The annotator runs `lslc` on a temp copy of the buffer for every
  pause-on-typing event. Heavy `-D` macros or very large `#include`s can
  slow this down.
- The `Hot Reload` action depends on Firestorm being already running and
  the script's edit dialog being open. The plugin cannot authenticate
  against the SL grid for you.

## Companion projects

`sakura-intellij-lsl` is the IDE front-end in Sakura Studios' five-tool
open-source LSL toolchain:

* [`sakura-lslc`](https://github.com/ShihoSakura/sakura-lslc) — compiler
* [`sakura-slemu`](https://github.com/ShihoSakura/sakura-slemu) — emulator
* [`sakura-lsldb`](https://github.com/ShihoSakura/sakura-lsldb) — debugger
* [`sakura-lsltest`](https://github.com/ShihoSakura/sakura-lsltest) — test framework

## License

MIT — see `LICENSE`.
