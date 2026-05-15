# Sakura LSL — Reference Documentation

This document covers every feature of the Sakura LSL IntelliJ plugin in
depth. The user-facing intro is in `README.md`; this file is the
implementation and behaviour reference.

---

## 1. File-type recognition

- **Class**: `com.sakurastudios.lsl.LslFileType`
- **Language**: `com.sakurastudios.lsl.LslLanguage` (`Language("LSL")`)
- **Extensions**: `.lsl`, `.lslh`
- **Icon**: `icons/lsl.svg`, a small 16×16 pink sakura petal vector.

The file type is registered via `<fileType>` in `plugin.xml`. The language
is case-sensitive (`isCaseSensitive() = true`).

## 2. Syntax highlighting

- **Lexer**: `LslLexer`, a single-state hand-rolled `LexerBase`. It produces
  the token kinds enumerated in `LslTokenTypes`:

  - `WHITESPACE`, `NEWLINE`
  - `LINE_COMMENT` (`//…`, plus `#…` at line-start as a primitive
    pre-processor pass-through)
  - `BLOCK_COMMENT` (`/* … */`)
  - `IDENTIFIER`, `LL_IDENTIFIER` (anything matching `^ll[A-Z]…`),
    `KEYWORD`, `TYPE_KEYWORD`, `EVENT_KEYWORD`, `CONSTANT`
  - `INTEGER_LITERAL`, `FLOAT_LITERAL`, `STRING_LITERAL`
  - punctuation: `LBRACE`, `RBRACE`, `LPAREN`, `RPAREN`, `LBRACKET`,
    `RBRACKET`, `SEMI`, `COMMA`, `DOT`, `AT`, `OPERATOR`
  - `BAD_CHARACTER` for unrecognised input.

- **Highlighter**: `LslSyntaxHighlighter`, registered via
  `LslSyntaxHighlighterFactory`. Token kinds are mapped to
  `TextAttributesKey`s that extend the platform defaults
  (`DefaultLanguageHighlighterColors.*`), so the user's theme drives the
  actual colours.

### Limitations

The lexer does **not** build a PSI tree. Refactoring, find-usages, and
go-to-definition therefore are not provided in 1.0.0. The compile-time
checks come from `lslc` via the external annotator (§4), not from a parser
in the plugin.

## 3. Completion

- **Class**: `LslCompletionContributor`
- **Triggered on**: every position inside an LSL file (`PlatformPatterns
  .psiElement().withLanguage(LslLanguage)`).
- **Data source**: `LslBuiltins` — a Kotlin object holding ~60 built-in
  function names, ~40 constants, all event handlers, the type keywords, and
  the control-flow keywords.

Functions are completed with a trailing `()` and the caret moved between
them. Constants are bold-faced. The `withTypeText` slot displays whether a
candidate is a function, constant, event, type or keyword.

Lookup priorities (higher number = higher in the list):

| Kind | Priority |
| --- | ---: |
| `ll*` built-in function | 80 |
| ALL_CAPS constant | 60 |
| event handler | 50 |
| type keyword | 40 |
| control-flow keyword | 30 |

## 4. External annotator (live `lslc`)

- **Class**: `LslAnnotator`
- **Hook**: `<externalAnnotator language="LSL">`

### Lifecycle

1. `collectInformation(file, editor, hasErrors)` snapshots the file path,
   project, and editor text into a `CompileInfo` record. This runs on the
   EDT — no process is launched here.
2. `doAnnotate(info)` runs on a background thread. It writes the snapshot
   to a temp file (`sakura-lsl-XXXXX.lsl`), invokes:

   ```
   <lslcPath> --fno-color -Wall <extraLslcFlags> <tmpfile>
   ```

   waits up to 10 seconds, and parses `stderr` (which is merged with
   `stdout` via `redirectErrorStream(true)`) for gcc-style diagnostics of
   the form:

   ```
   path:line:col: severity: message
   ```

   Severities are mapped:

   | gcc word | IntelliJ `HighlightSeverity` |
   | --- | --- |
   | `error`, `fatal error` | `ERROR` |
   | `warning` | `WARNING` |
   | `note` | `WEAK_WARNING` |
   | `info` | `INFORMATION` |

3. `apply(file, info, holder)` runs on the EDT again. Each diagnostic is
   converted to a `TextRange` covering at least one character on the
   reported line and posted to the `AnnotationHolder`. If `lslc` couldn't
   be invoked at all (binary missing, permission denied, etc.) a single
   `WEAK_WARNING` is posted at offset 0 explaining the problem.

4. Finally, `LslCompileResultBus.update(project, result)` is called so the
   status-bar widget can refresh.

### Notes

- Diagnostics whose embedded path does not match the temp file are skipped
  — this prevents diagnostics from `#included` files from sticking to the
  wrong document.
- The annotator does not block on save; it kicks in whenever IntelliJ
  decides the editor is idle.

## 5. Run configurations

