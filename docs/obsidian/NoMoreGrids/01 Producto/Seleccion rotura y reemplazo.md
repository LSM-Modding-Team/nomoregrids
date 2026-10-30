# Selección, rotura y reemplazo

## Selección

El trazado recorre casillas y consulta piezas únicas; transforma el rayo a coordenadas locales para encontrar la primera superficie real. El resultado incluye UUID de pieza, cara/superficie, punto local y mundial, distancia y revisión.

No se selecciona solo la envolvente: un rayo puede pasar por un hueco real. Superficies coincidentes se desempatan por distancia, prioridad de superficie y UUID estable; el usuario puede recorrer candidatos accesibles dentro de una tolerancia de 1/64 de bloque en torno al punto señalado. No incluye piezas detrás de un sólido.

## Rotura

Progreso por pieza y jugador usando herramienta, dureza y reglas del origen. Cambiar de objetivo cancela el progreso. El servidor comprueba alcance, línea de visión y revisión al terminar. Retirar una pieza emite loot una sola vez y elimina todas sus referencias. Después se resuelven pérdidas de apoyo.

Una explosión y una rotura simultánea no producen dos recompensas. Se deduplican por identidad de operación. Los bloques funcionales no se rompen por una de sus referencias como si fueran copias.

## Reemplazo

Acción R + usar, configurable. Construye un candidato usando la pieza del inventario, el mismo pivote mundial y orientación compatible. No escala ni deforma la nueva pieza para que encaje. Conserva el soporte solo si la nueva geometría lo cumple.

En supervivencia exige la herramienta y el tiempo de rotura de la pieza anterior. Durante ese tiempo se muestra la previsualización; al finalizar se revalidan inventario y mundo. Una única transacción retira, entrega loot, consume y coloca. En creativo es inmediato.

Si no cabe o cambió el objetivo, no se modifica nada. Los datos propios de la pieza anterior no se transfieren a otro material salvo regla del adaptador.

## Herramientas externas

La acción nativa de pintar/cincelar modifica el componente seleccionado, no el contenedor global. La edición geométrica pasa por validación de [[Transacciones y eventos]]. Pintar no consume el bloque sostenido como material de construcción.
