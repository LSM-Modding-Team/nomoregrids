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
| F4 | Materiales/luz, parches exactos, cadenas estables, dependientes en operaciones NMG, contactos por área, fricción/rebote/sonidos, escalada pérdida de soporte externo journalizada, fuego y explosión por componente | Decoración con datos, efectos de contacto avanzado y fidelidad de propagación de explosiones |
| F5 | Cavidades 16³ conectadas a diario, réplicas, cubos, redistribución, flujo/reacciones, máscara convexa, cadencias y malla de caras fusionadas | Ampliar planificación de regiones activas grandes; validar puente vanilla, efectos físicos, cadencias y reacciones en el juego |
| F6 | API versionada, captura/colocación PB, convexos/render, apoyo nativo, pincel por áreas, herramientas simples, volumen/pendientes con selección por revisiones, costes por copia y loot nativo | Lotes que mezclen PB nativos y convertidos; pruebas del ciclo completo en dedicado |
| F7 | Autoridad única, captura nativa, codec/checkpoints privados, barriles y hornos nativos con recetas, ticking y menús conectados | Capacidades transaccionales entre mods, XP y autofill de menús, cadencia con más de 16 hornos, grupos y migración de dueño al mover |
| F8 | Planificador de barrido, primer hook de pistones con base/cabezal journalizados, red lógica con retardos y continuidad de raíles | Empujes mixtos con bloques nativos, grupos, PB, continuidad nativa de raíles y circuitos/redstone conectados |
| F9 | Presupuestos previos, caché de formas por celda, reparación diferida/coalescida por tick, LRU global de geometría por peso, tiempos de operaciones y compactación por generaciones con índices de deduplicación en disco | Conectar cachés globales y mediciones; compactación segura del diario; escenas y certificación medidas |

## Verificación realizada

- `python tools/check_repository.py`: recursos JSON, TOML, MIT, checksum del
  wrapper, enlaces wiki e independencia del núcleo.
- Parser Tree-sitter: 178 fuentes Java sin errores sintácticos; no resuelve tipos ni firmas de APIs.
- Archivo de APIs: 64 imports explícitos presentes en los jars consultados;
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
- `/nmg compact` crea checkpoint y ledgers ordenados en disco, cambia el manifiesto
  atómicamente y libera imágenes antiguas de la memoria. Mantiene una generación
  anterior; se limpia la más antigua tras la siguiente compactación. No se truncan
  colas corruptas. La compactación manual tiene un límite de checkpoint de 64 MiB;
  faltan planificación incremental y certificación para mundos grandes.
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

F7 activa un perfil explícito de barril, horno, alto horno y ahumador. Cada
inventario solo existe en FunctionalStore.State dentro del diario; los objetos
nativos usados para planificar o mostrar el menú son copias temporales. Los
menús transfieren sin propiedad de ítems en el cursor: clic derecho en máquina
selecciona destino, clic en inventario transfiere y clic en máquina recupera.
Faltan cursor/drag y autofill transaccionales, XP, capacidades de automatización,
y gestión de grupos. Se procesan hasta 16 funcionales por tick, aún sin compensar
cadencia para cantidades mayores. F7 continúa parcial.

F8 conecta el evento nativo de pistón cuando su cara encuentra directamente
componentes NMG: barrido, entidades y límite 12; extensión/retracción de base y
cabezal pertenecen al mismo plan durable. El movimiento se aplica de forma
instantánea. Faltan empujes mixtos de bloques nativos, grupos y movimiento PB;
se rechazan. Redstone y raíles todavía no tienen un puente completo al mundo.

## Alcance todavía sin implementar al cerrar esta entrega

Esta entrega amplía la integración y publica sus commits en `26.2`, pero **no
termina todo el trabajo solicitado**. Estos pendientes son de código, además
de la compilación/ejecución expresamente no realizadas:

- F1: recuperación intermod con pérdida/restauración de adaptadores y todas las
  representaciones nativas orientadas; la restauración actual exige marco exacto.
- F4: decoración con BE/datos, contactos avanzados y propagación exacta de energía
  de explosiones para geometría arbitraria.
- F5: partición consistente de redes activas mayores a 256 celdas y contacto
  físico exacto de entidades con cada microvolumen; completar/verificar puente
  de flujos vanilla en todos los casos de fuente y carga.
- F6: volúmenes mixtos con PB nativo y convertido, y creación volumétrica conjunta.
- F7: capacidades de transferencia/energía entre mods, grupos, cursor/drag/autofill
  y XP transaccionales, cadencia a escala y migración de autoridad al mover.
- F8: circuitos/redstone y raíles conectados al mundo; pistones de lotes mixtos,
  grupos, PB y trayectorias convexas exactas; cierre de apoyos en la misma operación
  cuando intervienen los estados nativos del mecanismo.
- F9: compactación incremental/automática y límites globales adicionales, con
  mediciones reales pendientes.

F2/F3 necesitan completar su matriz de comprobaciones geométricas/visuales y
corregir lo que revele la ejecución; no se consideran aceptadas.
