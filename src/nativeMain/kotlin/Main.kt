import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.darwin.*
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
        
        val useArrowKeys = isInteractiveTerminal()
        
        if (useArrowKeys) {
            // Use arrow key navigation in interactive terminal
            ensureRawTerminalMode()
            
            try {
                while (isRunning) {
                    displayMenu()
                    val key = readKey()
                    handleKeyPress(key)
                }
            } finally {
                ensureNormalTerminalMode()
            }
        } else {
            // Use number-based navigation with enhanced display
            while (isRunning) {
                displayMenuWithPrompt()
                val input = readlnOrNull()?.trim() ?: ""
                handleTextInput(input)
            }
        }
        
        return null
    }
    
    private fun displayMenu() {
        // Clear screen and move cursor to home
        print(ANSI_CLEAR_SCREEN + ANSI_HOME)
        
        // Display title and breadcrumbs
        println("${ANSI_BOLD}${ANSI_CYAN}=== Kotlin Native AI Chat - Enhanced Navigation ===${ANSI_RESET}")
        
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
            
            println("$marker${index + 1}. $itemText$hasSubmenu")
        }
        
        println()
        println("${ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${ANSI_RESET}")
        println("${ANSI_BLUE}Navigation:${ANSI_RESET} ↑/↓ Select | Enter Confirm | →/← Submenu | Q/Esc Quit")
        
        if (selectedIndex < currentMenu.size) {
            val currentItem = currentMenu[selectedIndex]
            if (currentItem.submenu != null) {
                println("${ANSI_GREEN}→ Press → or Enter to access submenu${ANSI_RESET}")
            } else {
                println("${ANSI_YELLOW}Press Enter to execute this action${ANSI_RESET}")
            }
        }
    }
    
    private fun displayMenuWithPrompt() {
        // Clear screen and move cursor to home
        print(ANSI_CLEAR_SCREEN + ANSI_HOME)
        
        // Display title and breadcrumbs
        println("${ANSI_BOLD}${ANSI_CYAN}=== Kotlin Native AI Chat - Enhanced Navigation ===${ANSI_RESET}")
        
        // Show breadcrumbs
        if (breadcrumbs.isNotEmpty()) {
            val breadcrumbPath = breadcrumbs.joinToString(" > ")
            println("${ANSI_BLUE}📍 $breadcrumbPath${ANSI_RESET}")
        }
        println()
        
        // Display menu items with numbers
        currentMenu.forEachIndexed { index, item ->
            val hasSubmenu = if (item.submenu != null) " ${ANSI_GREEN}→${ANSI_RESET}" else ""
            val itemText = "${ANSI_YELLOW}${item.text}${ANSI_RESET}"
            println("${ANSI_BOLD}${index + 1}.${ANSI_RESET} $itemText$hasSubmenu")
        }
        
        println()
        println("${ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${ANSI_RESET}")
        println("${ANSI_BLUE}Navigation:${ANSI_RESET} Enter number (1-${currentMenu.size}) | Q/Esc to quit")
        print("${ANSI_GREEN}Your choice:${ANSI_RESET} ")
    }
    
    private fun handleTextInput(input: String) {
        when {
            input.equals("q", ignoreCase = true) -> isRunning = false
            input.equals("quit", ignoreCase = true) -> isRunning = false
            input.equals("esc", ignoreCase = true) -> isRunning = false
            input.equals("escape", ignoreCase = true) -> isRunning = false
            input.toIntOrNull() != null -> {
                val choice = input.toInt()
                if (choice in 1..currentMenu.size) {
                    selectedIndex = choice - 1
                    executeCurrentAction()
                } else {
                    println("${ANSI_YELLOW}Invalid choice. Please enter 1-${currentMenu.size}${ANSI_RESET}")
                    platform.posix.usleep(1000000u) // 1 second
                }
            }
            else -> {
                println("${ANSI_YELLOW}Please enter a number (1-${currentMenu.size}), 'q', or 'esc' to quit${ANSI_RESET}")
                platform.posix.usleep(1000000u) // 1 second
            }
        }
    }
    
    private fun readKey(): Int {
        return getchar()
    }
    
    private fun handleKeyPress(key: Int) {
        when (key) {
            KEY_ESCAPE -> {
                // Check for arrow keys (ESC + [ + direction)
                val next1 = getchar()
                if (next1 == KEY_BRACKET) {
                    val direction = getchar()
                    when (direction) {
                        KEY_UP -> moveUp()
                        KEY_DOWN -> moveDown()
                        KEY_RIGHT -> navigateForward()
                        KEY_LEFT -> navigateBack()
                    }
                } else {
                    // Just ESC - quit
                    isRunning = false
                }
            }
            KEY_ENTER -> executeCurrentAction()
            KEY_Q -> isRunning = false
            in 49..57 -> { // Keys 1-9
                val index = key - 49 // Convert to 0-based index
                if (index < currentMenu.size) {
                    selectedIndex = index
                    executeCurrentAction()
                }
            }
        }
    }
    
    private fun moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--
        } else {
            selectedIndex = currentMenu.size - 1 // Wrap to bottom
        }
    }
    
    private fun moveDown() {
        if (selectedIndex < currentMenu.size - 1) {
            selectedIndex++
        } else {
            selectedIndex = 0 // Wrap to top
        }
    }
    
    private fun navigateForward() {
        val currentItem = currentMenu[selectedIndex]
        if (currentItem.submenu != null) {
            enterSubmenu(currentItem)
        }
    }
    
    private fun navigateBack() {
        if (breadcrumbs.size > 1) {
            // Go back to parent menu
            breadcrumbs.removeLastOrNull()
            // This would require menu stack implementation for full functionality
            // For now, just quit to main menu
            isRunning = false
        }
    }
    
    private fun enterSubmenu(item: MenuItem) {
        item.submenu?.let { submenu ->
            breadcrumbs.add(item.text)
            currentMenu = submenu
            selectedIndex = 0
        }
    }
    
    private fun executeCurrentAction() {
        if (selectedIndex < currentMenu.size) {
            val currentItem = currentMenu[selectedIndex]
            
            if (currentItem.submenu != null) {
                enterSubmenu(currentItem)
            } else {
                currentItem.action?.invoke()
                isRunning = false
            }
        }
    }
}

