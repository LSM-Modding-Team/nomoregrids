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

## Construcción conectada al servidor y economía real (F1–F4)

Host por dimensión con historial durable, reconstrucción de autoridad, reparación
solo de chunks cargados y recibos por jugador/dimensión. Los datos desconocidos o
futuros se conservan; un conflicto bloquea escritura. Un fallo posterior al commit
bloquea también reparar réplicas con el índice anterior hasta recuperar el diario.

Se verificó en el parche exacto `Entity.java.patch` de userdev 26.2.0.88 que
`getPersistentData()` se guarda en la clave raíz `NeoForgeData`. No se buscan
recibos recursivamente en NBT de ítems. Inventario y recibo se guardan juntos;
se fuerza el archivo y directorio y se vuelve a leer el recibo de disco antes de
instalar checkpoints. Userdev SHA256:
`9e4baada142094ae39670b76ddc225125ec079136105373fc702683e810b100f`.

Candidatos compartidos cliente/servidor, captura de formas vanilla reales,
composición seca, selección exacta por pieza, minería con tiempo/herramienta,
reemplazo conservando pivote/orientación, alcance/carga/permisos/entidades,
eventos de permisos y commit. Manifest explícito ampliado a 312 IDs. Las consultas
por rayo y estado de colocación usan solo casillas cargadas. Presupuestos de
geometría, réplica, colisión y sección se verifican antes del consumo.

Controles reconfigurables P/O/J/K, Alt/R/L; rueda y clic central para ajustes.
Alineación congela plano y propiedades nativas relevantes. Preview con Gizmos
26.2 y profundidad: ámbar pendiente de servidor, verde/rojo según respuesta al
candidato vigente. Handshake de protocolo/manifest y preferencias por jugador;
los modos transitorios se reinician. Aún falta el handshake completo de
adaptadores/límites y el transporte fragmentado de réplicas grandes.

FragmentItem conserva material y longitud (solo paneles/barrotes declarados),
recetas de mitades/unión, reserva exacta o del fragmento mayor mínimo y sobrante.
No se cobra un panel entero por cada fragmento. J+rueda fija longitud 1–16;
J+clic central vuelve a automático. Los ítems de fragmento nunca caen en la ruta
vanilla de colocar un panel completo. Loot depende de la tabla/herramienta nativa.

Decisiones: si loot o sobrante no caben, rechazar todo; todavía no se generan
entidades de ítem. Losas dobles nativas se separan en dos piezas, consumiendo
solo la mitad realmente colocada. Mantener texturas superior/inferior nativas;
canonicalizar solo para orientación lateral explícita. No inventar herramientas
PB contra un mundo falso ni recuperar pintura como un bloque vanilla sin datos.
Carpetas con apoyo, composición mojada y adaptadores funcionales no conectados
se rechazan. Empuje vanilla del bloque técnico está bloqueado. Estas restricciones
son carencias de integración, no meras pruebas pendientes.

Pruebas nuevas escritas: rechazo sin commit/consumo, repetición sin doble cobro,
fallo tras recibo y recuperación, réplica pública sin datos privados, secciones
con checksum/corrupción conservada y formas de colisión vacías. No ejecutadas.
La revisión estructural y parser no comprueban tipos ni ejecutan Java.

## Revisión de invariantes de simulación y durabilidad

La cola lógica de señales conserva un solo evento pendiente por nodo, indexa
aristas y propaga los eventos demorados desde su tick lógico, incluso al agotar
presupuesto. Se evita crecimiento por eventos obsoletos. El planificador de
pistón consulta también la franja contigua al barrido para encontrar adhesión
lateral en una casilla vecina. Solo las casillas realmente barridas exigen carga;
los miembros añadidos se comprueban en la siguiente pasada.

El horno recuerda la receta del progreso y lo reinicia si cambia; no admite
experiencia NaN/infinita. Grupos y orientación de fragmentos validan sus claves
antes de modificar estado. Crear/reabrir el diario fuerza también el directorio.
Pruebas de regresión escritas para estos casos, sin ejecución Java. Revisión
estructural, parser de 123 fuentes y whitespace aprobados; no comprueban tipos.


