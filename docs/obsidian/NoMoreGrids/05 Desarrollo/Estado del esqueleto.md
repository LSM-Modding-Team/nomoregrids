# Estado del esqueleto

La base inicial evolucionó a una implementación experimental por petición del
usuario. Identidades y versiones permanecen fijadas: NoMoreGrids,
net.nicomar.nomoregrids, Minecraft 26.2, NeoForge 26.2.0.88, Java 25,
Gradle 9.2.1 y ModDevGradle 2.0.148.

Existen registros reales de contenedor, block entity, fragmentos y recetas;
protocolo, operaciones durables, servidor, controles y render. Los módulos de
apoyo, decoración, fluidos, funcionales, grupos y mecanismos tienen puentes al
mundo para perfiles explícitos. Paintable Blocks se consulta opcionalmente para
diseños, herramientas y máquinas; no es una dependencia obligatoria.
No se ha compilado ni ejecutado Java/Minecraft ni se acredita compatibilidad.

El estado vigente por fase, perfiles, límites y validación está centralizado en
`docs/ESTADO_DESARROLLO.md`; la evidencia está en `docs/evidence`.
Los antiguos documentos F0/F1 describen entregas históricas, no el estado actual.
[[Plan de fases]] conserva las puertas de aceptación y [[Matriz de aceptacion]]
los casos pendientes de ejecución. Ninguna fase está aceptada.
