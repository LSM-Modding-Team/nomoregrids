# Red y multijugador

## Autoridad

Cliente y servidor requieren NMG con protocolo compatible. El cliente envía intención, no un estado de mundo aceptado. El servidor recalcula alcance, superficie, orientación válida, inventario, permisos y revisiones.

Handshake incluye protocolo, esquema, adaptadores con versiones, hash de perfiles/reglas y límites. Diferencias físicas incompatibles impiden entrar con explicación. Preferencias visuales pueden diferir.

## Mensajes previstos

| Mensaje | Dirección | Contenido |
|---|---|---|
| SessionRules | S→C | Capacidades, perfiles y límites |
| SectionSnapshot | S→C | Piezas/referencias visibles al comenzar seguimiento |
| PieceDelta | S→C | UUID, revisión antes/después y campos cambiados |
| OperationIntent | C→S | ID, objetivo, modo, coordenadas y revisión esperada |
| OperationResult | S→C | Commit o rechazo con causa |
| ResyncRequest | C→S | Revisión desconocida o delta perdido |
| AdapterState | S→C | Datos específicos autorizados |

No se envían inventarios internos ni datos privados a clientes sin permiso. Render obtiene solo datos públicos mínimos.

## Revisiones

Si la revisión antes no coincide, el cliente descarta delta y solicita snapshot. Paquetes repetidos no repiten consumo. Dos jugadores sobre una pieza: confirma el primer plan válido; el segundo se rechaza como desactualizado y recibe nueva vista.

El cliente puede previsualizar y animar provisionalmente, pero no conserva piezas aceptadas localmente sin confirmación. Cancelación o rechazo restaura la predicción.

## Límites iniciales

Intento C→S máximo 8 KiB decodificados. Snapshot/delta fragmentado en bloques de hasta 64 KiB; ensamblado por sección máximo 2 MiB. Verificación de tamaño antes de asignar y antes de descomprimir. Máximo 4 mensajes grandes en ensamblado por cliente, caducidad 10 segundos.

Límite 20 intenciones por segundo y jugador con ráfaga de 40. Máximo 4 MiB/s de datos NMG por cliente; cola coalescida conserva última revisión y prioriza interacción cercana. Al saturar se difiere envío, no se salta validación.

Cambios de una pieza se agrupan por tick cuando no alteran el resultado lógico. La red transmite identificadores y datos versionados, nunca clases ejecutables del servidor.

Ver [[Limites y rendimiento]].
