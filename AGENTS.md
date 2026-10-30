# No More Grids — instrucciones del proyecto

## Identidad fijada
- package/group: net.nicomar.nomoregrids
- mod ID: nomoregrids
- entrada común: net.nicomar.nomoregrids.NoMoreGrids
- nombre visible: No More Grids
- Minecraft 26.2; NeoForge 26.2.0.88; Java 25
- Gradle 9.2.1; ModDevGradle 2.0.148

## Estado y alcance
El usuario pidió un esqueleto; esta entrega no implementa las mecánicas. Las entradas mínimas solo reservan el arranque. Los package-info.java documentan paquetes; no son sistemas terminados. No crear registros ficticios, bloques de ejemplo ni pruebas que simulen funciones ausentes.

Leer docs/obsidian/NoMoreGrids/00 Inicio/Inicio.md, Reglas fundamentales, Decisiones tecnicas, Guia para el agente y Plan de fases antes de implementar. Una petición posterior de implementación autoriza el trabajo de esa fase. El alcance original de «solo diseño» quedó ampliado aquí únicamente a crear la base del proyecto.

## Trabajo futuro
- Preservar separación cliente/servidor y núcleo geométrico puro.
- Revisar APIs exactas de 26.2.0.88; no inventar firmas a partir de contratos conceptuales.
- Validar todo antes de consumir materiales o mutar el mundo.
- Un UUID y una autoridad por pieza, sin inventarios duplicados.
- Compatibilidad por capacidades y versión; Paintable Blocks sigue sin implementar.
- Cada fase requiere evidencia de compilación y pruebas pertinentes para declararse completa.
- Distinguir revisión estática, compilación, arranque y pruebas de mecánicas.
- No cambiar versiones fijadas ni publicar/crear repositorios sin una solicitud que lo autorice.
- Actualizar documentación y estado al cambiar comportamiento.
- Resolver decisiones rutinarias sin preguntas innecesarias; comunicarlas al entregar.

## Restricciones de ejecución

- NO compiles, generes builds, ejecutes tareas de construcción ni inicies Minecraft, servidores o clientes de desarrollo, salvo petición explícita del usuario para hacerlo.
- Esta prohibición incluye invocaciones indirectas: tareas de IDE, scripts, CI, comandos de Gradle y pruebas que desencadenen compilación o arranquen el juego. No ejecutes una herramienta de comprobación sin conocer sus efectos.
- Solo se hará lo anterior con la solicitud explícita del usuario.
