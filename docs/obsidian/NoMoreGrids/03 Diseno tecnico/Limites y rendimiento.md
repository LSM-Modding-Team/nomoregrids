# Límites y rendimiento

Estos números son decisiones iniciales de ingeniería, no resultados medidos. Se pueden cambiar mediante configuración y pruebas, manteniendo formato y datos. No se elimina una construcción porque un administrador reduzca un límite.

## Presupuesto inicial

| Recurso | Valor predeterminado |
|---|---|
| Componentes que tocan una casilla | 64 |
| Registros autoritativos por sección | 4096 |
| Referencias de pieza por sección | 16384 |
| Cajas de colisión agregadas por casilla | 512 tras combinación exacta |
| Primitivas geométricas por pieza | 16384 |
| Triángulos de render por sección | 262144 |
| Payload persistente por pieza | 256 KiB sin comprimir |
| Datos de sección NMG | 2 MiB sin comprimir |
| Envolvente de pieza atómica | 1×1×1 bloques locales |
| Casillas tocadas por pieza atómica | Hasta 8 |
| Cierre de movimiento de pistón | 12 componentes |
| Resolución de conexiones | 16 pasadas |
| Operación normal | Hasta 128 piezas y 512 casillas afectadas |
| Fluido: casillas activas por sección | 256 |
| Fluido: pasos de cavidad por tick/dimensión | 8192 |
| Trabajo diferido de simulación | 2 ms objetivo por tick/dimensión |
| Construcción de mallas cliente | 4 ms objetivo por frame, diferible |
| Caché geométrica cliente/servidor | 128/64 MiB respectivamente, LRU |

No todos los máximos se pueden alcanzar simultáneamente: se aplican todos los presupuestos. Las geometrías densas de Paintable Blocks pueden ser rechazadas aunque su payload quepa; se informa el límite concreto. El adaptador debe medir complejidad tras unir cajas y ocultar caras.

## Política

Una operación que excede un máximo duro se rechaza sin consumir. Una cola temporal saturada muestra pendiente o ocupado. Las piezas existentes fuera del nuevo límite siguen cargando; se bloquean ampliaciones que aumenten el exceso y se permite reducir/retirar. Datos corruptos o versiones desconocidas siguen cuarentena, no esta excepción.

Solo las zonas con actividad se simulan. No hay un tick por microcubo ni por pieza decorativa. Índices amplios por sección y máscaras filtran antes de geometría exacta. Las notificaciones se coalescen y las cachés tienen contadores de aciertos y tamaño.

Los límites de almacenamiento no se validan sobre NBT comprimido, evitando aceptar datos pequeños que exploten al leer.

## Medición de aceptación

Escenas: pared de 10000 piezas simples; sección con 4096 componentes simples dentro de límites; 256 diseños distintos y pintados; frontera de 8 chunks; spam válido/inválido con 4 jugadores; fluidos parciales.

Registrar CPU/GPU, memoria, distancia de render, JVM, percentiles de frame/tick, tamaño de guardado y tráfico. Objetivo de referencia: tick p95 ≤50 ms y incremento p95 de frame ≤5 ms frente a escena comparable, en hardware documentado. Son metas de aprobación; si no se cumplen se optimiza o baja el límite por decisión registrada, sin declarar un rendimiento universal.
