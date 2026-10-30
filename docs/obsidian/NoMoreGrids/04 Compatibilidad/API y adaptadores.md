# API y adaptadores

API de diseño versión 1. Los siguientes nombres expresan contratos, no firmas verificadas de NeoForge.

## Contratos públicos

| Contrato | Responsabilidad |
|---|---|
| PieceAdapter | Identificar bloque/ítem, capacidades y versión |
| PieceCodec | Capturar, guardar, migrar y restaurar datos |
| GeometryProvider | Ocupación, colisión, selección, apoyo y envolvente |
| SurfaceResolver | Cara/ID de superficie y coordenadas locales |
| MaterialProvider | Luz, sonido, dureza, fricción y resistencia |
| InteractionBridge | Traducir herramientas y menús al componente |
| EditPlanner | Proponer cambios sin efectos laterales |
| DropProvider | Resolver obtención una sola vez |
| ConnectionProvider | Puertos y conexiones geométricas |
| MovementProvider | Movilidad, grupos y estado durante traslado |
| RenderProvider | Snapshot público y materiales/malla |
| InventoryBridge | Coste/recompensa y recibos persistentes |

Los proveedores de geometría deben ser deterministas y no mutar el mundo. Los datos del cliente no sustituyen validación del servidor. Un adaptador no expone inventarios privados para render.

## Flujo externo

Capturar -> crear snapshot local -> exponer forma -> planificar interacción -> validar NMG -> validar mod de origen -> confirmar una vez -> invalidar índices/mallas -> sincronizar.

Cada superficie tiene ID estable dentro de la revisión. Si una edición cambia topología, un clic de revisión anterior se rechaza, no se reasigna a una cara distinta.

## Distribución

Compat vanilla dentro del núcleo; Paintable Blocks en módulo opcional aislado. Add-ons externos pueden registrar adaptadores antes de cargar mundos. Colisión de adaptadores para un mismo bloque se resuelve mediante prioridad declarada, no orden accidental de carga.

Perfiles de datos no contienen código y no implementan inventarios/ticks. API sigue versionado semántico; cambio incompatible exige mayor y migración de payload si procede.

## Contrato de pruebas

Todo adaptador entrega pruebas de ida/vuelta de datos, transformación, colisión, selección, materiales, rechazos, carga/descarga y ejecución en servidor dedicado. Las capacidades se habilitan una a una tras pasar su matriz; no hay casilla global «compatible».
