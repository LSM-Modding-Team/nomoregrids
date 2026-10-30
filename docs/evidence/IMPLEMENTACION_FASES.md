# Implementación autorizada F0–F9 (pendiente de compilación)

La solicitud posterior autoriza escribir todas las fases sin esperar a ejecutar F0.
Las puertas de aceptación se conservan para validación y publicación, no como
prohibición de desarrollar código posterior. No se ejecutan Gradle, compiladores
ni Minecraft. No se declara certificada ninguna capacidad.

## Operaciones, diario y red

Añadidos planes inmutables con imágenes antes/después y recibos de recursos;
codec del diario limitado a 16 MiB por operación; log append-only con CRC32C,
lock de archivo, intención y commit con force(true), detección de truncamientos
y deduplicación por UUID. Los logs truncados se conservan y se bloquea su escritura
para reparación; no se adivina si una cola corrupta contenía un commit durable.

El coordinador valida antes del commit y conserva/quarantena fallos posteriores.
La recuperación exige checkpoints/recibos del host y compara estados; no cobra
recursos a ciegas. El host concreto debe verificar todas las casillas, permisos,
alcance, soporte y entidades; no hay un validador que acepte todo por defecto.

Red: ensamblador por conexión con fragmentos de 64 KiB, 2 MiB totales, cuatro
ensamblados simultáneos, caducidad de diez segundos, hash y duplicados coherentes.
Rate limit de intenciones: 20/s con ráfaga 40.

Pruebas escritas de reapertura/deduplicación del diario, cola truncada, fragmentos
fuera de orden y límites. No ejecutadas. Revisión estructural y parser Java.

Fuentes de plataforma: fuentes NeoForge 26.2.0.88 y jars oficiales Minecraft 26.2
consultados mediante javap (lectura de firmas, sin ejecutar clases del juego ni
compilar). Se usan Identifier, ValueInput/ValueOutput y las firmas de 26.2.
