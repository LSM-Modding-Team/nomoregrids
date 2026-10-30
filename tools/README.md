# Herramientas

`python tools/check_repository.py` valida recursos, metadatos, wrapper y enlaces
sin compilar ni ejecutar Java, Gradle o Minecraft. Solo usa la biblioteca estándar.

`python tools/check_java_syntax.py` analiza sintaxis con Tree-sitter; no resuelve
tipos, no compila y no ejecuta tests. Requiere `tree-sitter==0.25.2` y
`tree-sitter-java==0.23.5` en el Python elegido. Para instalar sin construir
extensiones se pueden usar wheels con `pip install --only-binary=:all:`. No es
dependencia del mod ni se ejecuta durante el build.

Herramientas propias de desarrollo y diagnóstico. No contiene scripts de instalación, descarga ni publicación automáticos.
