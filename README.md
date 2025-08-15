# Kotlin Native Claude Chat - Multiplataforma

Esta aplicación implementa un cliente de chat multiplataforma para APIs de IA usando Kotlin Native, con compatibilidad automática para macOS, Windows y Linux.

## ✅ Solución Multiplataforma Implementada

Hemos fusionado exitosamente los cambios de la rama `macos` a `master`, creando una base multiplataforma que incluye:

### Características Integradas
- **Detección automática de plataforma** con motores HTTP específicos
- **Mejoras en manejo de errores** con estructuras `ErrorResponse` y `ApiError` 
- **Funciones de configuración mejoradas** usando Okio
- **Compatibilidad completa** con todos los cambios de la rama macos
- **Documentación completa** (CLAUDE.md)

### Estado Actual por Plataforma
- ✅ **macOS**: Funcional con detección automática y motor Darwin
- ✅ **Windows**: Detección automática configurada con motor WinHttp  
- ✅ **Linux**: Detección automática configurada con motor CIO
- ✅ **Multiplataforma**: **Versión única que funciona en todas las plataformas**

## 🎯 Detección Automática Implementada

**¡La aplicación ahora detecta automáticamente la plataforma y usa el motor HTTP apropiado!**

### Funcionamiento Automático
- **macOS**: Detecta `MACOSX` → Usa motor `Darwin`
- **Windows**: Detecta `MINGW`/`WINDOWS` → Usa motor `WinHttp`  
- **Linux**: Detecta `LINUX` → Usa motor `CIO`
- **Otros**: Fallback → Usa motor `CIO`

### Ventajas de la Versión Única
- ✅ **Un solo código fuente** para todas las plataformas
- ✅ **Build automático** según la plataforma de compilación
- ✅ **Sin configuración manual** requerida
- ✅ **Mantenimiento simplificado**

### Evidencia de Funcionamiento
```
Detected platform: MACOSX
Using Darwin HTTP engine for macOS
```

## 🚀 Comandos de Desarrollo

```bash
# Construir proyecto
./gradlew build

# Ejecutar en modo debug (recomendado para desarrollo)
./gradlew runDebugExecutableNative

# Ejecutar en modo release (optimizado)
./gradlew runReleaseExecutableNative

# Limpiar artifacts
./gradlew clean
```

## ✨ Implementación Completa Lograda

### Cambios Integrados de la Rama macOS
- **Mejoras en configuración**: Funciones optimizadas de carga/guardado con Okio
- **Estructuras API corregidas**: `ContentBlock`, `ErrorResponse`, `ApiError`  
- **Manejo de errores mejorado**: Detección y procesamiento de errores de API
- **Documentación completa**: Archivo CLAUDE.md con guías detalladas

### Nueva Funcionalidad Multiplataforma  
- **Detección automática de plataforma**: `@OptIn(ExperimentalNativeApi::class)`
- **Selección inteligente de motores HTTP**: Darwin/WinHttp/CIO según OS
- **Build system optimizado**: Dependencias específicas por plataforma
- **Versión única universal**: Funciona en macOS, Windows y Linux

## 💡 Uso

**La aplicación funciona igual en todas las plataformas:**

1. **Detección automática**: La aplicación detecta tu OS y selecciona el motor HTTP apropiado
2. **Configuración inteligente**: Carga `config.json` si existe, sino solicita datos al usuario
3. **Chat universal**: Funciona idénticamente en macOS, Windows y Linux

### Para Windows
```bash
# Compila automáticamente con motor WinHttp
./gradlew runDebugExecutableNative
```

### Para Linux  
```bash  
# Compila automáticamente con motor CIO
./gradlew runDebugExecutableNative
```

### Para macOS
```bash
# Compila automáticamente con motor Darwin (actual)
./gradlew runDebugExecutableNative
```

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