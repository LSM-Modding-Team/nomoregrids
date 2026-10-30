package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.nicomar.nomoregrids.platform.NmgRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Skip native inventory/XP drops only for the exact image already owned by a durable NMG capture. */
@Mixin(LevelChunk.class)
public abstract class NativeInventoryRemovalMixin {
    @Redirect(method="setBlockState",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void nmg$transfer(BlockEntity entity,BlockPos pos,BlockState state){if(entity.getLevel() instanceof ServerLevel level){var service=NmgRuntime.existing(level);if(service==null)service=net.nicomar.nomoregrids.platform.WorldService.writingService(level);if(service!=null&&service.writing()&&service.capturedNative(entity))return;}entity.preRemoveSideEffects(pos,state);}
}
