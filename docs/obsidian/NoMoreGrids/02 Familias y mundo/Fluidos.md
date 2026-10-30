# Fluidos

## Decisión de alcance

No se simulará agua parcial en el primer prototipo. F5 incorpora un sistema específico de cavidades; antes se rechaza toda operación compuesta que afecte una casilla con fluido o con bloque waterlogged. Se permite coexistencia nativa fuera de los contenedores.

Objetivo acordado: ocupar espacio con un sólido no debe borrar el fluido que permanece en huecos compatibles.

## Representación

Cada casilla compuesta con fluido tiene una máscara dispersa de cavidades de 16×16×16. Para fluidos, un microvolumen se considera bloqueado si la geometría sólida lo intersecta con volumen positivo. La colisión y el render de sólidos conservan su precisión; solo la simulación de fluido utiliza esta aproximación conservadora.

Las cavidades se agrupan por conectividad de caras. Agua y lava no ocupan simultáneamente un microvolumen. Se guardan tipo, nivel/estado de fuente y distribución necesaria para reconstrucción. No se ejecutan 4096 entidades ni 4096 ticks vanilla por casilla.

## Flujo

La gravedad prioriza abajo; después se propaga horizontalmente entre cavidades conectadas. La integración con fluidos vanilla se hace en las caras externas mediante un puente único por casilla, evitando que dos motores escriban a la vez. Cada tipo mantiene su cadencia y distancia de propagación vanilla en la medida definida por su perfil.

No habrá fuentes fraccionarias obtenibles como cubos. Un cubo crea/recoge una fuente vanilla completa solo si hay volumen libre equivalente a una casilla y una única fuente accesible. El resto del agua parcial se alimenta de fuentes conectadas y se drena al perderlas, según reglas de flujo, sin crear recursos.

## Desplazamiento y reacciones

Antes de colocar se calcula si el fluido desplazado puede redistribuirse por cavidades conectadas dentro de un radio de 4 bloques cargados. En caso contrario se rechaza; no se evapora ni expulsa a través de sólidos. Radio y presupuesto son configurables.

Contacto agua/lava genera el producto definido por sus perfiles solo si el volumen necesario cabe. Si una reacción no es representable, se conserva el estado previo y se bloquea ese paso, con diagnóstico. F5 no se declara terminada sin resolver productos, fuego, cubos y límites de carga.

## Carga y coste

Fronteras no cargadas actúan cerradas temporalmente. No se generan chunks. Trabajo diferido: [[Limites y rendimiento]]. Agotar presupuesto retrasa actualización, no destruye fluido ni cambia a una aproximación invasiva.

Esta política de microvolúmenes puede cerrar ranuras inferiores a 1/16 en diseños externos. La interfaz y documentación lo advertirán. Fluidos externos se excluyen hasta disponer de perfil específico.