// Terminal management functions
fun isInteractiveTerminal(): Boolean {
    // Check if we're in an interactive terminal by testing if isatty works
    return try {
        val result = system("tty > /dev/null 2>&1")
        result == 0
    } catch (e: Exception) {
        false
    }
}

fun ensureNormalTerminalMode() {
    try {
        system("stty echo icanon 2>/dev/null")
    } catch (e: Exception) {
        // Ignore errors - not in a terminal
    }
}

fun ensureRawTerminalMode() {
    try {
        if (isInteractiveTerminal()) {
            system("stty -echo -icanon min 1 time 0 2>/dev/null")
        }
    } catch (e: Exception) {
        // Ignore errors - not in a terminal
    }
}

// Chat control functions
enum class ChatCommand {
    CONTINUE,     // Continue chatting normally
    BACK_TO_MENU, // Go back to main menu
    EXIT_APP,     // Exit the application
    HELP          // Show chat commands
}

data class ChatInput(
    val command: ChatCommand,
    val message: String? = null
)

fun parseChatInput(input: String): ChatInput {
    val trimmedInput = input.trim()
    
    return when {
        trimmedInput.isEmpty() -> ChatInput(ChatCommand.BACK_TO_MENU)
        trimmedInput.equals("/menu", ignoreCase = true) -> ChatInput(ChatCommand.BACK_TO_MENU)
        trimmedInput.equals("/back", ignoreCase = true) -> ChatInput(ChatCommand.BACK_TO_MENU)
        trimmedInput.equals("/exit", ignoreCase = true) -> ChatInput(ChatCommand.EXIT_APP)
        trimmedInput.equals("/quit", ignoreCase = true) -> ChatInput(ChatCommand.EXIT_APP)
        trimmedInput.equals("/help", ignoreCase = true) -> ChatInput(ChatCommand.HELP)
        trimmedInput.equals("?", ignoreCase = true) -> ChatInput(ChatCommand.HELP)
        else -> ChatInput(ChatCommand.CONTINUE, trimmedInput)
    }
}

