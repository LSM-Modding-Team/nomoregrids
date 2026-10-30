# No More Grids

Implementación experimental de construcción por componentes para Minecraft Java **26.2**, NeoForge **26.2.0.88** y Java **25**. Posiciones en unidades de 1/16, 24 rotaciones y varias piezas por casilla, con identidad y autoridad únicas.

| Identidad | Valor |
|---|---|
| Paquete y grupo Maven | net.nicomar.nomoregrids |
| Mod ID | nomoregrids |
| Entrada común | NoMoreGrids |
| Versión | 0.1.0-SNAPSHOT |
| Herramientas fijadas | Gradle 9.2.1 / ModDevGradle 2.0.148 |

Hay código en todas las fases F0–F9. La construcción incorpora servidor, contenedores, diario durable, recibos de inventario, secciones, red fragmentada, controles, contorno por componente y render. También hay puentes parciales para cavidades/fluidos, herramientas Paintable Blocks, barriles/hornos y pistones, junto con compactación por generaciones y métricas. **No están terminadas todas las fases**; quedan integraciones de código y validación. El detalle está en [docs/ESTADO_DESARROLLO.md](docs/ESTADO_DESARROLLO.md).

**No se ha compilado, ejecutado pruebas Java ni iniciado Minecraft.** La edición experimental requiere `/nmg enable` con permisos de propietario, por sesión y dimensión, o la propiedad `-Dnomoregrids.experimental=true`. No habilitarla en un mundo valioso sin completar la validación. Las capacidades no integradas rechazan la operación; los datos desconocidos se conservan como sustitutos protegidos.

Abrir la raíz como proyecto Gradle con JDK 25. [docs/PRIMEROS_PASOS.md](docs/PRIMEROS_PASOS.md) describe los comandos para cuando se autorice ejecutarlos. AGENTS.md mantiene la restricción de compilación y arranque solicitada por el usuario.

La especificación está en `docs/obsidian/NoMoreGrids`: abrirla como bóveda Obsidian y comenzar por `00 Inicio/Inicio.md`. Consultar [estructura](docs/ESTRUCTURA.md) y [evidencia](docs/evidence/IMPLEMENTACION_FASES.md).

Comprobaciones sin compilación: `python tools/check_repository.py`, `python tools/check_java_syntax.py` (requiere las dependencias indicadas en el script) y `git diff --check`. Son revisión estructural y sintáctica, sin comprobación de tipos ni certificación del mod.
