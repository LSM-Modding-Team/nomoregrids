# Fuentes y evidencia

## Procedencia de decisiones

La especificación de producto procede del diálogo con el usuario. Las decisiones técnicas son propuestas de diseño aceptadas bajo su instrucción de aceptación automática. Las fuentes externas sirven para entender plataforma e integración; no demuestran que NMG funcione.

Fecha de edición: 2026-10-30, referencia de cliente America/Lima. No se atribuye una fecha futura al estado de los repositorios.

## NeoForge: fuentes primarias

- [Índice oficial NeoForge 26.2.0.88](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.88/): confirma publicación de artefactos.
- [Primer oficial 26.1.x a 26.2](https://docs.neoforged.net/primer/docs/26.2/): guía de migración vanilla, no catálogo completo de extensiones NeoForge.
- [Entidades de bloque](https://docs.neoforged.net/docs/blockentities/): guía general identificada como 26.1 al consultar.
- [Payloads de red](https://docs.neoforged.net/docs/networking/payload/): guía conceptual identificada como 26.1.

La consulta del primer confirma que el render de 26.2 requiere revisión específica. Las páginas generales no verifican nombres ni firmas de todas las operaciones propuestas. No se copiaron sus fragmentos de código.

F0 debe inspeccionar fuentes exactas, metadatos de plataforma y documentación pertinente antes de fijar clases/métodos. Java 25 y versiones Gradle/ModDev elegidas son la línea de partida compartida con la referencia; su resolución se valida en F0.

## Paintable Blocks: fuentes primarias leídas

Base fijada: [commit f3b3998d6a239b776121f3003b6f5a6c052a0a65](https://github.com/LSM-Modding-Team/paintableblocks/tree/f3b3998d6a239b776121f3003b6f5a6c052a0a65).

- [README](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/README.md): estado declarado y plataforma.
- [BlockDesign](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/src/main/java/net/nicomar/paintableblocks/design/BlockDesign.java): geometría, pintura, orientación y pendiente.
- [PaintableBlockEntity](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/src/main/java/net/nicomar/paintableblocks/blockentity/PaintableBlockEntity.java): datos, revisión, formas y sincronización.
- [PaintableBlock](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/src/main/java/net/nicomar/paintableblocks/block/PaintableBlock.java): puente de formas/loot al mundo.
- [PaintableBlockEditEvent](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/src/main/java/net/nicomar/paintableblocks/event/PaintableBlockEditEvent.java): cancelación de edición.
- [Bóveda de referencia](https://github.com/LSM-Modding-Team/paintableblocks/tree/f3b3998d6a239b776121f3003b6f5a6c052a0a65/docs/obsidian/PaintableBlocks): estructura observada en árbol del repositorio.

## Referencia de implementación actual

La lectura posterior para diseños, volúmenes y máquinas PB se fijó en
[26.2, e93bc03f350dfb03b4031f1e479f4a37f28b556e](https://github.com/LSM-Modding-Team/paintableblocks/tree/e93bc03f350dfb03b4031f1e479f4a37f28b556e).
Se consultaron además DesignOperations, VolumeRegion/VolumeOperations,
herramientas, ProductionMenu/BucketMenu, estaciones, máquinas y cubos, sus
block entities e interacciones del servidor. Se conservan arriba los enlaces
iniciales por trazabilidad; el hash vigente del puente está en PaintableAdapter.
No se alteró el repositorio PB ni se copiaron sus fuentes al proyecto.

Para NMG se leyeron los jars oficiales Minecraft 26.2, fuentes NeoForge
26.2.0.88, parches userdev y access transformers; se contrastaron firmas y
bytecode con javap. Estas lecturas y las comprobaciones Python son estáticas,
sin compilar ni ejecutar Java/Minecraft. Los resultados están registrados en
`docs/evidence/IMPLEMENTACION_FASES.md` y `docs/ESTADO_DESARROLLO.md`.

## Referencia secundaria identificada

[Investigación NeoForge del otro proyecto](https://github.com/LSM-Modding-Team/paintableblocks/blob/f3b3998d6a239b776121f3003b6f5a6c052a0a65/docs/technical/neoforge-26.2.md). Su inspección de artefactos y verificaciones son afirmaciones de ese documento, no actividades ejecutadas para NMG.

## Alcance de esta entrega

En la entrega documental inicial se creó documentación original y se validaron archivos/enlaces/ZIP, con un esqueleto mínimo. La implementación posterior está descrita en `docs/ESTADO_DESARROLLO.md` y `docs/evidence/IMPLEMENTACION_FASES.md`; no se ha compilado ni ejecutado No More Grids. Tampoco se probó Paintable Blocks ni se modificó su repositorio. No se incluyeron sus fuentes o recursos en el ZIP.
