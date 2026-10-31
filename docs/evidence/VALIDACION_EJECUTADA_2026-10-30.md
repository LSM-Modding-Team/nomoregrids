# Validación ejecutada — 2026-10-30 (America/Lima)

Autorización puntual del usuario para esta entrega: dependencias, compilación,
tests, cliente/dedicado, multijugador, recuperación, PB y rendimiento, sin usar
pantalla. No extiende el permiso de ejecución a tareas posteriores. Se usaron
exclusivamente perfiles y mundos desechables bajo `run/validation-*`.

## Resultado observado

| Comprobación | Resultado y alcance |
|---|---|
| Dependencias | Resueltas mediante Gradle 9.2.1, ModDevGradle 2.0.148, Java 25.0.2, NeoForge 26.2.0.88 y Minecraft 26.2 |
| `compileJava` | Pasó. Quedan avisos de APIs deprecated; no hubo errores de compilación del mod |
| Compilación de tests | El primer intento falló: Gson no estaba en el classpath de tests. Se añadió `testImplementation com.google.code.gson:gson:2.14.0`, la versión del manifiesto Minecraft 26.2 |
| Tests iniciales | 105 tests pasaron tras corregir el classpath, sin fallos ni omitidos |
| Tests finales | **106 tests, 42 suites, 0 fallos, 0 errores, 0 omitidos**. Incluye una nueva prueba de terminación forzada de una JVM escritora real |
| JAR NMG | Generado: `build/libs/nomoregrids-0.1.0-SNAPSHOT.jar`. No incluye el mod de sonda ni las pruebas |
| Dedicado sin PB | Cargó NMG, llegó a `Done (7.829s)` y guardó las tres dimensiones al recibir SIGTERM. Gradle devolvió 143 por esa parada intencionada; no fue un crash del mod |
| PB | El commit `e93bc03f350dfb03b4031f1e479f4a37f28b556e` compiló y generó `paintableblocks-1.0.0.jar` en una copia aislada. No se alteró/publicó el repositorio PB |
| Dedicado con PB | NMG y PB 1.0.0 cargaron juntos; servidor listo. `nmg status` sin cuarentena y `nmg enable` correcto |
| Clientes | Dos JVM reales de Minecraft cargaron NMG/PB, recursos, OpenGL y audio sin salida. Se utilizó Xvfb/llvmpipe sin capturas, lectura de pantalla ni entrada de ratón/teclado |
| Multijugador | Dos clientes locales distintos conectados simultáneamente. Tras recuperar el mundo, ambos volvieron a conectar y recibieron los chunks que contenían las piezas de la sonda |
| Sonda real | Piedra desplazada media casilla, movimiento vertical y pieza PB desplazada media casilla. Dos UUID, cuatro celdas; réplicas iguales a la autoridad y celdas antiguas vacías |
| Corte del dedicado | SIGKILL después del ack de la sonda, sin ejecutar `stop`/`save-all` entre el seed y el corte. Reinicio del mismo mundo reconstruyó las dos piezas y las cuatro réplicas |
| Comprobación posterior | `PROBE VERIFIED pieces=2 cells=4 quarantined=false`; `nmg validate`: 2 comprobadas, 0 conflictos, 0 referencias descargadas |
| Selección tras recuperar | `execute as ... at @s run nmg inspect` desde cada jugador identificó el UUID correcto de piedra/PB, revisiones 1/0, dos celdas y `substitute=false` |
| Cierre | `stop` por consola local, guardado de las tres dimensiones. Se terminaron los clientes y el display virtual de la prueba |

Los arranques usan el entorno ModDev y las clases compiladas del proyecto.
El JAR se generó e inspeccionó, pero no se hizo una instalación independiente
con un launcher de producción. La comprobación por rayo es del servidor;
no prueba controles de ratón, preview, edición de herramientas o fidelidad visual.

## Prueba de proceso y sonda de mundo

