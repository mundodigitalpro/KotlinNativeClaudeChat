# Aplicación de Chat en Kotlin con Gestión de Configuración

Esta aplicación de chat basada en Kotlin demuestra cómo integrar un cliente HTTP con servicios API externos utilizando Ktor, gestionar configuraciones usando Okio y serializar/deserializar datos JSON. Interactúa con el servicio API de Anthropic Claude , enviando mensajes y recibiendo respuestas.

## Características

- Solicitudes HTTP a servicios externos con cliente Ktor y motor WinHttp.
- Gestión de configuración (carga/guardado) usando Okio.
- Serialización/deserialización JSON con kotlinx.serialization.
- Chat interactivo con API externa.

## Prerrequisitos

- Compilador y entorno de ejecución de Kotlin.
- Gradle u otra herramienta de construcción que soporte proyectos Kotlin.

## Instalación

1. Clona el repositorio en tu máquina local.

```bash
git clone <url-del-repositorio>
```

2. Navega al directorio clonado.

```bash
cd <directorio-clonado>
```

3. Construye el proyecto usando Gradle.

```bash
gradle build
```

4. Ejecuta la aplicación.

```bash
gradle run
```

## Uso

Al ejecutar la aplicación, se te solicitará que ingreses detalles de configuración como la versión de la API de Anthropic, la clave API, el modelo y la URL. Estos detalles son necesarios para que la aplicación interactúe con el servicio API de chat externo.

Si existe un archivo de configuración llamado `config.json` en el directorio de la aplicación, se carga esta configuración automáticamente. De lo contrario, se solicita al usuario que ingrese los detalles de configuración y se guardan para uso futuro.

Para interactuar con el servicio de chat, simplemente escribe tus mensajes en la consola después del indicador "You: ". La aplicación enviará tu mensaje al servicio de chat y mostrará la respuesta.

## Configuración

La aplicación utiliza un archivo `config.json` para almacenar los detalles de configuración. Este archivo incluye:

- `anthropicVersion`: Versión de la API. Ejemplo: "2023-06-01"
- `anthropicApiKey`: Clave API para autenticación. Ejemplo: "sk-ant-api00"
- `model`: Identificador del modelo para el servicio de chat. Ejemplo:"claude-3-haiku-20240307"
- `url`: URL del servicio API de chat. Ejemplo: "https://api.anthropic.com/v1/messages"

## Contribuir

Las contribuciones al proyecto son bienvenidas. Por favor, sigue estos pasos para contribuir:

1. Haz fork del repositorio.
2. Crea una nueva rama para tu característica (`git checkout -b feature/FuncionalidadIncreíble`).
3. Haz commit de tus cambios (`git commit -am 'Añadir alguna FuncionalidadIncreíble'`).
4. Empuja a la rama (`git push origin feature/FuncionalidadIncreíble`).
5. Abre un Pull Request.

## Licencia

Este proyecto está licenciado bajo la Licencia Apache - vea el archivo LICENSE.md para detalles.