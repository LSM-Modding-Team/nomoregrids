package net.nicomar.nomoregrids.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.nicomar.nomoregrids.platform.WorldFunctionals;

/** Native screens/slots, server-side durable transfers with no unpersisted cursor ownership. */
@Mixin(AbstractContainerMenu.class)
public abstract class FunctionalMenuMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void nmgTransfer(int slot,int button,ContainerInput input,Player actor,CallbackInfo callback){if(WorldFunctionals.click((AbstractContainerMenu)(Object)this,slot,button,input,actor))callback.cancel();}
}
