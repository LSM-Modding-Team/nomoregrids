# Transacciones y eventos

## Pipeline

Toda acción se convierte en OperationPlan inmutable. Incluye ID de operación, actor, revisiones esperadas, piezas afectadas, casillas, antes/después, coste, loot, apoyo, conexiones y notificaciones.

1. Resolver objetivo con alcance y línea de visión del servidor.
2. Capturar revisiones y construir candidato sin efectos.
3. Comprobar todas las casillas, entidades, apoyos, límites y adaptadores.
4. Consultar permisos y eventos cancelables.
5. Reservar materiales en el hilo del servidor.
6. Revalidar revisiones y registrar intención/commit en diario.
7. Aplicar estado e inventario; emitir loot una vez.
8. Publicar revisiones y consecuencias.

Rechazar deja mundo e inventario como estaban. Todos los índices se bloquean lógicamente en orden de dimensión/sección/casilla/UUID para evitar cambios intercalados. No se ejecuta lógica mutable de mundo en trabajadores.

## Eventos públicos previstos

BeforePlacePiece, BeforeEditPiece, BeforeRemovePiece, BeforeMoveGroup y BeforeReplacePiece son cancelables; contienen actor, causa, volúmenes y plan legible. AfterCommit es informativo y no puede cancelar retrospectivamente. Nombres de diseño, pendientes de vincular a clases de la plataforma.

Se emiten eventos vanilla pertinentes para todas las casillas afectadas de forma deduplicada. El evento propio incorpora identidad del componente. Un mod de protección sin adaptación no se considera compatible por recibir un evento de bloque aislado.

Protección requiere permiso en cada casilla tocada y para cada inventario/recurso afectado. Un desplazamiento que cruza una parcela no puede validarse solo en el pivote.

## Paintable Blocks

Su evento cancelable puede validar una edición antes del commit. Si su pipeline no permite hacerlo o sus herramientas consultan directamente una entidad por casilla, se añade un puente explícito en ambos lados; no se afirma que el evento existente lo resuelva todo.

Un único pipeline cobra materiales/durabilidad y emite recompensas. NMG no duplica el coste ya aplicado por Paintable Blocks. Las nuevas revisiones invalidan formas, soporte, malla y referencias.

## Consecuencias

Notificaciones de apoyo, conexiones, luz y redstone se acumulan por casilla y causa. Se aplica primero la geometría confirmada y después las consecuencias. Cambios dependientes obligatorios se incluyen en el plan o se programan con identidad propia para evitar roturas dobles.

Si un manejador externo lanza una excepción antes de commit, abortar. Si ocurre después, registrar fallo y conservar el commit: no «deshacer» solo una parte. Recuperación: [[Guardado y recuperacion]].