## Apoyos transaccionales y modelos nativos (F4)

Parches por familia (alfombras, antorchas, faroles, escalera de mano), transformados
con la pieza, y comprobación exacta de cobertura contra la propuesta y vecinos
nativos cargados. Los objetos dependientes no se usan como raíces de apoyo.
La colocación precisa usa el plano de anclaje y no confunde la caja visual de un
farol colgante o antorcha de pared con su plano de soporte. Un hueco de 1/16 se
rechaza. Retirar/cambiar un soporte mediante NMG añade sus dependientes al mismo
plan con un loot por pieza, sin desgaste adicional de herramienta por cascada.
La pérdida de un soporte nativo fuera de NMG aún requiere puente de actualización.

El renderer conserva ahora los quads nativos, UV, colores y capas, los transforma
y recorta por casilla. Los fragmentos cortan altura y añaden tapa con textura de
la cara correspondiente. El tint del material se resuelve contra su estado y
bioma, no contra el contenedor. Un error descarta solo la malla incompleta de esa
pieza antes de mostrar sustituto. No se usa la caja de selección como modelo
visible de antorchas/faroles. Geometría PB conserva su ruta convexa independiente.

Manifest explícito: 324 IDs; cadenas de hierro y cobre encerado sin oxidación
activa. La nieve y el cobre no encerado continúan excluidos de captura genérica
por sus transformaciones. Pruebas de parches cruzando casillas y las 24
rotaciones escritas. Parser de 126 fuentes y comprobaciones estructurales
aprobados; sin compilación, ejecución ni acreditación de fidelidad visual.


## Conexiones y escaleras 3D integradas (F2–F3)

Puertos de paneles/barrotes, vallas y muros transformados con la pieza: coinciden
plano, base, normal, eje vertical, sección y tipo. No se conectan por compartir
casilla ni con un salto de 1/16. La cobertura de una cara sólida puede conectar
un extremo. Vallas de madera y nether conservan tipos separados. Muros resuelven
brazos bajos/altos y poste según contacto superior. Escaleras resuelven esquinas
interiores/exteriores en el marco propio, incluyendo orientaciones laterales.

Las conexiones se planifican antes de validar ocupación final, con máximo 16
pasadas. Las instantáneas intermedias solo existen dentro del planificador;
nunca se instalan si invaden otras piezas. Las preferencias bloqueadas conservan
los extremos decorativos, sujetas a la validación final de invasión.

Decisión: capturar el cierre de vecinos nativos conectados, bajo el límite de
128 cambios, para que la mitad nativa no pierda su conexión al ver un contenedor.
Se rechaza una región que supere presupuesto o carga. NativeStateImage conserva
el estado previo a conversión para contrastar el mundo y reparar una réplica
antigua aunque el nuevo estado tenga conexiones distintas; no es otra autoridad.
El manifest explícito contiene 363 IDs y sigue verificando tickers/BE/fluidos.

La reacción a modificaciones nativas externas posteriores requiere aún el puente
de vecindario. Se escribieron casos de puertos desplazados y las 24 rotaciones.
Comprobaciones estructurales y parser de 130 fuentes aprobados, sin tipos, Java,
Minecraft ni certificación multijugador ejecutados.


## Sesiones, diagnóstico y reparación presupuestada (F1, F9)

Handshake requerido del protocolo experimental 2: hash físico, versiones de
adaptadores, esquema/API y presupuestos. El servidor exige acuse coincidente
antes de aceptar edición. Reglas del mundo persistidas; versiones cambiadas y
manifest físico distinto requieren migración. Una dependencia ausente conserva
su versión registrada y permite cargar sustitutos, sin reinterpretar payloads.

