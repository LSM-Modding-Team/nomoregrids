# No More Grids

**Especificación de producto y diseño técnico — edición 1.1**

Minecraft Java 26.2 · NeoForge 26.2.0.88 · documentación en español.

No More Grids elimina la exclusividad de las casillas: varias piezas pueden compartir una casilla y una pieza puede extenderse entre varias. Cada componente conserva su identidad y sus propiedades. El ajuste de posición es de 1/16 de bloque; la geometría puede ser más precisa.

> Estado: implementación experimental en F0–F9, con perfiles de construcción e integraciones avanzadas conectados al mundo, pendientes de validación. Validación puntual con compilación, 106 tests, cliente/dedicado, dos clientes locales y recuperación de una escena pequeña ejecutados; sin revisión visual ni aceptación completa. El estado consolidado está en `docs/ESTADO_DESARROLLO.md`; ninguna fase está aceptada.

## Leer primero

1. [[Vision y alcance]]: qué hace el mod.
2. [[Reglas fundamentales]]: contratos que no deben romperse.
3. [[Orientacion]] y [[Colocacion]]: experiencia del jugador.
4. [[Arquitectura]] y [[Modelo de piezas]]: representación técnica.
5. [[Paintable Blocks]]: integración de referencia.
6. [[Plan de fases]] y [[Matriz de aceptacion]]: orden y verificación.

## Mapas

- [[Indice completo]]
- [[Decisiones tecnicas]]
- [[Catalogo de familias]]
- [[Limites y rendimiento]]
- [[Fuentes y evidencia]]
- [[Glosario]]
- [[Registro de cambios]]

## Cómo abrir

Extraer el proyecto y abrir docs/obsidian/NoMoreGrids como bóveda en Obsidian. No se necesitan complementos comunitarios. Los enlaces internos son wikilinks con nombres únicos. Esta bóveda ya está ubicada en docs/obsidian/NoMoreGrids dentro del proyecto Gradle. La base contiene núcleo geométrico, registros técnicos y host experimental; todavía no ha sido compilada.

Las propuestas del chat están aceptadas por defecto, salvo rechazo explícito del usuario. Las decisiones de esta edición precisan las ambigüedades restantes. Consultar [[Decisiones tecnicas]] para conocer sus consecuencias.

- [[Estado del esqueleto]]
