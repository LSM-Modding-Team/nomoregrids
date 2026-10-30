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
        modEventBus.addListener(NoMoreGridsClient::models);
    }
    private static void models(net.neoforged.neoforge.client.event.RegisterBlockStateModels event){
        event.registerModel(net.minecraft.resources.Identifier.fromNamespaceAndPath("nomoregrids","cell"),net.nicomar.nomoregrids.client.render.CellModel.Unbaked.CODEC);
    }
}
