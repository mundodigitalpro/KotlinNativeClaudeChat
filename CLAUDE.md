# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Native application that implements a multi-provider command-line chat client supporting both Anthropic Claude and OpenRouter APIs. The application provides access to 400+ AI models through a unified interface, featuring real-time model browsing, dynamic configuration management, and cross-platform compatibility for macOS, Linux, and Windows.

## Key Components

- **Main.kt** (`src/nativeMain/kotlin/Main.kt`): Contains the complete application logic including:
  - Multi-provider API support (Anthropic & OpenRouter)
  - Interactive model browser with real-time API integration
  - Dynamic configuration system with startup menu
  - Cross-platform HTTP client setup
  - Conversation loop with memory management
- **Config system**: Uses `config.json` for API configuration supporting both providers
- **Cross-platform targeting**: Automatically selects appropriate native target based on host OS/architecture

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application (debug mode - recommended for development)
./gradlew runDebugExecutableNative

# Run the application (release mode - optimized)
./gradlew runReleaseExecutableNative

# Run tests (minimal test structure exists)
./gradlew test

# Clean build artifacts
./gradlew clean

# Compile native code only
./gradlew compileKotlinNative

# Link native binaries
./gradlew linkNative
```

### Platform-specific builds
The build system automatically detects the host platform and configures the appropriate native target:
- macOS (ARM64/x64) - Uses Darwin engine
- Linux (ARM64/x64) 
- Windows (mingwX64)

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

### HTTP Client Setup
Uses Ktor client with platform-specific engines:
- Darwin engine on macOS with content negotiation
- Lenient JSON parsing with unknown key tolerance
- Provider-specific authentication headers:
  - Anthropic: `x-api-key` + `anthropic-version`
  - OpenRouter: `Authorization` Bearer token + optional tracking headers

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

## Dependencies

- `kotlinx-serialization-json`: JSON serialization for API communication
- `ktor-client-*`: HTTP client with content negotiation
  - `ktor-client-core`: Core HTTP functionality
  - `ktor-client-content-negotiation`: JSON content handling
  - `ktor-serialization-kotlinx-json`: Kotlinx serialization integration
  - `ktor-client-darwin`: macOS/iOS native engine
- `okio`: Cross-platform file system operations

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
- Real-time model fetching is optimized with caching
- HTTP client reuse for multiple API calls during model browsing
- Efficient JSON parsing with unknown key tolerance