Comandos administrativos inspect/validate/report/recover/export en fuente.
Reportes y exportación pública excluyen inventarios y recibos. recover produce
solo una propuesta; nunca trunca el diario ni sustituye datos automáticamente.
La validación administrativa recorre como máximo 4096 piezas, cuenta conflictos,
réplicas y referencias no cargadas, y declara lo diferido.

La reparación de chunks espera al evento Post del servidor y usa getChunkNow,
según las firmas exactas y la advertencia de ChunkEvent.Load. server.execute no
asegura diferir si ya se está en el hilo del servidor. Trabajos coalescidos,
16 celdas por avance y presupuesto de dos milisegundos entre avances; un avance
individual aún puede sobrepasarlo y no es un benchmark. Descarga cancela trabajo.
Las formas de consulta se cachean por revisión de la réplica y se invalidan al
cargar/instalar. Directorios nuevos se fuerzan junto a sus padres antes de que
un recibo dependa de ellos. Secciones rechazan UUID repetidos/checkpoints negativos.

Preview no tira loot, no consume azar de durabilidad ni dispara BreakBlockEvent.
BeforeOperationEvent expone preview para que los observadores solo validen.
Reemplazos/cascadas quedan ámbar si aún dependen de loot y callbacks de rotura.
Tras permisos se vuelven a contrastar inventario, imágenes nativas y réplicas;
una celda desconocida no se sobrescribe. El recorte visible usa geometría pública,
sin filtrar el payload privado del ítem para determinar su altura.

Pruebas de reglas futuras, cambios de versión y dependencia ausente escritas;
comprobaciones estructurales y parser de 136 fuentes aprobados. No compilación,
tests Java, Minecraft, benchmarks ni certificación ejecutados.


## Checkpoints de simulación (F5, F7–F8)

SimulationSnapshot/Codec/Store guardan región, secuencia, cavidades (máscara y
fluidos de dos bits), una autoridad por máquina con slots/energía/datos, grupos,
grafos de señal, niveles, fuentes, reloj y eventos pendientes. Límites de
colecciones y bytes, CRC32C y reemplazo durable. No se sobrescribe corrupción,
versión futura ni un checkpoint anterior/conflictivo. writeCommitted exige una
secuencia ya durable. La recuperación de circuitos mantiene el tick pendiente;
la del inventario no crea copias en las referencias o grupos.

Decisión: mantener estos checkpoints privados; no enviar inventarios por el
snapshot público. No hay todavía una transacción de host que conecte estas
mutaciones con operaciones, menús, buckets o ticks nativos. El codec y almacén
están implementados, pero su integración sigue pendiente de código.

CavityMaskJob produce incrementalmente máscaras conservadoras desde convexos
externos, sin cambiar su geometría física/render. Un caso numéricamente incierto
cierra el microvolumen solo para fluidos. El resultado no se publica hasta acabar.
La colisión box-box usa aritmética integral directamente; convexos conservan SAT.

Pruebas escritas de roundtrip, fuentes completas, slots únicos, retardos tras
restaurar, corrupción conservada, secuencia durable, máscara de losa y pendiente.
Revisión estructural y parser de 142 fuentes aprobados; Java no ejecutado.


## Puente nativo Paintable Blocks (F6)

PaintableBridge aislado y opcional contra la referencia fijada. preparedFor
reutiliza la preparación nativa del ítem; captura la BE válida con diseño,
identidad/revisión, componentes y NBT privado restante. Los originales se
contrastan antes del commit y al reparar; nunca se sobrescribe otro bloque PB
por coincidir solamente su BlockState. El template del ítem omite la copia del
diseño y se combina con el diseño recuperado, conservando sus otros componentes.

La geometría de ocupación/selección conserva convexos y raycast de unión;
intervalos fusionados evitan seleccionar caras internas. La instantánea de cajas
es filtro conservador, no sustituto para aceptar invasión. Colisión y apoyo se
obtienen de DesignGeometry.shape/supportShape del origen, incluida su política
nativa de colisión 16³ para pendientes. No se anuncia física continua de pendientes.
NMG aplica solo la transformación externa. K/R conservan orientación interna PB
cuando corresponde; recuperar compone la rotación externa mediante correspondencia
de ejes, sin asumir que los IDs PB sean los mismos IDs NMG.

