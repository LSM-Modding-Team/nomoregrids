# Validación en un mundo real

Este módulo usa el servidor, los bloques, las capacidades y las operaciones reales
del mod. No contiene un mundo simulado. Está separado de `main` y `test` y no
entra en el JAR normal. Compilarlo no ejecuta sus casos.

La entrega actual **solo ha compilado** estas ampliaciones. Para arrancar,
ensayar casos, generar JAR o ejecutar JUnit hace falta una autorización posterior,
según AGENTS.md. Los comandos de este documento son instrucciones para esa
sesión futura; no son un registro de ejecución.

## Preparación

Java 25, Minecraft 26.2, NeoForge 26.2.0.88 y Gradle 9.2.1, sin cambiar versiones.
La compilación aislada autorizada es:

```sh
./gradlew compileJava compileTestJava compileValidationJava
```

`compileValidationJava` no depende de `test`, `validationJar` ni de ningún run.
`build`/`test` tampoco ejecutan los comandos de mundo. El JAR auxiliar opcional
se genera con `validationJar`, solo cuando esté autorizado; incluye metadatos
`nmg_validation_probe` y nunca se incorpora al JAR de producción.

Con permiso de ejecución, `-PnmgValidation` añade la sonda como segundo mod y
habilita las herramientas de cliente. Ejemplo de configuración de un dedicado:

```sh
./gradlew -PnmgValidation -PnmgValidationProfile=wall-nmg \
  -PnmgHardwareId=equipo-descrito-en-evidencia -PnmgRevision=SHA_COMPLETO \
  runValidationServer
```

El directorio será `run/validation-wall-nmg-validationServer`; el mundo quedará
bajo él. El perfil usa edición experimental. El servidor necesita la preparación
normal de EULA, propiedades y mundo desechable. Nunca reutilizar mundos del
usuario. Usar **un perfil/mundo nuevo por escena o caso**; `nmgValidationProfile`
solo admite letras, números, guion y guion bajo. Los clientes con `runClient`
necesitan perfiles distintos para evitar compartir directorio y archivos.
Las opciones se aplican solo con `-PnmgValidation`; el arranque normal no registra
la sonda. No se configura CI ni se arranca un proceso automáticamente.

PB no es dependencia obligatoria. Para los casos PB instalar en el perfil una
compilación de `LSM-Modding-Team/paintableblocks` fijada a
`e93bc03f350dfb03b4031f1e479f4a37f28b556e` (schema 2), igual en ambos extremos.
No se necesita modificar su repositorio. Guardar SHA del código y del JAR en la
futura evidencia; una versión distinta requiere revisar el adaptador.

## Casos reales de mundo

Comandos de propietario; coordenadas absolutas recomendadas. Preparar aire libre,
altura/borde válidos y chunks ya cargados, sin autoridad NMG ni efectos previos.
No se fuerza carga. Los casos conservan las piezas ante una aserción fallida.
Los resultados se escriben bajo `WORLD/nomoregrids/acceptance`.

| Comando | Comprobación parcial real |
|---|---|
| `nmg_accept run eight_cells <x y z>` | Identidad de un cubo con ocho referencias, movimiento y retirada |
| `nmg_accept run mixed_slabs <x y z>` | Dos materiales, rechazo de invasión y restauración nativa exacta |
| `nmg_accept run protection <x y z>` | Veto de evento antes de commit y sin modificación del diario |
| `nmg_accept run barrels <x y z>` | Inventarios independientes, abort/commit NeoForge, copia sin autoridad y movimiento |
| `nmg_accept run doors <x y z>` | Grupo de puerta, apertura y retirada coherente |
| `nmg_accept run support <x y z>` | Apoyo insuficiente y pérdida de soporte nativo con loot durable |
| `nmg_accept run pb_quarter <x y z>` | PB de altura 4/16 y cubo apoyado a esa altura |
| `nmg_accept run stale_revision <x y z>` | Segunda operación obsoleta no modifica al ganador |
| `nmg_checkpoint_test <x y z>` | 272 movimientos, trabajo de compactación automático, cola simultánea y deduplicación |
| `nmg_protocol_test` | Decodificadores reales rechazan tamaño total/cuerpo excesivo sin asignar ese cuerpo |

Los casos `nmg_accept` requieren anchor múltiplo de 16 en los tres ejes (para
comprobar realmente la migración de sección) y el margen cargado y vacío de anchor+(-2,-1,-2)
a anchor+(7,4,7). Usan el actor del mundo: **no prueban por sí solos consumo,
menús, XP, GUI ni economía de un jugador**. `nmg_checkpoint_test` no mata procesos;
no cubre el corte durante publicación. `nmg_protocol_test` no envía un ataque por
socket. Esos alcances están también en el catálogo de la matriz.

## Escenas y medidas

Ver [F9](../../docs/acceptance/F9.md) para escenarios, controles, captura de frames,
cuatro conexiones, tamaños guardados y comparación estricta. `nmg_bench start
<scene> <x y z>` exige anchor múltiplo de 16 en los tres ejes, chunks cargados,
aire libre y un diario nuevo. No reduce silenciosamente una escena que exceda
el límite real de sección. `nmg_bench status` indica su fase.

`nmg_bench clean` retira solo fixtures identificadas y sin cambios externos.
Después de reiniciar, `nmg_bench resume_clean <run UUID>` reconstruye el plan
identificable desde su manifiesto; no vuelve a crear piezas. Si un PB nativo no
tiene su hash durable, o su imagen cambió, se conserva y se informa el conflicto.
No se borra a ciegas por coordenadas. La limpieza no devuelve un mundo a un
diario vacío: los recibos y efectos durables siguen existiendo. Usar otro mundo
para la siguiente escena.

## Sonda histórica

`nmg_validation_probe seed`, `verify`, `unrelated_water` y `dry_verify` conservan la escena pequeña de la validación anterior. Requieren
sus posiciones cargadas y preparadas: piezas en (40,100,84)..(46,103,85), agua
lejana en (40,110,104), PB para `seed`. El procedimiento observado y sus límites
están en [la evidencia histórica](../../docs/evidence/VALIDACION_EJECUTADA_2026-10-30.md).
No se volvió a ejecutar en esta entrega.

Para cortes de proceso, identificar siempre el PID del dedicado desechable y
registrar el punto confirmado por el diario/recibo antes de terminarlo; reiniciar
el mismo perfil e inspeccionar autoridad, réplicas e inventarios. La matriz exige
varias ventanas de corte y retirada/reinstalación de mods, además del caso
histórico de dos piezas. Nunca usar un mundo del usuario.
