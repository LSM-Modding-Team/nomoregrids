# Controles y vista previa

Todas las acciones son reasignables en la categoría No More Grids. Se proponen valores iniciales; los conflictos se detectan y muestran. Ninguna tecla es un requisito fijo.

| Acción | Valor sugerido | Tipo |
|---|---|---|
| Orientación ampliada | O | Alternancia |
| Colocación precisa | P | Alternancia |
| Completa/ajustada | J | Alternancia |
| Fijar/liberar alineación | K | Alternancia |
| Orientación lateral de losas/escaleras | Alt izquierdo | Mantener |
| Ajuste de posición | Alt + rueda | Pasos de 1/16 |
| Eje de ajuste | Alt + botón central | Ciclar X/Y/Z |
| Recorrer candidatos accesibles | Ctrl + rueda | Ciclar |
| Reemplazo de componente | R + usar | Mantener |
| Bloquear/desbloquear conexiones | L | Acción sobre objetivo |

No se captura el teclado ni la rueda cuando hay chat, inventario u otra pantalla abierta. Mantener agacharse permite el comportamiento vanilla de colocar sin abrir una interfaz. Las herramientas de edición compatibles reciben prioridad; si se desea colocar con ellas debe existir una acción explícita y no una interpretación ambigua.

Los modos se recuerdan por jugador/servidor. Al entrar se muestra un indicador breve; fijación de alineación y candidatos se reinician. Se puede configurar reinicio de modos al conectar.

## Vista previa

Muestra geometría final, contorno de apoyo y del componente seleccionado, orientación y consumo incluyendo sobrantes. Verde + marca significa válido; rojo + cruz significa rechazo; ámbar + reloj significa pendiente de datos. Se ofrecen paletas accesibles y opacidad configurable.

Razones breves y localizadas: solapamiento, apoyo insuficiente, familia no adaptada, zona no cargada, protección, entidad obstruyendo, inventario insuficiente, límite de complejidad o revisión desactualizada.

No atraviesa paredes y no muestra información de zonas no enviadas por el servidor. Las superficies translúcidas de la vista previa no alteran el objetivo real de selección.

## Cambios de modo

La orientación ampliada no habilita bloques excluidos. Modo ajustado sobre familia no recortable muestra «sin ajuste para esta pieza» y conserva la previsualización completa; no recorta. La colocación habitual sigue disponible en todo momento.

Relacionado: [[Orientacion]], [[Colocacion]], [[Seleccion rotura y reemplazo]].
