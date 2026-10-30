# Primeros pasos

## Requisitos

JDK 25; Internet para Gradle, dependencias NeoForge/Minecraft y plugins. El proyecto no incluye dependencias descargadas ni requiere una instalación global de Gradle. El resolver Foojay de la plantilla puede descargar toolchains, pero se recomienda configurar JDK 25 explícitamente.

## Comandos desde la raíz

| Objetivo | Windows PowerShell | macOS/Linux |
|---|---|---|
| Verificar Gradle/JVM | .\gradlew.bat --version | ./gradlew --version |
| Compilar base | .\gradlew.bat build | ./gradlew build |
| Cliente de desarrollo | .\gradlew.bat runClient | ./gradlew runClient |
| Servidor de desarrollo | .\gradlew.bat runServer | ./gradlew runServer |
| Generación de recursos | .\gradlew.bat runData | ./gradlew runData |
| Listar tareas | .\gradlew.bat tasks | ./gradlew tasks |

Son instrucciones para el desarrollo posterior: no se ejecutaron para producir este ZIP. runData no genera contenido de NMG hasta que existan proveedores. No hay GameTests ni un source set gametest conectado todavía.

El servidor puede solicitar aceptar la EULA de Minecraft en su directorio de ejecución. No se incluye una aceptación automática.

## Resultado esperado de la base

Cuando se compile y arranque correctamente, NMG aparecerá en la lista de mods y registrará su entrada mínima. Todavía no habrá piezas nuevas, cambios de colocación ni controles activos. El JAR de compilación estará en build/libs.

## IDE y documentación

IntelliJ IDEA: abrir la raíz que contiene settings.gradle e importar Gradle con el wrapper. VS Code: abrir esa misma carpeta y utilizar las extensiones Java/Gradle. No abrir solo src como proyecto independiente.

Obsidian: abrir docs/obsidian/NoMoreGrids como bóveda. El IDE y Obsidian pueden trabajar con los mismos archivos del repositorio.

## Si falla

Revisar que la JVM de Gradle sea 25, que haya acceso a los repositorios y que las versiones sean exactas. Registrar error y fase; no cambiar de versión a ciegas. El esqueleto no incluye un JDK ni cachés de otro equipo.
