# Puente de salida de fluidos y superficies — 2026-10-31

Base: `be387280d2bf7b941ab28fff27cbb46f00e50229`.

Las casillas de aire nuevas que reciben flujo por una microcara mojada se
proyectan como fluido nativo en la operación durable, sin crear una cavidad NMG
para ese flujo. La cara emisora debe contener el mismo fluido; los sólidos no
pueden emitirlo. El flujo horizontal usa la distancia almacenada, y el flujo
hacia abajo se representa como caída nativa; ninguno se convierte en fuente.
Si las reacciones consumen el fluido, no se exporta desde el estado anterior.
Las cavidades ya existentes y las celdas ocupadas conservan su autoridad.

Las columnas mojadas con un suelo sólido común y sin techo ahora pueden usar la
misma interpolación de superficie que las columnas sobre el fondo vacío. Para
columnas con diferentes suelos, huecos, techos o fluidos mezclados se mantiene
la geometría por microceldas, evitando interpolar a través de piezas. La
compatibilidad con mundos de versiones anteriores ya no es un requisito por
instrucción del usuario; no se modificó el formato en esta entrega.

Java 25 y Gradle 9.2.1: compilación de fuentes principales, pruebas y validación
correcta. Se añadieron casos para caras mojadas y bloqueadas, caída y suelo
uniforme; no se ejecutaron. No se inició Minecraft ni se midió el resultado.
La propagación sostenida entre motores, secado, reacciones, cubos, superficies,
fronteras de chunks, persistencia y multijugador requieren pruebas ejecutadas;
no se declara certificada la transición ni que todos los perfiles irregulares
sean convertidos o suavizados.
