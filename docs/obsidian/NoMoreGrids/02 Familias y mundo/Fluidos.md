# Fluidos

Por decisión del usuario, las celdas compuestas no almacenan agua, lava ni otros
fluidos. No admiten colocación mediante cubos ni entrada por flujo. Las
operaciones NMG que ocuparían una casilla con fluido o un bloque waterlogged se
rechazan antes de consumir o mutar; el fluido nativo fuera de las celdas sigue
su comportamiento normal.

Se han retirado las cavidades, su simulación, render, sincronización, efectos
sobre entidades y puente de transición. No se requiere migración ni compatibilidad
con mundos de versiones anteriores. La validación en cliente y servidor está
pendiente; compilar no acredita pruebas en Minecraft.
