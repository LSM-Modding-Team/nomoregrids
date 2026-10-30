package net.nicomar.nomoregrids;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entrada común del esqueleto. No registra bloques ni implementa mecánicas.
 */
@Mod(NoMoreGrids.MOD_ID)
public final class NoMoreGrids {
    public static final String MOD_ID = "nomoregrids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoMoreGrids(IEventBus modEventBus) {
        net.nicomar.nomoregrids.registry.NmgContent.register(modEventBus);
        LOGGER.info("No More Grids: contenedor experimental registrado; implementación pendiente de validación.");
    }
}
