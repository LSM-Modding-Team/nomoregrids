package net.nicomar.nomoregrids.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;

/** Read-only query facade. Unloaded cells are closed barriers and never trigger a chunk request. */
public final class LoadedRay implements BlockGetter {
    private final Level level;
    private LoadedRay(Level level){this.level=level;}
    @Override public BlockEntity getBlockEntity(BlockPos pos){return level.isLoaded(pos)?level.getBlockEntity(pos):null;}
    @Override public BlockState getBlockState(BlockPos pos){return level.isLoaded(pos)?level.getBlockState(pos):Blocks.BARRIER.defaultBlockState();}
    @Override public FluidState getFluidState(BlockPos pos){return level.isLoaded(pos)?level.getFluidState(pos):Fluids.EMPTY.defaultFluidState();}
    @Override public int getHeight(){return level.getHeight();}
    @Override public int getMinY(){return level.getMinY();}
    public static HitResult pick(Player player){Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(player.blockInteractionRange()));return clip(player,start,end,ClipContext.Block.OUTLINE);}
    public static BlockHitResult clip(Player player,Vec3 start,Vec3 end,ClipContext.Block shape){return new LoadedRay(player.level()).clip(new ClipContext(start,end,shape,ClipContext.Fluid.NONE,player));}
}
