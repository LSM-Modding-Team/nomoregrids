# Matriz y F9: continuación con compilación autorizada

Continuación exacta de `cc63061`. El primer commit de esta entrega es
`c0b6987cb67f85db5e594db22ade8aa0c962edba`, publicado en `26.2` y verificado.
La autorización actual permite **compilar Java**; no permite ejecutar tests,
cliente/dedicado, escenas, cortes, benchmark ni generar JAR. La prohibición
habitual sigue en AGENTS.md para tareas posteriores.

## Implementación

- Catálogo de los **72 casos originales**, con expectativas sin modificar,
  procedimientos completos, fuentes de cobertura parcial y evidencia requerida.
  `check_acceptance.py` comprueba IDs y referencias; no otorga aceptación.
- Source set `validation` aislado: ocho casos de mundo real, compactación con
  escrituras simultáneas y decodificación real de paquetes excesivos. Sin mundo
  simulado ni casos vacíos que devuelvan éxito. Excluido del JAR normal y sin
  arranque automático por compilar, `test` o `build`.
- Seis escenas F9: pared 10000, sección 4096, 256 diseños PB distintos/pintados,
  frontera de ocho chunks, fluidos 256 y 320; seis controles nativos equivalentes.
  Operaciones por lotes, presupuestos reales, manifiesto durable, chunks cargados
  durante la medición y comprobación de identidad/estado final de fixtures.
- Ventanas finitas con calentamiento separado y hasta 12000 muestras. No se
  sustituyen muestras antiguas al calcular percentiles del intervalo completo.
  JSON incompleto/abortado no se trata como evidencia completa.
- Caché global por peso de cavidades decodificadas: 8 MiB estimados, inmutable y
  sin referencias a mundos. Datos corruptos o futuros no se aceptan ni cachean.
  En notificaciones externas sin cambios de piezas ni familias cercanas, se
  evita reconstruir el índice de conexiones; apoyos/fluidos siguen observándose.
- Contadores de cuerpos públicos preparados y copias de fragmentos encoladas
  por dimensión. Se distinguen de bytes de socket, cabeceras y compresión.
- Perfil de cliente opt-in: intervalo de frame y CPU de render separados,
  GPU/backend/driver, resolución/opciones/recursos, hardware/cuotas/JVM, cámara,
  SHA y huella física. Cambio de cámara/opciones/GUI aborta; no se lee pantalla
  ni mide tiempo GPU.
- Generador cliente opt-in de PROBE en loopback, con destino actual y obsoleto;
  respuestas reales, duplicados, pendientes y cuatro identidades de conexión.
  Hasta 24000 paquetes por ventana; sin acciones de consumo/edición ni mapas
  ilimitados. El solapamiento excluye la espera final de respuestas.
- Observación de bytes lógicos tras guardado flush fuera de la ventana de tick;
  comparación Python de informes equivalentes con umbrales separados. No se
  confunden ventanas incompletas, reposo vacío o perfiles distintos con resultados
  comparables. La herramienta siempre conserva `f9Certified=false`.

## Comprobaciones realizadas

Java **25.0.2**, Gradle **9.2.1**, Minecraft **26.2**, NeoForge **26.2.0.88**,
ModDevGradle **2.0.148**. Resultado correcto de:

```sh
./gradlew --no-daemon --console=plain -PnmgValidation \
  -PnmgValidationProfile=compile compileJava compileTestJava compileValidationJava
```

También se compilan los mismos targets sin activar el perfil de ejecución.
El grafo solo prepara artefactos de plataforma/recursos y clases; no incluye
`test`, JAR ni ningún run. Quedan avisos de API deprecated, sin errores Java.

Hay **115 métodos `@Test` en 45 clases compilados**, nueve nuevos respecto de
la evidencia anterior. Cubren ventanas/tráfico, aislamiento de caché, luz por
máximo/sellado, recortes incompatibles y escenas de índice 10000/4096 con límites
reales. **No se ejecutaron**. Compilar acredita tipos, no resultados ni tiempos.

Revisión estática: catálogo de 72 IDs y referencias correcto, parser de 255
fuentes Java sin errores de sintaxis, recursos/TOML/enlaces/imports de núcleo y
`git diff --check` correctos. Los scripts estáticos no observan ni certifican
el estado de las pruebas de ejecución. El resumen reproducible y hashes de
fuentes están en [el registro JSON](MATRIZ_F9_COMPILACION.json).

## Decisiones e incidencias resueltas

- Se compara cada escena con su equivalente nativo. El reposo vacío se conserva
  solo para calibración; no rebaja el objetivo de referencia.
- La carga usa conexiones reales y PROBE sin mutaciones. Concurrencia de edición,
  consumo y duplicación necesitan sus casos de jugador, no un contador ficticio
  de cuatro actores. Una respuesta ausente no se llama descarte del limitador.
- Los fluidos usan fuentes y cerramiento idénticos y deben conservar su carga
  activa al final. Una escena que exceda límites falla sin reducir cantidades.
- La limpieza solo retira piezas identificadas sin cambios y PB nativo con hash
  durable coincidente. Un corte con imagen no confirmada conserva el bloque.
- Se corrigieron durante la compilación las APIs exactas del bus de eventos
  (`unregister`), la familia de alfombras 26.2 (`Blocks.CARPET.red()`) y la
  conversión exacta de coordenadas `long` a los `int` de `ChunkPos`.
- PB se inspeccionó en `e93bc03f350dfb03b4031f1e479f4a37f28b556e`, schema 2;
  no se alteró ni volvió a compilar su repositorio en esta entrega.

## Pendiente para aceptación

No se marca una fase aceptada. El catálogo cubre **procedimientos para los 72
casos**, con sondas parciales; no son 72 tests de mundo automatizados. Falta
ejecutarlos y evaluar los casos de jugador/GUI/economía, integración externa,
Windows, autenticación y cortes en las distintas ventanas de persistencia y
transferencia. F9 necesita resultados nuevos de escenas/control, frames, cuatro
conexiones, memoria estable y tráfico real. Los procedimientos están en
[la matriz trazable](../acceptance/MATRIZ_TRAZABLE.md), [F9](../acceptance/F9.md)
y [la sonda](../../tools/validation/README.md).

Los **106 tests**, las sesiones y el JAR del informe anterior son evidencia
histórica de `cc63061`, no resultados de estas ampliaciones. No se distribuye
ese JAR antiguo como si incluyera los cambios nuevos. No hay mejora temporal ni
compatibilidad certificada por haber compilado.
