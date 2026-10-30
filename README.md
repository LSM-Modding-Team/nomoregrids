# No More Grids

Esqueleto de desarrollo para Minecraft Java **26.2**, NeoForge **26.2.0.88** y Java **25**.

| Identidad | Valor |
|---|---|
| Paquete y grupo Maven | net.nicomar.nomoregrids |
| Mod ID | nomoregrids |
| Clase principal | NoMoreGrids |
| Nombre visible | No More Grids |
| Versión del esqueleto | 0.1.0-SNAPSHOT |

## Estado

Incluye configuración Gradle, wrapper completo, entradas común/cliente, recursos iniciales y documentación de Obsidian. Se inició F1 con coordenadas, rotaciones, uniones de cajas y trazado de selección independientes de Minecraft, más pruebas Java escritas. No hay mecánicas activas de colocación, composición, fluidos ni compatibilidad Paintable Blocks.

Validación realizada: estructura del proyecto, sintaxis de recursos JSON/TOML, coherencia de nombres y versiones, enlaces de documentación, integridad del ZIP y checksum oficial del wrapper. **No se ejecutó Gradle, no se compiló y no se inició Minecraft.**

## Abrir el proyecto

1. Extraer la carpeta NoMoreGrids del ZIP.
2. Instalar/configurar JDK 25 y abrir la carpeta como proyecto Gradle en IntelliJ IDEA, Eclipse o Visual Studio Code con extensiones Java/Gradle.
3. Elegir el wrapper del proyecto y JDK 25 para Gradle.
4. Esperar la descarga inicial de dependencias; requiere conexión a Internet.
5. Consultar docs/PRIMEROS_PASOS.md para los comandos.

En Windows se utiliza .\gradlew.bat; en macOS/Linux, ./gradlew. El ZIP marca gradlew como ejecutable; si el descompresor no conserva el permiso, ejecutar chmod +x gradlew.

## Documentación

Abrir docs/obsidian/NoMoreGrids como bóveda de Obsidian y comenzar por 00 Inicio/Inicio.md. La especificación completa está incluida. El mapa real del esqueleto está en docs/ESTRUCTURA.md.

F0 sigue pendiente de compilación y arranque; F1 es parcial. El estado y las limitaciones están en docs/evidence. `python tools/check_repository.py` comprueba estructura sin compilar ni arrancar Minecraft. Las pruebas Java no se han ejecutado. AGENTS.md exige solicitud explícita para compilar o arrancar.
