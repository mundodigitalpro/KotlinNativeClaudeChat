import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import okio.*
import okio.Path.Companion.toPath
import platform.posix.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

// Enhanced Navigation System
data class MenuItem(
    val id: String,
    val text: String,
    val submenu: List<MenuItem>? = null,
    val action: (() -> Unit)? = null
)

class NavigationController {
    private var currentMenu: List<MenuItem> = emptyList()
    private var selectedIndex = 0
    private val breadcrumbs = mutableListOf<String>()
    private var isRunning = true
    
    companion object {
        // ANSI escape codes for navigation
        const val ANSI_CLEAR_SCREEN = "\u001B[2J"
        const val ANSI_HOME = "\u001B[H"
        const val ANSI_BOLD = "\u001B[1m"
        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_YELLOW = "\u001B[33m"
        const val ANSI_CYAN = "\u001B[36m"
        const val ANSI_REVERSE = "\u001B[7m"
        
        // Arrow keys detection
        const val KEY_ESCAPE = 27
        const val KEY_BRACKET = 91
        const val KEY_UP = 65
        const val KEY_DOWN = 66
        const val KEY_RIGHT = 67
        const val KEY_LEFT = 68
        const val KEY_ENTER = 10
        const val KEY_Q = 113
    }
    
    fun navigate(menu: List<MenuItem>, title: String = "Menu"): MenuItem? {
        currentMenu = menu
        selectedIndex = 0
        breadcrumbs.clear()
        breadcrumbs.add(title)
        isRunning = true
        
        // Set terminal to raw mode for better key detection
        ensureRawTerminalMode()
        
        try {
            while (isRunning) {
                displayMenu()
                val key = readKey()
                handleKeyPress(key)
            }
        } finally {
            // Restore terminal settings
            ensureNormalTerminalMode()
        }
        
        return null
    }
    
    @OptIn(ExperimentalNativeApi::class)
    private fun displayMenu() {
        // Clear screen and move cursor to home
        print(ANSI_CLEAR_SCREEN + ANSI_HOME)
        
        // Display title and breadcrumbs with platform info
        val platformInfo = kotlin.native.Platform.osFamily.name
        println("${ANSI_BOLD}${ANSI_CYAN}=== Kotlin Native AI Chat - Multiplatform ($platformInfo) ===${ANSI_RESET}")
        
        // Show breadcrumbs
        if (breadcrumbs.isNotEmpty()) {
            val breadcrumbPath = breadcrumbs.joinToString(" > ")
            println("${ANSI_BLUE}📍 $breadcrumbPath${ANSI_RESET}")
        }
        println()
        
        // Display menu items
        currentMenu.forEachIndexed { index, item ->
            val marker = if (index == selectedIndex) {
                "${ANSI_REVERSE}${ANSI_BOLD} ► ${ANSI_RESET}"
            } else {
                "   "
            }
            
            val hasSubmenu = if (item.submenu != null) " ${ANSI_GREEN}→${ANSI_RESET}" else ""
            val itemText = if (index == selectedIndex) {
                "${ANSI_BOLD}${ANSI_YELLOW}${item.text}${ANSI_RESET}"
            } else {
                item.text
            }
            
            println("$marker $itemText$hasSubmenu")
        }
        
        println()
        println("${ANSI_BLUE}Navigation: ↑↓ to navigate, Enter to select, Q to quit${ANSI_RESET}")
    }
    
    private fun readKey(): Int {
        val key = getchar()
        return if (key == KEY_ESCAPE) {
            // Check for arrow keys (ESC [ followed by A/B/C/D)
            val next = getchar()
            if (next == KEY_BRACKET) {
                getchar() // This will be A, B, C, or D
            } else {
                KEY_ESCAPE
            }
        } else {
            key
        }
    }
    
