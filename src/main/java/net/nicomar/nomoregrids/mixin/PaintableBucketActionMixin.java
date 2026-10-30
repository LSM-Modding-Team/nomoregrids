package net.nicomar.nomoregrids.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.nicomar.nomoregrids.platform.WorldFunctionals;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.nicomar.paintableblocks.menu.PaintBucketMenu",remap=false)
public abstract class PaintableBucketActionMixin {
    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void nmg$button(Player player,int button,CallbackInfoReturnable<Boolean> ci){var menu=(AbstractContainerMenu)(Object)this;if(player instanceof ServerPlayer server&&WorldFunctionals.stage(menu,server,()->menu.clickMenuButton(player,button)))ci.setReturnValue(true);}
}
