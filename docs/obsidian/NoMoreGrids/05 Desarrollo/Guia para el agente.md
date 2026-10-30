# Guía para el agente

## Mandato actual

El usuario autorizó implementar F0–F9, dejando pendientes compilación y ejecución. Se avanza código posterior sin confundir implementación parcial con aceptación. No se heredan restricciones de commits/ejecución del repositorio de referencia. Consultar también AGENTS.md en la raíz del proyecto.

## Antes de implementar

Leer [[Inicio]], [[Reglas fundamentales]], [[Decisiones tecnicas]], [[Plan de fases]], [[Arquitectura]] y [[Matriz de aceptacion]]. Trabajar en la fase vigente y mantener el estado honesto. El usuario acepta propuestas por defecto salvo rechazo explícito; resolver decisiones rutinarias sin preguntas innecesarias y comunicarlas al entregar.

No inventar APIs de NeoForge a partir de los nombres conceptuales de esta bóveda. Consultar fuentes exactas de la versión fijada. Guías generales 26.1 ayudan a entender conceptos pero no acreditan firmas 26.2.

## Implementación futura

- Mantener núcleo geométrico puro.
- Separar cliente y servidor.
- Capturar datos antes de convertir bloques.
- Validar cambios antes de consumir.
- Conservar UUID/revisiones y no duplicar datos de inventario.
- No habilitar capacidades de un adaptador antes de sus pruebas.
- No degradar silenciosamente geometría ni borrar datos por límites.
- Actualizar casos de aceptación cuando cambie una regla.
- Usar UTF-8 y nombres únicos para notas; mantener wikilinks.
- No afirmar rendimiento/compatibilidad sin evidencia.

## Entrega por fase

Describir comportamiento final, archivos/módulos cambiados, pruebas ejecutadas y resultados, limitaciones y decisiones nuevas. Registrar fuente de cualquier cambio de versión. Cuando no se haya compilado o ejecutado, indicarlo.

## División documental

Producto explica experiencia; familias explican reglas; diseño técnico describe representación; compatibilidad define puentes; desarrollo define orden y evidencia. Evitar copiar una misma regla con versiones diferentes en muchas notas: enlazar la nota autoritativa.

Esta guía es documentación de proyecto, no una skill instalable ni un conjunto de instrucciones con autoridad sobre solicitudes explícitas posteriores del usuario.
