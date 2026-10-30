# Estado de desarrollo — 2026-10-30 UTC

El usuario autorizó implementar F0–F9 dejando pendientes compilación y ejecución.
Esto permite avanzar código de fases posteriores. No elimina las puertas de
aceptación ni convierte los módulos aislados en funcionalidades integradas.
**Ninguna fase está aceptada; tampoco están implementadas por completo todas las fases.**

| Fase | Código disponible | Trabajo de implementación restante |
|---|---|---|
| F0 | Versiones fijadas, wrapper oficial y metadatos MIT; registros técnicos; consulta de firmas exactas de Minecraft/NeoForge | Resolver cualquier incompatibilidad de tipos al compilar |
| F1 | Geometría pura, UUID/revisiones, índices, contenedor/BE, captura vanilla, diario durable, recibos, secciones, reparación al cargar, permisos y payloads de acciones | Restauración nativa exacta cuando vuelva a ser representable; snapshots/deltas generales y transporte fragmentado conectados; endurecer recuperación intermod y compactación |
| F2 | Colocación compartida cliente/servidor, 24 rotaciones, losas/escaleras/cubos, selección, minería, reemplazo, controles y preview con validación del servidor | Esquinas de escaleras en 3D y contorno fino por componente; fidelidad completa de modelos/tints |
| F3 | Paneles/barrotes recortables, ítem de fragmento, corte/unión, sobrantes; solver puro de conexiones y familias | Integrar vallas/muros y actualización de puertos/conexiones en el host; modelos de extremos |
| F4 | Materiales/luz, parches exactos de carpetas/antorchas/faroles/escaleras, cadenas estables; retirada de dependientes en operaciones NMG | Reaccionar a pérdida de soporte nativo externo; decoración con datos, contactos, efectos, fuego y explosiones por componente |
| F5 | Cavidades 16³, fuentes, cubos, redistribución conservadora, flujo incremental y reacciones en módulo puro | Guardado/sincronización de fluidos, cadencias, buckets y puente del mundo, malla; la ruta actual rechaza composición mojada |
| F6 | API por versión/capacidad; adaptador PB opcional de diseño, convexos, selección y render; conservación de codec nativo | Captura/colocación BE de PB, búsqueda de herramientas, edición/cobro/recuperación nativos y ciclo completo en dedicado |
| F7 | Autoridad única de inventario/energía, puertos sin ambigüedad, grupos y lógica de horno | Persistencia de estado funcional, captura nativa, menús, recetas, capacidades y ticking conectados |
| F8 | Planificador de pistones con barrido y grupos, red lógica con retardos y continuidad de raíles | Hooks nativos de pistones/redstone/raíles, guardado lógico y movimiento transaccional del mundo; el contenedor bloquea empuje vanilla |
| F9 | Presupuestos previos, LRU por peso, trabajos coalescidos y muestras de tiempos | Conectar todas las cachés/colas; compactación segura del diario; escenas y certificación medidas |

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
