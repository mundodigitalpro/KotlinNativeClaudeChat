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

fun requestConfigInput(): Config {
    println("Por favor, introduce la configuración requerida.")

    print("Anthropic Version: ")
    val version = readln()

    print("API Key: ")
    val apiKey = readln()

    print("Modelo: ")
    val model = readln()

    print("URL: ")
    val url = readln()
    return Config(version, apiKey, model, url)
}

fun loadConfigUsingOkio(path: okio.Path): Config? = try {
    val source = FileSystem.SYSTEM.source(path).buffer()
    val configJson = source.use { it.readUtf8() }
    Json.decodeFromString(Config.serializer(), configJson)
} catch (e: Exception) {
    println("Error al cargar la configuración: ${e.message}")
    null
}

fun saveConfigUsingOkio(config: Config, path: okio.Path) {
    val jsonConfig = Json.encodeToString(Config.serializer(), config)
    val sink = FileSystem.SYSTEM.sink(path).buffer()
    sink.use { it.writeUtf8(jsonConfig) }
    println("Configuración guardada correctamente.")
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

@Serializable
data class ContentItem(
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: JsonObject? = null
)

@Serializable
data class ApiResponse(
    val id: String,
    val content: List<ContentItem>,
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

    val client = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Necesario para manejar campos desconocidos en la respuesta
            })
        }
    }

    val conversation = mutableListOf<Message>()

    while (true) {
        print("You: ")
        val userInput = readlnOrNull() ?: break
        conversation.add(Message("user", userInput))
        val requestBody = RequestBody(config.model, conversation, 1024)

        try {
            val response: ApiResponse = client.post(config.url) {
                header("x-api-key", config.anthropicApiKey)
                header("anthropic-version", config.anthropicVersion)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            response.content.forEach { contentItem ->
                contentItem.text?.let {
                    println("Assistant: $it")
                }
            }

            conversation.add(Message("assistant", response.content.firstOrNull()?.text ?: "No response text found"))
        } catch (e: Exception) {
            println("Error in the request: ${e.message}")
            break
        }
    }
    client.close()
}


