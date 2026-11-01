# Plan de fases

Cada fase requiere documentación actualizada, compilación cuando haya código y evidencia de ejecución pertinente. No se declara completa por crear clases vacías. El mandato permite escribir F0–F9 sin esperar su aceptación. Hubo validación ejecutada puntual (ver evidencia); no autoriza ejecuciones futuras. Hay código y puentes al mundo en todas ellas para los perfiles explícitos de esta edición; su integración sigue pendiente de verificación. Ninguna fase está aceptada. Ver `docs/ESTADO_DESARROLLO.md` y `docs/evidence`.

| Fase | Entrega | Puerta de salida |
|---|---|---|
| F0 | Entorno fijado, registros técnicos y sondas de API | Resolver Java/Gradle/NeoForge exactos; cliente y dedicado arrancan |
| F1 | Núcleo, pieza/contendor, guardado y red mínima | Colocar/retirar pieza simple; negativos y fronteras; recuperación de fallos |
| F2 | Losas/escaleras/cubos, orientaciones, composición y controles | Matriz geométrica, loot, selección y preview en multijugador |
| F3 | Paneles/barrotes/vallas/muros, conexiones y recortes | Conservación, extremos/modelos y estabilidad de conexión |
| F4 | Decoración, apoyos, materiales y luz | Pérdida de apoyo, contactos, iluminación y efectos |
| F5 | Fluidos en celdas compuestas descartados por el usuario | Verificar rechazo sin consumo y flujo nativo fuera de celdas |
| F6 | Paintable Blocks | Ciclo completo de herramientas/datos, dedicado y carga |
| F7 | Funcionales y grupos | Inventarios/menús/puertos únicos y persistencia |
| F8 | Redstone/raíles/pistones | Tiempo lógico, trayectorias y movimientos transaccionales |
| F9 | Optimización y certificación | Escenas de rendimiento, adaptadores fijados y recuperación |

## Primer lanzamiento

La propuesta inicial de lanzamiento requería F0–F2 aceptadas y construcción seca. No se ha producido ese lanzamiento ni se ha aceptado ninguna fase. La rama experimental actual contiene además código F3–F9 por autorización del usuario; su presencia no acredita su funcionamiento ni compatibilidad Paintable Blocks.

Las fases no redefinen el objetivo: el alcance completo se mantiene. F6 puede desarrollarse en paralelo lógico a F5 cuando sus dependencias F1–F4 estén listas, pero ninguna integración se publica sin sus pruebas. Esto no solicita agentes paralelos ni autoriza cambios remotos.

## Orden interno F1

Geometría pura -> identidad/revisiones -> un contenedor -> conversión nativa -> persistencia local -> múltiples casillas -> múltiples secciones/chunks -> diario/recuperación -> red -> pruebas dedicadas.

Si el motor no expone un hook necesario, se registra un experimento mínimo con fuentes exactas; se resuelve con extensión pública o hook aislado. No se reescribe todo el almacenamiento para ocultar la dificultad.

## Evidencia por entrega

Lista de archivos y módulos, decisión asociada, pruebas ejecutadas y resultados, pasos manuales reproducibles, limitaciones, versiones y siguiente fase. Se distingue «no ejecutado» de «falló».
