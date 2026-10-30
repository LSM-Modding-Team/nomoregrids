package net.nicomar.nomoregrids.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.nicomar.nomoregrids.platform.WorldFunctionals;

@Mixin(ServerPlayer.class)
public abstract class FunctionalPlayerMixin {
    @Inject(method="doCloseContainer",at=@At("HEAD"))
    private void nmgClose(CallbackInfo callback){WorldFunctionals.closeCursor((ServerPlayer)(Object)this);}
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void nmgDrop(ItemStack stack,boolean random,boolean trace,CallbackInfoReturnable<ItemEntity> callback){if(WorldFunctionals.deferDrop((ServerPlayer)(Object)this,stack))callback.setReturnValue(null);}
}
