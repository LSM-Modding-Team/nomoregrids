package net.nicomar.nomoregrids.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.nicomar.nomoregrids.NoMoreGrids;

/**
 * Entrada exclusiva del cliente para controles, preview y modelos de sección.
 */
@Mod(value = NoMoreGrids.MOD_ID, dist = Dist.CLIENT)
public final class NoMoreGridsClient {
    public NoMoreGridsClient(IEventBus modEventBus) {
        modEventBus.addListener(NoMoreGridsClient::models);
        modEventBus.addListener(NoMoreGridsClient::renderers);
        net.nicomar.nomoregrids.client.input.ConstructionControls.register(modEventBus);
        ClientMeasurements.register();
        ClientIntentLoad.register();
    }
    private static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(net.nicomar.nomoregrids.registry.NmgContent.CELL_ENTITY.get(),context->new net.nicomar.nomoregrids.client.render.ComponentEntityRenderer());}
    private static void models(net.neoforged.neoforge.client.event.RegisterBlockStateModels event){
        event.registerModel(net.minecraft.resources.Identifier.fromNamespaceAndPath("nomoregrids","cell"),net.nicomar.nomoregrids.client.render.CellModel.Unbaked.CODEC);
    }
}
