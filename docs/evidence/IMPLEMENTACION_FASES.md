# Implementación autorizada F0–F9 (pendiente de compilación)

La solicitud posterior autoriza escribir todas las fases sin esperar a ejecutar F0.
Las puertas de aceptación se conservan para validación y publicación, no como
prohibición de desarrollar código posterior. No se ejecutan Gradle, compiladores
ni Minecraft. No se declara certificada ninguna capacidad.

## Operaciones, diario y red

Añadidos planes inmutables con imágenes antes/después y recibos de recursos;
codec del diario limitado a 16 MiB por operación; log append-only con CRC32C,
lock de archivo, intención y commit con force(true), detección de truncamientos
y deduplicación por UUID. Los logs truncados se conservan y se bloquea su escritura
para reparación; no se adivina si una cola corrupta contenía un commit durable.

El coordinador valida antes del commit y conserva/quarantena fallos posteriores.
La recuperación exige checkpoints/recibos del host y compara estados; no cobra
recursos a ciegas. El host concreto debe verificar todas las casillas, permisos,
alcance, soporte y entidades; no hay un validador que acepte todo por defecto.

Red: ensamblador por conexión con fragmentos de 64 KiB, 2 MiB totales, cuatro
ensamblados simultáneos, caducidad de diez segundos, hash y duplicados coherentes.
Rate limit de intenciones: 20/s con ráfaga 40.

Pruebas escritas de reapertura/deduplicación del diario, cola truncada, fragmentos
fuera de orden y límites. No ejecutadas. Revisión estructural y parser Java.

Fuentes de plataforma: fuentes NeoForge 26.2.0.88 y jars oficiales Minecraft 26.2
consultados mediante javap (lectura de firmas, sin ejecutar clases del juego ni
compilar). Se usan Identifier, ValueInput/ValueOutput y las firmas de 26.2.

## Construcción, familias, apoyo y materiales (F2–F4)

Implementados solver de colocación precisa, orientación por mirada, longitud
ajustada hasta el siguiente límite, estado de modos por jugador y familias de
geometría local. Economía de fragmentos con selección de tamaño exacto/mayor,
sobrante obligatorio, unión de dos fragmentos compatibles y recuperación según
loot. El inventario de entrada permanece intacto si el plan falla.

Conexiones: puertos con coincidencia espacial, tipo, orientación y sección;
resolución determinista con detección de ciclos y máximo 16 pasadas. Apoyos:
cobertura exacta por unión de rectángulos y rechazo de dependencias sin raíz.
Luz: emisión máxima y sellado opaco a resolución de casilla. Mallas: extracción
de caras de uniones de cajas con sustracción de superficies internas; las
geometrías/datos persistentes no se modifican al ocultarlas.

Decisión: las geometrías de familia son modelos locales del núcleo; la captura
vanilla debe tomar las formas reales de la versión instalada, especialmente la
colisión de vallas, que puede exceder el modelo visible. No se certifica ese caso
usando solamente estas plantillas. La integración del render debe conservar
UV y transformaciones locales; BoxMesh entrega rectángulos, no texturas horneadas.

Pruebas escritas de conservación, inventario lleno, apoyo real, ciclos y caras
internas. No ejecutadas. Parser y comprobación estructural sin compilación.

## Fluidos parciales y geometría convexa (F5, base F6)

Implementados máscaras de cavidad 16³, fuente completa separada del agua/lava
parcial, cubos que solo crean/recogen fuentes completas, redistribución
conservadora por cavidades cargadas dentro de cuatro casillas y rechazo sin
mutación cuando faltan espacio o presupuesto. El trabajo de flujo es incremental:
no publica un drenaje parcial al agotar presupuesto. Agua/lava mantienen alcance
horizontal propio; las fronteras ausentes están cerradas. Las reacciones solo
proponen productos completos representables; los contactos parciales se conservan.

Decisión conservadora: obstruir una fuente completa se rechaza; primero debe
recogerse/reubicarse la fuente. No se transforma una fuente en un cubo fraccionario
ni se inventa una nueva fuente en otra cavidad. La redistribución usa los caminos
abiertos anteriores a la colocación para evacuar el volumen desplazado, sin
atravesar sólidos preexistentes. Productos/reacciones y cadencias se integrarán
mediante el host de simulación; los algoritmos no escriben directamente el mundo.

Añadida geometría convexa por semiespacios con clipping, transformaciones,
selección por rayo y prueba de ejes separadores. Se conserva precisión no integral;
los contactos casi degenerados se clasifican indeterminados, no como permiso de
invasión. Pruebas de pendiente, cavidades, cubos, conservación y presupuesto
escritas y no ejecutadas. No se afirma que el motor de fluidos esté certificado.

