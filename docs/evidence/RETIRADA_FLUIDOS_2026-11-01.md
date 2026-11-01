# Retirada de fluidos en celdas compuestas — 2026-11-01

Por solicitud del usuario se eliminó la interacción entre fluidos y celdas
compuestas: simulación de cavidades, render, puente con vanilla, cubos,
efectos de entidad, estado de bloque y datos de red y guardado de cavidades.
Las celdas no son reemplazables por fluidos; colocar o mover piezas a una
casilla con fluido o waterlogged se rechaza antes del commit. Los fluidos
nativos fuera de las celdas conservan su comportamiento vanilla.

El protocolo de red pasa a 10, la réplica de celda a esquema 3 y el checkpoint
de simulación a esquema 4. No se ofrece migración de mundos anteriores por
instrucción explícita del usuario. Se retiraron las escenas y pruebas de
cavidades; la matriz conserva el caso de rechazo en mojado.

Se compilaron `compileJava`, `compileTestJava` y `compileValidationJava` tras
limpiar la salida. La regeneración de dependencias necesitó configurar el proxy
y el almacén de certificados del entorno. Las comprobaciones estáticas del
repositorio y de los 67 casos de la matriz pasaron. No se inició Minecraft ni
se ejecutaron tests Java; los casos de cubos, entrada por
flujo, lava, agua, colocación y movimiento en servidor/cliente requieren prueba
posterior.
