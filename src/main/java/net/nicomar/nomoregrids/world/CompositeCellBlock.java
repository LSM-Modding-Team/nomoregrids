package net.nicomar.nomoregrids.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Technical, non-obtainable multipart cell. Queries use replicas even with owner section unloaded. */
public final class CompositeCellBlock extends BaseEntityBlock {
    public static final MapCodec<CompositeCellBlock> CODEC=simpleCodec(CompositeCellBlock::new);
    public CompositeCellBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CompositeCellEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(false):Shapes.block();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(true):Shapes.block();
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected boolean useShapeForLightOcclusion(BlockState state){return true;}
    @Override protected VoxelShape getOcclusionShape(BlockState state){return Shapes.empty();}
    @Override protected boolean propagatesSkylightDown(BlockState state){return true;}
    @Override protected int getLightDampening(BlockState state){return 0;}
}
