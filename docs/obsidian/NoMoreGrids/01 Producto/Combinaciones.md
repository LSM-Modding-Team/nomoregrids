# Combinaciones

Se acepta cualquier combinación habilitada cuyo volumen ocupado no se solape, cumpla apoyo y no exceda límites. No existe una lista cerrada de parejas como única fuente de validez.

| Caso | Resultado |
|---|---|
| Losa inferior + superior de distinto material | Dos componentes formando un cubo |
| Losa norte + sur | Dos mitades verticales |
| Losa este + oeste | Dos mitades en el otro eje |
| Dos losas perpendiculares canónicas | Rechazo: se intersectan |
| Escalera + losa | Permitido solo si cabe su geometría real |
| Dos escaleras | Permitido si no se superponen tras resolver conexiones |
| Panel junto a losa vertical | Permitido si su grosor y posición caben |
| Cubo sobre losa inferior | Base a 8/16; techo a 24/16 |
| Cubo junto a superficie a 8/16 | Desplazado medio bloque; atraviesa límites si hace falta |

Cada componente mantiene sonido, textura, dureza y resultado de rotura. Dos losas iguales tampoco se fusionan automáticamente. Se puede deduplicar su representación visual, nunca su identidad.

## Orden de cálculo

Primero se plantea la pieza y sus conexiones; después se comprueba intersección. Las conexiones opcionales que produzcan invasión se omiten. La operación no puede modificar piezas vecinas para abrir espacio sin una acción explícita.

## Accesibilidad

Que exista un hueco no permite colocar a través de una pared. El objetivo debe ser alcanzable por el trazado de selección. Una ranura utilizable puede alojar una pieza fina aunque otra pieza ocupe el resto de la casilla.

## Grupos

Compartir casilla no crea un grupo. Los grupos se reservan para estructuras de varias partes (puerta/cama) y operaciones de mecanismos. No se añade herramienta general de pegar bloques ni se fusionan materiales.

Relacionado: [[Modelo de piezas]], [[Seleccion rotura y reemplazo]].
