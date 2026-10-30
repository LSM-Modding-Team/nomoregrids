package net.nicomar.nomoregrids;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Registra el contenedor, los fragmentos y el host transaccional experimental.
 */
@Mod(NoMoreGrids.MOD_ID)
public final class NoMoreGrids {
    public static final String MOD_ID = "nomoregrids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoMoreGrids(IEventBus modEventBus) {
        net.nicomar.nomoregrids.registry.NmgContent.register(modEventBus);
        net.nicomar.nomoregrids.network.NmgNetworking.register(modEventBus);
        net.nicomar.nomoregrids.platform.NmgRuntime.register();
        LOGGER.info("No More Grids: contenedor experimental registrado; implementación pendiente de validación.");
    }
}
