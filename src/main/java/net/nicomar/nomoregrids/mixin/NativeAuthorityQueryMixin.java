package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicomar.nomoregrids.platform.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Repair an older native machine before a player or hopper can acquire its stale inventory. */
@Mixin(Level.class)
public abstract class NativeAuthorityQueryMixin {
    @Inject(method="getBlockEntity",at=@At("RETURN"),cancellable=true)
    private void nmg$owned(BlockPos pos,CallbackInfoReturnable<BlockEntity> ci){if(!((Object)this instanceof ServerLevel level)||WorldFunctionals.planningCopy())return;var service=NmgRuntime.existing(level);var entity=ci.getReturnValue();if(service==null||service.writing()||service.operationBusy()||entity==null||entity instanceof net.nicomar.nomoregrids.world.CompositeCellEntity||!service.ownsNativeTicker(entity))return;if(service.quarantined()){ci.setReturnValue(null);return;}try{service.repairLoadedCells(java.util.List.of(pos));var repaired=level.getChunkAt(pos).getBlockEntity(pos);ci.setReturnValue(repaired==entity?null:repaired);}catch(java.io.IOException|RuntimeException ex){NmgRuntime.failed(level,ex);ci.setReturnValue(null);}}
}
