# Colocación

## Modos

Habitual: usa la posición vanilla cuando es válida; ofrece losas/escaleras ampliadas y combina una pieza con otra en la casilla objetivo si encajan. Si no encajan no cambia silenciosamente de casilla para simular éxito.

Preciso: el clic identifica una superficie real; la pieza queda exterior a esa superficie. La traslación final usa pasos de 1/16. Es independiente del modo de orientación ampliada.

## Algoritmo decidido

1. Resolver superficie visible y componente alcanzado dentro del alcance de interacción vigente del jugador.
2. Elegir orientación y dimensiones, incluido modo completo/ajustado.
3. Colocar la cara de apoyo de la pieza en el plano de contacto elegido.
4. Ajustar coordenadas tangenciales al paso más próximo; en empate, hacia la coordenada positiva.
5. Ajustar la coordenada normal hacia fuera del soporte: techo para normal positiva y suelo para normal negativa.
6. Aplicar desplazamiento manual y alineación fijada.
7. Validar todas las casillas, geometría, apoyo, protección, entidades, inventario y límites.
8. Mostrar resultado; al confirmar el servidor vuelve a validarlo.

Una superficie a 1,5/16 con normal +Y produce base a 2/16 y hueco de 0,5/16. Con normal negativa se redondea en sentido opuesto. No se fuerza contacto ficticio.

## Pendientes

La superficie mantiene su precisión. Para una cara de apoyo plana sobre una pendiente se busca una traslación que no intersecte el soporte y después se cuantiza hacia fuera. La validez del apoyo se evalúa tras el ajuste; un objeto que exige una base completa puede no admitir esa pendiente. No se inclina libremente la pieza ni se garantiza contacto sobre toda la base.

## Posición de referencia

Se almacena el origen del marco local sin rotar y un pivote estable definido por familia. Para cubos/losas/escaleras el pivote es el centro del cubo local original (8,8,8), no el centro visual del recorte. Evita que editar una pieza cambie su posición. La transformación exacta está en [[Tabla de rotaciones]]. Las piezas desplazadas usan suelo matemático para coordenadas negativas.

## Alineación

El jugador puede fijar plano, orientación y desplazamiento de referencia. Los siguientes candidatos conservan ese patrón y avanzan según el punto señalado. Se libera con la misma acción o al cambiar de dimensión. No coloca automáticamente filas ni consume varias piezas.

## Límites espaciales

Solo dentro de altura de construcción y borde del mundo. Todas las casillas afectadas deben estar cargadas; no se fuerzan cargas por mirar. No se permiten intersecciones con entidades que tengan colisión relevante. Contacto exacto con su frontera sí es válido.

Ver [[Geometria y coordenadas]], [[Controles y vista previa]] y [[Ejemplos resueltos]].
