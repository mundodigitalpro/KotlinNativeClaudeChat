# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/nativeMain/kotlin/` (entrypoint `main` in `Main.kt`).
- Tests: `src/nativeTest/kotlin/` (use `kotlin.test`).
- Build config: `build.gradle.kts`, `settings.gradle.kts`, `gradle/` wrapper.
- Docs: `README.md`, provider guides (`CLAUDE.md`, `GEMINI.md`, `QWEN.md`).

## Build, Test, and Development Commands
- Build all: `./gradlew build` — compiles, runs checks, assembles binaries.
- Run (Debug): `./gradlew runDebugExecutableNative` — runs native debug binary.
- Run (Release): `./gradlew runReleaseExecutableNative` — optimized run.
- Link only: `./gradlew linkDebugExecutableNative` — creates the debug executable without running.
- Tests: `./gradlew allTests` — executes native tests (if present).
- Clean: `./gradlew clean` — removes build outputs.
Windows users: use `gradlew.bat` equivalents.

## Coding Style & Naming Conventions
- Kotlin style: 4‑space indent, UTF‑8, trailing newline.
- Naming: `PascalCase` for classes/types, `camelCase` for functions/vars, `SCREAMING_SNAKE_CASE` for constants, `kebab-case` for file names only when required by Gradle; otherwise keep `PascalCase.kt`.
- Comments: prefer KDoc (`/** … */`) for public APIs.
- Formatting: use IntelliJ IDEA “Reformat Code” with Kotlin official style. No ktlint/detekt configured.

## Testing Guidelines
- Framework: `kotlin.test` on Kotlin/Native.
- Location: place tests under `src/nativeTest/kotlin/`.
- Naming: mirror source packages; suffix files/classes with `Test` (e.g., `ConfigLoaderTest.kt`).
- Run locally: `./gradlew allTests`. Aim to test config parsing, API client behavior (mocked), and CLI flows.

## Commit & Pull Request Guidelines
- Commits: use Conventional Commits (e.g., `feat: add OpenRouter browser`, `fix: handle API errors`). Keep messages in imperative mood; scope optional.
- PRs: include summary, rationale, screenshots/CLI output when UI/CLI behavior changes, and linked issues (e.g., `Closes #123`). Keep diffs focused; update docs when flags/commands change.

## Security & Configuration Tips
- API keys: never commit secrets. Provide keys at runtime via env vars or `config.json` loaded by the app.
- Platform targets: Gradle auto‑selects the correct Kotlin/Native target (macOS, Linux, Windows). Verify runs with the “Run (Debug)” command above for your OS.

