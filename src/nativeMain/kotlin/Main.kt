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
    println("Select API Provider:")
    println("1. Anthropic (Claude)")
    println("2. OpenRouter (Multiple AI Models)")
    print("Enter choice (1 or 2): ")
    
    return when (readlnOrNull()) {
        "2" -> ApiProvider.OPENROUTER
        else -> ApiProvider.ANTHROPIC
    }
}

fun requestAnthropicConfig(): Config {
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
    
    displayModelsMenu(models)
    
    println("\n📋 Options:")
    println("• Enter a number (1-${models.size}) to select a model")
    println("• Type 'free' to show only free models")
    println("• Type 'search <term>' to search models (e.g., 'search claude')")
    println("• Press Enter to keep current model: ${existingConfig.model}")
    
    print("\nYour choice: ")
    val input = readlnOrNull()?.trim() ?: ""
    
    when {
        input.isEmpty() -> {
            println("✅ Keeping current model: ${existingConfig.model}")
            return existingConfig
        }
        input.lowercase() == "free" -> {
            val freeModels = models.filter { it.isFree }
            println("\n🆓 === FREE MODELS ONLY ===")
            freeModels.forEachIndexed { index, model ->
                println("${index + 1}. ${model.id} - ${model.name}")
            }
            print("\nSelect free model (1-${freeModels.size}): ")
            val freeChoice = readlnOrNull()?.toIntOrNull()
            if (freeChoice != null && freeChoice in 1..freeModels.size) {
                val selectedModel = freeModels[freeChoice - 1]
                println("✅ Selected: ${selectedModel.id} (FREE)")
                return existingConfig.copy(model = selectedModel.id)
            }
        }
        input.lowercase().startsWith("search ") -> {
            val searchTerm = input.substring(7).lowercase()
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
                print("\nSelect model (1-${matchingModels.take(10).size}): ")
                val searchChoice = readlnOrNull()?.toIntOrNull()
                if (searchChoice != null && searchChoice in 1..matchingModels.take(10).size) {
                    val selectedModel = matchingModels[searchChoice - 1]
                    val freeText = if (selectedModel.isFree) " (FREE)" else " (PAID)"
                    println("✅ Selected: ${selectedModel.id}$freeText")
                    return existingConfig.copy(model = selectedModel.id)
                }
            } else {
                println("❌ No models found matching '$searchTerm'")
            }
        }
        else -> {
            val choice = input.toIntOrNull()
            if (choice != null && choice in 1..models.size) {
                val selectedModel = models[choice - 1]
                val freeText = if (selectedModel.isFree) " (FREE)" else " (PAID)"
                println("✅ Selected: ${selectedModel.id}$freeText")
                return existingConfig.copy(model = selectedModel.id)
            }
        }
    }
    
    println("❌ Invalid selection. Keeping current model: ${existingConfig.model}")
    return existingConfig
}

fun changeModelOnly(existingConfig: Config): Config {
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

fun showStartupMenu(config: Config? = null): Int {
    println("=== Kotlin Native AI Chat ===")
    
    config?.let {
        println("Current configuration: ${it.provider.uppercase()} API with model ${it.model}")
        println("1. Use existing configuration")
        println("2. Configure new API")
        println("3. Change model only (keep same API key)")
        if (it.provider == "openrouter") {
            println("4. Browse all OpenRouter models (free/paid)")
            println("5. Reconfigure existing setup")
            print("Enter choice (1, 2, 3, 4, or 5): ")
        } else {
            println("4. Reconfigure existing setup")
            print("Enter choice (1, 2, 3, or 4): ")
        }
    } ?: run {
        println("1. Use existing configuration")
        println("2. Configure new API")
        println("3. Reconfigure existing setup")
        print("Enter choice (1, 2, or 3): ")
    }
    
    return readlnOrNull()?.toIntOrNull() ?: 1
}

fun main() = runBlocking {
    val configFilePath = "config.json".toPath()
    val fileSystem = FileSystem.SYSTEM

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

    println("Configuration loaded: ${config.provider.uppercase()} API with model ${config.model}")

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

    val conversation = mutableListOf<Message>()

    while (true) {
        print("You: ")
        val userInput = readlnOrNull() ?: break
        conversation.add(Message("user", userInput))
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
                println("\n💭 Use option 3 in the main menu to change your model.")
                break
            }
            
            val assistantResponse = when (config.provider) {
                "anthropic" -> {
                    // Check if it's an error response
                    if (responseText.contains("\"type\":\"error\"")) {
                        val errorResponse = Json.decodeFromString<ErrorResponse>(responseText)
                        println("API Error: ${errorResponse.error.message}")
                        break
                    }
                    
                    // Parse as successful Anthropic response
                    val response = Json.decodeFromString<AnthropicApiResponse>(responseText)

                    // Process response content
                    response.content.forEach { contentBlock ->
                        if (contentBlock.type == "text") {
                            println("Assistant: ${contentBlock.text}")
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
                        println("API Error: No response choices received")
                        break
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
                    
                    println("Assistant: $assistantMessage")
                    
                    assistantMessage
                }
                else -> {
                    println("Unknown provider: ${config.provider}")
                    break
                }
            }

            conversation.add(Message("assistant", assistantResponse))

        } catch (e: Exception) {
            println("Error in the request: ${e.message}")
            break
        }
    }
    client.close()
}