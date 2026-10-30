# Ejemplos resueltos

## E01 — Dos materiales

Losa inferior de piedra y superior de madera: el volumen queda lleno, pero hay dos piezas. Picar la madera deja la piedra. Pasos sobre el techo usan madera. La casilla no se convierte en un material híbrido.

## E02 — Cubo sobre media altura

Losa en y=[0,8)/16. Cubo con base y=8/16 y techo=24/16. Ocupa dos casillas verticales; en la superior queda aire sobre y=24/16. Si se rompe por cualquiera de sus superficies, se elimina el cubo completo una vez.

## E03 — Junto a losa vertical

Losa en x=[0,8)/16. Cubo se apoya contra x=8/16 y ocupa [8,24)/16. No deja el hueco de media casilla exigido por la colocación vanilla. Solo se acepta si el volumen de la casilla vecina está libre.

## E04 — Forma PB de cuatro unidades

Diseño Paintable Blocks de altura 4/16. Cubo encima ocupa [4,20)/16. No se transforma el diseño en una losa de otro mod; su pintura y herramientas se conservan.

## E05 — Superficie no cuantizada

Superficie a y=1,5/16. Cubo empieza a 2/16 y llega a 18/16. Hueco 0,5/16. Un bloque flotante puede colocarse; una alfombra que exige contacto completo no puede usar ese hueco como apoyo.

## E06 — Panel completo/ajustado

Panel sobre losa inferior: completo mide 16/16 y llega a 24/16; ajustado mide 8/16 y llega a 16/16. Si parte de un panel completo, quedan 8/16 reutilizables. No obtiene un panel completo al romper cada mitad sin condición.

## E07 — Pieza centrada

Diseño de grosor 8/16 situado entre x=4/16 y x=12/16. Hay dos huecos de 4/16, no una mitad libre de 8/16. Una losa de grosor 8/16 no cabe en ninguno; una pieza de 4/16 puede caber si es accesible y compatible.

## E08 — Pendiente

Un cubo con base plana sobre una pendiente se sitúa sin intersección y se ajusta hacia fuera. El contacto puede ser parcial. Una antorcha requiere su parche de anclaje; no se acepta por estar «en la misma casilla» que el soporte.

## E09 — Pintura en pieza compuesta

Diseño pintable junto a losa. Pincel sobre diseño transforma clic a coordenadas locales y cambia solo pintura de la superficie. La losa no se modifica. Recortar después obliga a revisar soporte y geometría; pintar no.

## E10 — Edición invasiva

Una herramienta amplía el diseño PB hacia una losa vecina. El adaptador plantea el nuevo diseño, NMG detecta volumen compartido y rechaza antes de cobrar. No queda una mitad de la edición aplicada.

## E11 — Frontera sin propietario cargado

Una parte de una pieza se ve en chunk cargado pero su sección propietaria no lo está. Se representa y colisiona mediante snapshot; editar/romper espera que todas las regiones necesarias estén cargadas normalmente. No se duplica la pieza ni se fuerza carga por mirar.

## E12 — Competencia multijugador

Dos jugadores intentan reemplazar revisión 15. El primero confirma revisión 16. El segundo recibe STALE_REVISION y conserva materiales. Su nueva vista apunta al estado actual.

## E13 — Dependencia ausente

Falta Paintable Blocks. Se mantiene sustitución con datos y última forma válida. No se entrega piedra ni un ítem explotable. Restaurar la versión compatible permite validar y recuperar.

## E14 — Pistón

Pieza en dos casillas tocada por pistón: se desplaza toda un bloque manteniendo offset. No arrastra otra pieza por compartir casilla. Si el barrido choca, no se mueve aunque el destino final estuviera vacío.
