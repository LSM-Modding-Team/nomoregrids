package net.nicomar.nomoregrids.mixin;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.simulation.fluid.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Replace coarse CELL immersion with actual cavity/body/eye intersections while retaining native fluid trackers. */
@Mixin(EntityFluidInteraction.class)
public abstract class CavityEntityFluidMixin {
    @Unique private static final String NMG_UPDATE="update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)V";
    @Unique private static Method nmgTrackerFactory;
    @Unique private static boolean nmgBounded(Entity entity){var box=entity.getFluidInteractionBox();return box!=null&&(Math.ceil(box.maxX)-Math.floor(box.minX))*(Math.ceil(box.maxY)-Math.floor(box.minY))*(Math.ceil(box.maxZ)-Math.floor(box.minZ))<=512;}
    @Redirect(method=NMG_UPDATE,at=@At(value="INVOKE",target="Lnet/minecraft/world/level/BlockGetter;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState nmgFluid(BlockGetter world,BlockPos pos,Entity actor,Predicate<FluidType> push){
        if(nmgBounded(actor)&&world.getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.valid())return Fluids.EMPTY.defaultFluidState();return world.getFluidState(pos);
    }
    @Inject(method=NMG_UPDATE,at=@At("RETURN"))
    private void nmgContact(Entity actor,Predicate<FluidType> push,CallbackInfo callback){
        if(!nmgBounded(actor))return;AABB box=actor.getFluidInteractionBox();var body=new CavityContacts.Body(box.minX*16,box.minY*16,box.minZ*16,box.maxX*16,box.maxY*16,box.maxZ*16);var level=actor.level();
        for(int x=(int)Math.floor(box.minX);x<(int)Math.ceil(box.maxX);x++)for(int y=(int)Math.floor(box.minY);y<(int)Math.ceil(box.maxY);y++)for(int z=(int)Math.floor(box.minZ);z<(int)Math.ceil(box.maxZ);z++){
            var pos=new BlockPos(x,y,z);if(!level.isLoaded(pos)||!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid()||cell.view().cavity().isEmpty())continue;
            var contacts=CavityContacts.query(new CellPos(x,y,z),cell.view().cavity().orElseThrow(),body,actor.getX()*16,actor.getEyeY()*16,actor.getZ()*16);
            for(var entry:contacts.entrySet()){
                var fluid=entry.getKey()==CavityCell.Kind.WATER?Fluids.WATER.defaultFluidState():Fluids.LAVA.defaultFluidState();var type=fluid.getType().getFluidType();var tracker=nmgTracker(type);var contact=entry.getValue();double height=Math.max(0,contact.top16()/16-actor.getBoundingBox().minY);
                tracker.nmgIdle(0);tracker.nmgHeight(Math.max(tracker.nmgHeight(),height));tracker.nmgEyes(tracker.nmgEyes()||contact.eyesInside());if(push.test(type)){var current=fluid.getFlow(level,pos);if(height<0.4)current=current.scale(height);tracker.nmgCurrent(current);}
            }
        }
    }
    @Unique private FluidTrackerAccess nmgTracker(FluidType type){
        try{if(nmgTrackerFactory==null){nmgTrackerFactory=EntityFluidInteraction.class.getDeclaredMethod("getTrackerFor",FluidType.class);nmgTrackerFactory.setAccessible(true);}return (FluidTrackerAccess)nmgTrackerFactory.invoke(this,type);}catch(ReflectiveOperationException ex){throw new IllegalStateException("NMG fluid tracker API mismatch for NeoForge 26.2.0.88",ex);}
    }
}
