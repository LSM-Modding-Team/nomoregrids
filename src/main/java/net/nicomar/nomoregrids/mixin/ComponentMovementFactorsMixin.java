package net.nicomar.nomoregrids.mixin;

import net.minecraft.world.entity.Entity;
import net.nicomar.nomoregrids.platform.WorldMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ComponentMovementFactorsMixin {
    @Inject(method="getBlockSpeedFactor",at=@At("HEAD"),cancellable=true)
    private void nmg$speed(CallbackInfoReturnable<Float> ci){WorldMaterials.ground((Entity)(Object)this).ifPresent(p->ci.setReturnValue(net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getBlock().getSpeedFactor()));}
    @Inject(method="getBlockJumpFactor",at=@At("HEAD"),cancellable=true)
    private void nmg$jump(CallbackInfoReturnable<Float> ci){WorldMaterials.ground((Entity)(Object)this).ifPresent(p->ci.setReturnValue(net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getBlock().getJumpFactor()));}
}
