# Glosario

| Término | Significado |
|---|---|
| Casilla | Espacio mundial de 1×1×1 bloques |
| Pieza/componente | Objeto constructivo con identidad y datos propios |
| Composición | Varias piezas coexistiendo espacialmente |
| Cuadrícula de precisión | Posiciones separadas por 1/16 de bloque |
| Orientación | Rotación de una pieza; no necesariamente traslación |
| Pivote | Referencia local estable para situar/girar |
| Geometría local | Forma antes de colocarla en el mundo |
| AABB/envolvente | Caja alineada con ejes que contiene una forma |
| Voxel | Pequeña celda volumétrica; no exige crear un bloque de Minecraft |
| Raycast/trazado | Buscar primera superficie a lo largo de un rayo |
| Colisión | Forma que impide movimiento |
| Ocupación | Volumen reservado frente a otras piezas |
| Apoyo | Superficie necesaria para sostener un elemento |
| UV | Coordenadas para aplicar una textura |
| Chunk | Columna de terreno que Minecraft carga/guarda |
| Sección | Región de 16×16×16 casillas |
| Adaptador | Puente que conserva el comportamiento de otra familia/mod |
| API | Contratos públicos para integrar código externo |
| Snapshot/instantánea | Copia coherente e inmutable de un estado |
| Delta | Diferencia entre revisiones |
| Revisión | Número que identifica cambios de una pieza |
| UUID | Identificador persistente de una pieza |
| Payload | Datos transportados o guardados |
| Codec | Reglas para codificar/leer datos |
| Transacción | Operación que se valida y confirma como unidad |
| Idempotencia | Repetir una operación no duplica sus efectos |
| Diario/journal | Registro para reconstruir operaciones tras interrupción |
| Cuarentena | Datos conservados pero bloqueados por conflicto o incompatibilidad |
| Tick | Paso lógico de actualización del juego |
| Caché/LRU | Resultados reutilizados; se expulsan los menos usados recientemente |
| Puerto | Zona de conexión local de una pieza |
| Vanilla/nativo | Comportamiento original de Minecraft o del mod de origen |
| Waterlogged | Bloque que contiene agua en su representación nativa |
| p95 | Valor por debajo del cual cae el 95 % de mediciones |
| Puerta de aceptación | Condiciones necesarias para habilitar/publicar una fase |

En esta documentación, «píxel» se usa informalmente para una unidad de 1/16 de bloque; no depende de la resolución del paquete de texturas.
