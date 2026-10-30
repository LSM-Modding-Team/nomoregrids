package net.nicomar.nomoregrids.mixin;

import net.minecraft.world.entity.vehicle.minecart.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecartBehavior.class)
public interface MinecartBehaviorAccess {@Accessor("minecart") AbstractMinecart nmg$minecart();}
