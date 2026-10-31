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

`tools/validation/RuntimeProbe.java` es una sonda manual para un servidor real
y un mundo desechable. No se ejecuta automáticamente ni entra en el JAR.
Requiere autorización explícita; consultar [sus instrucciones](validation/README.md).
