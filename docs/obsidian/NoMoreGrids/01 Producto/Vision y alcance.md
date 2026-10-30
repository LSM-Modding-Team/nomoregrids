# Visión y alcance

## Identidad

Nombre público: **No More Grids**. Identificador técnico: **nomoregrids**. Abreviatura de documentación: NMG. Paquete previsto: net.nicomar.nomoregrids. Versión inicial de diseño: 1.0; primera entrega ejecutable prevista: 0.1.0 experimental.

El nombre representa la desaparición de la exclusividad de la casilla, no la eliminación de todo ajuste. Se conserva una retícula de 1/16 para posiciones reproducibles y un coste controlable.

## Objetivo

Construir con superficies y volúmenes reales: apoyar bloques en losas, llenar espacios junto a losas verticales, combinar materiales y aprovechar diseños recortados. El jugador dispone de una vista previa y controles configurables; no necesita conocer la representación interna.

## Alcance final

- Losas verticales y escaleras verticales disponibles de forma predeterminada.
- Orientación ampliada opcional para otras familias.
- Piezas independientes que comparten casillas y cruzan sus límites.
- Desplazamiento preciso, combinaciones, recortes autorizados y alineación repetible.
- Selección, rotura y reemplazo por componente.
- Integración progresiva con apoyos, conexiones, materiales, fluidos y mecanismos.
- API de adaptación y Paintable Blocks como primera integración específica.
- Cliente y servidor con las mismas reglas verificadas por el servidor.

## Fuera de alcance

Rotación libre, ángulos de 45°, escalado arbitrario, física estructural general, editor universal de mallas, conversión automática de cualquier máquina de otro mod y compatibilidad garantizada con todos los mods.

NMG no transforma toda construcción en fragmentos de 1/16: una pieza completa sigue siendo una pieza completa. No añade nuevos materiales decorativos ni máquinas de producción como requisito.

## Entregas

El alcance final no equivale al contenido de 0.1.0. [[Plan de fases]] fija puertas de aceptación. Los sistemas no adaptados conservan su representación nativa y no pueden entrar en composiciones inseguras.

Esta entrega contiene documentación y un esqueleto con entradas mínimas. No registra contenido nuevo de Minecraft ni modifica Paintable Blocks. Clase principal: NoMoreGrids; paquete net.nicomar.nomoregrids; mod ID nomoregrids.
