# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Native application that implements a multi-provider command-line chat client supporting both Anthropic Claude and OpenRouter APIs. The application provides access to 400+ AI models through a unified interface, featuring real-time model browsing, dynamic configuration management, and **universal cross-platform compatibility** for macOS, Linux, and Windows with automatic platform detection and optimized native engines.

## Key Components

- **Main.kt** (`src/nativeMain/kotlin/Main.kt`): Contains the complete application logic including:
  - Multi-provider API support (Anthropic & OpenRouter)
  - Interactive model browser with real-time API integration
  - Dynamic configuration system with startup menu
  - **Universal cross-platform HTTP client setup** with automatic platform detection
  - Conversation loop with memory management
  - Platform detection and engine selection system
- **Config system**: Uses `config.json` for API configuration supporting both providers
- **Cross-platform targeting**: Automatically selects appropriate native target based on host OS/architecture
- **Platform detection**: Runtime OS detection using `kotlin.native.Platform.osFamily` with `@OptIn(ExperimentalNativeApi::class)`

## Development Commands

### Build and Run
```bash
# Build the project (universal for current platform)
./gradlew build

# Run the application (debug mode - recommended for development)
./gradlew runDebugExecutableNative

# Run the application (release mode - optimized)
./gradlew runReleaseExecutableNative

# Run tests
./gradlew allTests

# Clean build artifacts
./gradlew clean

# Compile native code only
./gradlew compileKotlinNative

# Link native binaries
./gradlew linkDebugExecutableNative
./gradlew linkReleaseExecutableNative
```

### Platform-specific builds
The build system automatically detects the host platform and configures the appropriate native target:
- **macOS (ARM64/x64)**: Uses Darwin engine (optimized for macOS/iOS networking)
- **Linux (ARM64/x64)**: Uses CIO engine (cross-platform compatibility)
- **Windows (mingwX64)**: Uses WinHttp engine (native Windows HTTP API)

#### Platform-specific Commands
**macOS/Linux:**
```bash
./gradlew runDebugExecutableNative
```

**Windows (PowerShell/CMD):**
```bash
./gradlew.bat runDebugExecutableNative
```

**Windows (Git Bash):**
```bash
./gradlew runDebugExecutableNative
```

## Architecture Notes

### Multi-Provider Design
The application supports two distinct API providers through a unified interface:

**Anthropic Provider:**
- Direct access to Claude models (3.5 Sonnet, 3.5 Haiku, etc.)
- Uses Anthropic's native message format with content blocks
- Requires `anthropicVersion` field for API versioning

**OpenRouter Provider:**
- Access to 400+ models from multiple providers (OpenAI, Google, Mistral, Meta, etc.)
- Unified chat completions format compatible with OpenAI API
- Real-time model browsing with filtering and search capabilities
- Optional application tracking via `appName` and `siteUrl`

### Data Structures
- `Config`: Multi-provider configuration supporting both Anthropic and OpenRouter
- `Message`: Universal message format with role, content, and optional fields (refusal, reasoning)
- `AnthropicRequestBody` / `OpenRouterRequestBody`: Provider-specific request structures
- `AnthropicApiResponse` / `OpenRouterApiResponse`: Provider-specific response handling
- `OpenRouterModel`: Model metadata for browsing functionality with pricing and capabilities
- `ContentBlock`: Text content blocks for Anthropic responses
- `Usage` / `OpenRouterUsage`: Token usage tracking with provider-specific details

### Cross-Platform HTTP Client Setup
The application implements a sophisticated platform detection and HTTP engine selection system:

#### Platform Detection System
```kotlin
enum class Platform { MACOS, WINDOWS, LINUX, UNKNOWN }

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun detectPlatform(): Platform {
    val osName = kotlin.native.Platform.osFamily.name.lowercase()
    return when {
        osName.contains("macos") || osName.contains("osx") -> Platform.MACOS
        osName.contains("windows") || osName.contains("mingw") -> Platform.WINDOWS
        osName.contains("linux") -> Platform.LINUX
        else -> Platform.UNKNOWN
    }
}
```

