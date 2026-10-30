# Recortes y materiales

## Alcance

Paneles de cristal y barrotes admiten recorte inicial en su altura local. Vallas y muros tendrán modelos adaptados antes de habilitarlo. Cubos, losas, escaleras, máquinas y diseños externos no reciben recorte universal por esta función.

La pieza completa conserva dimensión 16/16; sobre una losa inferior llega a 24/16. La pieza ajustada ocupa 8/16 y termina a 16/16. La dirección de crecimiento es +Y local transformada. Si el inicio coincide con un límite, el objetivo es el siguiente límite, no tamaño cero.

Se admiten longitudes enteras de 1 a 16 unidades. Se conserva el tamaño exacto elegido al colocar; un obstáculo no provoca un recorte silencioso. Si el espacio restante no puede representarse en esas unidades, se ofrece el mayor tamaño entero que no invada y la vista previa muestra el hueco; nunca se consume sin confirmación.

## Apariencia

Las UV se recortan a escala original; no se estiran. Se cierran las secciones expuestas con textura apropiada por familia. Vallas y muros requieren extremos/postes definidos; no se corta ciegamente un modelo si produce una forma incoherente.

## Conservación

Una pieza de longitud 16 dividida en 6 produce dos fragmentos de longitudes 6 y 10. La colocación prefiere tamaño exacto; en su defecto corta una pieza mayor. Consume un solo recurso de origen. Si no cabe el sobrante en inventario, rechaza el corte: no lo elimina ni lo deja caer sin aviso.

Los fragmentos se apilan solo si coinciden material, sección, longitud, orientación de textura y datos relevantes. Corte y unión se ofrecen mediante recetas y una interfaz de selección de longitud; no se exige estación nueva. Una receta de unión admite hasta dos fragmentos por operación y solo crea un tramo recto de longitud ≤16, con iguales material/sección/datos. No produce un bloque original de distinto tipo.

Cortar la geometría no duplica inventarios ni metadatos con valor. Esos bloques están excluidos. Los fragmentos internos de una pieza se identifican por su origen lógico, no por crear UUID únicos en cada ítem de inventario.

## Rotura

Se aplica el loot del material y herramienta. Cristal sin recuperación autorizada no devuelve el fragmento. Con recuperación, devuelve el tamaño y datos correspondientes; no un panel completo por cada trozo. En creativo no hay consumo ni obtención de recursos.

## Mods externos

Paintable Blocks conserva su propia economía de edición. No se pasa por nuestras recetas de corte/unión hasta un contrato específico. Su geometría sí se utiliza para colocar otras piezas.

Relacionado: [[Transacciones y eventos]], [[Paintable Blocks]].
