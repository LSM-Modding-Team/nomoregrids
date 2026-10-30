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
| src/test | Pruebas JUnit del núcleo escritas, no ejecutadas |
| src/gametest | Reserva para integración futura; no conectada a Gradle |
| docs/obsidian/NoMoreGrids | Bóveda completa y actualización del estado |
| docs/evidence | Reserva para evidencia ejecutada |
| tools | Comprobaciones estáticas de estructura y sintaxis, sin compilación |

Ahora existen PieceRecord, geometría de cajas e índices puros de región, más tests escritos. No hay adaptadores, contenedores de Minecraft ni registros de contenido. Ver docs/evidence para el avance parcial de F1. Los archivos .gitkeep conservan carpetas en Git y se excluyen del JAR de recursos.

El repositorio usa Git. No se incluyen binarios compilados del mod, carpetas run/build ni credenciales. El único JAR suministrado es el wrapper oficial de Gradle, no un mod de Minecraft.

## Núcleo parcial de F1

- `core`: coordenadas, ResourceId, BinaryData, BoxGeometrySnapshot y PieceRecord.
- `geometry`: rotaciones, cajas, cuantización, formas canónicas y selección por rayo.
- `world`: PieceIndex inmutable, cambios por lote y rechazos tipados; sin acceso a Minecraft.
- `src/test/java`: pruebas JUnit escritas, no ejecutadas.
- `tools/check_repository.py`: comprobación estructural sin compilación ni juego.
- `persistence/BoxPieceCodec`: serialización acotada de registros de cajas, sin acceso al mundo o disco.
