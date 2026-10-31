# Sonda manual de integración

Requiere autorización explícita de ejecución, Java 25, el entorno ModDev de
Minecraft 26.2/NeoForge 26.2.0.88 y un mundo desechable. No se ejecuta con `test`,
`build`, el arranque normal ni CI. No se incluye en el JAR de NMG.

`RuntimeProbe.java` usa clases reales de NMG y del servidor; no ofrece un mundo
simulado. Compilar con `javac` usando el classpath preparado por
`RunGameTask.classpathProvider.asPath` (incluidos `build/classes/java/main` y
`build/resources/main`), y empaquetar `validation/RuntimeProbe.class` en un JAR
auxiliar con este archivo `META-INF/neoforge.mods.toml`:

```toml
license="MIT"
[[mods]]
modId="nmg_validation_probe"
version="1.0"
displayName="NMG Runtime Validation Probe"
```

Cargar el JAR auxiliar únicamente en el perfil de prueba. Se cargó también en
los dos clientes de desarrollo. PB debe estar presente para `seed`.

- Preparar/cargar y despejar el área `(40,100,84)` a `(46,103,85)` del mundo de
  prueba. La sonda no fuerza chunks y necesita una autoridad NMG vacía.
- `nmg_validation_probe seed` coloca piedra multicelda, la mueve un bloque hacia
  arriba, verifica las vacantes y coloca PB multicelda. No consume inventarios:
  son fixtures de prueba mediante operaciones de mundo.
- `nmg_validation_probe unrelated_water` coloca agua nativa lejos de las piezas
  en `(40,110,104)`. Cargar también ese chunk. Tras avanzar 120 ticks,
  `nmg_validation_probe dry_verify` exige cero cavidades NMG. Usar un mundo nuevo.
- `nmg_validation_probe wet_check`, después del caso seco y su recuperación,
  introduce agua junto a la piedra y exige 2048 microvolúmenes, sin fuente de cubo.
- `nmg_validation_probe verify` exige dos registros y comprueba las cuatro
  réplicas contra la autoridad. Exige permisos de propietario.
- Para una prueba de corte, detener únicamente el PID confirmado del servidor
  desechable después del ack y reiniciar ese mismo perfil. Nunca usar un mundo
  del usuario. Retirar la carga forzada al finalizar y cerrar ordenadamente.

El procedimiento observado y sus límites están en
[la evidencia de ejecución](../../docs/evidence/VALIDACION_EJECUTADA_2026-10-30.md).
El test `DurableJournalProcessTest` es independiente: usa un directorio temporal
JUnit y termina solamente la JVM escritora que él mismo crea.
