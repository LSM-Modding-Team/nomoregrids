# F1 — identidad e índices parciales, 2026-10-29 (America/Lima)

## Implementación

`PieceRecord` es un registro inmutable para familias de cajas integrales, con
UUID, revisión, origen, rotación, pivote estable, procedencia, estado de bloque,
datos opacos del adaptador/material/ítem, grupo y banderas. Mover conserva UUID
y aumenta revisión. Las formas de ocupación, colisión y selección se mantienen
separadas. No se implementa aún el contrato de parches de apoyo o render.

`PieceIndex` produce instantáneas inmutables de una región explícitamente
suministrada. Cada pieza tiene un registro; las casillas y secciones solo
contienen referencias UUID. La sección propietaria se deriva del mínimo de la
envolvente transformada. No hay duplicación de inventarios ni carga de chunks.

Los cambios por lote comprueban identidad, revisiones, solapamiento en el estado
final y presupuestos de componentes/referencias. Un rechazo conserva la
instantánea anterior. Las reducciones de límites permiten retirar piezas y no
borran las existentes. Los datos opacos se copian defensivamente y se limitan
antes de copiarlos.

## Límites deliberados

Este índice no es un almacén de mundo ni una transacción con inventarios. Faltan
el contenedor NeoForge, captura nativa, permisos, alcance, parches de apoyo,
recibos, diario, persistencia de secciones y red. No se conecta al arranque ni se
habilita composición. La instantánea de cajas no codifica geometría externa.
La comprobación de contención por caja es conservadora: puede rechazar una
forma equivalente particionada de otra manera; no se degrada para aceptarla.

`PieceIndex.changed` valida solo los tres presupuestos del índice, no todos los
límites de persistencia, render o colisión agregada. Nunca debe sustituir al
pipeline de operaciones y sus verificaciones de mundo.

## Verificación

Pruebas JUnit escritas: datos inmutables, propietario negativo, una autoridad
entre ocho secciones, retirada sin referencias residuales, revisión obsoleta,
UUID duplicado, cambio invasivo, intercambio de dos piezas y reducción de límites.
NO EJECUTADAS por la restricción de AGENTS.md.

Ejecutados: comprobador estático del repositorio, `git diff --check` y análisis de los 51 fuentes Java con Tree-sitter 0.25.2 / gramática Java 0.23.5 (sin errores sintácticos). El parser no resuelve tipos ni verifica APIs.
No se compila ni se ejecuta Minecraft. F0 y F1 siguen ABIERTAS; F2–F9 pendientes.
