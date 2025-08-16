# GEMINI.md

This document provides a technical overview of the `KotlinNativeClaudeChat` project for analysis and interaction by Google's Gemini models.

## Project Summary

This is a command-line chat application built with Kotlin/Native. It serves as a unified client for multiple AI model providers, including Anthropic and OpenRouter. Its key feature is its cross-platform nature, compiling to native executables for macOS, Windows, and Linux from a single codebase.

## Core Technologies

- **Language**: [Kotlin/Native](https://kotlinlang.org/docs/native-overview.html)
- **Build System**: [Gradle](https://gradle.org/) (`build.gradle.kts`)
- **HTTP Client**: [Ktor](https://ktor.io/) (multi-engine setup for cross-platform support)
- **JSON Serialization**: [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- **File I/O**: [Okio](https://square.github.io/okio/)

## Architecture and Features

### Multi-Provider Support

The application is designed to work with two main API providers:
1.  **Anthropic**: Direct integration with Claude models.
2.  **OpenRouter**: Acts as an aggregator, providing access to over 400 different AI models from various providers (OpenAI, Google, Mistral, etc.).

### Cross-Platform Implementation

A significant feature is its true cross-platform capability, achieved through:
- **Kotlin/Native**: Compiles Kotlin code directly to native binaries (`.kexe` for macOS/Linux, `.exe` for Windows).
- **Platform-Specific Ktor Engines**: The `build.gradle.kts` file configures platform-specific HTTP client engines to leverage native OS networking capabilities for optimal performance.
  - **macOS**: `ktor-client-darwin`
  - **Windows**: `ktor-client-winhttp`
  - **Linux**: `ktor-client-cio`
- **Runtime OS Detection**: The application uses `kotlin.native.Platform.osFamily` at runtime to identify the host OS and select the correct behavior.

### Application Logic (`Main.kt`)

The entire application logic is encapsulated within `src/nativeMain/kotlin/Main.kt`. This includes:
- **Configuration Management**:
  - Loads configuration from a `config.json` file.
  - If the file doesn't exist, it launches an interactive setup wizard.
  - Uses the Okio library for robust, cross-platform file handling.
- **API Interaction**:
  - Data classes (`@Serializable`) are defined for request and response structures for both Anthropic and OpenRouter APIs.
  - The Ktor client is used to make POST requests to the selected provider's endpoint.
- **User Interface**:
  - A terminal-based UI with an interactive navigation system.
  - Supports arrow-key navigation, submenus, and contextual help.
- **Chat Loop**:
  - Maintains conversation history in an in-memory list of `Message` objects.
  - Manages special chat commands like `/exit`, `/menu`, and `/help`.

## How to Build and Run

The project uses the Gradle wrapper.

- **Build the application**:
  ```bash
  ./gradlew build
  ```
- **Run the application (debug mode)**:
  ```bash
  ./gradlew runDebugExecutableNative
  ```
- **Run tests**:
  ```bash
  ./gradlew allTests
  ```

The native executable is generated in the `build/bin/native/` directory.
