# Kotlin Native Multi-API Chat Client

Esta aplicación de chat desarrollada en Kotlin Native permite interactuar con múltiples proveedores de IA a través de una sola aplicación. Soporta tanto la API de Anthropic Claude como OpenRouter (que da acceso a más de 400 modelos de IA), utilizando Ktor para HTTP, Okio para gestión de archivos y kotlinx.serialization para JSON.

## 🚀 Características

- **Multi-Proveedor**: Soporte para Anthropic Claude y OpenRouter APIs
- **400+ Modelos de IA**: Acceso a modelos de OpenAI, Anthropic, Google, Mistral y más a través de OpenRouter
- **Configuración Dinámica**: Menú interactivo para seleccionar y configurar proveedores
- **Multiplataforma**: Compatible con macOS, Linux y Windows (Kotlin Native)
- **Gestión de Configuración**: Carga/guardado automático usando Okio
- **Serialización JSON**: Manejo robusto de respuestas con kotlinx.serialization
- **Chat Interactivo**: Conversación en tiempo real con historial

## 🤖 Proveedores Soportados

### Anthropic Claude
- Acceso directo a modelos Claude (3.5 Sonnet, 3.5 Haiku, etc.)
- Autenticación con API key de Anthropic
- Formato nativo de Anthropic API

### OpenRouter
- Acceso a 400+ modelos de IA incluyendo:
  - **OpenAI**: GPT-4o, GPT-4o-mini, ChatGPT
  - **Anthropic**: Claude 3.5 Sonnet, Claude 3 Haiku
  - **Google**: Gemini 2.0 Flash, Gemini Pro
  - **Mistral**: Mistral Large, Codestral
  - **Meta**: Llama modelos
  - **Y muchos más...**
- Una sola API key para todos los modelos
- Tracking opcional de aplicaciones

## Prerrequisitos

- Compilador y entorno de ejecución de Kotlin.
- Gradle u otra herramienta de construcción que soporte proyectos Kotlin.

## 📦 Instalación

1. Clona el repositorio en tu máquina local:

```bash
git clone <url-del-repositorio>
cd KotlinNativeClaudeChat
```

2. Construye el proyecto usando Gradle:

```bash
./gradlew build
```

3. Ejecuta la aplicación:

```bash
./gradlew runDebugExecutableNative
```

## 💬 Uso

### Primer Uso
Al ejecutar la aplicación por primera vez, verás el menú de selección:

```
=== Kotlin Native AI Chat ===
1. Use existing configuration
2. Configure new API
3. Reconfigure existing setup
Enter choice (1, 2, or 3): 2
```

### Configurar OpenRouter
```
Select API Provider:
1. Anthropic (Claude)
2. OpenRouter (Multiple AI Models)
Enter choice (1 or 2): 2

Enter your OpenRouter API key: sk-or-v1-tu-api-key-aqui

Popular OpenRouter models:
- openai/gpt-4o
- anthropic/claude-3.5-sonnet
- google/gemini-2.0-flash-exp
- mistralai/mistral-large
Enter model name: openai/gpt-4o-mini

Enter your app/site name (optional): Mi App de Chat
Enter your site URL (optional): https://mi-sitio.com

Configuration saved to config.json
Configuration loaded: OPENROUTER API with model openai/gpt-4o-mini
```

### Configurar Anthropic Claude
```
Select API Provider:
1. Anthropic (Claude)
2. OpenRouter (Multiple AI Models)
Enter choice (1 or 2): 1

Enter Anthropic API version (e.g., 2023-06-01): 2023-06-01
Enter your Anthropic API key: sk-ant-tu-api-key-aqui
Enter model name (e.g., claude-3-5-haiku-20241022): claude-3-5-sonnet-20241022

Configuration loaded: ANTHROPIC API with model claude-3-5-sonnet-20241022
```

### Chatear
Una vez configurado, puedes chatear normalmente:

```
You: ¡Hola! ¿Cómo estás?
Assistant: ¡Hola! Estoy muy bien, gracias por preguntar. ¿En qué puedo ayudarte hoy?

You: ¿Puedes resolver 15 + 27?
Assistant: 15 + 27 = 42

You: [Presiona Enter para salir]
```