#### HTTP Engine Selection
Uses Ktor client with platform-specific engines selected automatically at runtime:
- **Darwin engine** on macOS (CFNetwork/NSURLSession based)
- **WinHttp engine** on Windows (native Windows HTTP API)
- **CIO engine** on Linux (coroutine-based I/O, JVM compatible)

#### Client Configuration
- Lenient JSON parsing with unknown key tolerance
- Provider-specific authentication headers:
  - Anthropic: `x-api-key` + `anthropic-version`
  - OpenRouter: `Authorization` Bearer token + optional tracking headers
- Content negotiation for JSON serialization
- Automatic engine fallback in case of issues

### Configuration Management
Advanced configuration system with interactive startup menu:
- Loads from `config.json` with fallback to user prompts
- Multi-provider setup wizard with model selection
- Smart model switching without re-entering API keys
- Real-time OpenRouter model browsing with search and filtering
- Okio-based cross-platform file operations

### Conversation Flow
Maintains conversation history in memory, sending full context with each API request to preserve conversation state. Handles provider-specific response formats transparently.

### Model Browser System
For OpenRouter configurations, the application provides:
- Real-time fetching of 400+ available models
- Separation of free (55+) and paid (259+) models
- Search functionality by model name or provider
- Detailed model information (pricing, context length, capabilities)
- Interactive selection with immediate configuration updates

### Streaming Implementation
The application implements real-time streaming responses using Server-Sent Events (SSE):

#### Streaming Architecture
- **ByteReadChannel**: Ktor's streaming HTTP response reader
- **SSE Parser**: Custom parser for Server-Sent Events format
- **Real-time Display**: Character-by-character response rendering
- **Error Handling**: Graceful degradation when streaming fails
- **Debug Logging**: Detailed streaming logs for troubleshooting

#### Streaming Features
- **Progressive Response**: Text appears as it's generated by the AI model
- **Loading Indicators**: Visual feedback during response generation
- **Stream Validation**: Automatic detection of streaming support per model
- **Fallback Support**: Graceful fallback to normal mode if streaming fails

#### Provider Support
- **OpenRouter**: Full streaming support with SSE format
- **Anthropic**: Streaming support with content deltas
- **Universal Format**: Unified streaming interface for both providers

### Enhanced Navigation System
Interactive menu navigation with advanced features:

#### Navigation Features
- **Arrow Key Navigation**: ↑/↓ to navigate menu options
- **Visual Feedback**: Selected option highlighted with ► indicator
- **Breadcrumb Trail**: Shows current location in menu hierarchy
- **Submenu Support**: →/← keys for submenu navigation
- **Color Coding**: Intuitive color scheme for different UI elements
- **Terminal Detection**: Automatic fallback to number input if arrow keys unavailable

#### Menu Structure
```kotlin
data class MenuItem(
    val id: String,
    val text: String,
    val submenu: List<MenuItem>? = null,
    val action: (() -> Unit)? = null
)

class NavigationController {
    // Enhanced navigation with breadcrumbs, arrow keys, and visual feedback
    fun navigate(menu: List<MenuItem>, title: String): MenuItem?
}
```

## Dependencies

### Core Dependencies
- `kotlinx-serialization-json`: JSON serialization for API communication
- `ktor-client-core`: Core HTTP functionality
- `ktor-client-content-negotiation`: JSON content handling
- `ktor-serialization-kotlinx-json`: Kotlinx serialization integration
- `okio`: Cross-platform file system operations

### Platform-Specific HTTP Engines
The build system automatically includes the appropriate engine based on the target platform:

**macOS (via `build.gradle.kts`):**
- `ktor-client-darwin`: macOS/iOS native engine (CFNetwork based)

**Windows (via `build.gradle.kts`):**
- `ktor-client-winhttp`: Windows native HTTP engine (WinHttp API)

**Linux (via `build.gradle.kts`):**
- `ktor-client-cio`: Coroutine-based I/O engine (cross-platform)

