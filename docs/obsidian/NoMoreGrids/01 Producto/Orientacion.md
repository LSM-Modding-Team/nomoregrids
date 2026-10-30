# Orientación

## Losas

Seis mitades canónicas de un cubo local: inferior, superior, norte, sur, oeste y este. Son funciones predeterminadas del mod. La orientación y el desplazamiento son independientes: una losa vertical puede desplazarse en modo preciso y ocupar posiciones no canónicas.

## Escaleras

Se admiten las 24 rotaciones ortogonales propias del cubo (sin reflejos); incluyen escaleras verticales. Se conserva un identificador de orientación de 0 a 23. Las simetrías geométricas se deduplican en caché, sin prometer 24 formas visualmente distintas.

La mirada decide el frente y la superficie de colocación decide el apoyo. Se escoge la rotación válida cuya dirección de ascenso se aproxime más a la mirada proyectada sobre el plano de apoyo. Si esa proyección es casi nula, se usa el último rumbo horizontal válido del jugador; al entrar al mundo se toma norte como desempate. El orden estable de desempate es Y, Z, X y luego signo positivo.

Los contactos interiores/exteriores se calculan en el marco local de la escalera; no se conserva una lógica limitada al suelo horizontal. Consultar [[Conexiones y apoyo]].

## Otras familias

Modo de orientación ampliada apagado inicialmente. Al activarlo, un frente local puede mirar hacia ±X, ±Y o ±Z. Se escoge la dirección cardinal más próxima al vector de mirada del jugador. En igualdad se usa el mismo desempate estable Y, Z, X. No hay giro independiente alrededor del frente.

Cada familia declara un frente y una transformación canónica. Para mirar hacia arriba o abajo se fija el «arriba local» hacia norte; los frentes horizontales conservan arriba mundial. Los bloques con eje pueden producir solo tres geometrías diferentes aunque existan seis direcciones seleccionables.

La activación no vuelve elegibles los bloques funcionales aún no adaptados. Las máquinas mantienen sus restricciones hasta la fase correspondiente. Los bloques puramente simétricos pueden no mostrar cambio.

## Texturas y datos

La rotación transforma geometría, selección, apoyo y caras del material de manera conjunta. Los datos locales originales no se reescriben acumulativamente: se compone una transformación para evitar errores tras varios giros. En Paintable Blocks se combina con su orientación nativa una sola vez.

## Colocación familiar

Sin modo preciso se preserva la elección vanilla siempre que exista. Para solicitar losa vertical se mantiene el modificador de orientación (Alt izquierdo sugerido) al colocar contra una cara lateral. Para escaleras se utiliza ese modificador para escoger el plano lateral. El modificador es configurable; no requiere activar orientación ampliada. La vista previa desambigua siempre.
