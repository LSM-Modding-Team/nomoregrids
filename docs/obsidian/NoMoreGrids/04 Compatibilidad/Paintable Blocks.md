# Paintable Blocks

## Referencia inspeccionada

Repositorio LSM-Modding-Team/paintableblocks, rama 26.2. La referencia documental inicial fue f3b3998d6a239b776121f3003b6f5a6c052a0a65; la implementación actual se contrastó con [e93bc03f350dfb03b4031f1e479f4a37f28b556e](https://github.com/LSM-Modding-Team/paintableblocks/tree/e93bc03f350dfb03b4031f1e479f4a37f28b556e), schema BlockDesign 2. Leer sus fuentes no acredita que NMG o PB compilen o funcionen juntos.

Fuentes directas en [[Fuentes y evidencia]]. La documentación del otro repositorio inspira la estructura, no impone sus instrucciones históricas al desarrollo de NMG.

## Lo observado

BlockDesign almacena resolución, ocupación, pinturas por superficie, orientación y plano de pendiente. PaintableBlockEntity mantiene diseño, identidad/revisión de edición, formas derivadas, persistencia y sincronización. PaintableBlock obtiene colisión/selección/apoyo desde esa entidad. Existe PaintableBlockEditEvent cancelable.

Consecuencia: envolver el bloque y copiar solo su BlockState no conserva el diseño. Sus herramientas necesitan encontrar un componente dentro del contenedor en lugar de asumir una entidad nativa única por BlockPos.

## Reparto

NMG: colocación mundial, traslación, convivencia, selección global, validación de espacio, referencias y permisos.

Paintable Blocks: diseño local, pintura, recortes, pendientes, herramientas, catálogo y economía de conservación.

Adaptador: traducción de coordenadas/superficies, render, captura/restauración, revisiones, planificación y sincronización única. Será necesaria cooperación explícita o un puente aislado si las herramientas actuales hacen consultas directas incompatibles. El evento existente por sí solo no resuelve eso.

## Formas recortadas

| Geometría del diseño | Tratamiento |
|---|---|
| Media altura y anchura/profundidad completas | Superficie equivalente a losa horizontal |
| Media anchura y demás dimensiones completas | Equivalente a losa vertical |
| Altura 4/16 | Apoyo a esa altura real |
| Pieza centrada de grosor 8/16 | Dos huecos laterales; no «mitad canónica» |
| Escalones | Superficies independientes por altura |
| Huecos internos accesibles | Colocación si cabe y llega el trazado |
| Pendientes | Geometría conservada; apoyo según contacto |

La clasificación reconoce equivalencias útiles, no cambia identidad ni recetas. El algoritmo general funciona sin clasificar como losa. Una geometría curva o inclinada no se reduce a su envolvente para aceptar colocaciones.

## Orientación

Se utiliza una sola composición matemática entre orientación de diseño y colocación externa. La pieza conserva su sistema local de pintura. Cuando la herramienta nativa gira el diseño, el adaptador crea el candidato y NMG verifica que no invada otra pieza. El modo de seis direcciones de NMG no elimina capacidades nativas adicionales de Paintable Blocks.

## Edición y datos

Pintar conserva geometría y solo invalida lo visual pertinente. Recortar o cambiar pendiente invalida colisión, apoyo, conexiones, contenedores y mallas. Ampliar dentro de otra pieza se rechaza antes de cobrar recursos.

Las caras ocultas por componentes vecinos no se borran del diseño. Las nuevas superficies creadas por un corte conservan la regla de pintura del mod de origen; NMG no inventa pigmento.

Recuperación mediante guantes/tenazas u otra herramienta respeta las reglas del origen. No aplica nuestro sistema genérico de fragmentos ni convierte trozos pintados en paneles completos.

## Etapas

F6a: captura/restauración y render. F6b: geometría, desplazamiento y composición. F6c: herramientas/pintura y edición validada. F6d: conservación, red, fronteras y recuperación. Las máquinas y cubos de producción no se incluyen por el adaptador de diseños: tienen un perfil funcional separado en F7, con autoridad privada, menús nativos, ticking y transferencias preparados. Su presencia tampoco acredita el ciclo completo de ejecución.

No declarar «compatible con Paintable Blocks» hasta completar F6d en cliente y servidor dedicado con versiones fijadas.

## Validación puntual posterior

PB en el hash fijado compiló y cargó junto con NMG. Una pieza PB desplazada entre
dos celdas se guardó, reconstruyó tras SIGKILL y seleccionó desde un cliente local.
Los menús, producción, herramientas y economía completos siguen sin verificarse.
Ver `docs/evidence/VALIDACION_EJECUTADA_2026-10-30.md`; no es certificación F6d.
