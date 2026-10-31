# Matriz y F9: implementación con compilación autorizada

Continuación exacta de cc63061. La autorización actual permite compilar Java;
no permite volver a ejecutar tests, clientes, dedicado, escenas o fallos.
Los 106 tests y las sesiones del informe anterior son evidencia histórica,
no resultados de las ampliaciones de esta entrega.

## Instrumentación y caché

- Ventanas finitas con calentamiento separado, hasta 12000 muestras; no se
  sustituyen muestras antiguas al medir percentiles del intervalo completo.
- Caché global por peso de cavidades inmutables decodificadas: 8 MiB estimados,
  sin referencias a mundos; los datos inválidos/futuros siguen rechazándose.
- Contadores de cuerpos públicos preparados y copias de fragmentos encoladas
  por dimensión, separados de bytes de socket, cabeceras y compresión.
- Perfil de cliente opt-in: intervalos entre frames y tiempo CPU de render por
  separado, hardware/opciones y exportación de ventanas completas o abortadas.
  No se mide tiempo GPU ni se lee la pantalla.
- Generador cliente opt-in limitado a loopback y PROBE (sin acciones de consumo/
  edición): mezcla destino actual y revisión intencionalmente obsoleta, recoge
  respuestas reales y no interpreta ausencia de respuesta como rechazo medido.

Compilación realizada con Java 25.0.2, Gradle 9.2.1 y plataforma fijada:
`compileJava compileTestJava compileValidationJava`. No se ejecutó `test` ni
ningún run. Se escribieron pruebas de ventanas, tráfico y aislamiento de caché;
quedan pendientes de ejecución. La compilación acredita tipos, no rendimiento.

Las escenas, procedimientos completos y su trazabilidad se documentan en la
entrega de validación asociada; ninguna fase se declara aceptada por estos cambios.
