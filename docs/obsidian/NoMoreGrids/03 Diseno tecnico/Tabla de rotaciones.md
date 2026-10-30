# Tabla de rotaciones

Tabla normativa para orientation 0–23. U es arriba local (+Y) transformado al mundo; F es frente local (-Z) transformado al mundo. R = F × U es derecha local (+X). Las columnas de la matriz son R, U y -F. El pivote permanece fijo.

| ID | U | F |
|---|---|---|
| 0 | +Y | -Z |
| 1 | +Y | +X |
| 2 | +Y | +Z |
| 3 | +Y | -X |
| 4 | -Y | -Z |
| 5 | -Y | -X |
| 6 | -Y | +Z |
| 7 | -Y | +X |
| 8 | -Z | +Y |
| 9 | -Z | +X |
| 10 | -Z | -Y |
| 11 | -Z | -X |
| 12 | +Z | +Y |
| 13 | +Z | -X |
| 14 | +Z | -Y |
| 15 | +Z | +X |
| 16 | -X | -Z |
| 17 | -X | +Y |
| 18 | -X | +Z |
| 19 | -X | -Y |
| 20 | +X | -Z |
| 21 | +X | -Y |
| 22 | +X | +Z |
| 23 | +X | +Y |

Cada matriz debe tener determinante +1 y ser distinta. ID 0 es identidad. La inversa y composición se derivan de matrices y se verifican antes de escribir mundo v1. Cambiar el significado de un ID publicado exige migración.

## Subconjunto de seis frentes

| Mirada cardinal | ID |
|---|---|
| Norte -Z | 0 |
| Este +X | 1 |
| Sur +Z | 2 |
| Oeste -X | 3 |
| Arriba +Y | 8 |
| Abajo -Y | 10 |

Arriba/abajo fijan U=-Z. Seis direcciones no implican que seis resultados geométricos sean distintos.

Para orientar respecto al centro de una envolvente local de un bloque se usa T(p)=R·(p-c)+c+t, con c=(8,8,8) en unidades de 1/16. El campo origin16 almacena t, origen del marco local sin rotar. En geometrías recortadas no se cambia c por su centro de masa. El pivote estable de familia declarado prevalece si difiere de este valor.

Ver [[Geometria y coordenadas]] y [[Orientacion]].
