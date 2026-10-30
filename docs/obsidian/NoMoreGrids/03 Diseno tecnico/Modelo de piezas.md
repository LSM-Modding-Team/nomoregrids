# Modelo de piezas

## Registro lógico PieceRecord v1

| Campo | Decisión |
|---|---|
| pieceId | UUID único, persistente |
| revision | Entero de 64 bits, creciente por cambio |
| adapterId / adapterVersion | Identificador estable y versión de datos |
| sourceBlockId / sourceState | Registro y propiedades de origen |
| origin16 | Tres enteros de 64 bits; traslación del marco local sin rotar en unidades de 1/16 |
| orientation | Rotación ortogonal 0–23; familias limitadas usan subconjunto |
| localBounds | Envolvente local conservadora |
| geometryPayload | Datos del adaptador, no una malla como verdad única |
| materialPayload | Datos físicos y de material cuando no derivan del origen |
| itemPayload | Datos necesarios para recuperación según reglas |
| ownerSection | Sección canónica que guarda el registro |
| groupId | Opcional, estructuras inseparables |
| flags | Bloqueo de conexión y estado de sustitución |
| schemaVersion | 1 |

No se guardan cachés de mallas como única copia de un diseño. Las derivadas se reconstruyen. Los datos del adaptador se preservan sin reinterpretarlos con recetas genéricas.

## Geometrías

BoxUnion para cubos/losas/escaleras; Layered para paneles y capas; External para diseños con polígonos y pendientes. Cada adaptador proporciona ocupación, colisión, selección, apoyo y representación visual a partir del mismo estado inmutable.

El pivote no cambia al recortar. Ediciones generan una instantánea nueva; la anterior existe hasta que la transacción se confirma.

## Ítems

Componentes de datos NMG guardan tipo y tamaño de fragmento. Las piezas completas se recuperan como ítems nativos si los datos caben y su adaptador lo permite. Un «ítem de pieza conservada» NMG solo se usa cuando una representación nativa no pueda expresar los datos; nunca permite recuperar algo que el loot prohíba.

## Identidad y revisiones

Mover, girar o editar conserva UUID y aumenta revisión. Cortar una pieza en dos objetos del mundo mantiene UUID del componente retenido y crea uno para el nuevo, con procedencia de operación; no duplica el registro. Al recoger, la identidad de mundo no obliga a impedir apilamiento de ítems equivalentes.

Piezas de datos desconocidos pasan a modo sustitución: colisión conservadora de la última instantánea válida, sin funcionamiento ni loot convertible. Se conserva carga opaca original.
