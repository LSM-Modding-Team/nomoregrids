# F1 — codec local de cajas, 2026-10-29 (America/Lima)

## Implementado

`BoxPieceCodec` serializa y lee el registro de las familias integrales de cajas.
Conserva UUID, revisión, adaptador/versión, bloque/estado de origen, transformación,
pivote, grupo, banderas, tres formas independientes y los tres payloads opacos.
La sección propietaria se deriva y no se almacena una autoridad duplicada.

El marco NMGB tiene magic, versión, longitud, cuerpo y CRC32C. El límite completo
es 256 KiB sin comprimir, incluyendo geometría y metadatos. Se limita la escritura
antes de crecer el buffer y se comprueban tamaños/conteos antes de asignar listas
o arrays al leer. El checksum detecta corrupción accidental; no autentica datos.
Se rechazan datos truncados, campos inválidos, banderas desconocidas y bytes extra.

Una versión positiva futura del marco se conserva íntegra en `Preserved`, sin
interpretar su cuerpo ni ofrecer un encoder de rebaja. No se devuelve un bloque
sustituto inventado ni loot. El llamador conserva el archivo original si hay
corrupción; este codec nunca toca disco ni repara o sobrescribe archivos.

## Decisión y alcance

Formato interno experimental de la geometría de cajas, sin publicación ni mundos
existentes. No es el codec de geometría externa, protocolo de red, NBT de chunks,
diario durable ni migración de mundo. Los cambios de formato posteriores deberán
versionarse; los datos Paintable Blocks no se reinterpretan con este formato.

La integración con contenedor, eventos, guardado de secciones e inventario debe
esperar a verificar las APIs exactas y la ejecución de F0/F1. No se habilitan
operaciones multicelda en el juego por disponer de un codec de memoria.

## Verificación

JUnit escrito para round-trip de todos los campos, determinismo, 24 rotaciones,
versión futura conservada, truncamientos, corrupción byte a byte, longitud enorme,
revisión inválida y presupuesto total. Tests Java NO EJECUTADOS.

Ejecutados: comprobador estructural, parser Java Tree-sitter (53 fuentes, sin errores sintácticos) y `git diff --check`. No hay resolución de tipos.
No se compila ni se arranca Minecraft. F0 y F1 abiertas; F2–F9 pendientes.

## Corrección encontrada en revisión estática

La reconstrucción de índices existentes se distingue de la colocación nueva: `PieceIndex.restored` valida estructura y solapamientos sin imponer los límites administrativos actuales. Reducir un límite no puede impedir recuperar una instantánea válida. Los cambios nuevos siguen comprobando límites relativos al estado previo. No se afirma que esta función cargue archivos o chunks.

Los grupos y sustituciones se conservan como datos, pero sus cambios normales se rechazan: F7 todavía no dispone de adaptador de grupo. Se añaden casos escritos para esta exclusión. También se conserva en Git el bit ejecutable de `gradlew`; no se ejecutó el wrapper.
