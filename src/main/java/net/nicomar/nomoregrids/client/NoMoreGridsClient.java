package net.nicomar.nomoregrids.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.nicomar.nomoregrids.NoMoreGrids;

/**
 * Entrada exclusiva del cliente. Reserva la conexión de controles y render.
 */
@Mod(value = NoMoreGrids.MOD_ID, dist = Dist.CLIENT)
public final class NoMoreGridsClient {
    public NoMoreGridsClient(IEventBus modEventBus) {
        // Los listeners se añadirán cuando se implemente la fase correspondiente.
    }
}
