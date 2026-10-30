# Geometría y coordenadas

## Coordenadas

X crece al este, Y arriba y Z al sur. Norte es -Z. Unidad q = 1/16 de bloque. Posiciones persistentes usan enteros de 64 bits; geometría local usa racionales cuando sea posible y dobles controlados en el puente.

Casilla de una coordenada q: floorDiv(q,16), nunca división truncada hacia cero. El resto floorMod queda entre 0 y 15. Un origen q=-1 pertenece a casilla -1 con desplazamiento 15/16.

Las envolventes espaciales son semiabiertas [mínimo,máximo). Una cara sobre el límite no ocupa la casilla siguiente si no existe volumen al otro lado.

## Transformaciones

24 matrices de rotación con entradas -1/0/1 y determinante +1. No hay reflejos ni escalado. Se fijará una tabla canónica versionada cuya orientación 0 sea identidad. Serializar ordinales de enums externos queda prohibido.

La tabla normativa completa está en [[Tabla de rotaciones]]. Su ID 0 es identidad. Se verifican unicidad, inversas y composición antes de fijar datos ejecutables; cualquier cambio posterior exige migración.

Rotar sobre pivote local y trasladar produce coordenadas mundiales. El rayo usa transformación inversa. Los límites de la envolvente se recalculan a partir de geometría, no de sumar ángulos.

## Intersección

Dos cajas con intersección estrictamente positiva en X/Y/Z se solapan; tocar frontera es válido. Geometría general utiliza volúmenes convexos recortados y pruebas de separación; no basta con AABB salvo rechazo rápido.

Tolerancia numérica: 1e-7 bloque para clasificar contactos. No puede convertir un hueco de 0,5/16 en apoyo. Resultados casi degenerados se recalculan de forma estable o se rechazan como indeterminados; el cliente no fuerza aceptación.

## Índices

Envolvente -> casillas candidatas -> máscara 16³ conservadora -> geometría exacta. La máscara es filtro; un positivo exige prueba exacta para pendientes. El raycast ignora volumen no ocupado y devuelve identidad de superficie del adaptador.

## Dimensiones iniciales

Cada pieza atómica de F2–F6 tiene envolvente local máxima de 16/16 por eje. Al desplazarse puede intersectar hasta 8 casillas y 8 chunks/secciones en una esquina. Estructuras mayores se representan como grupos de piezas atómicas; no se parte un diseño solo para duplicar sus datos.

La geometría externa más fina se conserva; la posición sigue en q. Soportar piezas atómicas de tamaño superior queda reservado para una versión de formato posterior.
