# Superficies y fronteras de fluido — 2026-10-31

Base: `13e889a3616dd595b557092f8cd785697b9069e6`.

- Las cavidades sin sólidos, de un solo fluido y con columnas conectadas al
  fondo usan una superficie visual por vértices; los vértices promedian alturas
  de columnas mojadas adyacentes, incluidas cavidades y líquidos nativos. Se
  omiten caras verticales interiores entre columnas mojadas. La simulación y
  su máscara persistente no se alteran.
- Cavidades con sólidos, mezcla de fluidos o huecos verticales conservan la
  malla anterior, para no interpolar agua a través de componentes.
- La unión con un líquido nativo elimina una cara solo si ese líquido cubre
  completamente la frontera; en superficies parciales se conserva la cara.
  La clave de geometría incluye vecinos y diagonales, y la réplica invalida
  también mallas diagonales al cambiar.
- Esta no es todavía la transición completa solicitada: las cavidades irregulares
  sin sólidos pueden seguir siendo `composite_cell` si no tienen una imagen
  nativa exacta; tampoco se suavizan perfiles que contienen piezas NMG. La
  exportación de dichos estados requiere un puente de flujo por caras que no
  convierta una cantidad parcial en fuente ni atraviese sólidos. Se eligió
  conservarlos para evitar pérdida o duplicación de fluido.

Java 25 / Gradle 9.2.1: `compileJava compileTestJava compileValidationJava`
correcto. Se compiló una prueba de selección de perfiles; no se ejecutaron
pruebas Java ni se inició Minecraft. El aspecto de las uniones, las máscaras,
propagación, físicas, cubos, persistencia y multijugador no están verificados.
