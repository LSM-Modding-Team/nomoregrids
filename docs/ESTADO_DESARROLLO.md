# Estado de desarrollo — 2026-10-30 (America/Lima)

Se ha escrito y conectado al host el trabajo de implementación pendiente de
esta edición: recuperación de adaptadores, decoración con datos, simulación de
fluidos por ventanas, herramientas PB mixtas, funcionales con menús/capacidades,
grupos, redstone, raíles, pistones mixtos y compactación automática.
El alcance son los perfiles explícitos descritos abajo. **Ninguna fase está
aceptada:** faltan compilación, pruebas Java, arranque e integración en Minecraft,
expresamente no autorizados en esta entrega. La revisión estática no acredita
que el código compile o que los hooks se apliquen correctamente.

## Código disponible por fase

| Fase | Implementación conectada | Validación pendiente |
|---|---|---|
| F0 — Base | Minecraft 26.2, NeoForge 26.2.0.88, Java 25, Gradle 9.2.1 y ModDevGradle 2.0.148 fijados; wrapper, registros, metadatos y licencia | Resolución de dependencias, tipos, carga de mixins, cliente y dedicado |
| F1 — Identidad y guardado | UUID/revisiones, geometría, índices, contenedor/BE, captura nativa, diario, recibos, secciones, reparación, snapshots/deltas fragmentados, sustitutos recuperables y representación nativa por igualdad exacta | Fallos entre inventario/diario/chunks, migraciones, retirada/reinstalación de mods y Windows |
| F2 — Construcción | Solver compartido, 24 rotaciones, composición, selección, minería, reemplazo, controles, preview validado y contorno fino por componente | Matriz geométrica/visual, recursos, alcance y multijugador |
| F3 — Conexiones | Fragmentos/recetas, recortes, paneles/barrotes/vallas/muros, puertos geométricos, escaleras 3D y observación de vecinos nativos | Extremos, tapas, modelos y estabilidad tras cambios externos |
| F4 — Materiales y apoyo | Apoyos exactos y cadenas, pérdida de soporte externo, contactos/fricción/rebote/velocidad/salto, decoración con NBT/render/editor de carteles, fuego y absorción de explosiones por componente | Render/datos decorativos, cadenas, contactos, fuego, explosiones e iluminación |
| F5 — Fluidos | Cavidades 16³, persistencia/red, cubos, redistribución, ventanas con frontera, cadencias, reacciones, malla y contactos/corrientes para entidades grandes | Conservación, fuentes, carga, fronteras vanilla, agua/lava, entidades y escala |
| F6 — Paintable Blocks | API/datos versionados, captura/colocación, convexos/render, pintura por áreas, herramientas, volúmenes y pendientes; selección/lotes mixtos nativos y NMG, creación creativa y costes/loot del origen | Ciclo completo con PB fijado, geometría/pintura, economía, red y dedicado |
| F7 — Funcionales y grupos | Autoridad única privada; barril/hornos y estaciones/máquinas/cubos PB; menús nativos con cursor, drag, autofill y XP preparados por recibo; ticks/recetas, puertos, transferencias/energía transaccionales; puertas/camas/trampillas con cierre de grupo y barrido | Automatización entre mods, menús completos, XP, producción, guardado/cortes, cambio de dueño, dormir/respawn y grupos movidos |
| F8 — Mecanismos | Redstone por puertos con retardos y checkpoints, entradas/salidas nativas, continuidad de raíles y vagones, detectores/activadores, empujes mixtos nativos/NMG/PB/grupos con barrido y metas nativas en el mismo plan | Pulsos/carga/burnout, raíles y física de vagones, slime/miel, pistones, entidades y mecanismos combinados |
| F9 — Optimización | Presupuestos, cachés LRU por peso, trabajo diferido, métricas conectadas, compactación automática con preparación I/O y copia incremental de cola, ledgers de deduplicación en disco | Recuperación de checkpoints, carga sostenida, memoria, tiempos y escenas de certificación |