## ⚙️ Configuración

La aplicación utiliza un archivo `config.json` para almacenar la configuración. Formato según el proveedor:

### Configuración de Anthropic
```json
{
  "provider": "anthropic",
  "anthropicVersion": "2023-06-01",
  "apiKey": "sk-ant-tu-api-key",
  "model": "claude-3-5-sonnet-20241022",
  "url": "https://api.anthropic.com/v1/messages"
}
```

### Configuración de OpenRouter
```json
{
  "provider": "openrouter",
  "apiKey": "sk-or-v1-tu-api-key",
  "model": "openai/gpt-4o-mini",
  "url": "https://openrouter.ai/api/v1/chat/completions",
  "appName": "Mi App de Chat",
  "siteUrl": "https://mi-sitio.com"
}
```

### Campos de Configuración

| Campo | Descripción | Obligatorio |
|-------|-------------|-------------|
| `provider` | Proveedor API ("anthropic" o "openrouter") | ✅ |
| `apiKey` | Clave API para autenticación | ✅ |
| `model` | Identificador del modelo de IA | ✅ |
| `url` | URL del endpoint API | ✅ |
| `anthropicVersion` | Versión de API Anthropic | ✅ (solo Anthropic) |
| `appName` | Nombre de tu aplicación | ❌ (solo OpenRouter) |
| `siteUrl` | URL de tu sitio web | ❌ (solo OpenRouter) |

## 🔑 Obtener API Keys

### OpenRouter
1. Visita [openrouter.ai](https://openrouter.ai)
2. Crea una cuenta
3. Ve a la sección API Keys
4. Genera una nueva API key
5. **Ventaja**: Una sola key para 400+ modelos

### Anthropic
1. Visita [console.anthropic.com](https://console.anthropic.com)
2. Crea una cuenta
3. Ve a API Keys
4. Genera una nueva API key

## 🏗️ Arquitectura del Proyecto

```
src/nativeMain/kotlin/
└── Main.kt                 # Aplicación principal
    ├── Config              # Gestión de configuración
    ├── Message             # Estructura de mensajes
    ├── AnthropicApiResponse # Respuestas de Anthropic
    ├── OpenRouterApiResponse # Respuestas de OpenRouter
    └── main()              # Función principal
```

## 🛠️ Comandos de Desarrollo

```bash
# Compilar solamente
./gradlew compileKotlinNative

# Construir todo
./gradlew build

# Ejecutar en modo debug
./gradlew runDebugExecutableNative

# Ejecutar en modo release
./gradlew runReleaseExecutableNative

# Limpiar build
./gradlew clean

# Ejecutar tests
./gradlew test
```

## 🔧 Solución de Problemas

### Error de Compilación
```bash
# Limpiar y reconstruir
./gradlew clean build
```

### Error de API Key Inválida
- Verifica que tu API key sea correcta
- Para OpenRouter: debe empezar con `sk-or-v1-`
- Para Anthropic: debe empezar con `sk-ant-`

### Error de Modelo No Encontrado
- Para OpenRouter: verifica el formato `proveedor/modelo` (ej: `openai/gpt-4o-mini`)
- Para Anthropic: usa nombres oficiales (ej: `claude-3-5-sonnet-20241022`)

### Reconfigurar API
```bash
# Eliminar configuración existente
rm config.json
./gradlew runDebugExecutableNative
# Selecciona opción 2 para nueva configuración
```

## 🤝 Contribuir

Las contribuciones son bienvenidas. Para contribuir:

1. Haz fork del repositorio
2. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
3. Haz commit de tus cambios: `git commit -am 'Añadir nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

## 📋 Próximas Funcionalidades

- [ ] Soporte para más proveedores (Cohere, Together.ai)
- [ ] Modo streaming de respuestas
- [ ] Guardado de historial de conversaciones
- [ ] Interfaz gráfica (Compose Multiplatform)
- [ ] Configuración de parámetros del modelo (temperatura, max tokens)

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia Apache 2.0 - ver el archivo LICENSE.md para más detalles.

---

**¿Necesitas ayuda?** Abre un issue en GitHub o revisa la documentación de [OpenRouter](https://openrouter.ai/docs) y [Anthropic](https://docs.anthropic.com).