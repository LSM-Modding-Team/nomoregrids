# Guardado y recuperación

## Versiones iniciales

Formato de mundo NMG = 1; PieceRecord = 1; API pública = 1; protocolo de red = 1. La versión 1.0 de esta documentación no significa mod ejecutable 1.0.

Se guarda registro por sección con IDs de recursos textuales, UUID, revisiones, transformaciones, versión de adaptador y datos opacos. Las réplicas contienen formas/material mínimos, nunca otro inventario autoritativo. Se almacena checksum para detectar corrupción, no para acreditar seguridad.

## Escrituras entre regiones

Minecraft no proporciona por sí solo una transacción atómica entre múltiples chunks e inventarios. Se decide implementar diario append-only por dimensión para operaciones NMG con ID, revisiones esperadas, snapshots antes/después y estado de commit.

Antes de publicar cambios se escribe intención completa y marcador de commit durable, agrupando escrituras cuando sea seguro. Operaciones sin commit se descartan; operaciones con commit se reproducen idempotentemente al cargar. Los chunks guardan última secuencia aplicada. El diario se compacta solo después de confirmar checkpoints de las secciones y recursos involucrados.

No se borran journals por asumir que un autosave terminó. El primer prototipo debe demostrar recuperación entre chunks con inyección de fallos antes de habilitar cruces de frontera.

## Inventarios y loot

El diario incluye el coste/recompensa y un recibo de operación. Ítems soltados tienen marca de operación; se deduplican durante recuperación antes de activar recogida. Transferencias a jugadores/contenedores requieren recibo persistente coordinado por el adaptador de inventario.

No se promete atomicidad frente a fallos de disco o mods que escriban fuera del contrato. Ante conflicto entre inventario guardado y diario se pone la operación en cuarentena para reparación; no se reproduce un coste o loot a ciegas. Los casos de crash quedan como puerta de aceptación técnica y no como garantía ya probada.

## Migraciones

Lectura por versión; cada migración produce copia nueva validada antes de reemplazar datos. Respaldar antes de cambiar versión de mundo. Versiones futuras desconocidas se abren como sustituciones sin edición; no se «rebajan».

La ausencia de un registro/mod conserva payload y última forma válida en un componente de sustitución visible. No da loot ni funciona; con dependencia restaurada se valida y recupera. Un dato sin forma confiable bloquea conservadoramente sus casillas declaradas. El diagnóstico identifica el problema.

## Desinstalación

Quitar NMG directamente no conserva las composiciones en vanilla. Se ofrecerá exportación de diagnóstico y comprobación de mundo; la conversión a vanilla solo se permite para piezas exactamente representables. Las demás requieren conservar el mod o exportar diseños, sin borrado automático.

En esta entrega no se ha ejecutado ninguna migración ni prueba de recuperación.
