package net.nicomar.nomoregrids.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.simulation.fluid.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Extinguishing/ignition effects use actual touched microvolumes, including mixed water/lava cells. */
@Mixin(FluidState.class)
public abstract class CavityFluidEffectMixin {
    @Unique private static final ThreadLocal<Boolean> NMG_DISPATCH=ThreadLocal.withInitial(()->false);
    @Inject(method="entityInside",at=@At("HEAD"),cancellable=true)
    private void nmgEffects(Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier applier,CallbackInfo callback){
        if(NMG_DISPATCH.get()||!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid()||cell.view().cavity().isEmpty())return;
        callback.cancel();var box=entity.getBoundingBox();var body=new CavityContacts.Body(box.minX*16,box.minY*16,box.minZ*16,box.maxX*16,box.maxY*16,box.maxZ*16);
        var contacts=CavityContacts.query(new CellPos(pos.getX(),pos.getY(),pos.getZ()),cell.view().cavity().orElseThrow(),body,entity.getX()*16,entity.getEyeY()*16,entity.getZ()*16);
        NMG_DISPATCH.set(true);try{for(var kind:contacts.keySet())(kind==CavityCell.Kind.WATER?Fluids.WATER:Fluids.LAVA).defaultFluidState().entityInside(level,pos,entity,applier);}finally{NMG_DISPATCH.remove();}
    }
}
