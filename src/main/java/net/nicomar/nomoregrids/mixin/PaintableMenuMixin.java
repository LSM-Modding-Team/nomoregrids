package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nicomar.nomoregrids.platform.WorldFunctionals;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets={"net.nicomar.paintableblocks.menu.ProductionMenu","net.nicomar.paintableblocks.menu.PaintBucketMenu"},remap=false)
public abstract class PaintableMenuMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity nmg$entity(Level level,BlockPos pos){return WorldFunctionals.menuEntity((AbstractContainerMenu)(Object)this,level,pos);}
    @Inject(method="stillValid",at=@At("HEAD"),cancellable=true)
    private void nmg$valid(Player player,CallbackInfoReturnable<Boolean> ci){var valid=WorldFunctionals.paintableMenuValid((AbstractContainerMenu)(Object)this,player);valid.ifPresent(ci::setReturnValue);}
}
