# Decisiones técnicas

Todas son decisiones vigentes. Las comprobaciones pendientes son validación de ejecución, no preguntas al usuario ni cambios implícitos de alcance.

| ID | Decisión | Consecuencia |
|---|---|---|
| D01 | Nombre No More Grids; ID nomoregrids | Identidad única |
| D02 | Minecraft 26.2 / NeoForge 26.2.0.88 | Sin soporte multiversión inicial |
| D03 | Java 25; Gradle 9.2.1; ModDevGradle 2.0.148 como línea inicial | F0 comprueba resolución y firmas antes de código dependiente |
| D04 | Posición entera en unidades 1/16 | Negativos con floorDiv/floorMod |
| D05 | Geometría independiente de la retícula | Pendientes y diseños externos conservados |
| D06 | 24 rotaciones propias; seis frentes para otras familias | Sin reflejos ni 45° |
| D07 | Contenedor por casilla + registros por sección | No reemplazar todo el motor |
| D08 | UUID único + revisión de 64 bits | Sin duplicados por cruce de casillas |
| D09 | Pieza atómica de envolvente máxima 1 bloque por eje | Hasta 8 casillas; mayores como grupos |
| D10 | Bloques nativos permanecen nativos mientras sean representables | Conversión bajo demanda |
| D11 | Sin mundo virtual universal | Complejos requieren adaptador |
| D12 | Volúmenes de ocupación/colisión/selección/apoyo separados | Transparencia no decide huecos |
| D13 | Contacto permitido; volumen positivo compartido prohibido | Validación geométrica exacta tras filtros |
| D14 | Modos habitual, preciso y orientación independientes | Teclas configurables |
| D15 | Redondeo normal hacia fuera y tangencial al más próximo | Puede quedar hueco visible |
| D16 | Recorte inicial solo altura local de familias declaradas | No hay editor universal |
| D17 | Corte devuelve sobrante; unión solo datos compatibles | Conservación de materiales |
| D18 | Rotura y reemplazo por componente | Loot único |
| D19 | Conexiones por puertos reales y apoyo por parches | Sin conexiones por compartir casilla |
| D20 | Ediciones planificadas y confirmadas en servidor | No mutación parcial |
| D21 | Diario por dimensión y recuperación idempotente | Cruces de chunk sujetos a pruebas de fallo |
| D22 | Red por revisiones, snapshots y deltas | Resync ante divergencia |
| D23 | Render estático por sección y cachés limitadas | Sin entidad dinámica por pieza |
| D24 | Luz de resolución vanilla con agregación conservadora | No se promete luz subbloque |
| D25 | Fluidos parciales F5 con máscara conservadora 16³ | Antes se bloquean composiciones mojadas |
| D26 | Funcionales F7 y mecanismos F8 | Capacidades no listas permanecen bloqueadas |
| D27 | Pistones: 12 componentes y volumen barrido | No 12 casillas arbitrariamente llenas |
| D28 | Compatibilidad por capacidad y versión | Certificación granular |
| D29 | Paintable Blocks mediante adaptador específico | Geometría/datos del origen preservados |
| D30 | Presupuestos concretos de complejidad | Rechazo previo, nunca borrado |
| D31 | Protocolo experimental 5; esquema/API de piezas 1 | Handshake de manifest, versiones y límites; migraciones explícitas |
| D32 | Bóveda Markdown sin plugins comunitarios | Navegable y portable |
| D33 | Bóveda integrada en docs/obsidian/NoMoreGrids | Esqueleto mínimo autorizado en edición 1.1 |
| D34 | Idioma inicial es_es y en_us | Traducciones, códigos de error estables |
| D35 | Fuentes/versiones externas no se consideran probadas | Comprobaciones registradas por fase |

## Aclaraciones respecto del diálogo

- Consumir un bloque entero por cada fragmento queda sustituido por corte con sobrante y recuperación según loot.
- «Seis posiciones» de otras familias significa seis frentes, sin giro adicional; no seis ubicaciones internas.
- Función acordada no significa disponible en 0.1.0.
- Una forma equivalente a losa externa no cambia identidad/recetas.
- La selección de candidatos no atraviesa obstáculos.
- Reglas de la otra documentación, como commits solo online o prohibición de compilar, no se heredan.
- La licencia y publicación no se inventan: este diseño no realiza distribución de código ni necesita resolverlas.
