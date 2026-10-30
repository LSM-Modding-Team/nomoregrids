package net.nicomar.nomoregrids.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.nicomar.nomoregrids.platform.WorldFunctionals;

/** Recipe-book autofill must not mutate a durable component through vanilla's unjournaled cursor path. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FunctionalRecipeMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handlePlaceRecipe",at=@At("HEAD"),cancellable=true)
    private void nmgRecipe(ServerboundPlaceRecipePacket packet,CallbackInfo callback){
        if(WorldFunctionals.managed(player.containerMenu)){player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Selecciona el destino con clic derecho y transfiere los ingredientes desde tu inventario."));callback.cancel();}
    }
}
