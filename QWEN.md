# QWEN.md

## Project Overview

This project is a Kotlin/Native command-line chat application that serves as a unified client for multiple AI model providers, specifically Anthropic Claude and OpenRouter. Its key feature is cross-platform compatibility, compiling to native executables for macOS, Windows, and Linux from a single codebase.

### Key Technologies

*   **Language**: Kotlin/Native
*   **Build System**: Gradle (Kotlin DSL - `build.gradle.kts`)
*   **HTTP Client**: Ktor (with platform-specific engines: Darwin for macOS, WinHttp for Windows, CIO for Linux)
*   **JSON Serialization**: kotlinx.serialization
*   **File I/O**: Okio

### Core Features

*   **Multi-Provider Support**: Direct integration with Anthropic Claude and access to 400+ models via OpenRouter.
*   **Cross-Platform**: Single codebase that compiles to optimized native executables for macOS, Windows, and Linux.
*   **Interactive UI**: Terminal-based interface with an interactive navigation system using arrow keys.
*   **Configuration Management**: Loads and saves API keys, model choices, and provider settings to `config.json`.
*   **Enhanced Chat**: Special commands (`/menu`, `/exit`, `/help`) for navigation and control. Supports models with reasoning capabilities.
*   **Model Browser**: For OpenRouter, provides a real-time list of available models with search and filtering.

## Project Structure

```
.
├── src/
│   ├── nativeMain/
│   │   └── kotlin/
│   │       └── Main.kt              # Contains all application logic
│   └── nativeTest/
│       └── kotlin/
│           └── Test.kt              # (Currently empty) Placeholder for tests
├── build.gradle.kts                 # Build configuration and dependencies
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Gradle properties
├── gradlew / gradlew.bat            # Gradle wrapper scripts
├── README.md                        # Primary documentation
├── CLAUDE.md                        # Instructions for Claude Code
├── GEMINI.md                        # Technical overview for Google Gemini models
├── IMPROVEMENT_PLAN.md              # Refactoring and enhancement plan
├── NUEVAS_CARACTERISTICAS.md        # Proposal for new features (in Spanish)
├── config.json                      # (Generated) User configuration file
├── QWEN.md                          # This file
└── ...
```

## Building and Running

The project uses the Gradle wrapper for building and running.

### Prerequisites

*   Kotlin/Native toolchain (usually included with a Kotlin installation or acquired via Gradle).
*   A terminal or command prompt.

### Commands

*   **Build the application (current platform)**:
    ```bash
    ./gradlew build
    ```
*   **Run the application (debug mode)**:
    ```bash
    ./gradlew runDebugExecutableNative
    ```
*   **Run the application (release mode)**:
    ```bash
    ./gradlew runReleaseExecutableNative
    ```
*   **Clean build artifacts**:
    ```bash
    ./gradlew clean
    ```
*   **Run tests (if any were implemented)**:
    ```bash
    ./gradlew allTests
    ```

The native executable will be located in `build/bin/native/debugExecutable/` or `build/bin/native/releaseExecutable/`.

## Development Conventions

*   **Monolithic Structure**: All application logic is currently contained within a single file, `src/nativeMain/kotlin/Main.kt`. This includes UI, API calls, configuration, and chat state management.
*   **Refactoring Plan**: `IMPROVEMENT_PLAN.md` outlines a detailed plan to refactor the monolith into separate components (e.g., `ConfigManager`, `ApiClient`, `TerminalUI`, `ChatController`) to improve modularity and maintainability.
*   **Security**: API keys are stored in `config.json`. The improvement plan suggests prioritizing environment variables for enhanced security.
*   **Testing**: The project currently lacks substantial unit tests. The improvement plan emphasizes expanding test coverage after refactoring.
*   **Dependencies**: Dependencies are managed in `build.gradle.kts`. Platform-specific Ktor engines are included based on the detected target OS during the build process.

## Key Files

*   `src/nativeMain/kotlin/Main.kt`: Central file containing all application logic.
*   `build.gradle.kts`: Defines build process, dependencies, and platform targets.
*   `README.md`: Primary user-facing documentation.
*   `CLAUDE.md`: Provides detailed guidance for Claude Code.
*   `GEMINI.md`: Offers a technical overview for Google's Gemini models.
*   `IMPROVEMENT_PLAN.md`: Detailed plan for refactoring the application.
*   `NUEVAS_CARACTERISTICAS.md`: Proposal for new features (in Spanish).