## Adaptadores y funcionales (F6–F7)

API versionada por capacidades, registro congelable y validación de ocupación
exacta. El índice admite un resolver geométrico de adaptadores y lo conserva
al reconstruir estados. PaintableAdapter carga PB solo de forma opcional,
verifica BlockDesign.VERSION=2, conserva su codec nativo completo y reconstruye
voxels/pendientes/pinturas mediante las APIs inspeccionadas en el commit fijado.
`physical()` compone la orientación nativa una vez; la transformación NMG es
externa. Ediciones se preparan y validan sin setters ni cobros nativos.

Decisión: no confundir captura/edición del diseño con que todas las herramientas
PB ya encuentren una pieza dentro del contenedor. La búsqueda de componente y
el cobro único requieren el puente de interacción concreto; el adaptador no
invoca herramientas contra un mundo falso. No se acredita compatibilidad PB.

Funcionales: almacén único por UUID con slots, energía, datos y revisión; las
consultas por contacto solo enrutan cuando hay un puerto único. Registro de
grupos inseparables y lógica explícita de horno con recetas, combustible,
progreso, resultado y experiencia; un recipiente sobrante nunca se descarta.
Los menús y recetas concretos deberán conectar esa autoridad al registro vanilla.
Pruebas escritas para autoridad, revisiones, puertos ambiguos y horno. No ejecutadas.

## Grupos, mecanismos y presupuestos (F8–F9)

Planificador de pistones: cierre por componentes/grupos, límite doce, traslación
entera de un bloque conservando offset, barrido de cajas, comprobación de carga,
obstáculos y adhesión explícita; extracción de cambios por lote. Los grupos solo
pueden editarse registrados y completos. Raíles comprueban extremo y tangente.
Redstone mantiene niveles 0–15, enlaces explícitos, caída según longitud real,
comparador y repetidor con retardo lógico y cola determinista/presupuestada.

Herramientas de F9: LRU por bytes, trabajos diferidos/coalescidos y muestras reales
con percentiles. Ninguna prueba de rendimiento se marca aprobada sin ejecutar.
No se introducen entidades por microcubo ni un ticker por pieza decorativa.

Decisión: el barrido de pistón implementado es exacto para cajas ortogonales;
la movilidad de geometría externa sigue requiriendo su proveedor específico.
Los algoritmos de mecanismos reciben obstáculos/carga/puertos del host, en vez
de llamar métodos vanilla sobre un mundo simulado. La certificación y las
mediciones siguen pendientes, aunque exista código de los sistemas.

Pruebas escritas: pieza desplazada que cruza casillas, barrido obstruido,
trece componentes, retardo de repetidor, nodo sin enlace, LRU y trabajo diferido.
No ejecutadas. Continúa integración de estos módulos con la plataforma.

## Puente real de contenedor y render (F1–F4, F6)

Registrado el bloque técnico sin ítem, entidad con réplicas públicas persistidas
y sincronización nativa de block entities. Consulta selección/colisión desde
réplicas, incluso sin sección propietaria. Payloads desconocidos se conservan y
bloquean de forma conservadora. Proyección pública excluye datos de ítem y
material privados; adaptadores desconocidos necesitan proyección explícita.

Modelo dinámico de sección 26.2 mediante RegisterBlockStateModels, con claves
por revisión/generación, materiales nativos, clipping por casilla y UV a escala
original. Diseños PB reconstruyen polígonos recortados y materiales de pintura,
con transformación externa única. Se consultaron firmas de jars oficiales con
javap, sin compilar ni ejecutar Minecraft. Manifest vanilla explícito: 310 IDs,
además de verificaciones de entidades, random tick, fluidos y geometría integral.

Decisión: ocupación local mantiene el bloque atómico; las consultas de colisión
pueden estar vacías o extenderse hasta una casilla adicional, preservando casos
nativos como vallas sin convertir todo su volumen visual en sólido. El codec
experimental de cajas admite la forma vacía de colisión. No se ha publicado
ningún formato ni existe evidencia de mundos NMG creados con la versión previa.

SectionStore guarda solo autoridades por sección, con checkpoint, CRC y reemplazo
atómico durable; no degrada una versión futura ni borra originales corruptos.
Todavía se requiere integrar operaciones/recibos/carga y herramientas en el host
servidor; disponer de bloque y renderer no acredita esas rutas del juego.
