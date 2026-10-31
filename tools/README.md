# Herramientas

`python tools/check_repository.py` valida recursos, metadatos, wrapper y enlaces
sin compilar ni ejecutar Java, Gradle o Minecraft. Solo usa la biblioteca estándar.

`python tools/check_java_syntax.py` analiza sintaxis con Tree-sitter; no resuelve
tipos, no compila y no ejecuta tests. Requiere `tree-sitter==0.25.2` y
`tree-sitter-java==0.23.5` en el Python elegido. Para instalar sin construir
extensiones se pueden usar wheels con `pip install --only-binary=:all:`. No es
dependencia del mod ni se ejecuta durante el build.

Herramientas propias de desarrollo y diagnóstico. No contiene scripts de instalación, descarga ni publicación automáticos.


`python tools/check_api_imports.py --minecraft <client.jar> --neoforge-sources
<neoforge-sources.jar>` busca imports explícitos en archivos de la versión fijada
proporcionados localmente. Solo lee ZIP y texto; no descarga, resuelve dependencias,
compila ni ejecuta Java. No valida métodos, comodines ni extensiones parcheadas.
Los artefactos inspeccionados se mantienen fuera de Git.

`python tools/check_acceptance.py` comprueba los 72 IDs contra la especificación,
sus expectativas y las referencias a métodos/fuentes. No ejecuta los casos ni
concede aceptación. `python tools/compare_profiles.py` lee informes reales para
comparar escenas F9 equivalentes; rechaza ventanas incompletas y perfiles distintos.
Ver [procedimiento F9](../docs/acceptance/F9.md).

`tools/validation` contiene sondas de un servidor real y escenas F9 en un mundo
desechable. El source set `validation` se compila con `compileValidationJava`,
sin ejecutar tests ni runs. No entra en el JAR normal.
Requiere autorización explícita; consultar [sus instrucciones](validation/README.md).
