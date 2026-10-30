# Renderizado

## Decisión

Geometría estática se integra en mallas de sección; no se dibuja cada pieza completa en cada frame mediante una entidad independiente. Cada casilla emite solo la parte que le corresponde para evitar dobles caras y culling incorrecto en fronteras.

Las transformaciones se calculan una vez por revisión. Mallas de geometría y material se cachean por claves separadas; pintar invalida material y selección de superficie pertinente, no toda la lógica física.

## Caras y texturas

Se eliminan superficies internas totalmente ocultas por sólidos opacos. Ocultarlas no las borra del diseño ni de la pintura guardada. Cristal y transparencias mantienen reglas específicas; no se eliminan interfaces entre materiales diferentes por coincidencia espacial sin revisar su aspecto.

UV locales conservan escala original. En piezas recortadas se añaden tapas según perfil. Texturas de resource packs se usan a través de modelos/materiales nativos compatibles; no se hornean capturas de pantalla como textura.

Pendientes externas se triangulan/poligonizan respetando la forma del adaptador. La aproximación de colisión del origen se conserva y se documenta si difiere de la superficie visual.

## Cliente y tareas

Extracción en hilo apropiado produce snapshot inmutable. Construcción de malla puede ir a trabajadores sin consultar un mundo mutable. Antes de instalar resultado se verifica revisión; una malla antigua se descarta. Subida GPU se hace en el contexto autorizado.

Contornos y vista previa se envían por el pipeline de 26.2. No se copian firmas de 1.21 ni de 26.1 sin contrastarlas. Fuente conceptual: [[Fuentes y evidencia]].

## Degradación

Se reduce primero frecuencia de vista previa lejana y se difieren reconstrucciones. No se simplifica geometría física ni se ocultan piezas confirmadas para cumplir un presupuesto. Si una nueva pieza supera el límite de render declarado, la operación se rechaza antes de colocar.

Shaders y motores alternativos requieren pruebas específicas. El render vanilla es la línea base de certificación.