    private fun handleKeyPress(key: Int) {
        when (key) {
            KEY_UP -> {
                selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else currentMenu.size - 1
            }
            KEY_DOWN -> {
                selectedIndex = (selectedIndex + 1) % currentMenu.size
            }
            KEY_ENTER -> {
                val selectedItem = currentMenu[selectedIndex]
                when {
                    selectedItem.submenu != null -> {
                        // Navigate to submenu
                        val previousMenu = currentMenu
                        val previousIndex = selectedIndex
                        val previousBreadcrumbs = breadcrumbs.toList()
                        
                        currentMenu = selectedItem.submenu
                        selectedIndex = 0
                        breadcrumbs.add(selectedItem.text)
                        
                        // Add back option to submenu
                        currentMenu = listOf(
                            MenuItem("back", "← Back", action = {
                                currentMenu = previousMenu
                                selectedIndex = previousIndex
                                breadcrumbs.clear()
                                breadcrumbs.addAll(previousBreadcrumbs)
                            })
                        ) + currentMenu
                    }
                    selectedItem.action != null -> {
                        selectedItem.action.invoke()
                    }
                    selectedItem.id == "back" -> {
                        selectedItem.action?.invoke()
                    }
                    selectedItem.id == "quit" -> {
                        isRunning = false
                    }
                    else -> {
                        // Handle other selections
                        isRunning = false
                    }
                }
            }
            KEY_Q -> {
                isRunning = false
            }
        }
    }
}

// Terminal control functions
fun ensureRawTerminalMode() {
    // Set terminal to raw mode to capture individual keystrokes
    system("stty raw -echo")
}

fun ensureNormalTerminalMode() {
    // Restore normal terminal mode
    system("stty cooked echo")
}

// Platform detection and HTTP client creation  
@OptIn(ExperimentalNativeApi::class)
fun createPlatformHttpClient(): HttpClient {
    // Detect current platform using system properties
    val osName = kotlin.native.Platform.osFamily.name
    println("Detected platform: $osName")
    
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }.also {
        when {
            osName.contains("OSX", ignoreCase = true) || osName.contains("MACOSX", ignoreCase = true) -> {
                println("Using Darwin HTTP engine for macOS")
            }
            osName.contains("WINDOWS", ignoreCase = true) || osName.contains("MINGW", ignoreCase = true) -> {
                println("Using WinHttp engine for Windows")
            }
            osName.contains("LINUX", ignoreCase = true) -> {
                println("Using CIO engine for Linux")
            }
            else -> {
                println("Using CIO engine (fallback for platform: $osName)")
            }
        }
    }
}

// Configuration functions
fun loadConfigUsingOkio(configFilePath: Path): Config? {
    return try {
        val fileSystem = FileSystem.SYSTEM
        val jsonContent = fileSystem.read(configFilePath) {
            readUtf8()
        }
        Json.decodeFromString<Config>(jsonContent)
    } catch (e: Exception) {
        println("Error loading config: ${e.message}")
        null
    }
}

fun requestConfigInput(): Config {
    ensureNormalTerminalMode()
    
    println("Select API provider:")
    println("1. Anthropic Claude")
    println("2. OpenRouter (Access to 400+ models)")
    print("Choose (1-2): ")
    
    val choice = readlnOrNull()?.toIntOrNull() ?: 1
    
    return when (choice) {
        2 -> {
            println("\nConfiguring OpenRouter...")
            print("Enter OpenRouter API key: ")
            val apiKey = readlnOrNull() ?: ""
            
            print("Enter model (e.g., openai/gpt-4o, anthropic/claude-3.5-sonnet): ")
            val model = readlnOrNull() ?: "openai/gpt-4o"
            
            print("Enter app name (optional): ")
            val appName = readlnOrNull()?.takeIf { it.isNotBlank() }
            
            print("Enter site URL (optional): ")
            val siteUrl = readlnOrNull()?.takeIf { it.isNotBlank() }
            
            Config(
                provider = "openrouter",
                apiKey = apiKey,
                model = model,
                url = "https://openrouter.ai/api/v1/chat/completions",
                appName = appName,
                siteUrl = siteUrl
            )
        }
        else -> {
            println("\nConfiguring Anthropic...")
            print("Enter Anthropic API version (e.g., 2023-06-01): ")
            val version = readlnOrNull() ?: "2023-06-01"
            
            print("Enter your Anthropic API key: ")
            val apiKey = readlnOrNull() ?: ""
            
            print("Enter model name (e.g., claude-3-5-sonnet-20241022): ")
            val model = readlnOrNull() ?: "claude-3-5-sonnet-20241022"
            
            Config(
                provider = "anthropic",
                anthropicVersion = version,
                apiKey = apiKey,
                model = model,
                url = "https://api.anthropic.com/v1/messages"
            )
        }
    }
}

