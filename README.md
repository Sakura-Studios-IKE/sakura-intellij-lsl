# Sakura LSL — IntelliJ Platform Plugin

A one-stop IDE experience for **Linden Scripting Language** (LSL) developers
using the Sakura toolchain (`sakura-lslc`, `sakura-slemu`, `sakura-lsltest`).

## Features

- Recognises `.lsl` and `.lslh` files with a sakura icon.
- Hand-rolled lexer with keyword, builtin, constant, comment and string
  highlighting.
- Smart completion of LSL built-in functions, constants, types, keywords and
  event handlers.
- Live error checking via an `ExternalAnnotator` that runs `sakura-lslc`
  in the background and surfaces gcc-style diagnostics inline.
- Two run configuration types:
  - **LSL: run in slemu** — compile with `lslc` and run the result in the
    Sakura emulator.
  - **LSL: lsltest run** — execute a directory of `test_*.py` files through
    the `lsltest` harness.
- **Hot Reload to Second Life** — write the current script to a Firestorm
  external-editor watch directory.
- **Copy LSL Script to Clipboard** — fallback for pasting into the in-world
  script editor.
- Status-bar compile light (green / yellow / red) — click to jump to the
  first diagnostic.

## Building

You need JDK 17 and Gradle 8.7 (or run `gradle wrapper --gradle-version 8.7`
once to materialise the wrapper that ships in this repository as a
placeholder).

```bash
./gradlew buildPlugin
```

The plugin distribution is written to:

```
build/distributions/sakura-lsl-1.0.0.zip
```

## Installing

1. Open IntelliJ IDEA (Community 2024.1 or later).
2. **Settings → Plugins → ⚙ (gear icon) → Install Plugin from Disk…**
3. Pick `build/distributions/sakura-lsl-1.0.0.zip`.
4. Restart the IDE when prompted.

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
