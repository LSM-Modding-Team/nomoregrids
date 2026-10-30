# Conexiones y apoyo

## Conexiones

Cada familia expone puertos geométricos: posición, normal, sección y tipo. Un enlace requiere coincidencia espacial compatible, no solo vecindad de casilla. El contacto de puertos usa tolerancia numérica, sin puentes de 1/16 inventados.

Paneles/barrotes usan brazos hacia planos compatibles; vallas/muros adaptan postes y travesaños. Un desplazamiento vertical incompatible no conecta. Escaleras resuelven esquina interior/exterior en su marco local cuando las caras y alturas pertinentes coinciden.

El algoritmo propone conexiones por orden estable de posición y UUID, comprueba invasiones y conserva solo las válidas. Se itera sobre el vecindario hasta estabilidad con máximo 16 pasadas; si no converge se rechaza la operación sin cambios. No se alternan estados indefinidamente cada tick.

Bloquear conexiones congela las conexiones actuales como preferencias explícitas. Si desaparece el vecino, el brazo puede mantenerse decorativamente solo cuando la familia permita un extremo sin vecino; de lo contrario desaparece. Bloqueo nunca autoriza solapamiento.

## Apoyo

| Familia | Contrato inicial |
|---|---|
| Cubos/losas/escaleras | Sin requisito estructural general |
| Antorcha/elemento anclado | Parche continuo bajo su base de anclaje |
| Farol colgante | Anclaje superior compatible |
| Alfombra | Cobertura completa del área inferior de la pieza |
| Raíl/redstone sobre suelo | Base plana continua y familia adaptada |
| Escalera de mano | Apoyo en sus anclajes posteriores |

Contacto puntual, por arista o con un hueco no satisface base completa. Un redondeo hacia fuera puede dejar un objeto sin apoyo; se rechaza si lo requiere. El material debe admitir soporte; no basta con coincidir visualmente.

Los soportes se expresan como parches/áreas, no como «casilla sólida». Se utiliza una tolerancia máxima geométrica de 1e-7 bloque para contacto numérico, nunca para salvar huecos reales.

## Cambios

Retirar o editar un soporte recalcula dependientes. Si pierden apoyo, se retiran según su regla de loot y se emite un único resultado por pieza. No hay destrucción en cascada de cubos que flotan normalmente.

No se admiten ciclos de objetos dependientes como único soporte: el grafo debe llegar a una pieza estable o bloque nativo válido. Los ciclos se rechazan en colocación.

Ver [[Transacciones y eventos]].