### 5.1 `LSL: run in slemu`

- **Type ID**: `SAKURA_LSL_SLEMU_RUN_CONFIG`
- **Class**: `LslRunConfiguration`, factory `LslRunConfigurationFactory`,
  type `LslRunConfigurationType`.

Fields:

| Field | Description |
| --- | --- |
| `scriptPath` | The `.lsl` file to compile + run |
| `slemuArgs`  | Whitespace-separated extra args to `slemu` |

Execution (`SlemuRunState.startProcess`):

1. Run `lslc --fno-color <extra-flags> <script>` synchronously. If it
   exits non-zero, the run fails with an `ExecutionException` containing
   the combined stdout/stderr.
2. Spawn `slemu <script> <slemuArgs>` via `OSProcessHandler` and stream the
   output to the run-tool-window console.

### 5.2 `LSL: lsltest run`

- **Type ID**: `SAKURA_LSL_LSLTEST_RUN_CONFIG`
- **Class**: `LsltestRunConfiguration`.

Fields:

| Field | Description |
| --- | --- |
| `testDir`   | Directory containing `test_*.py` files |
| `extraArgs` | Whitespace-separated extra args to `lsltest` |

Spawns `lsltest run <testDir> <extraArgs>`.

## 6. Settings

- **Class**: `LslcSettings` — `@State`-annotated, application-level
  `PersistentStateComponent`, stored in `sakura-lsl.xml`.
- **UI**: `LslcConfigurable` under **Tools → Sakura LSL**.

Persisted fields with their defaults:

| Field | Default |
| --- | --- |
| `lslcPath` | `lslc` |
| `slemuPath` | `slemu` |
| `lslTestPath` | `lsltest` |
| `firestormWatchDir` | *(empty)* |
| `extraLslcFlags` | *(empty)* |

## 7. Hot reload to Second Life

- **Action ID**: `Sakura.LSL.HotReload`
- **Class**: `LslHotReloadAction`
- **Default shortcut**: `Ctrl+Alt+R`
- **Visibility**: Only enabled when the focused virtual file has extension
  `lsl` or `lslh`.

Operation:

1. Save all documents (so the on-disk file matches the editor buffer).
2. If `firestormWatchDir` is unset, show a warning balloon and abort.
3. Else copy the source file to `firestormWatchDir/<filename>`.
4. Show an info balloon with the destination path.

### Why the Firestorm watch pattern?

Second Life has no public upload API for scripts. The Firestorm viewer
implements an **external editor watch**: it writes the in-world script to a
temp file, registers a watcher, and re-uploads the file whenever it
changes. The plugin therefore can only act as the *editor side* of the
watch — it cannot push to SL without a logged-in viewer.

### Clipboard fallback

- **Action ID**: `Sakura.LSL.CopyToClipboard`
- **Class**: `LslCopyToClipboardAction`

Saves the document, then copies its text to the system clipboard so the
user can paste it into the SL script editor by hand.

## 8. Status-bar widget

- **Class**: `LslStatusBarWidget`
- **Factory**: `LslStatusBarWidgetFactory`
- **Widget ID**: `SakuraLsl.CompileStatus`

A small filled-circle icon next to the other status-bar widgets:

| Colour | Meaning |
| --- | --- |
| Grey | No compile run yet (project just opened) |
| Green | Last compile clean |
| Yellow | Compile produced warnings |
| Red | Compile produced errors |

The widget polls `LslCompileResultBus` every 500 ms and only invalidates
itself when the status changes. Clicking it jumps to the first diagnostic
of the most recent run via
`OpenFileDescriptor(project, file, line, column).navigate(true)`.

## 9. Architecture diagram

```
   editor buffer
        │
        ▼
   LslAnnotator ──► lslc subprocess ──► gcc-style stderr
        │                                    │
        │                                    ▼
        │                          parse → List<LslDiagnostic>
        │                                    │
        ▼                                    ▼
   AnnotationHolder ◄─────────── LslCompileResultBus
                                             │
                                             ▼
                                 LslStatusBarWidget (poll)
```

## 10. Threading

- `collectInformation` and `apply` run on the EDT.
- `doAnnotate` runs on a daemon thread; subprocess I/O is blocking but the
  thread is owned by the platform.
- The status-bar widget's poll runs on the Swing thread (`Alarm
  .ThreadToUse.SWING_THREAD`).
- Actions run in `ActionUpdateThread.BGT` (background) for `update()`.

## 11. Limitations & future work

- No PSI parser → no refactoring, no go-to-definition, no rename.
- Built-in catalogue is a curated subset; full ~430-function table will be
  bundled as `builtins.txt` and lazy-loaded.
- The compile light is project-wide, not per-file; opening another LSL file
  doesn't immediately reset the light until the annotator next runs.
- The `LSL: run in slemu` config doesn't yet pretty-print JSON event lines
  with colour — they go straight to the console via the standard text
  builder.