Pincel y EditingToolItem simples usan DesignOperations para generar candidato;
resuelven punto/cara en marco local, coste de pigmento sobre inventario copiado,
evento PB y permisos de retirada; validación completa antes del commit NMG.
No se instalan setters contra un mundo falso. La recuperación evalúa la política
de loot nativa con una BE temporal sin nivel, restaurando sus datos privados;
no se publica ni convierte en segunda autoridad. Guantes/tenazas conservan sus
reglas originales. Las herramientas de volumen/pendientes y pintura por área
no están conectadas: F6 permanece parcial y sin certificación.

Se ampliaron las consultas de apoyo del contenedor y se aisló LinkageError de
carga opcional. Una consulta de imports detectó y corrigió ModelData/ModelProperty:
la fuente exacta usa net.neoforged.neoforge.model.data, sin el segmento client.
`tools/check_api_imports.py` comprueba 58 imports explícitos contra los archivos
inspeccionados; no verifica miembros, comodines, tipos ni compila. Pruebas de
rayos internos/alcance escritas. Estructura, parser de 145 fuentes e imports
aprobados. No se ha ejecutado Java, PB ni Minecraft.


## Intención indivisible y rechazo de objetivos obsoletos

Cada acción del cliente transporta sus preferencias actuales junto al UUID y
revisión esperados del objetivo. El servidor vuelve a trazar y seleccionar;
si no coincide, rechaza STALE_REVISION. Las preferencias separadas conservan
persistencia, pero ya no pueden desincronizar un clic por su rate limit o por
llegar en ticks distintos. Se mantiene el fallback nativo del servidor sin
geometría aceptada enviada por el cliente. Protocolo experimental elevado a 3;
el esquema de piezas sigue en 1.

Decisión: rechazar un clic obsoleto en vez de aplicarlo a una pieza que cambió
mientras llegaba la acción. Revisión estructural, parser de 145 fuentes e
imports explícitos aprobados; sin compilar, ejecutar Java ni Minecraft.


## Pulsos diferidos entre ticks

Corregido un caso de F8 en que un input nuevo sustituía un pulso de un tick
anterior que aún no se había procesado por presupuesto. Las fuentes conservan
una línea temporal por tick; solo se coalescen entradas del mismo tick. Los nodos
internos mantienen un evento pendiente. La cola tiene límite global con reserva
para propagación; al llenarse rechaza el nuevo input antes de modificar la fuente,
para que el host lo difiera conservando su tick. No se conecta una arista de
circuito hacia un nodo definido como fuente externa.

El checkpoint de simulación pasa a esquema 2 y lee también 1. Conserva la línea
temporal al restaurar. Caso de regresión escrito: pulso antiguo pendiente, nueva
entrada contraria, reapertura y dos retardos distintos. Revisión estructural,
parser de 145 fuentes y whitespace aprobados; pruebas Java no ejecutadas.


## Contactos y escalada (F4)

ContactResolver elige por área real bajo los pies y UUID estable en empate,
con unión de rectángulos para no contar caras repetidas. Se conectan los hooks
exactos de NeoForge para fricción, rebote, sonidos y extensión vertical de
colisión (vallas/muros). Las superficies se cachean con la réplica. No se ejecutan
métodos de comportamiento arbitrarios del bloque de origen contra el contenedor.

La escalera de mano comprueba contacto físico y usa isLadder de NeoForge.
Decisión: rechazar orientaciones que cambien su eje vertical; no se inventa una
mecánica de trepar por el techo ni se presenta una escalera decorativa como
funcional. Permanecen pendientes fuego, explosiones y efectos especiales.

Pruebas escritas de área, empate estable, unión sin doble peso, separación y
cara de escalera oculta. Revisión estructural, parser de 147 fuentes e imports
explícitos aprobados. Sin compilación ni ejecución Java/Minecraft.
