# Kotlin Native Multi-API Chat Client

Esta aplicación de chat desarrollada en Kotlin Native permite interactuar con múltiples proveedores de IA a través de una sola aplicación. Soporta tanto la API de Anthropic Claude como OpenRouter (que da acceso a más de 400 modelos de IA), utilizando Ktor para HTTP, Okio para gestión de archivos y kotlinx.serialization para JSON.

## 🚀 Características

- **Multi-Proveedor**: Soporte para Anthropic Claude y OpenRouter APIs
- **400+ Modelos de IA**: Acceso a modelos de OpenAI, Anthropic, Google, Mistral y más a través de OpenRouter
- **Navegador de Modelos**: Lista completa en tiempo real con filtros gratuito/pagado y búsqueda
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
  - **OpenAI**: GPT-4o, GPT-4o-mini, GPT-5 (BYOK), gpt-oss-20b
  - **Anthropic**: Claude 3.5 Sonnet, Claude Opus 4.1
  - **Google**: Gemini 2.5 Flash Lite (actualizado)
  - **Mistral**: Mistral Large, Codestral 2508
  - **Qwen**: Qwen3 Coder (gratis), Qwen3 235B Thinking
  - **Z.AI**: GLM 4.5, GLM 4.5 Air (gratis)
  - **Y muchos más...**
- Una sola API key para todos los modelos
- Tracking opcional de aplicaciones
- Modelos gratuitos disponibles

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
- google/gemini-2.5-flash-lite
- mistralai/mistral-large
- qwen/qwen3-coder:free
- z-ai/glm-4.5-air:free
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

## 🔍 Navegador de Modelos OpenRouter

### Nueva Funcionalidad: Explorar Todos los Modelos Disponibles

Cuando tienes configurado OpenRouter, aparece una nueva opción en el menú:

```
=== Kotlin Native AI Chat ===
Current configuration: OPENROUTER API with model openai/gpt-4o-mini
1. Use existing configuration
2. Configure new API
3. Change model only (keep same API key)
4. Browse all OpenRouter models (free/paid)  ← NUEVA OPCIÓN
5. Reconfigure existing setup
Enter choice (1, 2, 3, 4, or 5): 4
```

### Funcionalidades del Navegador

**🆓 Filtro de Modelos Gratuitos:**
```
🔍 Fetching latest OpenRouter models...

🆓 === FREE MODELS (55) ===
1. deepseek/deepseek-r1:free
   📝 DeepSeek: R1 (free)
   🔤 Context: 163840 tokens

13. google/gemini-2.0-flash-exp:free
   📝 Google: Gemini 2.0 Flash Experimental (free)
   🔤 Context: 1048576 tokens

21. meta-llama/llama-3.1-405b-instruct:free
   📝 Meta: Llama 3.1 405B Instruct (free)
   🔤 Context: 65536 tokens
```

**💰 Información de Modelos Pagados:**
```
💰 === PAID MODELS (259) === (showing first 20)
73. anthropic/claude-3.5-sonnet
   📝 Anthropic: Claude 3.5 Sonnet
   💵 $0.000003/1k prompt tokens, $0.000015/1k completion tokens
   🔤 Context: 200000 tokens
```

**🔍 Opciones de Navegación:**
```
📋 Options:
• Enter a number (1-314) to select a model
• Type 'free' to show only free models
• Type 'search <term>' to search models (e.g., 'search claude')
• Press Enter to keep current model
```

### Ejemplos de Uso

**Buscar modelos específicos:**
```
Your choice: search deepseek

🔍 Search results for 'deepseek':
1. deepseek/deepseek-chat-v3-0324:free [FREE]
2. deepseek/deepseek-r1:free [FREE]
3. deepseek/deepseek-r1-distill-llama-70b:free [FREE]
```

**Ver solo modelos gratuitos:**
```
Your choice: free

🆓 === FREE MODELS ONLY ===
1. deepseek/deepseek-r1:free - DeepSeek: R1 (free)
2. google/gemini-2.0-flash-exp:free - Google: Gemini 2.0 Flash Experimental
3. meta-llama/llama-3.1-405b-instruct:free - Meta: Llama 3.1 405B Instruct
```

### Modelos Destacados Disponibles

**🆓 Modelos Gratuitos Premium:**
- **DeepSeek R1** - Modelo de razonamiento avanzado (163K context)
- **Llama 3.1 405B** - Meta's largest model (65K context)
- **Google Gemini 2.0 Flash** - Latest Google model (1M context)
- **NVIDIA Nemotron Ultra 253B** - High-performance model (131K context)
- **Microsoft MAI DS R1** - Microsoft's reasoning model (163K context)

**💎 Ventajas del Navegador:**
- **Información en tiempo real** directa de OpenRouter API
- **314+ modelos** actualizados automáticamente
- **Precios exactos** para modelos pagados
- **Filtros inteligentes** por proveedor y tipo
- **Contexto y capacidades** de cada modelo

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

### Modelos Actualizados (Enero 2025)
**❌ Modelos Discontinuados:**
- `google/gemini-2.0-flash-exp` → Usar `google/gemini-2.5-flash-lite`
- `google/gemini-pro` → Usar `google/gemini-2.5-flash-lite`

**✅ Modelos Verificados Funcionando:**
- `google/gemini-2.5-flash-lite` - Google Gemini más reciente
- `openai/gpt-4o` - GPT-4o estándar
- `openai/gpt-4o-mini` - GPT-4o optimizado
- `anthropic/claude-3.5-sonnet` - Claude 3.5 Sonnet
- `qwen/qwen3-coder:free` - Especializado en código (gratis)
- `z-ai/glm-4.5-air:free` - Modelo general (gratis)
- `openai/gpt-oss-20b:free` - OpenAI open source (gratis)

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
- [ ] Soporte completo para modelos con reasoning tokens (GLM 4.5)

## 🔄 Actualizaciones Recientes

### v1.3.0 (Enero 2025) - 🔍 Navegador de Modelos
- ✅ **Navegador completo de modelos**: Lista en tiempo real de 314+ modelos OpenRouter
- ✅ **Filtros avanzados**: Separación clara entre modelos gratuitos (55) y pagados (259)
- ✅ **Búsqueda inteligente**: Encuentra modelos por nombre, proveedor o características
- ✅ **Información detallada**: Precios, contexto, descripciones para cada modelo
- ✅ **Acceso a modelos premium gratuitos**: DeepSeek R1, Llama 405B, Gemini 2.0, NVIDIA Nemotron

### v1.2.0 (Enero 2025) - 🔧 Mejoras Base  
- ✅ **Cambio de modelo inteligente**: Nueva opción para cambiar solo el modelo manteniendo la API key
- ✅ **Modelos actualizados**: Lista corregida con modelos verificados funcionando
- ✅ **Manejo de errores mejorado**: Detección específica de errores de OpenRouter con sugerencias
- ✅ **Modelos gratuitos**: Acceso a modelos gratuitos como GLM 4.5 Air y Qwen3 Coder
- ✅ **Google Gemini corregido**: Migración de `gemini-pro` a `gemini-2.5-flash-lite`

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia Apache 2.0 - ver el archivo LICENSE.md para más detalles.

---

**¿Necesitas ayuda?** Abre un issue en GitHub o revisa la documentación de [OpenRouter](https://openrouter.ai/docs) y [Anthropic](https://docs.anthropic.com).