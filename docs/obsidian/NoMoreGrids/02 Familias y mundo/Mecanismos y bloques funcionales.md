# Mecanismos y bloques funcionales

## Funcionales

Se incorporan en F7 mediante adaptadores. Cada pieza mantiene un inventario, menú, energía y estado únicos. Sus referencias no son entidades de bloque duplicadas. Acceder por una cara expuesta abre el mismo objeto.

Cada adaptador declara puertos por cara local y los transforma al mundo. Compartir casilla no une inventarios ni capacidades de transferencia. Una capacidad consultada solo por casilla se ofrece únicamente cuando exista un puerto único no ambiguo; si hay varios, requiere API por componente o se rechaza esa conexión.

No se simula un mundo vanilla completo para todas las entidades. Para una máquina compleja sin adaptador, desplazamiento/composición quedan deshabilitados.

Puertas/camas: grupo con UUID y miembros. Colocación/retirada se valida como una operación completa. El pivotado de trampillas/puertas comprueba volumen barrido y estado final; no atraviesa componentes ni entidades relevantes.

## Redstone

F8 usa puertos de entrada/salida reales y perfiles de familia. Valores 0–15 y temporización lógica vanilla. No hay conducción por compartir casilla. Polvo requiere apoyo plano; no se extiende automáticamente por pendientes arbitrarias.

El polvo conectado consume distancia por cada tramo de hasta un bloque siguiendo su recorrido; un contacto directo no añade una pérdida extra. Repetidores/comparadores conservan sus retardos por componente. Se integran consultas vanilla por casilla mediante agregación máxima solo para puertos externos compatibles; conexiones ambiguas no se habilitan.

Un cambio produce actualizaciones deduplicadas con presupuesto. Los circuitos no se evalúan en hilos de render ni con orden de llegada de red.

## Pistones

Movimiento de un bloque por extensión/retracción. Colisión de la cara móvil selecciona las piezas afectadas; cada una se mueve entera. Se construye un cierre de empuje incluyendo piezas que chocan en el destino y grupos inseparables.

Límite inicial: 12 componentes lógicos, cada miembro material de un grupo cuenta. También se aplica el presupuesto geométrico. No se aplica «12 por casilla». Un componente incompatible, inmovible, fuera de carga o que exceda límites bloquea toda la operación.

Slime/miel siguen perfiles de adhesión de superficies en contacto; compartir casilla no pega. Se verifica volumen barrido además de destino. Un pistón pegajoso tira solo de componentes que toca y que pueden adherirse.

## Raíles

Solo rectas, curvas y pendientes nativas que tengan una adaptación de apoyo/recorrido. Desplazamiento de 1/16 no garantiza continuidad: los puntos extremos y tangentes del raíl deben coincidir para que pase un vagón. Antes de esta adaptación los raíles permanecen nativos.