`DurableJournalProcessTest` arranca otra JVM con las clases reales del diario,
espera confirmación de commit manteniendo el archivo abierto, la termina con
`destroyForcibly()` y reabre el archivo desde el padre. Comprueba el plan exacto
y que volver a confirmar el mismo UUID no aumenta el archivo. No hay fake de
Minecraft ni sustitución del diario por una implementación de prueba.

`tools/validation/RuntimeProbe.java` es un mod auxiliar solo de validación,
compilado y empaquetado manualmente fuera de `src/main`. Registra un comando,
no bloques ni contenido de ejemplo. Usa ServerLevel/WorldService reales para
crear, mover y verificar las dos piezas. No simula subsistemas ausentes. La sonda
no entra en el JAR de NMG y exige una autoridad inicialmente vacía para seed.

Secuencia real de consola en el perfil de prueba:

```text
forceload add 32 80 63 95
fill 39 99 83 46 103 85 minecraft:air
save-all flush
nmg_validation_probe seed
nmg validate
# SIGKILL del único servidor de ese perfil y reinicio
nmg_validation_probe verify
nmg validate
# Conectar los dos clientes por Quick Play, situarlos en modo espectador
execute as NmgValidationOne at @s run nmg inspect
execute as NmgValidationTwo at @s run nmg inspect
debug start
# Muestra real de aproximadamente un minuto
debug stop
tick query
nmg validate
forceload remove 32 80 63 95
stop
```

Resultados de identidad:

```text
PROBE SEEDED stone=ca6c7039-172c-47eb-b697-48d53c6647f5 pb=773151c9-7bfc-4481-8e10-08ae8bee9502 cells=4 stoneRevision=1
PROBE VERIFIED pieces=2 cells=4 quarantined=false
NMG structural validation: checked=2, conflicts=0, unloaded references=0, deferred=0
```

La carga forzada anterior prepara los chunks de la prueba; no cambia la política
de carga de operaciones NMG. Las piezas se crean como fixtures del mundo de
prueba, sin afirmar validación de costes, inventarios o colocación por jugador.
El corte cubre un punto después del commit y antes de una parada ordenada;
no cubre todos los puntos de fallo, cortes eléctricos ni errores de fsync.

## Primera muestra de rendimiento, anterior a la corrección F5

Debian 13, Java 25.0.2, CPU visible AMD EPYC 9V74; cuota cgroup de **2 CPU**
(`cpu.max=200000 100000`), unos 9,7 GiB de RAM, sin swap. Heap máximo de 1536 MiB
por JVM de juego. OpenGL 4.5 por Mesa llvmpipe (LLVM 19.1.7), sin GPU física.
Dos clientes comparten la cuota con el dedicado; render distance 2, máximo
30 FPS, simulación cliente 5 y distancias servidor 3. No se midieron FPS.

- Servidor vacío: media 0,5 ms, p50 0,3 ms, p95 1,4 ms, p99 6,8 ms, 100 muestras.
- Dos clientes durante carga: media 8,6 ms, p95 21,8 ms, p99 76,4 ms; hubo avisos
  de atraso mientras se cargaban chunks/recursos y arrancaban ambas JVM.
- Dos piezas recuperadas y clientes posicionados: una muestra de transición dio
  media 11,0 ms, p95 41,7 ms y p99 109,7 ms.
- Muestra sostenida final: **1194 ticks en 59,67 s, 20,01 TPS**.
  `tick query` al finalizar: media **5,0 ms**, p50 **4,0 ms**, p95 **12,2 ms**,
  p99 **25,0 ms**, últimas 100 muestras.

Las últimas 100 muestras no son los percentiles del minuto entero. No se hizo
baseline comparable sin NMG/PB. Dos piezas no sustituyen escenas de 10000
componentes, diseños densos, fluidos extensos, redes o spam multijugador; estos
números no prueban la meta F9 ni atribuyen el coste exclusivamente al mod.