### Engine Selection Logic
The application automatically selects the appropriate engine at runtime:
```kotlin
fun createPlatformHttpClient(): HttpClient {
    return when (detectPlatform()) {
        Platform.MACOS -> HttpClient { /* Darwin engine via dependencies */ }
        Platform.WINDOWS -> HttpClient { /* WinHttp engine via dependencies */ }
        Platform.LINUX -> HttpClient { /* CIO engine via dependencies */ }
        Platform.UNKNOWN -> HttpClient { /* CIO fallback */ }
    }
}
```

## Development Notes

### Security Considerations
- API keys are stored in `config.json` - ensure this file is excluded from version control
- The application handles two different authentication schemes securely
- Configuration files are written using Okio with proper error handling

### Testing Structure
Minimal test structure with empty test file in `src/nativeTest/kotlin/Test.kt`. The application logic is contained in a single file making it suitable for integration testing rather than unit testing.

### Error Handling
The application includes comprehensive error handling for:
- Invalid API keys and authentication failures
- Model availability and compatibility issues
- Network connectivity problems
- Provider-specific error responses with actionable suggestions

### Performance Considerations
- **Platform-optimized HTTP engines**: Each platform uses its most efficient native HTTP implementation
- **Streaming Optimization**: ByteReadChannel with efficient UTF-8 line reading
- **Memory Management**: Conversation history stored efficiently in memory
- **File I/O Optimization**: Okio-based file operations for configuration and history
- Real-time model fetching is optimized with caching
- HTTP client reuse for multiple API calls during model browsing
- Efficient JSON parsing with unknown key tolerance
- **Timeout Management**: Configurable timeouts for HTTP requests (5 minutes default)
- **Native binary optimization**: Each platform generates optimized binaries:
  - macOS: `.kexe` files optimized for Darwin/ARM64/x64
  - Windows: `.exe` files optimized for Windows x64
  - Linux: `.kexe` files optimized for Linux x64/ARM64

### Advanced Features

#### Chat Session Management
- **Session Persistence**: Automatic conversation history tracking
- **Multiple Sessions**: Support for loading different conversation histories
- **Session Validation**: Config validation before starting chat sessions
- **Graceful Exit**: Clean shutdown with optional autosave

#### Error Handling
- **Streaming Errors**: Specific error handling for streaming API failures
- **Network Timeouts**: Robust timeout handling with user feedback
- **Model Availability**: Dynamic detection of model support for streaming
- **API Quota**: Intelligent handling of quota exceeded errors
- **Fallback Mechanisms**: Automatic fallback from streaming to normal mode

#### Development Features
- **Debug Logging**: Comprehensive logging system for development
- **Stream Debugging**: Line-by-line streaming response analysis
- **Performance Metrics**: Request timing and response size tracking
- **Configuration Validation**: Runtime validation of API configurations

### Cross-Platform Compatibility Notes
- **Universal codebase**: Single source code that adapts to all platforms
- **Runtime platform detection**: Uses Kotlin Native's experimental API for OS detection
- **Build-time optimization**: Gradle configures platform-specific dependencies automatically
- **Engine fallbacks**: CIO engine as universal fallback for unknown platforms
- **Testing**: Application should be tested on each target platform for optimal behavior

## Recent Updates (feature/streaming branch)

### Streaming Mode Implementation
- **Full SSE Support**: Complete Server-Sent Events implementation
- **Real-time Streaming**: Character-by-character response display
- **Dual Mode System**: Seamless switching between normal and streaming modes
- **Debug System**: Comprehensive logging for streaming troubleshooting

### Enhanced Chat Commands
- **Expanded Command Set**: 8 total commands including /clear, /save, /load, /config
- **History Management**: Full conversation save/load functionality
- **Configuration Display**: Runtime configuration viewing
- **Session Control**: Advanced session management with autosave

### Navigation Improvements
- **Interactive Menus**: Arrow key navigation with visual feedback
- **Enhanced Startup Menu**: 11 configuration options
- **Breadcrumb Navigation**: Context-aware menu location display
- **Terminal Compatibility**: Automatic fallback for different terminal types

### Security Enhancements
- **Gitignore Updates**: Enhanced protection against API key commits
- **Configuration Templates**: Safe configuration examples
- **Validation Systems**: Runtime configuration validation