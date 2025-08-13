# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Native application that implements a command-line chat client for Anthropic's Claude API. The application uses Kotlin Multiplatform with native targets for macOS, Linux, and Windows, featuring HTTP client functionality with Ktor and configuration management with Okio.

## Key Components

- **Main.kt** (`src/nativeMain/kotlin/Main.kt`): Contains the entire application logic including data structures, HTTP client setup, and conversation loop
- **Config system**: Uses `config.json` for API configuration (version, key, model, URL)
- **Cross-platform targeting**: Automatically selects the appropriate native target based on the host OS and architecture

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run tests (minimal test structure exists)
./gradlew test

# Clean build artifacts
./gradlew clean
```

### Platform-specific builds
The build system automatically detects the host platform and configures the appropriate native target:
- macOS (ARM64/x64)
- Linux (ARM64/x64) 
- Windows (mingwX64)

## Architecture Notes

### Data Structures
- `Config`: Stores Anthropic API configuration
- `Message`: Represents conversation messages with role and content
- `RequestBody`: HTTP request structure for Claude API
- `ApiResponse`: Handles Claude API response with content blocks
- `ContentBlock`: Individual text content within API responses
- `Usage`: Token usage tracking

### HTTP Client Setup
Uses Ktor client with Darwin engine on macOS, configured with:
- Content negotiation for JSON serialization
- Lenient JSON parsing with unknown key ignoring
- Custom headers for Anthropic API authentication

### Configuration Management
- Loads from `config.json` if exists, otherwise prompts user input
- Saves configuration using Okio file system operations
- Handles cross-platform file paths

### Conversation Flow
Maintains conversation history in memory, sending full context with each API request to preserve conversation state.

## Dependencies

- `kotlinx-serialization-json`: JSON serialization
- `ktor-client-*`: HTTP client and content negotiation
- `okio`: File system operations
- Darwin engine for macOS HTTP operations

## Development Notes

The application stores API keys in `config.json` - ensure this file is not committed to version control in production environments. The test structure is minimal with an empty test file in `src/nativeTest/kotlin/Test.kt`.