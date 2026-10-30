# Configuración y diagnóstico

## Ámbitos

Servidor: familias habilitadas, perfiles físicos, límites, permisos, adaptadores, fluidos y mecanismos. Cliente: teclas, colores, opacidad, indicadores y cachés visuales. Mundo: versión, adaptadores usados y reglas que alteran interpretación persistente.

El cliente no cambia física ni límites del servidor. Las reglas relevantes se sincronizan y tienen hash. Cambiar un perfil físico obliga a validar piezas existentes antes de aplicarlo; los datos incompatibles se conservan.

## Recursos previstos

Perfiles bajo data/nomoregrids/piece_profiles y etiquetas bajo data/nomoregrids/tags. Cada perfil declara origen, familia, capacidades, geometría declarativa permitida, soporte, material y versión. Las etiquetas agrupan; no conceden compatibilidad a entidades complejas.

Configuración de servidor con valores iniciales de [[Limites y rendimiento]]. Carga de perfiles es validada: campos desconocidos esenciales o conflictos de prioridad producen diagnóstico. Prioridad: adaptador explícito certificado > perfil específico por ID > perfil por etiqueta > exclusión predeterminada.

## Diagnóstico previsto

Comandos administrativos bajo /nmg:
- inspect: componente, origen, revisión, adaptador y presupuesto.
- validate: comprobar referencias, geometría y revisiones de una zona cargada.
- report: informe legible sin inventarios privados ni payloads secretos.
- recover: propuesta de reparación; no borra ni sustituye automáticamente.
- export: diseños/datos para respaldo y asistencia.

Los comandos inspect, validate, report, recover y export existen en fuente como diagnóstico administrativo experimental. validate limita el lote a 4096 piezas y declara referencias no cargadas; recover escribe una propuesta sin aplicar cambios; export produce un diseño público sin inventario. No se han ejecutado. Las reparaciones destructivas requerirán selección explícita del administrador y copia previa.

## Errores

Cada rechazo tiene código estable y texto localizado: OVERLAP, NO_SUPPORT, UNLOADED_REGION, UNSUPPORTED_ADAPTER, STALE_REVISION, COMPLEXITY_LIMIT, ENTITY_BLOCKED, PROTECTED, INVENTORY_MISSING, INVALID_DATA.

Los logs deduplican errores repetidos. Un adaptador que falla se desactiva para nuevas operaciones de esa familia y deja componentes en sustitución conservada, sin tumbar todo el servidor si el estado puede aislarse.
