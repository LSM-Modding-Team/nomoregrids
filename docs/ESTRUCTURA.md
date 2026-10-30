# Estructura del esqueleto

| Ruta | Contenido |
|---|---|
| build.gradle / settings.gradle / gradle.properties | Configuración real de la base |
| gradlew / gradlew.bat / gradle/wrapper | Wrapper completo de Gradle 9.2.1 |
| src/main/java/net/nicomar/nomoregrids/NoMoreGrids.java | Entrada común mínima |
| src/main/java/net/nicomar/nomoregrids/client/NoMoreGridsClient.java | Entrada exclusiva del cliente |
| src/main/java/net/nicomar/nomoregrids/**/package-info.java | Paquetes y responsabilidades reservadas |
| src/main/templates/META-INF/neoforge.mods.toml | Única fuente de metadatos, expandida por Gradle |
| src/main/resources/assets/nomoregrids | Idiomas y carpetas de recursos |
| src/main/resources/data/nomoregrids | Carpetas de perfiles, etiquetas, recetas y loot |
| src/generated/resources | Salida futura de generación de recursos |
| src/test | Reserva para pruebas del núcleo |
| src/gametest | Reserva para integración futura; no conectada a Gradle |
| docs/obsidian/NoMoreGrids | Bóveda completa y actualización del estado |
| docs/evidence | Reserva para evidencia ejecutada |
| tools | Reserva para herramientas futuras |

No se incluyen clases PieceRecord, adaptadores, contenedores ni registros simulados. Sus responsabilidades están en la bóveda; se crearán cuando se implemente cada fase. Los archivos .gitkeep conservan carpetas en Git y se excluyen del JAR de recursos.

El repositorio usa Git. No se incluyen binarios compilados del mod, carpetas run/build ni credenciales. El único JAR suministrado es el wrapper oficial de Gradle, no un mod de Minecraft.
