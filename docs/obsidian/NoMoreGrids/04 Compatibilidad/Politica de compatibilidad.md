# Política de compatibilidad

## Objetivo

Compatibilidad por capacidad y versión. «Se ve bien» no equivale a interacción, guardado o mecanismos correctos. Los estados son: diseñado, adaptado, compilado, probado y certificado. Esta edición está en diseñado.

## Niveles

1. Reconocimiento general: decorativos simples incluidos expresamente.
2. Perfil por datos: identificación, forma estática y restricciones sin comportamiento ejecutable complejo.
3. Adaptador específico: diseños dinámicos, entidades, herramientas, inventarios o mecanismos.

Capacidades: PLACE_NATIVE, OFFSET, ROTATE, COMPOSE, RESIZE, INTERACT, MOVE, FLUID_COEXIST. Un bloque puede tener solo algunas.

No se habilita composición por herencia de clase, nombre o mera existencia de VoxelShape. El resultado no garantiza preservar actualizaciones, propiedades ni datos.

## Nativo primero

Bloques sin necesidad de composición/desplazamiento permanecen nativos. Conversión y restauración pasan por captura/reconstrucción del adaptador. Nunca se llama a métodos arbitrarios contra un mundo falso como solución universal.

## Otros tipos de mods

| Tipo | Política |
|---|---|
| Materiales decorativos | Perfil probado o adaptador simple |
| Máquinas/energía/tuberías | Adaptador con puertos y estado único |
| Protección de terrenos | Permisos por todas las casillas y componente |
| Copia/pegado y esquemas | Exportación/importación NMG con remapeo de UUID |
| Render/shaders | Pruebas visuales y de frontera específicas |
| Física de estructuras móviles | Deshabilitado hasta puente de movimiento propio |
| Optimización de servidor | Verificar hooks, carga y orden de actualizaciones |
| Otros sistemas multipartes | Evitar contenedores anidados; puente de piezas explícito |

## Dependencias

Adaptadores opcionales no obligan a instalar el mod externo. No se cargan clases de una dependencia ausente. Cada adaptador declara rango probado y versión de esquema. Si no se reconoce una versión, se conserva la pieza y se bloquean cambios.

No se incluyen código ni recursos externos en esta bóveda. No se elige licencia de distribución del futuro mod por inferencia; es una decisión jurídica de publicación separada del diseño técnico.
