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
    println("- anthropic/claude-3.5-sonnet")
    println("- google/gemini-2.0-flash-exp")
    println("- mistralai/mistral-large")
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
data class Message(
    val role: String, 
    val content: String,
    val refusal: String? = null,
    val reasoning: String? = null
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

fun showStartupMenu(): Int {
    println("=== Kotlin Native AI Chat ===")
    println("1. Use existing configuration")
    println("2. Configure new API")
    println("3. Reconfigure existing setup")
    print("Enter choice (1, 2, or 3): ")
    
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
            when (showStartupMenu()) {
                2, 3 -> {
                    val newConfig = requestConfigInput()
                    saveConfigUsingOkio(newConfig, configFilePath)
                    newConfig
                }
                else -> {
                    loadConfigUsingOkio(configFilePath) ?: return@runBlocking
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
                    
                    val assistantMessage = response.choices[0].message.content
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