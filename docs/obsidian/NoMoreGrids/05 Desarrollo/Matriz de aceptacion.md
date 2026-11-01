# Matriz de aceptación

Estado inicial de todos los casos: NO EJECUTADO. Esta tabla especifica pruebas futuras; la validación del ZIP no cuenta como prueba del mod.

| ID | Caso | Resultado exigido |
|---|---|---|
| G01 | Origen -1/16 | Casilla -1, resto 15 |
| G02 | Cara exactamente en límite | No ocupa casilla de volumen cero |
| G03 | Todas las rotaciones y sus inversas | Recuperan geometría/datos exactos |
| G04 | Dos sólidos tocándose | Permitido sin vibración/solapamiento |
| G05 | Intersección pequeña positiva | Rechazo consistente cliente/servidor |
| G06 | Rayo a través de hueco | Alcanza superficie real posterior accesible |
| O01 | Seis losas canónicas | Mitades correctas y sin pérdida |
| O02 | Escalera vertical y esquina | Geometría y conexiones locales correctas |
| O03 | Modo ampliado apagado/encendido | Otras familias cambian solo cuando corresponde |
| P01 | Cubo sobre losa | Base 8/16, techo 24/16 |
| P02 | Superficie 1,5/16 | Base 2/16 y hueco visible |
| P03 | Normal negativa | Redondeo hacia fuera correcto |
| P04 | Pieza en 8 casillas | Una identidad y un loot |
| P05 | Región sin cargar | Rechazo sin forzar carga |
| P06 | Borde/altura del mundo | Rechazo de cualquier parte exterior |
| C01 | Dos losas de materiales distintos | Dos componentes seleccionables |
| C02 | Losas perpendiculares invasivas | Rechazo sin consumo |
| C03 | Escalera + pieza en hueco | Valida volumen real |
| R01 | Corte 16→6+10 | Conservación exacta |
| R02 | Inventario sin lugar para sobrante | Corte rechazado |
| R03 | Unión de fragmentos incompatibles | Rechazo |
| R04 | Cristal sin recuperación | No produce fragmento prohibido |
| I01 | Rotura de pieza compartida | Vecina intacta y un loot |
| I02 | Reemplazo inválido | Sin consumo ni eliminación |
| I03 | Reemplazo supervivencia | Respeta herramienta/tiempo |
| I04 | Candidatos detrás de pared | No seleccionables |
| S01 | Apoyo por punto para base completa | Rechazo |
| S02 | Pérdida de apoyo | Solo dependientes afectados |
| S03 | Ciclo de apoyos sin base estable | Rechazo |
| S04 | Conexiones que no convergen | Aborto sin estados alternantes |
| L01 | Dos emisores en una casilla | Máximo, no suma |
| L02 | Caras internas ocultas | No borran datos de material |
| F01 | Composición o movimiento hacia una casilla con fluido | Rechazo antes de consumir o mutar |
| M01 | Dos contenedores funcionales | Inventarios independientes |
| M02 | Pistón toca pieza multicelda | Una pieza movida, no fragmentada |
| M03 | 13 componentes en empuje | Movimiento bloqueado |
| M04 | Trayectoria obstruida | Bloqueo aunque destino esté vacío |
| M05 | Redstone compartiendo casilla | Sin conexión accidental |
| M06 | Menú con cursor/drag/autofill/XP y cierre durante movimiento | Recibo único; contenidos, cursor y experiencia conservados |
| M07 | Transferencia NeoForge abortada/confirmada y entre dimensiones | Rollback o un commit; conexión entre dimensiones rechazada |
| M08 | Puerta/cama compartida movida/reemplazada/ocupada | Dos miembros coherentes y barrido seguro; dormir/respawn solo en marco válido |
| M09 | Vagón entre raíl nativo y convertido, curva/pendiente | Extremos/tangentes continuos; detectores/activadores y colisiones correctos |
| M10 | Pulso corto de repetidor y descarga/carga de la red | Flancos/retardos persistidos, sin conducción por compartir host |
| B01 | PB forma losa horizontal/vertical | Apoyo real, identidad PB |
| B02 | PB altura 4/16 | Cubo a 4/16–20/16 |
| B03 | PB pintura en diseño desplazado | Superficie correcta |
| B04 | PB pendiente y giro | Geometría/pintura no se rotan dos veces |
| B05 | PB edición invade pieza | Rechazo antes de coste |
| B06 | PB herramienta de recuperación | Datos/loot del origen |
| B07 | Volumen/pintura PB mixtos nativos y NMG | Selección fijada; coste/loot y geometría validados en un plan |
| B08 | Estación/máquina/cubo PB con menú y automatización | Producción y contenidos privados únicos, guardado y recuperación |
| N01 | Dos jugadores misma revisión | Un commit; otro desactualizado |
| N02 | Repetir paquete de operación | Sin segundo consumo/loot |
| N03 | Delta fuera de orden | Resync sin corrupción |
| N04 | Payload excesivo | Rechazo antes de asignación grande |
| D01 | Guardar/cargar fronteras | Referencias y piezas idénticas |
| D02 | Crash antes/después de commit | Descartar/reproducir según diario |
| D03 | Crash entre inventario y chunk | Recibo o cuarentena; nunca replay ciego |
| D04 | Falta mod/adaptador | Sustitución conservada y recuperable |
| D05 | Versión de datos futura | No sobrescribe ni rebaja |
| D06 | Chunk nativo antiguo después de transferir/mover máquina | Reparación o cuarentena antes de ticks/acceso; sin drops/XP duplicados |
| D07 | Corte después del commit del mod externo y antes del recibo propio | Intent conservado; reconciliar prueba externa sin replay ciego |
| D08 | Compactación automática con escrituras simultáneas y corte de publicación | Prefijo/cola y deduplicación recuperados desde generación durable |
| Q01 | Permiso denegado en una casilla | Toda operación rechazada |
| Q02 | Límite reducido con piezas existentes | No borra; permite reducir |
| Q03 | Servidor dedicado | Sin cargar clases de cliente |
| Q04 | Escenas de rendimiento | Métricas y hardware documentados |

## Modalidades

Pruebas unitarias del núcleo geométrico; integración con mundo y adaptadores; pruebas en cliente y servidor dedicado; inyección de fallos de guardado; sesiones manuales para controles/render. No basta con pruebas que repitan las constantes de la implementación.

Cada resultado debe registrar versión, pasos, esperado, observado y evidencia. Las pruebas visuales incluyen capturas internas cuando sean útiles, sin incorporarlas automáticamente como recursos del mod.

## Continuación con compilación autorizada

Los 72 IDs se mantienen sin alterar expectativas. El catálogo
`docs/acceptance/catalog.json` y su vista `docs/acceptance/MATRIZ_TRAZABLE.md`
conectan cada caso con procedimientos, fuentes y evidencia requerida.
`python tools/check_acceptance.py` comprueba trazabilidad, sin ejecutar casos.
Las fuentes JVM y sondas reales cubren subconjuntos; no equivalen a 72 pruebas
automatizadas. Las escenas de Q04 y el procedimiento de F9 están en
`docs/acceptance/F9.md`. La entrega actual compila Java principal, tests y sondas;
no ejecuta las ampliaciones ni concede aceptación.
