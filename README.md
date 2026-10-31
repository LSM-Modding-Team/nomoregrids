# No More Grids

Implementación experimental de construcción por componentes para Minecraft Java **26.2**, NeoForge **26.2.0.88** y Java **25**. Posiciones en unidades de 1/16, 24 rotaciones y varias piezas por casilla, con identidad y autoridad únicas.

| Identidad | Valor |
|---|---|
| Paquete y grupo Maven | net.nicomar.nomoregrids |
| Mod ID | nomoregrids |
| Entrada común | NoMoreGrids |
| Versión | 0.1.0-SNAPSHOT |
| Herramientas fijadas | Gradle 9.2.1 / ModDevGradle 2.0.148 |

El código de F0–F9 conecta construcción, diario/recibos, recuperación, red fragmentada y render con los perfiles de decoración, fluidos, herramientas Paintable Blocks, funcionales/menús/capacidades, grupos, redstone, raíles y pistones mixtos. Incluye compactación automática por generaciones, cachés y métricas. **Compilación y validación parcial ejecutadas; ninguna fase está aceptada globalmente.** El alcance, los perfiles y las comprobaciones pendientes están en [docs/ESTADO_DESARROLLO.md](docs/ESTADO_DESARROLLO.md).

**Entrega actual:** matriz de 72 casos trazada, sondas de mundo y escenarios F9 con controles nativos implementados; `compileJava`, `compileTestJava` y `compileValidationJava` correctos. Las ampliaciones no se ejecutaron. Ver [matriz](docs/acceptance/MATRIZ_TRAZABLE.md), [F9](docs/acceptance/F9.md) y [evidencia de compilación](docs/evidence/MATRIZ_F9_COMPILACION.md).

**Validación histórica anterior: `compileJava`, 106 tests Java, JAR, cliente/dedicado, dos clientes locales y recuperación tras SIGKILL de dos piezas (incluida PB).** No acredita todas las mecánicas ni la fidelidad visual. Ver [evidencia ejecutada](docs/evidence/VALIDACION_EJECUTADA_2026-10-30.md). La edición experimental requiere `/nmg enable` con permisos de propietario, por sesión y dimensión, o la propiedad `-Dnomoregrids.experimental=true`. No habilitarla en un mundo valioso sin completar la validación. Las capacidades no integradas rechazan la operación; los datos desconocidos se conservan como sustitutos protegidos.

Abrir la raíz como proyecto Gradle con JDK 25. [docs/PRIMEROS_PASOS.md](docs/PRIMEROS_PASOS.md) describe los comandos para cuando se autorice ejecutarlos. AGENTS.md mantiene la restricción de compilación y arranque solicitada por el usuario.

La especificación está en `docs/obsidian/NoMoreGrids`: abrirla como bóveda Obsidian y comenzar por `00 Inicio/Inicio.md`. Consultar [estructura](docs/ESTRUCTURA.md) y [evidencia](docs/evidence/IMPLEMENTACION_FASES.md).

Comprobaciones sin compilación: `python tools/check_repository.py`, `python tools/check_acceptance.py`, `python tools/check_java_syntax.py` (requiere las dependencias indicadas en el script) y `git diff --check`. Son revisión estructural y sintáctica, sin comprobación de tipos ni certificación del mod.
