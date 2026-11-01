# Matriz trazable

67 casos; estado actual: compilación solamente.
Las fuentes son cobertura parcial, no resultados. Los procedimientos completos
y la evidencia requerida siguen siendo obligatorios. Consultar [F9](F9.md) y
[las sondas reales](../../tools/validation/README.md) para preparación y límites.

| Caso | Fuente JVM | Sonda real de mundo | Procedimiento adicional |
|---|---|---|---|
| G01 | `GeometryTest.java#negativeCoordinatesAndSectionBoundariesUseFloor` | — | Colocar/serializar origen (-1,-17,15) en q; comprobar casillas (-1,-2,0), restos (15,15,15) y dueño por floorDiv. |
| G02 | `GeometryTest.java#cubeOnSlabTouchesTwoCellsAndDoesNotIntersectSupport` | — | Enumerar un cubo [0,16) y otro [16,32); comprobar una sola casilla por cubo y que una cara en 16 no crea ocupación adicional. |
| G03 | `GeometryTest.java#allRotationsAreUniqueProperAndClosedUnderComposition`<br>`BoxPieceCodecTest.java#allOrientationsRetainIdentityAndExactOrigin` | — | Para las 24 orientaciones, componer inversa y volver a codificar pieza con pivote no nulo, coordenadas negativas y payload; comparar todos los campos. |
| G04 | `GeometryTest.java#touchingIsAllowedButOneUnitOfPenetrationIsNot` | — | Colocar dos cubos tocando exactamente una cara; mover una entidad sobre la unión y comprobar colisión continua sin penetración ni vibración. |
| G05 | `PieceIndexTest.java#componentsRemainIndependentAndInvalidChangesLeaveOriginalUntouched` | — | Invadir una pieza por 1 q; repetir con cliente/revisión obsoletos y comprobar rechazo, inventario y diario sin segundo commit. |
| G06 | `BoxRaycastTest.java#rayPassesThroughStairVoidAndStopsAtActualSurface`<br>`ConvexRaycastTest.java#realGapKeepsFirstVisibleSurfaceAndReachIsEnforced` | — | Mirar una pieza detrás del hueco de una escalera; contrastar selección con un rayo que atraviesa sólido y con alcance excedido. |
| O01 | `GeometryTest.java#slabHasSixDistinctHalvesAndPivotSurvivesRoundTrip` | — | Colocar las seis mitades de losa, romper cada una y volver a colocar su loot; comprobar forma, orientación y cantidad. |
| O02 | `FamilyPortsTest.java#rotatedPortsMaintainContactInEveryFrame` | — | Crear escalera vertical y dos vecinos para esquinas interiores/exteriores; girar el marco y cambiar un vecino nativo. |
| O03 | — | — | Con modo ampliado apagado/encendido colocar cubo, tronco, losa y escalera; comprobar seis frentes frente a modos específicos de losas/escaleras. |
| P01 | `GeometryTest.java#cubeOnSlabTouchesTwoCellsAndDoesNotIntersectSupport` | — | Colocar cubo sobre losa inferior y apuntar a ambas piezas; comprobar límites verticales 8..24 q y contacto sin intersección. |
| P02 | `GeometryTest.java#outwardQuantizationLeavesRealGapAndTiesGoPositive` | — | Usar superficie PB inclinada a 1,5 q; colocar cubo con precisión y medir origen 2 q, hueco de 0,5 q y techo 18 q. |
| P03 | `GeometryTest.java#outwardQuantizationLeavesRealGapAndTiesGoPositive` | — | Repetir contacto desde caras negativas y coordenadas negativas, incluidos empates tangenciales; comprobar redondeo hacia fuera. |
| P04 | `GeometryTest.java#offsetAtNegativeSectionCornerTouchesEightSections`<br>`PieceIndexTest.java#multicellPieceHasOneAuthorityAndRemovingItClearsAllReferences` | `nmg_accept run eight_cells <anchor>` | Desplazar cubo 8 q en los tres ejes; guardar/recargar, romper una referencia y comprobar UUID único, ocho referencias retiradas y loot único. |
| P05 | — | — | Situar candidato cruzando chunk descargado; registrar chunks cargados antes/después y comprobar rechazo sin consumo ni ticket nuevo. |
| P06 | — | — | Intentar colocar una pieza parcialmente fuera de altura y worldborder; repetir en negativo y con orientación multicelda. |
| C01 | `PieceIndexTest.java#componentsRemainIndependentAndInvalidChangesLeaveOriginalUntouched` | `nmg_accept run mixed_slabs <anchor>` | Colocar losa inferior de piedra y superior de madera en la misma casilla; seleccionar y retirar una conservando la otra y su material. |
| C02 | `PieceIndexTest.java#componentsRemainIndependentAndInvalidChangesLeaveOriginalUntouched` | `nmg_accept run mixed_slabs <anchor>` | Intentar losas perpendiculares con volumen común; comparar inventarios, UUID y revisiones antes/después del rechazo. |
| C03 | `BoxRaycastTest.java#rayPassesThroughStairVoidAndStopsAtActualSurface` | — | Colocar una pieza pequeña dentro del hueco real de escalera; probar otra que invada la parte sólida por 1 q. |
| R01 | `ConstructionRulesTest.java#cuttingConservesLengthAndRejectsMissingRemainderSpace` | — | Cortar fragmento de longitud 16 a 6 y comprobar sobrante 10; juntar ambos con datos idénticos y recuperar 16. |
| R02 | `ConstructionRulesTest.java#cuttingConservesLengthAndRejectsMissingRemainderSpace` | — | Llenar inventario y cortar un stack cuyo sobrante necesita slot; comprobar rechazo total y ausencia de entidades/drops. |
| R03 | `ConstructionRulesTest.java#incompatibleFragmentsNeverJoinOrChangeTheirSourceStacks` | — | Intentar unir fragmentos de distinto material, orientación, pintura o payload; comprobar rechazo sin alterar slots. |
| R04 | — | — | Romper cristal/barrotes de cristal con herramienta sin recuperación y con Silk Touch; comprobar que solo el caso permitido produce fragmento. |
| I01 | — | — | Componer dos piezas, romper una y repetir la intención de red; contar loot una vez y comprobar vecina/revisiones. |
| I02 | — | — | Reemplazar una pieza por otra que invade un vecino o carece de soporte; comparar inventario/cursor y ambas identidades. |
| I03 | — | — | En supervivencia, reemplazar antes de completar minería y luego con herramienta y duración correctas; repetir cambiando herramienta a mitad. |
| I04 | — | — | Intentar elegir candidato detrás de pared opaca y a través de un hueco real; solo el segundo debe ser accesible. |
| S01 | `ConstructionRulesTest.java#supportNeedsCompleteCoverageAtActualPlane`<br>`SupportPatchesTest.java#carpetCanSpanTwoSupportsButNotOneQuantumGap` | `nmg_accept run support <anchor>` | Poner alfombra de huella completa sobre apoyo de media huella/punto y luego sobre dos apoyos que cubren toda la base. |
| S02 | — | `nmg_accept run support <anchor>` | Retirar por Level.setBlock un apoyo nativo de alfombra multicelda; avanzar ticks y comprobar solo dependientes retirados y recibo durable de loot. |
| S03 | `ConstructionRulesTest.java#supportNeedsCompleteCoverageAtActualPlane` | — | Construir cadena de apoyos con raíz estable, quitar raíz y probar ciclo sin raíz; repetir con carteles colgantes. |
| S04 | — | — | Construir una cadena de 129 vallas nativas y una edición que requiera capturar todo el cierre; comprobar rechazo por límite sin consumo y originales intactos. Para una topología que oscile, registrar estados y verificar aborto en 16 pasadas, sin publicar estados intermedios. |
| L01 | `CellLightingTest.java#emittersUseMaximumAndTwoHalvesSealOnlyTheirUnion` | — | Componer dos emisores de niveles distintos; comprobar emisión máxima, no suma, antes/después de recargar. |
| L02 | `ConstructionRulesTest.java#internalBoxFacesAreRemovedWithoutChangingGeometry` | — | Pintar dos caras que queden ocultas al componer; separar piezas y comprobar que la pintura reaparece y la geometría persistente no cambió. |
| F01 | — | — | Intentar colocar o mover piezas hacia agua, lava y bloques anegados; verificar rechazo sin consumo ni mutación. Probar cubos y flujo contra celdas compuestas y flujo nativo externo. |
| M01 | `FunctionalStoreTest.java#referencesResolveToOneAuthorityAndStaleEditsAreRejected`<br>`FunctionalStoreTest.java#multiplePortsAtSameContactDoNotMergeInventories` | `nmg_accept run barrels <anchor>` | Componer dos barriles que comparten host; transferir a un puerto expuesto, abortar/confirmar y comprobar slots independientes y host ambiguo rechazado. |
| M02 | `MechanismTest.java#pistonMovesWholeOffsetPieceAndRejectsSweepObstruction` | — | Empujar componente que ocupa varias casillas; comprobar UUID/revisión, referencias, barrido y vuelta del pistón pegajoso. |
| M03 | `MechanismTest.java#thirteenComponentsDoNotBecomeTwelveCells` | — | Preparar cadena de 12 y 13 componentes, incluidos miembros de puerta; solo el cierre dentro de 12 puede moverse. |
| M04 | `ConvexSweepTest.java#extrusionRetainsSlopeAndNewEdgeFacets` | — | Obstruir el volumen barrido intermedio dejando destino libre; repetir con PB convexo, entidades y slime/miel. |
| M05 | — | — | Poner polvo y fuentes en una casilla con puertos separados; mover hasta contacto exacto y comprobar que solo entonces hay señal. |
| M06 | `ExperienceProgressTest.java#preservesFractionAcrossLevelBoundary` | — | En cada menú soportado usar clic, drag, shift-click, autofill y extracción con XP; cerrar/desconectar/mover mientras hay cursor y verificar slots/XP/recibo. |
| M07 | `FunctionalStoreTest.java#multiplePortsAtSameContactDoNotMergeInventories` | `nmg_accept run barrels <anchor>` | Con ResourceHandler real probar commit, abort y transacción anidada; repetir con energía soportada y otro nivel para comprobar rechazo entre dimensiones. |
| M08 | `GroupAuthorityTest.java#completeGroupCanBeCreatedAndMovedButNeverSplit` | `nmg_accept run doors <anchor>` | Colocar, abrir, reemplazar y retirar puertas/camas/trampillas; bloquear barrido con entidad; dormir, ocupar, despertar y respawn con marco válido/inválido. |
| M09 | `RailPathTest.java#matchingEndpointsAllowARealTurnAndAOneQuantumGapDoesNot`<br>`RailPathTest.java#ascendingEndpointMatchesTheNextBlockAndDistanceRoundTrips` | — | Recorrer con vagón recta, curva y pendiente nativa/NMG; añadir hueco de 1 q, ambigüedad, propulsor, detector y activador. |
| M10 | `CircuitTimingTest.java#deferredShortPulseCompletesBothObservableRepeaterEdges`<br>`CircuitTimingTest.java#torchInvertsAfterTwoTicksAndLockedRepeaterHoldsItsOutput` | — | Enviar pulso corto a repetidor, bloquearlo lateralmente y probar antorcha/burnout; descargar y recargar chunk con eventos pendientes. |
| B01 | — | `nmg_accept run pb_quarter <anchor>` | Crear PB con altura/anchura mitad, desplazarlo y apoyar bloque sobre su cara real; verificar identidad y loot PB. |
| B02 | — | `nmg_accept run pb_quarter <anchor>` | Crear PB de altura 4 q y colocar cubo encima; comprobar contacto 4 q, techo 20 q y selección de ambas piezas. |
| B03 | — | — | Pintar una superficie de PB desplazado y una región mixta nativa/NMG; comprobar coordenadas locales y costes reales de pigmento. |
| B04 | `ConvexVolumeTest.java#slopeUsesActualVolumeInsteadOfBoundingBox` | — | Aplicar pendiente, orientación nativa PB y giro NMG; comprobar colisión, render y pintura sin doble rotación tras guardar. |
| B05 | — | — | Ampliar diseño PB hacia otra pieza; comprobar rechazo antes de pigmento/material y revisión sin cambiar. |
| B06 | — | — | Recuperar PB con herramienta del origen; comparar diseño, orientación, pinturas, cantidades y datos del ítem. |
| B07 | — | — | Seleccionar volumen con PB nativo y convertido; editar, cambiar revisión/hash antes de confirmar y comprobar commit único o rechazo completo. |
| B08 | — | — | Usar estaciones, máquina y cubo PB con menú/recetas/automatización; mover, guardar y recuperar sin duplicar inventarios/producción. |
| N01 | `PieceIndexTest.java#staleRevisionAndDuplicateIdentityAreRejected` | `nmg_accept run stale_revision <anchor>` | Dos jugadores apuntan a la misma revisión y envían ediciones incompatibles; comprobar un único ganador y respuesta obsoleta al otro. |
| N02 | `OperationEngineTest.java#duplicateIntentDoesNotConsumeTwice` | — | Reenviar exactamente la intención de colocación/rotura ya confirmada; comprobar mismo resultado sin segundo cobro o loot, incluso tras checkpoint. |
| N03 | `CellDeltaTest.java#replacementRemovalAndNewIdentityRequireTheExactBase` | — | Reordenar/perder delta y cambiar hash base; comprobar snapshot de resync, límites de ensamblado y réplica final igual al servidor. |
| N04 | `SnapshotAssemblerTest.java#budgetAndHashAreEnforced` | `nmg_protocol_test` | Enviar frames/payloads excesivos y cabeceras inconsistentes mediante fixture de protocolo; comprobar rechazo previo a asignación grande y conexión estable. |
| D01 | `SectionStoreTest.java#authoritativeSectionRoundTripAndCorruptionRetention` | — | Guardar piezas con ocho referencias y límites negativos; descargar/cargar chunks en distinto orden y verificar dueño/referencias/revisiones. |
| D02 | `DurableJournalProcessTest.java#committedOperationSurvivesForciblyKilledWriterWithoutDoubleCommit`<br>`DurableJournalTest.java#tornTailIsRetainedRatherThanOverwritten` | — | Terminar escritor antes del commit y después del ack durable en mundo desechable; reiniciar y comparar autoridad con el último commit probado. |
| D03 | `OperationEngineTest.java#postCommitFailureQuarantinesAndRecoveryUsesReceiptImage` | — | Cortar entre recibo de inventario y chunk; reconectar actor y comprobar aplicación única o cuarentena conservando originales. |
| D04 | `WorldRulesTest.java#missingAdapterKeepsItsVersionAndChangedVersionNeedsMigration` | — | Guardar PB/funcionales, quitar adaptador y cargar copia; reinstalar versión exacta y comprobar diseño/slots sin sustituto persistido como original. |
| D05 | `BoxPieceCodecTest.java#unknownVersionIsPreservedVerbatimWithoutInterpretingBody`<br>`WorldRulesTest.java#unknownSchemaIsRetained` | — | Inyectar schema futuro en copia de diario/sección/BE; comprobar rechazo/protección y archivos originales byte-idénticos. |
| D06 | `NativeCaptureStampTest.java#operationPersistsTheOriginalCellOfAnAlreadyMovedNativeCapture` | — | Restaurar chunk nativo anterior a captura/movimiento de máquina; intentar tick, hopper y menú antes de reparación; sin segunda autoridad, drops o XP. |
| D07 | — | — | Cortar después del commit del mod externo antes del propio; comprobar intent conservado y exigir prueba externa, sin replay ni devolución ciega. |
| D08 | `JournalArchiveTest.java#sealedCheckpointCanReceiveAnIncrementalTailBeforePublication`<br>`JournalArchiveTest.java#aDamagedPreparedCheckpointCannotReplaceTheManifest` | `nmg_checkpoint_test <anchor>` | Ejecutar checkpoint automático con movimientos mientras worker prepara prefijo/cola; verificar deduplicación y réplica, y cortar también publicación en copia. |
| Q01 | `OperationEngineTest.java#rejectionHasNoDurableCommitOrResourceChange` | `nmg_accept run protection <anchor>` | Cancelar BeforeOperationEvent cuando solo una celda del plan esté protegida; comprobar plan completo rechazado y recibos/journal intactos. |
| Q02 | `PieceIndexTest.java#loweringLimitsAllowsRemovalAndDoesNotEraseExistingPieces` | — | Cargar construcción bajo límite anterior y reducir máximo; permitir retirada/reducción y rechazar crecimiento sin borrar existentes. |
| Q03 | — | — | Arrancar dedicado sin librerías de cliente activadas y con/sin PB; revisar mezcla de clases, registro y carga de datos. |
| Q04 | `LargeSceneIndexTest.java#tenThousandPieceWallBuildsAndRemovesInBoundedTransactions`<br>`LargeSceneIndexTest.java#denseSectionChecksTheRealUncompressedCodecBudget`<br>`MeasurementWindowTest.java#warmupAndLaterFramesCannotAlterTheMeasuredInterval` | `nmg_bench start <scene> <anchor>` | Ejecutar escenas F9 y controles nativos equivalentes, cuatro clientes para carga, ventanas cliente/servidor completas y comparación con hardware/revisión idénticos. |

Catálogo: [catalog.json](catalog.json). Auditor estático: `python tools/check_acceptance.py`.
No hay stubs que devuelvan PASS para casos sin automatización.
