# Contenedores y regiones

## Términos

Casilla: volumen de un bloque. Sección: 16×16×16 casillas. Chunk: columna de secciones. Pieza: componente lógico; no equivale a entidad de bloque vanilla.

## Representación

Un bloque técnico nomoregrids:composite_cell ocupa cada casilla no nativa que contiene volumen de piezas. Su entidad CellView contiene referencias UUID+revisión, fragmentos locales de forma y propiedades agregadas. No ejecuta un ticker si no existe trabajo de simulación.

El contenedor no es un ítem obtenible. Su loot no genera un contenedor genérico; delega en la pieza retirada y operación autorizada.

## Autoridad

La sección del mínimo de la envolvente mundial es ownerSection; se ordena por X,Y,Z. Guarda una sola copia autoritativa del PieceRecord. Las otras secciones tienen índices e instantáneas de lectura con revisión y geometría/material mínimo. Son réplicas, no otras piezas ni inventarios.

Una consulta de colisión/luz en una sección cargada usa su instantánea, aunque el propietario no esté cargado. Una edición exige cargar normalmente todas las secciones afectadas y el propietario; no solicita tickets por sí misma. Si faltan, queda bloqueada.

Una frontera no cargada no es aire para colocar ni simular. El jugador ve el rechazo correspondiente.

## Ciclo

1. Capturar bloque nativo y datos mediante adaptador.
2. Planificar todas las casillas y referencias.
3. Validar geometría/permiso/inventario.
4. Registrar intención persistente y confirmar.
5. Instalar contenedores y réplicas.
6. Emitir consecuencias deduplicadas.

Si desaparece el último componente, restaurar aire. Si queda una única representación nativa exacta, simplificar mediante transacción sin cambiar identidad lógica de una operación en curso.

## Chunk y descarga

Una instantánea de sección es inmutable para lectores. No se mantiene todo chunk referenciado permanentemente cargado. Se libera render/caché al descargar, no los datos. Al cargar se contrastan revisiones con el diario y se reparan índices antes de permitir edición.

La gestión de chunks y sus hooks debe verificarse en NeoForge 26.2.0.88 antes de aceptar F1.