## Problemas encontrados y decisiones

- Java no heredó el proxy HTTP del entorno: el wrapper primero falló con
  `Connection refused`. Se configuró el proxy y el truststore del entorno en el
  Gradle user home, fuera de Git. Se verificó el SHA256 de la distribución
  Gradle local y se usó también el wrapper fijado.
- Gson se declara solo para tests. Minecraft sigue suministrándolo al mod;
  no se empaqueta otra copia ni se cambian versiones fijadas.
- Sin pantalla se usó Xvfb privado sin listener TCP. El primer cliente quedó
  en onboarding; se reinició con opciones del perfil de pruebas. Audio real
  ausente: backend OpenAL `No Output`. Un ajuste de simulación cliente 3 era
  inválido en 26.2 y se corrigió a 5 en esos perfiles, sin cambios de producto.
- Las consultas nativas a claves Yggdrasil fallaron por conectividad de authlib
  en el entorno. El dedicado local de multijugador se limitó a `127.0.0.1`,
  en modo offline con dos identidades de desarrollo; no se usaron credenciales
  ni se acredita autenticación online/chat firmado.
- El primer guard de crash intentó leer `/proc/<pid>/cwd`, no permitido por el
  entorno. Ese intento no mató al escritor. Un arranque duplicado fue rechazado
  por `session.lock`. Se verificó después el único PID del comando exacto del
  perfil conocido y se aplicó el SIGKILL de prueba. El reinicio efectivo es el
  registrado en `nmg-validation-recovery-retry.log`.
- Avisos de mixins PB ausentes en el arranque sin PB son de hooks opcionales;
  no aparecieron al cargar PB. No se observaron errores de mixin ni de recursos
  NMG en los dos clientes finales con las piezas recuperadas.

## Corrección F5 y repetición con el código final

La inspección del primer informe detectó una activación excesiva: los hooks de
cualquier cambio nativo programaban cavidades incluso lejos de componentes.
El diario contenía claves de **2056 posiciones de fluido**; por ejemplo,
`fluid:-11:-17:97`, lejos de los dos sólidos de la sonda en y=101. El informe
registraba 56 operaciones y 2100615 bytes de diario. Eso no era una escena seca
mínima y no debía atribuirse a los dos componentes. Se conserva el mundo original.

`WorldFluids.changed` ahora exige autoridad NMG en la celda o una de sus seis
vecinas: una pieza o un efecto de cavidad existente. Los nativos siguen siendo
fronteras de entrada; sus cambios lejanos no inician una simulación NMG. Las
cavidades ya existentes siguen activas y no se borran como reparación automática.
La comprobación usa los índices/claves, sin decodificar cavidades para filtrar.

Se repitieron `compileJava test jar` con la corrección: **BUILD SUCCESSFUL en
14 s, 106 tests sin fallos**. Se recompiló la sonda y creó un mundo separado en
`run/validation-fluid-server`, con las mismas versiones PB/NMG y perfil local.

1. Cargar cuatro chunks de fixture y guardar la base antes del seed.
2. `dry_verify`: cero cavidades iniciales.
3. Crear/mover piedra y colocar PB como antes; UUID finales:
   `e5cd6821-7862-4800-85d9-600c86aae57e` y
   `2ce013d4-9573-44ed-b965-d3bf68dc6e7b`.
4. `unrelated_water`: colocar agua vanilla en `(40,110,104)`, lejos de las piezas;
   `tick sprint 120t`; `dry_verify` confirmó **cavityEffects=0** después del avance.
5. SIGKILL del dedicado y reinicio del mismo perfil: `verify` confirmó las dos
   piezas/cuatro réplicas y `dry_verify` mantuvo cero cavidades/cuarentena.
6. Caso positivo `wet_check`: introducir agua en la media celda libre de la
   piedra produjo **water=2048, source=EMPTY**. La corrección permite la entrada
   local y no transforma una fuente parcial en un cubo recolectable.
