package net.nicomar.nomoregrids.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

/** Access only to the inspected 26.2.0.88 tracker fields and current accumulator. */
@Mixin(targets="net.minecraft.world.entity.EntityFluidInteraction$Tracker")
public interface FluidTrackerAccess {
    @Accessor("height") double nmgHeight();
    @Accessor("height") void nmgHeight(double value);
    @Accessor("eyesInside") boolean nmgEyes();
    @Accessor("eyesInside") void nmgEyes(boolean value);
    @Accessor("idleTicks") void nmgIdle(int value);
    @Invoker("accumulateCurrent") void nmgCurrent(Vec3 current);
}