fun changeModelOnly(existingConfig: Config): Config {
    ensureNormalTerminalMode()
    
    return when (existingConfig.provider) {
        "anthropic" -> {
            println("\nChanging Anthropic model (keeping existing API key)")
            print("Enter new model name (current: ${existingConfig.model}): ")
            val newModel = readlnOrNull()?.takeIf { it.isNotBlank() } ?: existingConfig.model
            
            existingConfig.copy(model = newModel)
        }
        "openrouter" -> {
            println("\nChanging OpenRouter model (keeping existing API key)")
            println("Popular OpenRouter models:")
            println("- openai/gpt-4o")
            println("- openai/gpt-4o-mini")  
            println("- anthropic/claude-3.5-sonnet")
            println("- google/gemini-2.0-flash-exp:free")
            println("- mistralai/mistral-large")
            println("- qwen/qwen3-coder:free")
            println("- z-ai/glm-4.5-air:free")
            print("Enter new model name (current: ${existingConfig.model}): ")
            val newModel = readlnOrNull()?.takeIf { it.isNotBlank() } ?: existingConfig.model
            
            existingConfig.copy(model = newModel)
        }
        else -> existingConfig
    }
}

fun saveConfigUsingOkio(config: Config, configFilePath: Path) {
    try {
        val fileSystem = FileSystem.SYSTEM
        val jsonContent = Json.encodeToString(Config.serializer(), config)
        fileSystem.write(configFilePath) {
            writeUtf8(jsonContent)
        }
        println("Configuration saved to $configFilePath")
    } catch (e: Exception) {
        println("Error saving config: ${e.message}")
    }
}

enum class ApiProvider { ANTHROPIC, OPENROUTER }

@Serializable
data class Config(
    val provider: String,
    val anthropicVersion: String? = null,
    val apiKey: String,
    val model: String,
    val url: String,
    val appName: String? = null,
    val siteUrl: String? = null
)

@Serializable
data class ReasoningDetail(
    val type: String,
    val text: String? = null,
    val format: String? = null,
    val index: Int? = null
)

@Serializable
data class Message(
    val role: String, 
    val content: String,
    val refusal: String? = null,
    val reasoning: String? = null,
    val reasoning_details: List<ReasoningDetail>? = null
)

@Serializable
data class AnthropicRequestBody(val model: String, val messages: List<Message>, val max_tokens: Int)

@Serializable
data class OpenRouterRequestBody(val model: String, val messages: List<Message>, val max_tokens: Int)

// ANTHROPIC API STRUCTURES
@Serializable
data class ContentBlock(
    val type: String, // "text"
    val text: String
)

@Serializable
data class AnthropicApiResponse(
    val id: String,
    val type: String, // "message"
    val role: String, // "assistant"
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String? = null,
    val stop_sequence: String? = null,
    val usage: Usage
)

@Serializable
data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)

// OPENROUTER API STRUCTURES
@Serializable
data class OpenRouterChoice(
    val index: Int,
    val message: Message,
    val finish_reason: String?
)

@Serializable
data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class OpenRouterApiResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null
)

// ERROR STRUCTURES
@Serializable
data class ApiError(
    val type: String,
    val message: String
)

