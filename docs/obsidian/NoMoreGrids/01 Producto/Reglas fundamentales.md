# Reglas fundamentales

## Contratos

1. Una casilla no tiene propietario exclusivo: contiene referencias a cero, una o varias piezas.
2. Una pieza tiene identidad única aunque ocupe varias casillas.
3. Dos sólidos pueden tocarse; no pueden interpenetrarse con volumen positivo.
4. La posición se cuantiza a 1/16. No se redondea destructivamente la geometría de un adaptador.
5. Forma visible, colisión, ocupación, selección y apoyo tienen contratos distintos y coherentes.
6. Cada material conserva su procedencia y propiedades; compartir casilla no implica fusión.
7. Colocar, editar, reemplazar y mover son operaciones completas: se validan antes de consumir o mutar.
8. El servidor es la autoridad del mundo y de los materiales.
9. La vista previa es una predicción; nunca concede una colocación por sí misma.
10. Los errores, límites o dependencias ausentes nunca autorizan borrar piezas o regalar materiales.
11. Lo que no tiene adaptador válido se mantiene nativo o se rechaza con una razón.
12. No se activa simulación física general: los bloques que flotan normalmente pueden seguir flotando.

## Distinciones obligatorias

- Seis posiciones de losa no son seis frentes de un bloque.
- Forma de losa no significa identidad de losa vanilla.
- Una textura transparente no implica espacio físicamente vacío.
- Compartir casilla no implica conexión eléctrica, adhesión ni inventario compartido.
- Ocultar una cara por otra pieza no elimina pintura ni geometría persistente.
- «Implementado», «compilado», «probado» y «compatible» son estados diferentes.

## Precedencia documental

Rechazo o cambio explícito posterior del usuario > decisiones técnicas vigentes > reglas por familia > ejemplos. Si dos notas entran en conflicto, el agente corrige ambas antes de ampliar el comportamiento. Las cifras de rendimiento son presupuestos iniciales, no mediciones.
