# Catálogo de familias

La inclusión se realiza por identificadores de registro y propiedades verificadas, no solo por coincidencia de nombre. Los conjuntos de materiales se generan al implementar y su manifiesto se guarda con la entrega. «Todos» significa todos los miembros vanilla que cumplen el criterio; las excepciones están expresamente excluidas.

| Familia | Fase | Capacidades |
|---|---|---|
| Cubos decorativos sólidos estáticos de piedra, madera procesada, metales, terracota, lana y hormigón | F2 | Desplazar/componer; orientar texturas con modo ampliado |
| Losas y escaleras de construcción vanilla | F2 | Desplazar/componer/orientar por defecto |
| Troncos, tallos y pilares estáticos | F2 | Desplazar/componer y orientar eje en modo ampliado |
| Cristal y cristal tintado de colores | F2 | Desplazar/componer; transparencia propia |
| Cristal tintado que bloquea luz | F2 | Desplazar/componer; política óptica propia |
| Paneles de cristal y barrotes | F3 | Desplazar/componer/recortar/conectar |
| Vallas y muros | F3 | Desplazar/componer/conectar; recorte tras modelos adaptados |
| Alfombras y capas de nieve | F3 | Superficie parcial; reglas propias de capas/apoyo |
| Cadenas, antorchas, faroles y escaleras de mano | F4 | Desplazar/orientar según soporte |
| Carteles y trampillas | F4 | Adaptador de datos/interacción |
| Hornos, contenedores y estaciones | F7 | Solo por adaptador funcional validado |
| Puertas y camas | F7 | Grupo multipartes |
| Redstone, raíles y pistones | F8 | Adaptación específica |
| Diseños Paintable Blocks | F6 | Adaptador específico |
| Máquinas y recipientes Paintable Blocks | F7 | Adaptación separada; no heredan la de diseños |

## Exclusiones del reconocimiento general

Arena, grava, polvo de hormigón, hojas, hielo que se derrite, bloques de coral vivos, cultivos, plantas, fluidos, portales, spawners, comandos, bloques con inventario/entidad, mecanismos y bloques con actualización periódica o comportamiento dependiente de vecinos no adaptado.

La exclusión es de la composición/desplazamiento, no del mundo: siguen funcionando de forma nativa. La gravedad y transformaciones se incorporan posteriormente por contrato, sin habilitar todos estos casos por reflexión.

Los bloques añadidos por otros mods no se habilitan automáticamente por heredar una clase vanilla. Un perfil por datos puede declarar decorativos simples, pero una entidad de bloque, ticker o comportamiento especial exige adaptador.

## Materiales sensibles

Troncos mantienen capacidad de ser pelados solo después de adaptar esa interacción. Cobre conserva oxidación/encerado únicamente cuando el transformador por pieza esté listo; antes se admiten variantes estables sin transición habilitada o se excluye su composición. Nunca se desactiva una propiedad del material silenciosamente.

Ver [[Matriz de compatibilidad]] y [[Plan de fases]].
