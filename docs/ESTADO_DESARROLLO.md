# Estado de desarrollo — 2026-10-30 UTC

El usuario autorizó implementar F0–F9 dejando pendientes compilación y ejecución.
Esto permite avanzar código de fases posteriores. No elimina las puertas de
aceptación ni convierte los módulos aislados en funcionalidades integradas.
**Ninguna fase está aceptada; tampoco están implementadas por completo todas las fases.**

| Fase | Código disponible | Trabajo de implementación restante |
|---|---|---|
| F0 | Versiones fijadas, wrapper oficial y metadatos MIT; registros técnicos; consulta de firmas exactas de Minecraft/NeoForge | Resolver cualquier incompatibilidad de tipos al compilar |
| F1 | Geometría pura, UUID/revisiones, índices, contenedor/BE, captura vanilla, diario durable, recibos, secciones, reparación al cargar, permisos, efectos privados del mundo y restauración nativa exacta para cajas sin transformación | Endurecer y ejecutar recuperación intermod; compactación |
| F2 | Colocación compartida cliente/servidor, 24 rotaciones, losas/escaleras/cubos, selección, minería, reemplazo, controles, contorno por componente con aristas reales y preview con validación del servidor | Completar validación de la matriz geométrica y fidelidad visual/modelos/tints en Minecraft |
| F3 | Paneles/barrotes recortables, fragmentos/recetas, vallas/muros, puertos geométricos, resolución estable de conexiones y esquinas de escaleras 3D en el host | Verificar en ejecución cambios nativos externos, extremos, tapas y recursos visuales |
| F4 | Materiales/luz, parches exactos, cadenas estables, dependientes en operaciones NMG, contactos por área, fricción/rebote/sonidos, escalada y pérdida de soporte externo journalizada | Decoración con datos, efectos, fuego y explosiones por componente |
| F5 | Cavidades 16³ conectadas a diario, réplicas, cubos, redistribución, flujo/reacciones, máscara convexa, cadencias y malla de caras fusionadas | Ampliar planificación de regiones activas grandes; validar puente vanilla, efectos físicos, cadencias y reacciones en el juego |
| F6 | API versionada, captura/colocación PB, convexos/render, apoyo nativo, pincel por áreas, herramientas simples, volumen/pendientes con selección por revisiones, costes por copia y loot nativo | Lotes que mezclen PB nativos y convertidos; pruebas del ciclo completo en dedicado |
| F7 | Autoridad única de inventario/energía, puertos, grupos, horno y codec/checkpoints privados | Conectar persistencia de estado funcional, captura nativa, menús, recetas, capacidades y ticking |
| F8 | Planificador de pistones con barrido y grupos, red lógica con retardos y continuidad de raíles | Hooks nativos de pistones/redstone/raíles, conectar checkpoints lógicos y movimiento transaccional del mundo; el contenedor bloquea empuje vanilla |
| F9 | Presupuestos previos, caché de formas por celda, reparación diferida/coalescida por tick, LRU por peso, muestras y comandos de diagnóstico/exportación | Conectar cachés globales y mediciones; compactación segura del diario; escenas y certificación medidas |

## Verificación realizada

- `python tools/check_repository.py`: recursos JSON, TOML, MIT, checksum del
  wrapper, enlaces wiki e independencia del núcleo.
- Parser Tree-sitter: 147 fuentes Java sin errores sintácticos; no resuelve tipos ni firmas de APIs.
- Archivo de APIs: 58 imports explícitos presentes en los jars consultados;
  no comprueba miembros, comodines ni extensiones parcheadas.
- `git diff --check`: whitespace.
- Lectura de fuentes NeoForge 26.2.0.88, parches userdev, jars oficiales 26.2 y
  firmas mediante `javap`; no ejecuta código del juego ni compila.

Hay pruebas Java escritas para geometría, índices, codecs, diario, coordinador,
red, economía, apoyo, cavidades, convexos, funcionales, mecanismos y presupuestos.
**No ejecutadas**: Gradle, compiladores, tests Java, GameTests, Minecraft,
inyección de fallos real, integración PB y benchmarks. No existe JAR generado.

## Decisiones y límites de la integración actual

- Commits por bloques coherentes, publicados en GitHub en `26.2`; sin cambios de versiones fijadas.
- Edición experimental explícita. Soporte no integrado se rechaza antes del consumo.
- El inventario y su recibo se guardan juntos en los datos nativos del jugador;
  se verifica en disco antes de escribir checkpoints de secciones.
- Fallo tras commit: se bloquean operaciones y reparación con la instantánea
  anterior; la recuperación usa el diario, sin cobrar ni soltar loot a ciegas.
- Si loot o sobrante no caben en el inventario, se rechaza la operación entera.
  Todavía no hay entrega mediante entidades de ítem en el mundo.
- Réplicas públicas hasta 2 MiB viajan en fragmentos de 64 KiB; deltas con hashes
  de base/destino y revisiones, snapshot al seguir el chunk y resync ante divergencia.
- El diario conserva toda la historia en disco y memoria. No está compactado ni
  certificado para mundos grandes; no se trunca automáticamente una cola corrupta.
- En Windows NIO se omite únicamente el fsync de directorios, que ese proveedor
  no admite. Se mantienen force(true) de archivos y reemplazos atómicos; la
  durabilidad de metadatos de directorio ante un corte eléctrico es menor.
  Los errores reales de acceso/escritura siguen bloqueando. Falta ejecución Windows.
- Las réplicas y diseños desconocidos conservan sus originales y se protegen;
  la caja conservadora no acredita la geometría exacta de un mod ausente.

La evidencia detallada y decisiones por entrega están en
[evidence/IMPLEMENTACION_FASES.md](evidence/IMPLEMENTACION_FASES.md).

Los cambios externos se observan en `Level.setBlock` con un mixin aislado de 26.2.
Las proyecciones nativas mantienen UUID en la autoridad hasta que una modificación
externa las libera mediante el diario; así el replay no revive un bloque ya roto.
Los drops por pérdida de apoyo se conservan en una cola privada durable y se
entregan automáticamente al inventario de un jugador cercano cuando caben. No
se crean entidades de ítem antes de una entrega confirmada. Los writes directos
de otros mods a ChunkAccess, fuera de Level, requieren una integración específica.

F5 usa una región activa acotada a 256 celdas por trabajo. Si la región cargada
excede ese límite se aplaza sin publicar un resultado parcial. Sigue pendiente
dividir redes de fluido grandes con fronteras temporales consistentes. La máscara
conservadora puede cerrar ranuras menores a 1/16; no modifica el sólido original.
Los efectos físicos vanilla reciben una proyección conservadora de fluido de
celda; falta contrastar inmersión/contacto exacto dentro de cavidades.
