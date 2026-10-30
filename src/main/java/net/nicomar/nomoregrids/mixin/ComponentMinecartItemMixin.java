package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nicomar.nomoregrids.platform.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Scoped to native cart placement; does not substitute Level or install a temporary rail. */
@Mixin(MinecartItem.class)
public abstract class ComponentMinecartItemMixin {
    @Redirect(method="useOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState nmg$rail(Level level,BlockPos pos,UseOnContext context){return WorldRails.selected(context).map(NativeStates::resolve).orElseGet(()->level.getBlockState(pos));}
    @Redirect(method="useOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;createMinecart(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;"))
    private AbstractMinecart nmg$place(Level level,double x,double y,double z,EntityType<? extends AbstractMinecart> type,EntitySpawnReason reason,ItemStack stack,Player player,UseOnContext context){var selected=WorldRails.selected(context);if(selected.isPresent()){var p=selected.orElseThrow();var path=WorldRails.path(p);var click=context.getClickLocation();var sample=path.sample(path.nearest(new net.nicomar.nomoregrids.geometry.ConvexVolume.Vec(click.x*16,click.y*16,click.z*16)));x=sample.position().x()/16;y=sample.position().y()/16;z=sample.position().z()/16;if(player==null||player.isSpectator()||!player.getAbilities().mayBuild||!level.mayInteract(player,context.getClickedPos()))return null;}var cart=AbstractMinecart.createMinecart(level,x,y,z,type,reason,stack,player);if(cart!=null&&selected.isPresent()&&!level.noCollision(cart,cart.getBoundingBox()))return null;return cart;}
}
