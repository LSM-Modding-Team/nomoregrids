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
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID=net.minecraft.world.level.block.state.properties.IntegerProperty.create("fluid",0,2);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SOURCE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("source");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty EMISSION=net.minecraft.world.level.block.state.properties.IntegerProperty.create("emission",0,15);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ATTENUATION=net.minecraft.world.level.block.state.properties.IntegerProperty.create("attenuation",0,15);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SEALED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("sealed");
    public static final MapCodec<CompositeCellBlock> CODEC=simpleCodec(CompositeCellBlock::new);
    public CompositeCellBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(EMISSION,0).setValue(ATTENUATION,0).setValue(SEALED,false).setValue(FLUID,0).setValue(SOURCE,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(EMISSION,ATTENUATION,SEALED,FLUID,SOURCE);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CompositeCellEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(false):Shapes.block();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(true):Shapes.block();
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter world,BlockPos pos){return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.supportShape():Shapes.empty();}
    @Override public boolean isLadder(BlockState state,LevelReader world,BlockPos pos,net.minecraft.world.entity.LivingEntity entity){return world.getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.climbs(entity);}
    @Override public boolean collisionExtendsVertically(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.world.entity.Entity entity){return world.getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.extendsVertically(entity);}
    @Override public float getFriction(BlockState state,LevelReader world,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(world.getBlockEntity(pos) instanceof CompositeCellEntity cell)try{return cell.ground(entity).map(p->net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getBlock().getFriction()).orElse(getFriction());}catch(RuntimeException ignored){}return getFriction();
    }
    @Override public float getBounceRestitution(Level world,BlockPos pos,BlockState state,net.minecraft.world.entity.Entity entity){
        if(world.getBlockEntity(pos) instanceof CompositeCellEntity cell)try{return cell.ground(entity).map(p->net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getBlock().getBounceRestitution()).orElse(getBounceRestitution());}catch(RuntimeException ignored){}return getBounceRestitution();
    }
    @Override public SoundType getSoundType(BlockState state,LevelReader world,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(world.getBlockEntity(pos) instanceof CompositeCellEntity cell)try{return cell.ground(entity).map(p->net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getSoundType()).orElse(SoundType.STONE);}catch(RuntimeException ignored){}return SoundType.STONE;
    }
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state){
        if(state.getValue(FLUID)==0)return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
        var fluid=state.getValue(FLUID)==1?net.minecraft.world.level.material.Fluids.WATER:net.minecraft.world.level.material.Fluids.LAVA;return state.getValue(SOURCE)?fluid.getSource(false):fluid.getFlowing(1,false);
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected boolean useShapeForLightOcclusion(BlockState state){return true;}
    @Override protected VoxelShape getOcclusionShape(BlockState state){return state.getValue(SEALED)?Shapes.block():Shapes.empty();}
    @Override protected boolean propagatesSkylightDown(BlockState state){return !state.getValue(SEALED);}
    @Override protected int getLightDampening(BlockState state){return state.getValue(ATTENUATION);}
    @Override protected float getDestroyProgress(BlockState state,net.minecraft.world.entity.player.Player player,BlockGetter world,BlockPos pos){
        if(!(world.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid())return 0;
        try{return net.nicomar.nomoregrids.platform.ComponentSelection.select(player,cell.view(),0).filter(p->!p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE)).map(p->net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getDestroyProgress(player,world,pos)).orElse(0f);}catch(RuntimeException ex){return 0;}
    }
}
