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
    @Unique private static boolean nmgBounded(Entity entity){var box=entity.getFluidInteractionBox();return box!=null;}
    @Redirect(method=NMG_UPDATE,at=@At(value="INVOKE",target="Lnet/minecraft/world/level/BlockGetter;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState nmgFluid(BlockGetter world,BlockPos pos,Entity actor,Predicate<FluidType> push){
        if(nmgBounded(actor)&&world.getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.valid())return Fluids.EMPTY.defaultFluidState();return world.getFluidState(pos);
    }
    @Inject(method=NMG_UPDATE,at=@At("RETURN"))
    private void nmgContact(Entity actor,Predicate<FluidType> push,CallbackInfo callback){
        if(!nmgBounded(actor))return;AABB box=actor.getFluidInteractionBox();var body=new CavityContacts.Body(box.minX*16,box.minY*16,box.minZ*16,box.maxX*16,box.maxY*16,box.maxZ*16);var level=actor.level();var neighbors=new java.util.HashMap<CellPos,CavityCell>();java.util.function.Function<CellPos,CavityCell> neighborhood=c->{if(neighbors.containsKey(c))return neighbors.get(c);var at=new BlockPos(Math.toIntExact(c.x()),Math.toIntExact(c.y()),Math.toIntExact(c.z()));if(!level.isLoaded(at)){neighbors.put(c,null);return null;}if(level.getBlockEntity(at) instanceof CompositeCellEntity be&&be.valid()){var result=be.view().cavity().orElse(null);neighbors.put(c,result);return result;}var state=level.getBlockState(at);var nativeFluid=state.getFluidState();var mask=new java.util.BitSet(4096);try{mask=CavityCell.blocked(net.nicomar.nomoregrids.platform.NativeStates.boxes(state.getCollisionShape(level,at)));}catch(IllegalArgumentException unknown){mask.set(0,4096);}var values=new byte[4096];var distances=new byte[4096];java.util.Arrays.fill(distances,(byte)127);CavityCell.Kind kind=nativeFluid.getType()==Fluids.WATER||nativeFluid.getType()==Fluids.FLOWING_WATER?CavityCell.Kind.WATER:nativeFluid.getType()==Fluids.LAVA||nativeFluid.getType()==Fluids.FLOWING_LAVA?CavityCell.Kind.LAVA:CavityCell.Kind.EMPTY;int height=(int)Math.ceil(nativeFluid.getHeight(level,at)*16);if(kind!=CavityCell.Kind.EMPTY)for(int i=0;i<4096;i++)if(!mask.get(i)&&i/16%16<height){values[i]=CavityCell.code(kind);distances[i]=(byte)Math.min(kind==CavityCell.Kind.WATER?112:48,Math.max(0,(8-nativeFluid.getAmount())*16));}var result=new CavityCell(mask,values,kind!=CavityCell.Kind.EMPTY&&nativeFluid.isSource()&&mask.isEmpty()&&height==16?kind:CavityCell.Kind.EMPTY,distances);neighbors.put(c,result);return result;};
        for(var candidate:net.nicomar.nomoregrids.world.LoadedCells.intersecting(level,box)){
            var pos=candidate.immutable();int x=pos.getX(),y=pos.getY(),z=pos.getZ();if(!level.isLoaded(pos)||!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid()||cell.view().cavity().isEmpty())continue;
            var contacts=CavityContacts.query(new CellPos(x,y,z),cell.view().cavity().orElseThrow(),body,actor.getX()*16,actor.getEyeY()*16,actor.getZ()*16);
            for(var entry:contacts.entrySet()){
                var fluid=entry.getKey()==CavityCell.Kind.WATER?Fluids.WATER.defaultFluidState():Fluids.LAVA.defaultFluidState();var type=fluid.getType().getFluidType();var tracker=nmgTracker(type);var contact=entry.getValue();double height=Math.max(0,contact.top16()/16-actor.getBoundingBox().minY);
                tracker.nmgIdle(0);tracker.nmgHeight(Math.max(tracker.nmgHeight(),height));tracker.nmgEyes(tracker.nmgEyes()||contact.eyesInside());if(push.test(type)){var flow=CavityCurrents.flow(new CellPos(x,y,z),cell.view().cavity().orElseThrow(),body,entry.getKey(),neighborhood);var current=new net.minecraft.world.phys.Vec3(flow.x(),flow.y(),flow.z());if(height<0.4)current=current.scale(height);tracker.nmgCurrent(current);}
            }
        }
    }
    @Unique private FluidTrackerAccess nmgTracker(FluidType type){
        try{if(nmgTrackerFactory==null){nmgTrackerFactory=EntityFluidInteraction.class.getDeclaredMethod("getTrackerFor",FluidType.class);nmgTrackerFactory.setAccessible(true);}return (FluidTrackerAccess)nmgTrackerFactory.invoke(this,type);}catch(ReflectiveOperationException ex){throw new IllegalStateException("NMG fluid tracker API mismatch for NeoForge 26.2.0.88",ex);}
    }
}
