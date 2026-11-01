# Transferencia experimental de fluidos — 2026-10-31

Base: `35fa89ebfd63b6df4331ea2dc63c8f25c3292e77`.

Se compara cada imagen de cavidad sin sólidos con el patrón de fuente o de nivel
nativo, incluidos tipo, altura y distancias persistidas. Si la casilla no tiene
piezas y el bloque actual es aire o contenedor NMG, la eliminación del efecto de
cavidad y la proyección nativa se incluyen en una misma operación durable. Al
liberar una proyección para que vanilla modifique el bloque se evita recapturar
el fluido; al colocar piezas en una proyección se retira la autoridad nativa en
el plan de conversión. Las imágenes no representables permanecen intactas.

La transición es parcial: no convierte perfiles irregulares ni implementa una
superficie suavizada para cavidades, ni una unión visual continua con fluido
vanilla. No se afirma que elimine las bandas de la captura. Requiere pruebas de
propagación, conservación, reacciones, cubos, guardado y reinicio, cargas de
chunks, multijugador y representación. No hay evidencia de ejecución en juego.

Java 25 / Gradle 9.2.1: `compileJava compileTestJava compileValidationJava`
correcto. Se añadió una prueba de imágenes exactas y de rechazo de imágenes
parciales; se compiló pero no se ejecutó. No se inició Minecraft ni se ejecutaron
pruebas Java, conforme al alcance de ejecución autorizado.
