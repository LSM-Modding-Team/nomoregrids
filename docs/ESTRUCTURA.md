# Estructura del proyecto

| Ruta bajo src/main/java/net/nicomar/nomoregrids | Responsabilidad |
|---|---|
| core / geometry | Coordenadas, piezas, geometrías de cajas/convexos, rotaciones, rayos y mallas; sin Minecraft |
| operation / persistence | Planes, coordinador, diario con CRC/force y archivos de sección |
| world | Índices inmutables, réplicas públicas, bloque/entidad y consultas |
| platform | Captura nativa, inventarios, candidatos, host servidor y eventos |
| registry / event / network | Contenido real, permisos, protocolo, acciones y límites |
| client | Controles, preview Gizmo y modelos estáticos de sección |
| material / simulation/connection / simulation/support | Fragmentos, familias, materiales, puertos y apoyos |
| simulation/fluid / simulation/functional / simulation/movement / simulation/redstone | Algoritmos de fases avanzadas; consultar estado de integración |
| api / compat | Adaptadores y puente opcional Paintable Blocks |
| diagnostic | Caché por peso, trabajos y muestras |

Las rutas exactas de paquetes se obtienen con `rg --files src/main/java`.
Los metadatos se expanden desde `src/main/templates/META-INF/neoforge.mods.toml`.
Los recursos contienen perfiles vanilla explícitos, modelos, idiomas y recetas
de fragmentos. No existe ítem del contenedor técnico.

`src/test/java` contiene casos JUnit escritos, no ejecutados. `src/gametest`
continúa reservado y no está conectado como source set. `tools` contiene solo
comprobaciones de estructura y parser, sin compilación. `docs/evidence`
distingue fuentes inspeccionadas de resultados ejecutados.

El único JAR versionado es el wrapper oficial de Gradle. No se incluyen binarios
del mod, dependencias descargadas, JDK, cachés ni credenciales. El estado vigente
está en [ESTADO_DESARROLLO.md](ESTADO_DESARROLLO.md).
