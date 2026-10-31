# Estructura del proyecto

| Ruta bajo src/main/java/net/nicomar/nomoregrids | Responsabilidad |
|---|---|
| core / geometry | Coordenadas, piezas, geometrías de cajas/convexos, rotaciones, rayos y mallas; sin Minecraft |
| operation / persistence | Planes, coordinador, diario con CRC/force y archivos de sección |
| world | Índices inmutables, réplicas públicas, bloque/entidad y consultas |
| platform | Captura nativa, inventarios, candidatos, host servidor y eventos |
| registry / event / network | Contenido real, permisos, protocolo, acciones y límites |
| client | Controles, selecciones/preview, pantallas/menús, modelos de sección y render decorativo nativo |
| material / simulation/connection / simulation/support | Fragmentos, familias, materiales, puertos y apoyos |
| simulation/fluid / simulation/functional / simulation/movement / simulation/redstone | Simulaciones y contratos usados por el host; consultar perfiles y validación |
| api / compat | Adaptadores y puente opcional Paintable Blocks |
| diagnostic | Caché por peso, trabajos y muestras |

Las rutas exactas de paquetes se obtienen con `rg --files src/main/java`.
Los metadatos se expanden desde `src/main/templates/META-INF/neoforge.mods.toml`.
Los recursos contienen perfiles vanilla explícitos, modelos, idiomas y recetas
de fragmentos. No existe ítem del contenedor técnico.

`src/test/java` contiene 115 métodos JUnit compilados (45 clases); 106 casos de
la edición anterior se ejecutaron en la validación histórica. Los nueve nuevos
no se ejecutaron. `src/gametest` continúa reservado y sin source set.
`tools/validation` es el source set aislado `validation`: operaciones/capacidades
reales, casos de mundo y escenas F9. `compileValidationJava` solo compila;
`-PnmgValidation` conecta la sonda a perfiles desechables con permiso de ejecución.
No entra en el JAR normal ni ejecuta casos con `test`/`build`.
`docs/acceptance` conserva el catálogo de 72 casos y las instrucciones de F9;
`tools/check_acceptance.py` comprueba su trazabilidad y `tools/compare_profiles.py`
lee informes de ventanas completas. `docs/evidence` separa código inspeccionado,
compilación y resultados ejecutados.

El único JAR versionado es el wrapper oficial de Gradle. No se incluyen binarios
del mod, dependencias descargadas, JDK, cachés ni credenciales. El estado vigente
está en [ESTADO_DESARROLLO.md](ESTADO_DESARROLLO.md).
