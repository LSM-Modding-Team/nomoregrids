package net.nicomar.nomoregrids.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.nicomar.nomoregrids.platform.WorldFunctionals;

@Mixin(Player.class)
public abstract class FunctionalDropMixin {
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void nmgDrop(ItemStack stack,boolean random,CallbackInfoReturnable<ItemEntity> callback){if((Object)this instanceof ServerPlayer player&&WorldFunctionals.deferDrop(player,stack))callback.setReturnValue(null);}
}
