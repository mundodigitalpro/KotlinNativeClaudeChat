# Plan de Mejora para KotlinNativeClaudeChat

## 1. Introducción

Este documento describe un plan estratégico para refactorizar y mejorar el proyecto `KotlinNativeClaudeChat`. El objetivo principal es transformar la base del código actual, centrada en un único fichero, hacia una arquitectura más modular, mantenible y robusta, aplicando principios de diseño de software consolidados.

## 2. Objetivos Principales

*   **Mejorar la Modularidad:** Separar las responsabilidades del código en componentes lógicos.
*   **Aumentar la Mantenibilidad:** Facilitar la corrección de errores y la adición de nuevas funcionalidades.
*   **Incrementar la Robustez:** Mejorar el manejo de errores y la gestión de configuraciones sensibles.
*   **Facilitar las Pruebas:** Permitir la creación de pruebas unitarias aisladas para cada componente.

---

## 3. Plan de Acción

### 3.1. Refactorización del Monolito `Main.kt`

**Problema:** Actualmente, `src/nativeMain/kotlin/Main.kt` contiene la mayor parte de la lógica de la aplicación (UI, llamadas de red, gestión de configuración, manejo del estado del chat). Esto viola el Principio de Responsabilidad Única y dificulta la escalabilidad.

**Solución:** Descomponer `Main.kt` en varios ficheros/clases, cada uno con una responsabilidad clara.

*   **`ConfigManager.kt`:**
    *   **Responsabilidad:** Cargar, guardar y gestionar el fichero `config.json`. Contendrá la lógica de serialización (usando `kotlinx.serialization`) y de I/O de ficheros (usando `Okio`).
    *   **Acciones:** Mover las funciones relacionadas con `config.json` desde `Main.kt` a esta nueva clase.

*   **`ApiClient.kt` / `ChatService.kt`:**
    *   **Responsabilidad:** Gestionar toda la comunicación con las APIs externas (Anthropic, OpenRouter). Encapsulará la configuración y el uso del cliente Ktor.
    *   **Acciones:** Mover las data classes de las peticiones/respuestas (`AnthropicRequest`, `OpenRouterRequest`, etc.) y la lógica de red a este fichero. Debería exponer métodos como `suspend fun getClaudeResponse(history: List<Message>): Result<ApiResponse>`.

*   **`TerminalUI.kt` / `View.kt`:**
    *   **Responsabilidad:** Manejar toda la interacción con el usuario en la terminal. Esto incluye imprimir menús, recibir entradas del usuario, y mostrar las respuestas del chat y los mensajes de estado.
    *   **Acciones:** Mover las funciones `println`, `readLine`, y la lógica de navegación por menús a esta clase.

*   **`ChatController.kt`:**
    *   **Responsabilidad:** Orquestar el flujo de la aplicación. Mantendrá el estado de la conversación (el historial de mensajes) y coordinará los otros componentes.
    *   **Acciones:** `Main.kt` se reducirá a su mínima expresión, probablemente solo para inicializar `ChatController` y lanzar el bucle principal de la aplicación. El bucle `while (true)` principal residirá aquí.

### 3.2. Gestión Segura de Claves de API

**Problema:** Almacenar claves de API directamente en `config.json` es una mala práctica de seguridad.

**Solución:** Modificar `ConfigManager` para que priorice la lectura de claves desde variables de entorno del sistema.

*   **Acciones:**
    1.  Al iniciar, la aplicación intentará leer `ANTHROPIC_API_KEY` y `OPENROUTER_API_KEY` desde las variables de entorno.
    2.  Si no existen, como fallback, buscará en el fichero `config.json`.
    3.  Actualizar la documentación (`README.md`) para reflejar este nuevo método como la forma recomendada de configurar las claves.

### 3.3. Expansión de la Cobertura de Pruebas

**Problema:** Un único fichero `Test.kt` es insuficiente para garantizar la calidad del código, especialmente después de la refactorización.

**Solución:** Crear pruebas unitarias para los nuevos componentes.

*   **Acciones:**
    1.  Crear `ApiClientTest.kt` para probar la lógica de red, usando un `MockEngine` de Ktor para simular respuestas de la API sin realizar llamadas reales.
    2.  Crear `ConfigManagerTest.kt` para probar la lógica de carga y guardado de la configuración, usando ficheros temporales.
    3.  Crear `ChatControllerTest.kt` para probar la lógica de estado del chat (manejo de historial, comandos como `/exit`, etc.).

### 3.4. Mejora del Manejo de Errores

**Problema:** La aplicación debe ser resiliente a fallos de red, errores de API (ej. 401, 429, 500), o un `config.json` malformado.

**Solución:** Implementar un manejo de errores más granular y proporcionar feedback claro al usuario.

*   **Acciones:**
    1.  En `ApiClient`, envolver las llamadas de red en bloques `try-catch` para capturar excepciones de Ktor (ej. `HttpRequestTimeoutException`, `ClientRequestException`).
    2.  Utilizar la clase `Result<T>` de Kotlin para propagar los éxitos o fallos desde las capas de servicio (`ApiClient`) hacia el `ChatController`.
    3.  En `TerminalUI`, crear funciones específicas para mostrar mensajes de error de forma clara y consistente al usuario.

### 3.5. Actualización de Dependencias

**Problema:** Las dependencias pueden quedar desactualizadas, introduciendo bugs o vulnerabilidades.

**Solución:** Revisar y actualizar las dependencias del proyecto.

*   **Acciones:**
    1.  Ejecutar `./gradlew dependencyUpdates` para identificar dependencias obsoletas.
    2.  Actualizar las versiones en `build.gradle.kts` a las últimas versiones estables.
    3.  Ejecutar `./gradlew allTests` para asegurar que las actualizaciones no han roto la funcionalidad existente.