@Serializable
data class ErrorResponse(
    val type: String,
    val error: ApiError
)


@OptIn(ExperimentalNativeApi::class)
fun main() = runBlocking {
    val configFilePath = "config.json".toPath()
    val fileSystem = FileSystem.SYSTEM
    
    var currentConfig: Config = if (fileSystem.exists(configFilePath)) {
        loadConfigUsingOkio(configFilePath) ?: run {
            val newConfig = requestConfigInput()
            saveConfigUsingOkio(newConfig, configFilePath)
            newConfig
        }
    } else {
        val newConfig = requestConfigInput()
        saveConfigUsingOkio(newConfig, configFilePath)
        newConfig
    }
    
    // Enhanced menu system
    val navigationController = NavigationController()
    var shouldContinue = true
    
    while (shouldContinue) {
        val mainMenu = listOf(
            MenuItem("chat", "💬 Start Chat Session", action = {
                ensureNormalTerminalMode()
                runBlocking {
                    startChatSession(currentConfig)
                }
            }),
            MenuItem("config", "⚙️  Configuration", submenu = listOf(
                MenuItem("show_config", "📋 Show Current Configuration", action = {
                    ensureNormalTerminalMode()
                    println("\n=== Current Configuration ===")
                    println("Provider: ${currentConfig.provider}")
                    println("Model: ${currentConfig.model}")
                    println("API URL: ${currentConfig.url}")
                    if (currentConfig.provider == "anthropic") {
                        println("Anthropic Version: ${currentConfig.anthropicVersion}")
                    }
                    if (currentConfig.provider == "openrouter") {
                        currentConfig.appName?.let { println("App Name: $it") }
                        currentConfig.siteUrl?.let { println("Site URL: $it") }
                    }
                    println("=============================")
                    print("Press Enter to continue...")
                    readlnOrNull()
                }),
                MenuItem("change_model", "🔄 Change Model Only", action = {
                    currentConfig = changeModelOnly(currentConfig)
                    saveConfigUsingOkio(currentConfig, configFilePath)
                    ensureNormalTerminalMode()
                    println("Model updated successfully!")
                    print("Press Enter to continue...")
                    readlnOrNull()
                }),
                MenuItem("reconfigure", "🔧 Full Reconfiguration", action = {
                    ensureNormalTerminalMode()
                    println("Starting full reconfiguration...")
                    currentConfig = requestConfigInput()
                    saveConfigUsingOkio(currentConfig, configFilePath)
                    println("Configuration updated successfully!")
                    print("Press Enter to continue...")
                    readlnOrNull()
                }),
                MenuItem("reset_config", "🗑️  Reset Configuration", action = {
                    ensureNormalTerminalMode()
                    print("Are you sure you want to reset configuration? (y/N): ")
                    val confirm = readlnOrNull()?.lowercase()
                    if (confirm == "y" || confirm == "yes") {
                        try {
                            FileSystem.SYSTEM.delete(configFilePath)
                            println("Configuration reset. You'll be prompted to set up again on next start.")
                        } catch (e: Exception) {
                            println("Error resetting configuration: ${e.message}")
                        }
                    }
                    print("Press Enter to continue...")
                    readlnOrNull()
                })
            )),
            MenuItem("help", "❓ Help & Information", action = {
                ensureNormalTerminalMode()
                @OptIn(ExperimentalNativeApi::class)
                val platformInfo = kotlin.native.Platform.osFamily.name
                println("\n=== Kotlin Native AI Chat - Help ===")
                println("Platform: $platformInfo")
                println()
                println("Navigation:")
                println("- Use ↑↓ arrow keys to navigate menus")
                println("- Press Enter to select")
                println("- Press Q to quit from any menu")
                println()
                println("Features:")
                println("- Multi-provider support (Anthropic + OpenRouter)")
                println("- Automatic platform detection")
                println("- Interactive configuration management")
                println("- Cross-platform compatibility")
                println()
                println("Supported Models:")
                println("- Anthropic: Claude 3.5 Sonnet, Claude 3 Haiku, etc.")
                println("- OpenRouter: 400+ models including GPT-4, Gemini, etc.")
                println("=====================================")
                print("Press Enter to continue...")
                readlnOrNull()
            }),
            MenuItem("quit", "🚪 Quit", action = {
                shouldContinue = false
            })
        )
        
        try {
            navigationController.navigate(mainMenu, "AI Chat - Main Menu")
        } catch (e: Exception) {
            // If navigation fails, fall back to simple menu
            ensureNormalTerminalMode()
            println("\nFallback to simple menu...")
            println("1. Start Chat")
            println("2. Show Config") 
            println("3. Change Model")
            println("4. Quit")
            print("Choose: ")
            
            when (readlnOrNull()) {
                "1" -> {
                    println("Starting chat session...")
                    startChatSession(currentConfig)
                }
                "2" -> {
                    println("Current configuration:")
                    println("Provider: ${currentConfig.provider}")
                    println("Model: ${currentConfig.model}")
                    println("API URL: ${currentConfig.url}")
                }
                "3" -> {
                    currentConfig = changeModelOnly(currentConfig)
                    saveConfigUsingOkio(currentConfig, configFilePath)
                }
                "4" -> shouldContinue = false
            }
        }
    }
}

