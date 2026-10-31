# Revisión estática de rendimiento — 2026-10-31

Base revisada: `0872ca4c03690e66889e1d274961dfc05e8308e5`, rama `26.2`.

## Trabajo reducido

- El índice de piezas actualiza las casillas, secciones y grupos afectados,
  reutilizando conjuntos inmutables de las demás entradas. Se comprueban los
  límites en las claves que pueden haber cambiado y se conservan las pruebas de
  solapamiento y revisiones. Se mantienen listas ordenadas por adaptador para
  camas, vías, circuitos y funcionales, en vez de recorrer y ordenar todos los
  registros en cada tick. Los mapas generales todavía se copian al editar.
- La planificación omite conexiones, grupos, circuitos, redistribución de
  fluidos y comprobaciones de efectos cuando los datos de la operación hacen
  que esas tareas no puedan producir cambios. Los circuitos reutilizan los
  grafos de la misma instantánea; la búsqueda de señal visita las casillas
  cercanas al puerto. Los cálculos de ocupación de vías consultan el índice y
  las diferencias del plan, no todo el mundo para cada casilla vecina.
- Los efectos de circuitos y fluidos se consultan por prefijo ordenado. Una
  caché de valores de señal se invalida al cambiar los checkpoints y tiene un
  máximo de 65536 entradas; si se supera, usa la consulta completa. La decisión
  de notificar vecinos se calcula una vez por operación.
- La relajación de fluidos termina tras una pasada sin cambios, sin publicar
  trabajo parcial. Las consultas interiores evitan construir coordenadas de
  vecinos; los conteos por tipo se calculan al crear la cavidad y las reacciones
  inspeccionan las 256 posiciones de la cara pertinente. La malla omite tipos
  ausentes, reutiliza el búfer de visibilidad y prepara el material por cara.
  Una ventana estable puede completarse antes que con las 16 pasadas anteriores;
  sigue requiriendo comprobación del ritmo de propagación en el juego.
- Para Paintable Blocks se añadieron cachés LRU separadas de 16 MiB para
  primitivas y 8 MiB estimados para el índice de caras, sin referencias al mundo.
  El índice usa hasta 262144 referencias y vuelve al recorrido completo si no
  puede acotar la geometría. Las consultas conservan la comprobación exacta de
  planos; los materiales y modelos se reutilizan dentro de cada construcción.

## Comprobaciones y alcance

- Java 25 / Gradle 9.2.1: `compileJava compileTestJava compileValidationJava`
  completados correctamente. No se ejecutaron tests ni se inició Minecraft.
- Las comprobaciones estáticas del repositorio, la matriz trazable y la sintaxis
  Java se ejecutaron por separado. No miden tiempo ni ejecutan casos de juego.
- Se añadieron casos de regresión para comparar el índice incremental con una
  reconstrucción completa, conservar grupos, verificar conteos/reacciones de
  fluido y comparar el índice espacial con búsquedas exhaustivas. Sus fuentes
  están compiladas; los casos no se ejecutaron.
- La compilación inicial sin acceso a red falló al no encontrar una IP para
  los bloqueos de Gradle; se repitió con el acceso de red del entorno habilitado
  y `--offline`, usando las dependencias ya presentes.

No hay mediciones de FPS, latencia, uso de memoria ni resultados comparativos.
No se afirma haber encontrado todos los cuellos de botella. Las escenas y el
procedimiento para medirlos permanecen en `docs/acceptance/F9.md`; están
pendientes de ejecución autorizada, al igual que las pruebas de fluidos,
redstone, guardado, clientes y compatibilidad Paintable Blocks.
