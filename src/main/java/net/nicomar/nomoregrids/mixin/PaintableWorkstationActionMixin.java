package net.nicomar.nomoregrids.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.nicomar.nomoregrids.platform.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.nicomar.paintableblocks.menu.WorkstationMenu",remap=false)
public abstract class PaintableWorkstationActionMixin {
    @Inject(method="action",at=@At("HEAD"),cancellable=true)
    private void nmg$action(Player player,int action,int variant,int resolution,int material,CallbackInfoReturnable<Boolean> ci){var menu=(AbstractContainerMenu)(Object)this;if(player instanceof ServerPlayer server&&WorldFunctionals.stage(menu,server,()->PaintableFunctionals.action(menu,player,action,variant,resolution,material)))ci.setReturnValue(true);}
}