## Perfiles y límites de esta edición

- Construcción: posición 1/16, 24 rotaciones, hasta 8 celdas por pieza atómica;
  operación de hasta 128 cambios y 512 celdas. Los límites geométricos y de
  almacenamiento se aplican conjuntamente, antes de consumir recursos.
- Decoración con datos: carteles normales/colgantes, estandartes y cabezas de los
  perfiles vanilla declarados. Se conserva NBT privado, se publica solo lo
  necesario para render y se valida de nuevo la edición del cartel en servidor.
- Funcionales vanilla: barril, horno, alto horno y ahumador. PB añade estaciones,
  máquinas de pintura y cubos del API inspeccionado. No se ofrece un adaptador
  universal para cualquier BE, cofre o máquina de otros mods.
- Grupos nativos: puertas y camas de dos miembros; trampillas individuales.
  Se valida la estructura y el barrido de apertura. Dormir/respawn requiere una
  superficie de cabeza horizontal, accesible y no ambigua.
- Redstone: polvo, repetidores, comparadores, antorchas, bloque de redstone,
  palancas, botones y placas de presión de los perfiles vanilla. Cada red admite
  hasta 256 componentes reales y el presupuesto binario de su checkpoint;
  compartir casilla no crea una conexión. El atraso puede añadir latencia al
  publicar flancos en el mundo nativo, conservando los eventos lógicos.
- Raíles: rectas, curvas y pendientes nativas con extremos/tangentes continuos;
  normal, propulsor, detector y activador. Los vagones necesitan un marco
  horizontal y una ruta no ambigua. No se acredita física idéntica a vanilla.
- Pistones: movimiento instantáneo de un bloque; cierre máximo de 12 componentes,
  contando cada miembro de grupo. Un perfil incompatible o una trayectoria
  obstruida bloquea el plan entero. No se incluye animación de empuje por pieza.
- Fluidos: máscara conservadora 16³. Cada trabajo actualiza hasta 16 celdas con
  fronteras cargadas capturadas; publica después de completar su relajación y
  comprobar revisiones. No se limita toda una red a 256 celdas. La redistribución
  de una construcción sí limita su región local a 256 celdas. El presupuesto de
  avance actual es 65536 pasos de microcavidad por turno, sin forzar chunks.
  Las ranuras inferiores a 1/16 pueden quedar cerradas por la máscara, sin
  cambiar el sólido original. Solo una fuente completa llena/consume un cubo.
- Herramientas de volumen PB: hasta 16 bloques por eje y 128 componentes,
  sujetos también al presupuesto geométrico. La selección fija UUID/revisión
  NMG o imagen/hash nativos y verifica ambas antes del commit.
- Réplicas públicas: hasta 2 MiB, fragmentos de 64 KiB, deltas con hashes y resync;
  los inventarios no viajan en réplicas públicas. Protocolo experimental **9**.
- Compactación: preparación automática desde 256 operaciones u 8 MiB de diario;
  checkpoint máximo 64 MiB y copia de cola por lotes de 64 operaciones. Conserva
  una generación anterior. Los límites siguen vigentes en mundos grandes.

Los perfiles desconocidos o de API incompatible se conservan protegidos; una
capacidad no ofrecida se rechaza antes del consumo. Estos límites son decisiones
de implementación, sin certificación de compatibilidad ni rendimiento.

## Autoridad, recuperación y decisiones de entrega

Los registros originales se conservan en el diario/checkpoint. La ausencia de
un adaptador produce una proyección protegida, sin sobrescribir el diseño ni
persistir un sustituto como original; reinstalar el adaptador permite reconstruir
la proyección. Solo se restaura un bloque nativo cuando sus propiedades y sus
formas expresan exactamente la geometría mundial actual. Las demás piezas
continúan en su contenedor.

