# Luz y materiales

## Iluminación decidida

Se conserva el motor de luz a resolución de casilla; no se promete iluminación de 1/16. Cada contenedor calcula una emisión máxima de sus componentes, no una suma. Una pieza luminosa que atraviesa varias casillas aporta emisión en las casillas con volumen emisor real.

Si la unión de materiales opacos sella el volumen y las caras de una casilla, se usa bloqueo completo. Para ocupación parcial se usan formas de oclusión por cara y una atenuación conservadora: como valor de reserva, 0 para aire abierto y la máxima atenuación aplicable de los materiales no totalmente opacos. Los materiales especiales necesitan perfil óptico.

Antes de habilitar una familia se prueba que no produzca fugas inaceptables. Vidrio tintado bloqueador de luz exige política explícita; no se trata como cristal transparente de colores.

Puede haber diferencias visuales respecto de un motor de luz volumétrico. No se falsea su disponibilidad. Sombras, shaders y motores alternativos de render se certifican por separado.

## Propiedades

- Dureza/herramienta/loot: componente apuntado.
- Pasos y sonidos: superficie que sostiene el punto de contacto; en empate, área de apoyo mayor y UUID estable.
- Fricción/rebote: material de mayor área de contacto; efectos de daño se aplican si existe contacto relevante y se deduplican por entidad/tick/tipo.
- Fuego: combustibilidad por componente y propagación por superficies expuestas.
- Explosión: se visita cada pieza una vez, considerando exposición y resistencia; se retira entera salvo adaptador de destrucción parcial.
- Gravedad: solo en familias adaptadas; una pieza entera cae manteniendo identidad y geometría, nunca una copia por casilla.
- Transformación por agua, oxidación o herramienta: candidato nuevo validado transaccionalmente.

Pintar con aspecto de otro material no cambia propiedades físicas si el mod original no lo hace. En Paintable Blocks se conserva esa separación.

## Casos excluidos al principio

Superficies con comportamiento de entidad, crecimiento, derretimiento o portales permanecen nativas hasta una adaptación. No se ejecutan métodos arbitrarios de un bloque sobre el contenedor pretendiendo que sea una copia del mundo.
