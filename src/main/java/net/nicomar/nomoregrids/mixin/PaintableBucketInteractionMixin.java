package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicomar.nomoregrids.platform.WorldFunctionals;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="net.nicomar.paintableblocks.interaction.ServerInteractions",remap=false)
public abstract class PaintableBucketInteractionMixin {
    @Redirect(method="bucket(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/InteractionHand;Z)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity nmg$bucket(Level level,BlockPos pos){return WorldFunctionals.scopedEntity(level,pos);}
}
