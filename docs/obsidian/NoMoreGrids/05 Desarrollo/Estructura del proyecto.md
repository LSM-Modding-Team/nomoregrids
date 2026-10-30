# Estructura del proyecto

El ZIP actual contiene el proyecto Gradle NoMoreGrids con entradas mínimas y paquetes reservados. La tabla distingue responsabilidades de desarrollo; las clases mecánicas todavía no existen. El mapa de archivos reales está en docs/ESTRUCTURA.md, fuera de esta bóveda.

| Ruta prevista | Contenido |
|---|---|
| README.md | Estado real, plataforma, instalación y enlaces |
| AGENTS.md | Instrucciones del proyecto y validación |
| build.gradle / settings.gradle / gradle.properties | Build fijado, sin dependencias flotantes |
| gradle/wrapper/ | Wrapper verificado |
| src/main/java/net/nicomar/nomoregrids/core/ | Modelo y coordenadas |
| src/main/java/net/nicomar/nomoregrids/geometry/ | Formas y pruebas espaciales |
| src/main/java/net/nicomar/nomoregrids/world/ | Contenedores e índices |
| src/main/java/net/nicomar/nomoregrids/operation/ | Planes y transacciones |
| src/main/java/net/nicomar/nomoregrids/persistence/ | Guardado y diario |
| src/main/java/net/nicomar/nomoregrids/network/ | Protocolo |
| src/main/java/net/nicomar/nomoregrids/client/ | Entrada, preview y render |
| src/main/java/net/nicomar/nomoregrids/material/ | Materiales y fragmentos |
| src/main/java/net/nicomar/nomoregrids/simulation/ | Apoyo/conexión/fluido/mecanismos |
| src/main/java/net/nicomar/nomoregrids/api/ | API pública |
| src/main/java/net/nicomar/nomoregrids/compat/vanilla/ | Perfiles/adaptadores iniciales |
| src/main/java/net/nicomar/nomoregrids/compat/paintableblocks/ | Puente opcional |
| src/main/resources/META-INF/ | Metadatos NeoForge verificados |
| src/main/resources/assets/nomoregrids/ | Idiomas, modelos y texturas propias |
| src/main/resources/data/nomoregrids/ | Perfiles, etiquetas y recetas |
| src/test/ | Pruebas del núcleo |
| src/gametest/ | Ubicación prevista de integración; F0 ajusta source set real |
| docs/obsidian/NoMoreGrids/ | Esta bóveda |
| docs/evidence/ | Resultados por versión, rendimiento y pruebas |

No se crea un archivo Java vacío para simular avance. Las clases se introducen con responsabilidad y pruebas apropiadas. Nombres de tipos sugeridos: PieceRecord, PieceAdapter, CellView, SectionPieceIndex, OperationPlan, GeometrySnapshot y AdapterRegistry.

El controlador de render no depende de objetos mutables de servidor. Compat opcional se aísla en carga; si conviene separarlo en un subproyecto Gradle se hará sin cambiar el contrato público.