El inventario funcional existe una vez en `FunctionalStore.State`. Las BEs usadas
por recetas, menús y automatización son copias controladas. Antes de quitar una
BE nativa se guarda su recibo de captura; durante la transferencia se evitan drops
nativos/XP duplicados y se vacía el objeto retirado. Un chunk antiguo no ejecuta
una segunda máquina ni entrega ese inventario antes de reparar. Las últimas
celdas retiradas/movidas conservan una meta AIR durable para impedir que un chunk
viejo las reviva. Si una imagen nativa ya difiere de la capturada se conserva y
se bloquea la reparación destructiva.

Los cambios del jugador incluyen slots, cursor y XP en un recibo durable. Las
transferencias NeoForge usan la transacción del origen y un intent persistido.
Si se interrumpe una transacción externa antes de probar su commit propio, el
mundo queda protegido para reconciliar el recibo del otro mod; no se supone el
resultado de una transacción ajena. No se unen transacciones entre dimensiones.

Las operaciones validan permisos, carga, apoyo, colisión, entidades y presupuesto
antes de consumir. Un fallo posterior al commit entra en recuperación, sin
revertir parcialmente ni volver a cobrar. Si el inventario del jugador no admite
el resultado inmediato se rechaza el plan. Los drops de pérdidas externas se
guardan en una cola privada y se entregan a un jugador cercano cuando caben;
no se crean entidades de ítem sin recibo confirmado.

Los cambios externos se observan en hooks de `Level`; las notificaciones de
vecinos y señales se publican después del commit. Las escrituras directas de
otros mods que omitan `Level`, o sus acciones externas sin recibo transaccional,
requieren un contrato específico del mod. No se implementa un mundo virtual
vanilla universal. En Windows NIO se omite únicamente el fsync de directorios
no admitido por el proveedor; se mantienen force de archivos y reemplazos
atómicos. Falta contrastar la recuperación real en Windows.

## Evidencia disponible y trabajo que sigue pendiente

Comprobaciones realizadas, sin compilación ni ejecución de Java:

- Tree-sitter: **240 fuentes Java** sin errores sintácticos, incluidas 42 fuentes
  de pruebas escritas. No resuelve tipos ni inyecciones.
- `python tools/check_repository.py`: JSON, TOML, licencia, wrapper, enlaces wiki
  e independencia del núcleo.
- `python tools/check_api_imports.py`: **92 imports explícitos** Minecraft/NeoForge
  presentes en los artefactos consultados. No comprueba miembros, comodines,
  genéricos ni firmas parcheadas.
- Lectura de fuentes/parches NeoForge 26.2.0.88, jars oficiales 26.2 y firmas con
  `javap`; contraste del API PB con su rama 26.2, commit
  `e93bc03f350dfb03b4031f1e479f4a37f28b556e`.
- `git diff --check`: whitespace. Commits publicados en GitHub, rama `26.2`;
  sin force push, workflows, builds o cambios de versiones.

No se han ejecutado Gradle, compiladores, tests Java, GameTests, cliente,
servidor dedicado, PB, inyección de fallos ni benchmarks. No se ha generado un
JAR del mod. No se presenta la implementación como aprobada por estas lecturas.

El siguiente trabajo requiere autorización de ejecución: compilar y resolver
los fallos de tipos/API que revele; ejecutar las pruebas escritas; arrancar
cliente y dedicado; recorrer la matriz de aceptación, incluidos cortes de
escritura, Windows, multijugador, automatización y PB; medir las escenas F9.
También deberán corregirse los defectos que aparezcan. No queda como módulo
algorítmico aislado ninguno de los pendientes de implementación enumerados al
iniciar esta entrega; sí queda toda esa validación y sus eventuales correcciones.

La cronología de cambios, fuentes, decisiones y límites está en
[evidence/IMPLEMENTACION_FASES.md](evidence/IMPLEMENTACION_FASES.md). Las primeras
secciones de ese documento describen entregas históricas, no carencias vigentes.
