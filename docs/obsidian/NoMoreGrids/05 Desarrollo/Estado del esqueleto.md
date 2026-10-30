# Estado del esqueleto

Identidades confirmadas: paquete net.nicomar.nomoregrids, mod ID nomoregrids, clase principal NoMoreGrids y nombre visible No More Grids.

La raíz del proyecto contiene Gradle 9.2.1, ModDevGradle 2.0.148, configuración de Java 25 y NeoForge 26.2.0.88. Incluye wrapper completo, entradas mínima común y cliente, paquetes documentados y traducciones reservadas.

Los metadatos se generan desde src/main/templates/META-INF/neoforge.mods.toml. No hay bloques/ítems de ejemplo, mixins, access transformers, máquinas, protocolo ni controles implementados. Existen registros e índices puros de piezas; no están conectados al mundo. Paintable Blocks no es una dependencia de la base.

Los package-info.java conservan las carpetas sin fingir clases implementadas. La carpeta test contiene pruebas del núcleo escritas; gametest sigue reservada, sin source set conectado. No hay pruebas de mecánicas ejecutadas.

Se verificaron estructura, nombres, recursos, enlaces, checksum del wrapper y ZIP. No se ejecutó Gradle ni Minecraft. F0 sigue pendiente de resolución de dependencias, compilación y arranque dedicado/cliente.

Documentos de raíz: README.md y AGENTS.md. Guías externas a la bóveda: docs/PRIMEROS_PASOS.md, docs/ESTRUCTURA.md.

Siguiente trabajo: [[Plan de fases]]. El diseño completo se conserva en [[Indice completo]].

## Revisión de la base

Ver `docs/evidence/F0-revision-estatica.md`: wrapper restaurado en Git y metadatos alineados con LICENSE (MIT). F0 continúa abierta; no se ejecutaron Gradle ni Minecraft.

## Inicio de F1

Implementados en fuente coordenadas, rotaciones, cajas, cuantización y selección geométrica, sin puente al juego. JUnit está conectado y hay casos escritos, NO EJECUTADOS. Ver `docs/evidence/F1-nucleo-geometrico.md`. F0 y F1 siguen abiertas.

## Identidad e índices de F1

`PieceRecord`, datos opacos inmutables, geometrías de consulta separadas y `PieceIndex` con cambios por lote ya están escritos. No hay transacciones de mundo/inventario. Ver `docs/evidence/F1-identidad-indices.md`; pruebas Java NO EJECUTADAS.
