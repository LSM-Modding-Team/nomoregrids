package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.nicomar.nomoregrids.platform.WorldMutationHooks;

/** Exact 26.2 Level entry point; no injection in clients or chunk generation. */
@Mixin(Level.class)
public abstract class LevelMutationMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void nmgBefore(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> callback){
        if((Object)this instanceof ServerLevel level&&!WorldMutationHooks.before(level,pos,state))callback.setReturnValue(false);
    }
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("RETURN"))
    private void nmgAfter(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> callback){
        if(callback.getReturnValueZ()&&(Object)this instanceof ServerLevel level)WorldMutationHooks.written(level,pos);
    }
    @Inject(method="blockEntityChanged",at=@At("RETURN"))
    private void nmgInventoryChanged(BlockPos pos,org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback){if((Object)this instanceof ServerLevel level)WorldMutationHooks.changed(level,pos);}
}
