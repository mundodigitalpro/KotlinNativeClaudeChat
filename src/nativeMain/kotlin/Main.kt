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
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

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
    print("Enter Anthropic API version (e.g., 2023-06-01): ")
    val version = readlnOrNull() ?: "2023-06-01"
    
    print("Enter your Anthropic API key: ")
    val apiKey = readlnOrNull() ?: ""
    
    print("Enter model name (e.g., claude-3-sonnet-20240229): ")
    val model = readlnOrNull() ?: "claude-3-sonnet-20240229"
    
    print("Enter API URL (e.g., https://api.anthropic.com/v1/messages): ")
    val url = readlnOrNull() ?: "https://api.anthropic.com/v1/messages"
    
    return Config(version, apiKey, model, url)
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

@Serializable
data class Config(
    val anthropicVersion: String,
    val anthropicApiKey: String,
    val model: String,
    val url: String
)


@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class RequestBody(val model: String, val messages: List<Message>, val max_tokens: Int)

// ESTRUCTURAS CORREGIDAS
@Serializable
data class ContentBlock(
    val type: String, // "text"
    val text: String
)

@Serializable
data class ApiResponse(
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
data class ApiError(
    val type: String,
    val message: String
)

@Serializable
data class ErrorResponse(
    val type: String,
    val error: ApiError
)

@Serializable
data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)


@OptIn(ExperimentalNativeApi::class)
fun main() = runBlocking {

    val configFilePath = "config.json".toPath()
    val fileSystem = FileSystem.SYSTEM

    val config: Config = if (fileSystem.exists(configFilePath)) {
        // Carga la configuración desde el archivo
        loadConfigUsingOkio(configFilePath) ?: return@runBlocking
    } else {
        // Solicita los datos y guarda la configuración
        val newConfig = requestConfigInput()
        saveConfigUsingOkio(newConfig, configFilePath)
        newConfig
    }

    // Procede con la ejecución normal de la aplicación
    println("Configuración cargada: $config")

    // Create HTTP client with platform-specific engine
    val client = createPlatformHttpClient()

    val conversation = mutableListOf<Message>()

    while (true) {
        print("You: ")
        val userInput = readlnOrNull() ?: break
        conversation.add(Message("user", userInput))
        val requestBody = RequestBody(config.model, conversation, 1024)

        try {
            val httpResponse = client.post(config.url) {
                header("x-api-key", config.anthropicApiKey)
                header("anthropic-version", config.anthropicVersion)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val responseText = httpResponse.body<String>()
            
            // Check if it's an error response
            if (responseText.contains("\"type\":\"error\"")) {
                val errorResponse = Json.decodeFromString<ErrorResponse>(responseText)
                println("API Error: ${errorResponse.error.message}")
                break
            }
            
            // Parse as successful response
            val response = Json.decodeFromString<ApiResponse>(responseText)

            // Process response content
            response.content.forEach { contentBlock ->
                if (contentBlock.type == "text") {
                    println("Assistant: ${contentBlock.text}")
                }
            }

            // Add assistant response to conversation
            val assistantResponse = response.content
                .filter { it.type == "text" }
                .joinToString("") { it.text }

            conversation.add(Message("assistant", assistantResponse))
        } catch (e: Exception) {
            println("Error in the request: ${e.message}")
            break
        }
    }
    client.close()
}