7. Dos clientes reales volvieron a conectar con el código final. Tras situarlos
   frente a los fixtures, `nmg inspect` devolvió los dos UUID correctos, revisiones
   1/0, dos celdas por pieza y `substitute=false`. No hubo errores de mixin o
   excepciones en los logs de esos dos clientes.

La sonda positiva comprueba una entrada inmediata de agua, no conservación,
reacciones/cubos, barridos ni cadencias a gran escala. El mundo original con
cavidades antiguas no se depuró: no se puede distinguir y borrar datos guardados
legítimos a partir del mero cambio de alcance de activación.

## Muestra final tras la corrección y artefactos

En el mundo nuevo con la corrección, después de la recuperación y la entrada de
agua local, ambos clientes inspeccionaron sus piezas. La muestra final fue de
**1301 ticks en 65,01 s: 20,01 TPS**. Las últimas 100 muestras al terminar dieron
media **3,1 ms**, p50 **1,8 ms**, p95 **10,3 ms**, p99 **13,7 ms**.
No son los percentiles del intervalo entero ni una comparación controlada con
el mundo anterior. Se cerró el dedicado con `stop`, guardando las tres dimensiones,
y se terminaron clientes/Xvfb. No quedaron procesos de juego de esta validación.

[Resumen de resultados JSON](VALIDACION_EJECUTADA_2026-10-30.json), incluido el
informe NMG real final (sin inventarios). Su campo histórico `javaValidated=false`
no promueve automáticamente la aceptación de fases por haber ejecutado tests.

JAR NMG: `build/libs/nomoregrids-0.1.0-SNAPSHOT.jar`.
SHA256: `1046599253f85427f5f019a2d97b7e7f896cc38af293f2c48ebdcd6571496857`.
Se inspeccionó su contenido: metadatos presentes, sin sonda, tests ni Gson sombreado.
JAR PB compilado de referencia: SHA256
`89ba1e0d2b17f0e7eb3a28a8b652870a554f403afed882c1eaba94d161b659a9`.
Los JARs, mundos, cachés y contraseñas locales de RCON quedan fuera de Git.

Los logs crudos de esta sesión se conservan en `/tmp/nmg-validation-*.log`;
los del servidor/cliente están además en los directorios de perfil bajo `run`.
Los informes JUnit están en `build/reports/tests/test/index.html` y
`build/test-results/test`. Esta evidencia versionada conserva los resultados y
los incidentes aunque se eliminen después esos archivos temporales.

## Pendiente y motivo

- **Visual/interacción:** no se observó pantalla ni se usaron entradas GUI.
  Falta evaluar texturas, tints, contornos, preview, cursor/drag, menús y todas
  las herramientas PB con interacción real.
- **Mecánicas:** no se recorrió toda la matriz: flujo/reacciones/cubos completos en el mundo, decoración,
  apoyos/cascadas, automatización, XP, producción PB, redstone/raíles/pistones,
  supervivencia, permisos de terceros, concurrencia de ediciones y dimensiones.
  El arranque, los tests puros y dos piezas no equivalen a esas comprobaciones.
- **Recuperación:** faltan cortes en cada ventana de commit, inventarios,
  transacciones de otro mod, adaptadores ausentes/reinstalados, compactación bajo
  escritura y disco lleno/Windows. Aquí solo se usó Linux y dos piezas sin slots.
- **Multijugador online:** falta autenticación con cuentas reales y otro host;
  lo probado es conexión local offline y recepción de chunks/selección.
- **Rendimiento F9:** faltan escenas grandes y mediciones comparables de frame,
  red, memoria y disco en hardware documentado con GPU. No se certifica F9.

No se acepta globalmente ninguna fase a partir de estos resultados parciales.
La restricción habitual de ejecución vuelve a aplicarse al cerrar esta entrega.
