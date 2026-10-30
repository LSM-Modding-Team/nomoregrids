# F0 — revisión estática, 2026-10-29 (America/Lima)

## Alcance y reglas

La solicitud actual autoriza implementar las fases y realizar commits. Se conserva
la restricción explícita de AGENTS.md: no ejecutar Gradle, compiladores, tareas
de construcción ni Minecraft sin petición explícita de ejecución. No se declara
F0 terminada ni se habilitan mecánicas sin sus puertas de aceptación.

No hay regla local de commits solo online ni obligación de push. Se realizan
commits locales coherentes; no se modifica el repositorio de Paintable Blocks.

## Hallazgos y decisiones

- `gradle/` estaba excluido y el wrapper faltaba en Git. Se recupera el JAR
  empacado dentro de la distribución oficial Gradle 9.2.1 disponible en el
  entorno, sin ejecutar Gradle ni generar un wrapper nuevo.
- SHA-256 del wrapper: `423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9`.
  Contrastado con el checksum publicado por services.gradle.org.
- Se conservan las versiones fijadas y el checksum de la distribución en las
  propiedades del wrapper. El checksum del wrapper no sustituye al del ZIP.
- LICENSE ya contiene MIT. Los metadatos se alinean con esa licencia existente;
  no se elige una licencia nueva.
- Existe JDK 25.0.2 bajo `/workspace/.nmg-tools/jdk-25.0.2`; `java -version`
  confirma esa instalación. El Java predeterminado del shell es 21. No se
  introduce en el proyecto una ruta absoluta propia de este entorno.

## Fuentes exactas inspeccionadas

[Fuentes NeoForge 26.2.0.88](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.88/neoforge-26.2.0.88-sources.jar),
SHA-256 `4a2d5079b93659206e3078aee205311f86b549cfbd11eebf74bee36960d5f9e0`.
Descargadas fuera del repositorio y leídas como ZIP, sin ejecutar código.

- `event/level/block/BreakBlockEvent.java`: evento cancelable de rotura separado;
  no asumir el antiguo `BlockEvent.BreakEvent`.
- `event/entity/player/PlayerInteractEvent.java`: `RightClickBlock` cancelable,
  selección mediante `getHitVec()` y decisiones `TriState`.
- `network/event/RegisterPayloadHandlersEvent.java`: `registrar(String version)`.
- `client/event/RegisterKeyMappingsEvent.java`: registro de categoría y tecla;
  utiliza `net.minecraft.resources.Identifier`.
- `client/event/RegisterRenderPipelinesEvent.java`: registro de pipelines.

Estos hallazgos no verifican todas las APIs vanilla, almacenamiento de secciones,
persistencia, selección o render. No se escriben hooks especulativos.

## Verificación y pendientes

Revisión de Git, archivos de configuración, entradas Java, documentación y
checksums. No ejecutados: resolución Gradle, compilación, tests Java, cliente,
dedicado, GameTests ni pruebas de fallo. F0 permanece ABIERTA. F1 solo puede
avanzar en componentes independientes de las APIs de Minecraft hasta resolver
esas comprobaciones; las capacidades del juego continúan deshabilitadas.