fun showChatHelp() {
    println("\n${NavigationController.ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_BOLD}${NavigationController.ANSI_BLUE}💬 Chat Commands:${NavigationController.ANSI_RESET}")
    println("  ${NavigationController.ANSI_GREEN}/menu${NavigationController.ANSI_RESET} or ${NavigationController.ANSI_GREEN}/back${NavigationController.ANSI_RESET}  - Return to main menu")
    println("  ${NavigationController.ANSI_GREEN}/exit${NavigationController.ANSI_RESET} or ${NavigationController.ANSI_GREEN}/quit${NavigationController.ANSI_RESET}  - Exit application")
    println("  ${NavigationController.ANSI_GREEN}/help${NavigationController.ANSI_RESET} or ${NavigationController.ANSI_GREEN}?${NavigationController.ANSI_RESET}      - Show this help")
    println("  ${NavigationController.ANSI_GREEN}[Enter]${NavigationController.ANSI_RESET}       - Return to main menu (empty message)")
    println("  ${NavigationController.ANSI_YELLOW}Type any message to chat with the AI model${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NavigationController.ANSI_RESET}\n")
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

fun selectApiProvider(): ApiProvider {
    var selectedProvider = ApiProvider.ANTHROPIC
    
    val menuItems = listOf(
        MenuItem("anthropic", "Anthropic (Claude)", action = { selectedProvider = ApiProvider.ANTHROPIC }),
        MenuItem("openrouter", "OpenRouter (Multiple AI Models)", action = { selectedProvider = ApiProvider.OPENROUTER })
    )
    
    val controller = NavigationController()
    controller.navigate(menuItems, "API Provider Selection")
    
    return selectedProvider
}

fun requestAnthropicConfig(): Config {
    // Ensure terminal is in normal mode for text input
    ensureNormalTerminalMode()
    
    print("Enter Anthropic API version (e.g., 2023-06-01): ")
    val version = readlnOrNull() ?: "2023-06-01"
    
    print("Enter your Anthropic API key: ")
    val apiKey = readlnOrNull() ?: ""
    
    print("Enter model name (e.g., claude-3-5-haiku-20241022): ")
    val model = readlnOrNull() ?: "claude-3-5-haiku-20241022"
    
    val url = "https://api.anthropic.com/v1/messages"
    
    return Config("anthropic", version, apiKey, model, url)
}

fun requestOpenRouterConfig(): Config {
    // Ensure terminal is in normal mode for text input
    ensureNormalTerminalMode()
    
    print("Enter your OpenRouter API key: ")
    val apiKey = readlnOrNull() ?: ""
    
    println("\nPopular OpenRouter models:")
    println("- openai/gpt-4o")
    println("- openai/gpt-4o-mini")
    println("- anthropic/claude-3.5-sonnet")
    println("- google/gemini-2.5-flash-lite")
    println("- mistralai/mistral-large")
    println("- qwen/qwen3-coder:free")
    println("- z-ai/glm-4.5-air:free")
    print("Enter model name: ")
    val model = readlnOrNull() ?: "openai/gpt-4o"
    
    print("Enter your app/site name (optional): ")
    val appName = readlnOrNull()?.takeIf { it.isNotBlank() }
    
    print("Enter your site URL (optional): ")
    val siteUrl = readlnOrNull()?.takeIf { it.isNotBlank() }
    
    val url = "https://openrouter.ai/api/v1/chat/completions"
    
    return Config("openrouter", null, apiKey, model, url, appName, siteUrl)
}

fun requestConfigInput(): Config {
    val provider = selectApiProvider()
    return when (provider) {
        ApiProvider.ANTHROPIC -> requestAnthropicConfig()
        ApiProvider.OPENROUTER -> requestOpenRouterConfig()
    }
}

suspend fun fetchOpenRouterModels(apiKey: String, client: HttpClient): List<OpenRouterModel> {
    return try {
        val response = client.get("https://openrouter.ai/api/v1/models") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
        }
        
        val modelsResponse = response.body<OpenRouterModelsResponse>()
        modelsResponse.data.sortedWith(compareBy<OpenRouterModel> { !it.isFree }.thenBy { it.provider }.thenBy { it.modelName })
    } catch (e: Exception) {
        println("❌ Error fetching models: ${e.message}")
        emptyList()
    }
}