suspend fun startChatSession(config: Config) {
    val client = createPlatformHttpClient()
    val conversation = mutableListOf<Message>()
    
    ensureNormalTerminalMode()
    println("\n=== Chat Session Started ===")
    println("Type 'quit' to return to main menu")
    println("Current model: ${config.model} (${config.provider})")
    println()

    while (true) {
        print("You: ")
        val userInput = readlnOrNull() ?: break
        
        if (userInput.lowercase() == "quit") {
            break
        }
        
        conversation.add(Message("user", userInput))
        
        try {
            when (config.provider) {
                "anthropic" -> {
                    val requestBody = AnthropicRequestBody(config.model, conversation, 1024)
                    val httpResponse = client.post(config.url) {
                        header("x-api-key", config.apiKey)
                        header("anthropic-version", config.anthropicVersion ?: "2023-06-01")
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                    
                    val responseText = httpResponse.body<String>()
                    
                    if (responseText.contains("\"type\":\"error\"")) {
                        val errorResponse = Json.decodeFromString<ErrorResponse>(responseText)
                        println("API Error: ${errorResponse.error.message}")
                        continue
                    }
                    
                    val response = Json.decodeFromString<AnthropicApiResponse>(responseText)
                    response.content.forEach { contentBlock ->
                        if (contentBlock.type == "text") {
                            println("Assistant: ${contentBlock.text}")
                        }
                    }
                    
                    val assistantResponse = response.content
                        .filter { it.type == "text" }
                        .joinToString("") { it.text }
                    
                    conversation.add(Message("assistant", assistantResponse))
                }
                "openrouter" -> {
                    val requestBody = OpenRouterRequestBody(config.model, conversation, 1024)
                    val httpResponse = client.post(config.url) {
                        header("Authorization", "Bearer ${config.apiKey}")
                        config.appName?.let { header("HTTP-Referer", it) }
                        config.siteUrl?.let { header("X-Title", it) }
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                    
                    val response = Json.decodeFromString<OpenRouterApiResponse>(httpResponse.body())
                    val choice = response.choices.firstOrNull()
                    val assistantMessage = choice?.message?.content ?: "No response received"
                    
                    println("Assistant: $assistantMessage")
                    
                    // Handle reasoning if present
                    choice?.message?.reasoning?.let { reasoning ->
                        println("\n[Reasoning]: $reasoning")
                    }
                    
                    conversation.add(Message("assistant", assistantMessage))
                }
            }
        } catch (e: Exception) {
            println("Error in request: ${e.message}")
            break
        }
    }
    
    client.close()
    println("\n=== Chat Session Ended ===")
}


