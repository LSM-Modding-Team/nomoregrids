# Riesgos y comprobaciones

No quedan preguntas de producto bloqueantes. Sí existen hipótesis técnicas que solo se pueden verificar con fuentes exactas, compilación y ejecución. No se presentarán como resueltas por escribir esta bóveda.

| Riesgo | Decisión y comprobación |
|---|---|
| APIs 26.2 distintas de guías 26.1 | Consultar primer y fuentes exactas; F0 valida firmas |
| Colisión/selección a través de chunks | Instantáneas locales y pruebas de frontera F1 |
| Persistencia entre regiones e inventario | Diario/recibos y fallos inyectados |
| Herramientas PB consultan entidad nativa | Puente explícito, no confiar solo en evento |
| Formas densas disparan coste | Presupuestos de primitivas/malla/bytes y rechazo |
| Render parcial/transparencias | Mallas recortadas y pruebas con materiales |
| Luz no coincide con geometría fina | Motor de casilla, política óptica y límites declarados |
| Fluidos junto a celdas | Verificar rechazo sin consumo y comportamiento nativo exterior |
| Bloques funcionales requieren mundo nativo | Adaptación por familia; sin emulación universal |
| Otros mods esperan un bloque por casilla | API por componente y matriz por versión |
| Transacciones externas fuera de control | Contratos y cuarentena ante conflicto |
| Cambios de perfil invalidan piezas | Validación y conservación, sin borrado |
| Más rotaciones que formas distintas | Identidad de orientación separada de caché |

## Qué se comprobó para esta entrega

Lectura del repositorio Paintable Blocks enlazado, estructura de su bóveda, README y clases clave; consulta de documentación oficial general de NeoForge, primer 26.2 e índice Maven de 26.2.0.88. Generación y validación estructural de esta documentación.

No se descargaron/inspeccionaron aquí todas las fuentes exactas de Minecraft/NeoForge. No se verificaron APIs por compilación. La investigación técnica del otro repositorio se usa como referencia secundaria identificada, no como ejecución propia.

## Si falla una hipótesis

Aislar el caso mínimo, documentar resultado y escoger implementación que preserve contratos. Si obliga a cambiar comportamiento acordado, registrar decisión y comunicarla, sin ocultar la limitación bajo una etiqueta «compatible». No ampliar el alcance con un motor completo nuevo sin necesidad.