fun displayModelsMenu(models: List<OpenRouterModel>) {
    if (models.isEmpty()) {
        println("❌ No models available or error fetching models.")
        return
    }
    
    val freeModels = models.filter { it.isFree }
    val paidModels = models.filter { !it.isFree }
    
    println("\n🆓 === FREE MODELS (${freeModels.size}) ===")
    freeModels.forEachIndexed { index, model ->
        println("${index + 1}. ${model.id}")
        println("   📝 ${model.name}")
        if (model.context_length != null) {
            println("   🔤 Context: ${model.context_length} tokens")
        }
        println()
    }
    
    println("💰 === PAID MODELS (${paidModels.size}) === (showing first 20)")
    paidModels.take(20).forEachIndexed { index, model ->
        val actualIndex = freeModels.size + index + 1
        println("$actualIndex. ${model.id}")
        println("   📝 ${model.name}")
        println("   💵 \$${model.pricing.prompt}/1k prompt tokens, \$${model.pricing.completion}/1k completion tokens")
        if (model.context_length != null) {
            println("   🔤 Context: ${model.context_length} tokens")
        }
        println()
    }
    
    println("ℹ️  Total models: ${models.size} (${freeModels.size} free, ${paidModels.size} paid)")
    if (paidModels.size > 20) {
        println("   (Showing first 20 paid models. ${paidModels.size - 20} more available)")
    }
}

suspend fun selectModelFromList(existingConfig: Config, client: HttpClient): Config {
    println("\n🔍 Fetching latest OpenRouter models...")
    val models = fetchOpenRouterModels(existingConfig.apiKey, client)
    
    if (models.isEmpty()) {
        println("❌ Could not fetch models. Using existing model: ${existingConfig.model}")
        return existingConfig
    }
    
    var selectedConfig = existingConfig
    var shouldSearch = false
    val freeModels = models.filter { it.isFree }
    val paidModels = models.filter { !it.isFree }
    
    // Create main model browser menu
    val mainMenuItems = mutableListOf<MenuItem>()
    
    // Add option to keep current model
    mainMenuItems.add(MenuItem("keep_current", "Keep current model: ${existingConfig.model}", action = {
        selectedConfig = existingConfig
    }))
    
    // Add free models submenu
    val freeMenuItems = freeModels.mapIndexed { index, model ->
        MenuItem("free_$index", "${model.id} - ${model.name}", action = {
            selectedConfig = existingConfig.copy(model = model.id)
            println("✅ Selected: ${model.id} (FREE)")
        })
    }
    mainMenuItems.add(MenuItem("free_models", "🆓 Browse Free Models (${freeModels.size})", submenu = freeMenuItems))
    
    // Add paid models submenu (first 50 to avoid overwhelming)
    val paidMenuItems = paidModels.take(50).mapIndexed { index, model ->
        val pricing = "💵 \$${model.pricing.prompt}/1k prompt, \$${model.pricing.completion}/1k completion"
        MenuItem("paid_$index", "${model.id} - ${model.name} ($pricing)", action = {
            selectedConfig = existingConfig.copy(model = model.id)
            println("✅ Selected: ${model.id} (PAID)")
        })
    }
    mainMenuItems.add(MenuItem("paid_models", "💰 Browse Paid Models (showing ${paidMenuItems.size}/${paidModels.size})", submenu = paidMenuItems))
    
    // Add search functionality - use flag to execute outside NavigationController
    mainMenuItems.add(MenuItem("search", "🔍 Search models (text-based)", action = {
        shouldSearch = true
    }))
    
    val controller = NavigationController()
    controller.navigate(mainMenuItems, "OpenRouter Model Browser")
    
    // Execute search outside NavigationController if requested
    if (shouldSearch) {
        searchModelsLegacy(models, existingConfig)?.let { newConfig ->
            selectedConfig = newConfig
        }
        // Give user time to see the result
        println("\n${NavigationController.ANSI_CYAN}Press Enter to continue...${NavigationController.ANSI_RESET}")
        readlnOrNull()
    }
    
    return selectedConfig
}

