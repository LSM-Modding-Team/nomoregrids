# Pruebas escritas

Casos JUnit Jupiter del núcleo, geometría, índices, codecs, operaciones/diario,
red, fragmentos, apoyo, convexos, funcionales, mecanismos, secciones
y presupuestos. Prueban contratos concretos; no sustituyen integración del juego.

La validación histórica ejecutó 106 tests sin fallos; ver
[su evidencia](../../docs/evidence/VALIDACION_EJECUTADA_2026-10-30.md).
Las fuentes actuales están compiladas mediante `compileTestJava`, sin volver a
ejecutar tests. Las antiguas pruebas de cavidades se retiraron con esa función.

`test` requiere petición explícita conforme a AGENTS.md. `compileTestJava`
comprueba tipos sin ejecutar JUnit. No hay GameTests conectados. Las sondas de
mundo real están en `tools/validation`, como source set separado, con
[instrucciones](../../tools/validation/README.md). Los scripts Python de `tools`
verifican estructura y sintaxis sin compilar ni ejecutar Java.
