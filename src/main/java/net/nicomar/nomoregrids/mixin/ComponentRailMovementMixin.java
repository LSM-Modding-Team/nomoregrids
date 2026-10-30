package net.nicomar.nomoregrids.mixin;

import net.minecraft.world.entity.vehicle.minecart.*;
import net.nicomar.nomoregrids.platform.WorldRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NewMinecartBehavior.class,OldMinecartBehavior.class})
public abstract class ComponentRailMovementMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void nmg$track(CallbackInfo ci){if(WorldRails.tick(((MinecartBehaviorAccess)(Object)this).nmg$minecart()))ci.cancel();}
}