// Legacy search function for model searching
fun searchModelsLegacy(models: List<OpenRouterModel>, existingConfig: Config): Config? {
    // Ensure we're completely out of navigation mode with multiple terminal resets
    println("\n${NavigationController.ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_BOLD}${NavigationController.ANSI_BLUE}🔍 Model Search - Text Input Mode${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_YELLOW}Terminal restored to normal mode. You should see your text as you type.${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NavigationController.ANSI_RESET}")
    
    // Force terminal restoration with multiple commands
    system("stty echo icanon")
    system("stty sane")  // Reset to sane defaults
    
    // Add a small delay to ensure terminal is ready
    platform.posix.usleep(100000u) // 100ms
    
    print("\n${NavigationController.ANSI_GREEN}Enter search term:${NavigationController.ANSI_RESET} ")
    
    val searchTerm = readlnOrNull()?.lowercase() ?: return null
    
    val matchingModels = models.filter { 
        it.id.lowercase().contains(searchTerm) || 
        it.name.lowercase().contains(searchTerm) 
    }
    
    if (matchingModels.isNotEmpty()) {
        println("\n🔍 Search results for '$searchTerm':")
        matchingModels.take(10).forEachIndexed { index, model ->
            val freeText = if (model.isFree) " [FREE]" else ""
            println("${index + 1}. ${model.id}$freeText")
            println("   📝 ${model.name}")
        }
        print("\n${NavigationController.ANSI_GREEN}Select model (1-${matchingModels.take(10).size}):${NavigationController.ANSI_RESET} ")
        val searchChoice = readlnOrNull()?.toIntOrNull()
        if (searchChoice != null && searchChoice in 1..matchingModels.take(10).size) {
            val selectedModel = matchingModels[searchChoice - 1]
            val freeText = if (selectedModel.isFree) " (FREE)" else " (PAID)"
            println("${NavigationController.ANSI_GREEN}✅ Selected: ${selectedModel.id}$freeText${NavigationController.ANSI_RESET}")
            return existingConfig.copy(model = selectedModel.id)
        } else {
            println("${NavigationController.ANSI_YELLOW}❌ Invalid selection. No model selected.${NavigationController.ANSI_RESET}")
        }
    } else {
        println("${NavigationController.ANSI_YELLOW}❌ No models found matching '$searchTerm'${NavigationController.ANSI_RESET}")
    }
    
    return null
}

fun changeModelOnly(existingConfig: Config): Config {
    // Ensure terminal is in normal mode for text input
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
            println("- google/gemini-2.5-flash-lite")
            println("- mistralai/mistral-large")
            println("- qwen/qwen3-coder:free")
            println("- z-ai/glm-4.5-air:free")
            println("- openai/gpt-oss-20b:free")
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
    val finish_reason: String? = null,
    val logprobs: String? = null,
    val native_finish_reason: String? = null
)

@Serializable
data class TokenDetails(
    val cached_tokens: Int? = null,
    val reasoning_tokens: Int? = null
)

@Serializable  
data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
    val prompt_tokens_details: TokenDetails? = null,
    val completion_tokens_details: TokenDetails? = null
)

// OPENROUTER MODELS API STRUCTURES
@Serializable
data class ModelPricing(
    val prompt: String,
    val completion: String,
    val request: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val web_search: String? = null,
    val internal_reasoning: String? = null
)

@Serializable
data class OpenRouterModel(
    val id: String,
    val name: String,
    val description: String? = null,
    val pricing: ModelPricing,
    val context_length: Int? = null,
    val created: Long? = null
) {
    val isFree: Boolean
        get() = pricing.prompt == "0" && pricing.completion == "0"
        
    val provider: String
        get() = id.substringBefore("/")
        
    val modelName: String
        get() = id.substringAfter("/")
}

@Serializable
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModel>
)

@Serializable
data class OpenRouterApiResponse(
    val id: String,
    @kotlinx.serialization.SerialName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage,
    val provider: String? = null,
    val warnings: List<String>? = null,
    val system_fingerprint: String? = null
)

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

// Enhanced menu functions using NavigationController
fun showEnhancedStartupMenu(config: Config? = null): Int {
    var selectedChoice = 1
    
    val menuItems = config?.let {
        val baseItems = mutableListOf(
            MenuItem("use_existing", "Use existing configuration", action = { selectedChoice = 1 }),
            MenuItem("configure_new", "Configure new API", action = { selectedChoice = 2 }),
            MenuItem("change_model", "Change model only (keep same API key)", action = { selectedChoice = 3 })
        )
        
        if (it.provider == "openrouter") {
            baseItems.add(MenuItem("browse_models", "Browse all OpenRouter models (free/paid)", action = { selectedChoice = 4 }))
            baseItems.add(MenuItem("reconfigure", "Reconfigure existing setup", action = { selectedChoice = 5 }))
        } else {
            baseItems.add(MenuItem("reconfigure", "Reconfigure existing setup", action = { selectedChoice = 4 }))
        }
        
        baseItems
    } ?: listOf(
        MenuItem("use_existing", "Use existing configuration", action = { selectedChoice = 1 }),
        MenuItem("configure_new", "Configure new API", action = { selectedChoice = 2 }),
        MenuItem("reconfigure", "Reconfigure existing setup", action = { selectedChoice = 3 })
    )
    
    val controller = NavigationController()
    val title = if (config != null) {
        "Main Menu - Current: ${config.provider.uppercase()} API with model ${config.model}"
    } else {
        "Main Menu - No Configuration Found"
    }
    
    controller.navigate(menuItems, title)
    
    return selectedChoice
}

