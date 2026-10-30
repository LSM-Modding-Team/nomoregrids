package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.nicomar.nomoregrids.platform.NmgRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** An older native chunk image must not run a second machine while its NMG projection is repaired. */
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class CapturedNativeTickerMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void nmg$authority(BlockEntityTicker<BlockEntity> ticker,Level level,BlockPos pos,BlockState state,BlockEntity entity){if(level instanceof ServerLevel server){var service=NmgRuntime.existing(server);if(service!=null&&service.ownsNativeTicker(entity))return;}ticker.tick(level,pos,state,entity);}
}
