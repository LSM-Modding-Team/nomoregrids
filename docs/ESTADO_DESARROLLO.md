# Estado de desarrollo — 2026-10-30 UTC

El usuario autorizó implementar F0–F9 dejando pendientes compilación y ejecución.
Esto permite avanzar código de fases posteriores. No elimina las puertas de
aceptación ni convierte los módulos aislados en funcionalidades integradas.
**Ninguna fase está aceptada; tampoco están implementadas por completo todas las fases.**

| Fase | Código disponible | Trabajo de implementación restante |
|---|---|---|
| F0 | Versiones fijadas, wrapper oficial y metadatos MIT; registros técnicos; consulta de firmas exactas de Minecraft/NeoForge | Resolver cualquier incompatibilidad de tipos al compilar |
| F1 | Geometría pura, UUID/revisiones, índices, contenedor/BE, captura vanilla, diario durable, recibos, secciones, reparación al cargar, permisos y payloads de acciones | Restauración nativa exacta cuando vuelva a ser representable; deltas generales y transporte fragmentado conectados; endurecer recuperación intermod y compactación |
| F2 | Colocación compartida cliente/servidor, 24 rotaciones, losas/escaleras/cubos, selección, minería, reemplazo, controles y preview con validación del servidor | Contorno fino por componente, matriz completa y fidelidad de modelos/tints ejecutada |
| F3 | Paneles/barrotes recortables, fragmentos/recetas, vallas/muros, puertos geométricos, resolución estable de conexiones y esquinas de escaleras 3D en el host | Reaccionar a cambios nativos externos; verificar extremos, tapas y recursos visuales |
| F4 | Materiales/luz, parches exactos de carpetas/antorchas/faroles/escaleras, cadenas estables; retirada de dependientes en operaciones NMG | Contactos por área, fricción/rebote/sonido y escalada; pendiente reaccionar a pérdida de soporte nativo externo, decoración con datos, efectos, fuego y explosiones por componente |
| F5 | Cavidades 16³, fuentes, cubos, redistribución, flujo/reacciones, máscara convexa incremental y codec/checkpoints | Conectar guardado/sincronización de fluidos, cadencias, buckets y puente del mundo, malla; la ruta actual rechaza composición mojada |
| F6 | API versionada, captura/colocación PB, convexos/render, apoyo nativo, pincel y herramientas simples, costes por copia y loot nativo | Pintura de áreas, herramientas de volumen/pendientes y sus selecciones; pruebas del ciclo completo en dedicado |
| F7 | Autoridad única de inventario/energía, puertos, grupos, horno y codec/checkpoints privados | Conectar persistencia de estado funcional, captura nativa, menús, recetas, capacidades y ticking conectados |
| F8 | Planificador de pistones con barrido y grupos, red lógica con retardos y continuidad de raíles | Hooks nativos de pistones/redstone/raíles, conectar checkpoints lógicos y movimiento transaccional del mundo; el contenedor bloquea empuje vanilla |
| F9 | Presupuestos previos, caché de formas por celda, reparación diferida/coalescida por tick, LRU por peso, muestras y comandos de diagnóstico/exportación | Conectar cachés globales y mediciones; compactación segura del diario; escenas y certificación medidas |

## Verificación realizada

- `python tools/check_repository.py`: recursos JSON, TOML, MIT, checksum del
  wrapper, enlaces wiki e independencia del núcleo.
- Parser Tree-sitter: sintaxis Java; no resuelve tipos ni firmas de APIs.
- `git diff --check`: whitespace.
- Lectura de fuentes NeoForge 26.2.0.88, parches userdev, jars oficiales 26.2 y
  firmas mediante `javap`; no ejecuta código del juego ni compila.

Hay pruebas Java escritas para geometría, índices, codecs, diario, coordinador,
red, economía, apoyo, cavidades, convexos, funcionales, mecanismos y presupuestos.
**No ejecutadas**: Gradle, compiladores, tests Java, GameTests, Minecraft,
inyección de fallos real, integración PB y benchmarks. No existe JAR generado.

## Decisiones y límites de la integración actual

- Commits locales por bloques coherentes; sin push, publicación ni cambios de versiones.
- Edición experimental explícita. Soporte no integrado se rechaza antes del consumo.
- El inventario y su recibo se guardan juntos en los datos nativos del jugador;
  se verifica en disco antes de escribir checkpoints de secciones.
- Fallo tras commit: se bloquean operaciones y reparación con la instantánea
  anterior; la recuperación usa el diario, sin cobrar ni soltar loot a ciegas.
- Si loot o sobrante no caben en el inventario, se rechaza la operación entera.
  Todavía no hay entrega mediante entidades de ítem en el mundo.
- Hasta conectar transporte fragmentado, una réplica de celda no supera 64 KiB.
- El diario conserva toda la historia en disco y memoria. No está compactado ni
  certificado para mundos grandes; no se trunca automáticamente una cola corrupta.
- Los fsync de directorio son obligatorios en las rutas durables actuales;
  sistemas de archivos sin soporte fallan de forma conservadora. Falta validación Windows.
- Las réplicas y diseños desconocidos conservan sus originales y se protegen;
  la caja conservadora no acredita la geometría exacta de un mod ausente.

La evidencia detallada y decisiones por entrega están en
[evidence/IMPLEMENTACION_FASES.md](evidence/IMPLEMENTACION_FASES.md).
