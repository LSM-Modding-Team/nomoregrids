# F1 — núcleo geométrico parcial, 2026-10-29 (America/Lima)

## Implementado en código fuente

- Coordenadas enteras de 64 bits en unidades de 1/16, negativos con suelo
  matemático, secciones y orden X/Y/Z. Desbordamientos rechazados.
- Las 24 rotaciones normativas, composición, inversas y pivote estable.
- Cajas semiabiertas y uniones inmutables, contacto permitido, intersección de
  volumen positivo, cálculo de casillas ocupadas con presupuesto previo.
- Geometría canónica de cubo, seis losas y escalera recta. Son geometrías del
  núcleo; no hay familias registradas ni conexiones/esquinas habilitadas.
- Cuantización tangencial y hacia fuera; selección de superficies por rayo con
  huecos e intervalos unidos para no seleccionar caras internas.
- JUnit Jupiter 5.13.4 conectado a Gradle. Casos escritos para negativos,
  fronteras, 24 rotaciones y sus composiciones, apoyo geométrico de cubo sobre
  losa, ocho secciones, hueco de escalera, alcance y errores de entrada.

## Decisiones y límites

`Box16` y `BoxUnion` son exclusivamente la geometría integral inicial. No
redondean diseños externos ni sustituyen geometría convexa, pendientes o formas
más finas. Esas familias siguen sin adaptador y no se aceptan en el juego.
`BoxRaycast` usa coordenadas locales en unidades de 1/16; la transformación de
rayos desde el mundo y la autoridad de selección siguen pendientes del puente.

No se registran contenedores, controles ni bloques de ejemplo para aparentar
una mecánica completa. Las formas de ocupación del núcleo no se usan como una
API universal de apoyo/colisión/render.

## Evidencia

Ejecutados: `python tools/check_repository.py` y `git diff --check` (revisión
estructural, sin invocar compiladores). El comprobador no ejecuta Java ni Gradle.
No ejecutados: compilación y tests Java, GameTests, cliente, dedicado y matriz
de aceptación. Los casos G01–G06/O01/P01/P04 tienen cobertura escrita parcial,
pero su estado de aceptación sigue siendo NO EJECUTADO.

F0 y F1 permanecen ABIERTAS. F2–F9 no están aceptadas ni habilitadas.