// Legacy function for compatibility - now uses enhanced navigation
fun showStartupMenu(config: Config? = null): Int {
    return showEnhancedStartupMenu(config)
}

fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }
}

suspend fun runChatSession(config: Config): Boolean {
    return startChatSession(config)
}

suspend fun startChatSession(config: Config): Boolean {
    val client = createPlatformHttpClient()
    val conversation = mutableListOf<Message>()
    
    // Show initial chat instructions
    println("\n${NavigationController.ANSI_BOLD}${NavigationController.ANSI_GREEN}💬 Chat Session Started${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_BLUE}Model: ${NavigationController.ANSI_YELLOW}${config.model}${NavigationController.ANSI_RESET}")
    println("${NavigationController.ANSI_CYAN}Type /help or ? for chat commands${NavigationController.ANSI_RESET}\n")
    
    var shouldReturnToMenu = false

    while (!shouldReturnToMenu) {
        print("${NavigationController.ANSI_BOLD}You:${NavigationController.ANSI_RESET} ")
        val rawInput = readlnOrNull() ?: break
        val chatInput = parseChatInput(rawInput)
        
        when (chatInput.command) {
            ChatCommand.BACK_TO_MENU -> {
                println("${NavigationController.ANSI_GREEN}📋 Returning to main menu...${NavigationController.ANSI_RESET}")
                shouldReturnToMenu = true
                break
            }
            ChatCommand.EXIT_APP -> {
                println("${NavigationController.ANSI_YELLOW}👋 Goodbye!${NavigationController.ANSI_RESET}")
                client.close()
                return false // Signal to exit the application
            }
            ChatCommand.HELP -> {
                showChatHelp()
                continue // Don't add help command to conversation
            }
            ChatCommand.CONTINUE -> {
                // Process the actual chat message
                val userMessage = chatInput.message ?: continue
                conversation.add(Message("user", userMessage))
                
                try {
                    val httpResponse = when (config.provider) {
                        "anthropic" -> {
                            val requestBody = AnthropicRequestBody(config.model, conversation, 1024)
                            client.post(config.url) {
                                header("x-api-key", config.apiKey)
                                header("anthropic-version", config.anthropicVersion ?: "2023-06-01")
                                contentType(ContentType.Application.Json)
                                setBody(requestBody)
                            }
                        }
                        "openrouter" -> {
                            val requestBody = OpenRouterRequestBody(config.model, conversation, 1024)
                            client.post(config.url) {
                                header("Authorization", "Bearer ${config.apiKey}")
                                config.siteUrl?.let { header("HTTP-Referer", it) }
                                config.appName?.let { header("X-Title", it) }
                                contentType(ContentType.Application.Json)
                                setBody(requestBody)
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown provider: ${config.provider}")
                    }
                    
                    val responseText = httpResponse.body<String>()
                    
                    // Check for OpenRouter errors
                    if (config.provider == "openrouter" && responseText.contains("\"error\"")) {
                        if (responseText.contains("\"code\":404") && responseText.contains("No endpoints found")) {
                            println("❌ Model not available: ${config.model}")
                            println("💡 This model might be discontinued or temporarily unavailable.")
                        } else if (responseText.contains("\"code\":400") && responseText.contains("not a valid model ID")) {
                            println("❌ Invalid model ID: ${config.model}")
                            println("💡 Please check the model name format.")
                        } else {
                            println("❌ OpenRouter API Error: ${responseText}")
                        }
                        println("\n🔄 Try these working alternatives:")
                        println("   - google/gemini-2.5-flash-lite (Google Gemini)")
                        println("   - openai/gpt-4o-mini (OpenAI GPT-4o Mini)")
                        println("   - anthropic/claude-3.5-sonnet (Claude 3.5 Sonnet)")
                        println("   - qwen/qwen3-coder:free (Qwen Coder - Free)")
                        println("   - z-ai/glm-4.5-air:free (GLM 4.5 Air - Free)")
                        println("\n💭 Type /menu to go back and change your model.")
                        continue
                    }
                    
                    val assistantResponse = when (config.provider) {
                        "anthropic" -> {
                            // Check if it's an error response
                            if (responseText.contains("\"type\":\"error\"")) {
                                val errorResponse = Json.decodeFromString<ErrorResponse>(responseText)
                                println("❌ API Error: ${errorResponse.error.message}")
                                continue
                            }
                            
                            // Parse as successful Anthropic response
                            val response = Json.decodeFromString<AnthropicApiResponse>(responseText)

                            // Process response content
                            response.content.forEach { contentBlock ->
                                if (contentBlock.type == "text") {
                                    println("${NavigationController.ANSI_BOLD}Assistant:${NavigationController.ANSI_RESET} ${contentBlock.text}")
                                }
                            }

                            // Extract assistant response text
                            response.content
                                .filter { it.type == "text" }
                                .joinToString("") { it.text }
                        }
                        "openrouter" -> {
                            // Parse as OpenRouter response
                            val response = Json.decodeFromString<OpenRouterApiResponse>(responseText)
                            
                            if (response.choices.isEmpty()) {
                                println("❌ API Error: No response choices received")
                                continue
                            }
                            
                            val choice = response.choices[0]
                            val assistantMessage = choice.message.content
                            
                            // Display reasoning if available
                            choice.message.reasoning?.let { reasoning ->
                                println("🧠 Model Reasoning:")
                                println("$reasoning")
                                println("---")
                            }
                            
                            // Display reasoning details if available
                            choice.message.reasoning_details?.let { reasoningDetails ->
                                println("🔍 Reasoning Details:")
                                reasoningDetails.forEach { detail ->
                                    println("Type: ${detail.type}")
                                    detail.text?.let { text ->
                                        println("Content: $text")
                                    }
                                    println("---")
                                }
                            }
                            
                            println("${NavigationController.ANSI_BOLD}Assistant:${NavigationController.ANSI_RESET} $assistantMessage")
                            
                            assistantMessage
                        }
                        else -> {
                            println("❌ Unknown provider: ${config.provider}")
                            continue
                        }
                    }

                    conversation.add(Message("assistant", assistantResponse))

                } catch (e: Exception) {
                    println("❌ Error in the request: ${e.message}")
                    println("💡 Type /menu to return to the main menu or /help for commands.")
                }
            }
        }
    }
    
    client.close()
    return true // Signal to return to menu
}

fun main() = runBlocking {
    val configFilePath = "config.json".toPath()
    val fileSystem = FileSystem.SYSTEM
    
    var shouldContinue = true
    
    while (shouldContinue) {
        val config: Config = when {
            !fileSystem.exists(configFilePath) -> {
                println("No configuration found. Setting up new API...")
                val newConfig = requestConfigInput()
                saveConfigUsingOkio(newConfig, configFilePath)
                newConfig
            }
            else -> {
                val existingConfig = loadConfigUsingOkio(configFilePath) ?: return@runBlocking
                
                // Create HTTP client early for model browsing
                val client = HttpClient(Darwin) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                        })
                    }
                }
                
                val menuChoice = showStartupMenu(existingConfig)
                when {
                    menuChoice == 2 -> {
                        client.close()
                        val newConfig = requestConfigInput()
                        saveConfigUsingOkio(newConfig, configFilePath)
                        newConfig
                    }
                    menuChoice == 3 -> {
                        val updatedConfig = changeModelOnly(existingConfig)
                        client.close()
                        saveConfigUsingOkio(updatedConfig, configFilePath)
                        updatedConfig
                    }
                    menuChoice == 4 && existingConfig.provider == "openrouter" -> {
                        val updatedConfig = selectModelFromList(existingConfig, client)
                        client.close()
                        saveConfigUsingOkio(updatedConfig, configFilePath)
                        updatedConfig
                    }
                    (menuChoice == 4 && existingConfig.provider != "openrouter") || menuChoice == 5 -> {
                        client.close()
                        val newConfig = requestConfigInput()
                        saveConfigUsingOkio(newConfig, configFilePath)
                        newConfig
                    }
                    else -> {
                        client.close()
                        existingConfig
                    }
                }
            }
        }

        println("${NavigationController.ANSI_GREEN}✅ Configuration loaded: ${config.provider.uppercase()} API with model ${config.model}${NavigationController.ANSI_RESET}")
        
        // Run the chat session and check if we should continue or exit
        shouldContinue = runChatSession(config)
    }
}