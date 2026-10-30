# Estado de desarrollo — 2026-10-29 (America/Lima)

La solicitud de implementar las fases amplía el alcance del esqueleto. Se
mantienen las versiones y la restricción de AGENTS.md sobre compilación y
arranque: requieren una petición explícita de ejecución. Ninguna fase está
aceptada. Las pruebas escritas no se presentan como resultados ejecutados.

| Fase | Avance real | Pendiente para aceptación |
|---|---|---|
| F0 | Wrapper restaurado y checksum contrastado; licencia coherente; JDK 25 localizado; fuentes exactas NeoForge inspeccionadas parcialmente | Resolver build y APIs restantes, compilar, arrancar cliente/dedicado |
| F1 | Coordenadas, rotaciones, geometría de cajas, selección local, identidad, índices inmutables y codec de memoria; tests Java escritos | Contenedor, captura/restauración nativa, operaciones con permisos/apoyo/inventario, persistencia de secciones, diario/recibos, red y pruebas de fallo |
| F2 | Geometrías canónicas preparatorias dentro del núcleo F1; ninguna mecánica habilitada | Adaptadores vanilla, controles, preview, selección/loot del mundo, multijugador y matriz geométrica |
| F3 | Diseño documentado; sin implementación | Paneles/barrotes/vallas/muros, modelos, recortes y conexiones tras F1–F2 |
| F4 | Diseño documentado; sin implementación | Familias decorativas, parches de apoyo, materiales y luz tras integración de mundo |
| F5 | Diseño documentado; sin implementación | Cavidades, redistribución, fuentes, cubos y reacciones; depende de F1–F4 |
| F6 | Referencia PB fijada en documentación; sin adaptador | Puente de datos/herramientas/render y certificación con PB, tras F1–F4 |
| F7 | Diseño documentado; sin implementación | Inventarios/menús/puertos/grupos únicos y persistencia |
| F8 | Diseño documentado; sin implementación | Redstone/raíles/pistones, temporización y volumen barrido |
| F9 | Presupuestos y escenas definidos; sin medición | Optimización medida, recuperación y certificación ejecutada |

## Verificación realizada

- `python tools/check_repository.py`: JSON, TOML expandido en memoria, licencia,
  checksum del wrapper, enlaces wiki e independencia del núcleo.
- `python tools/check_java_syntax.py`: parser Tree-sitter 0.25.2 / Java 0.23.5,
  53 fuentes sin errores sintácticos. No comprueba tipos ni APIs.
- `git diff --check`: sin errores de whitespace.

No ejecutados: Gradle, compiladores Java, tests Java, GameTests, Minecraft,
inyección de fallos, mediciones de rendimiento ni integración Paintable Blocks.
No hay JAR del mod generado ni mecánicas activas en las entradas del juego.

## Decisiones durante el trabajo

- Commits locales por bloque coherente: no hay una política local de solo-online,
  ni se heredan reglas del otro repositorio. No se hace push ni se publica.
- Conservar MIT ya existente en LICENSE; alinear los metadatos.
- Restaurar el wrapper oficial desde la distribución 9.2.1 y su permiso ejecutable sin ejecutar Gradle.
- Avanzar únicamente en código independiente del mundo mientras F0 no pueda
  verificarse. No inventar hooks ni habilitar capacidades sin sus pruebas.
- Limitar este núcleo a cajas integrales; geometrías externas siguen intactas
  y excluidas hasta su adaptador. No convertir pendientes en cajas aproximadas.
- Mantener el codec experimental separado de guardado de mundo y red. Una
  serialización en memoria no satisface la recuperación entre chunks/inventarios.
- Conservar grupos/sustituciones como datos y bloquear sus cambios hasta contar con su adaptador.
- Preservar instantáneas existentes al reducir límites; exigirlos para cambios
  que aumenten el exceso. Ningún límite nuevo autoriza borrar piezas.

La siguiente tarea dependiente es verificar F0 y el contenedor real de F1.
Seguir habilitando F2–F9 sin ello contradiría las puertas de aceptación y la
regla de no crear registros ficticios. El trabajo independiente autorizado está
registrado en `docs/evidence`; el resto permanece pendiente, sin marcarlo completo.
