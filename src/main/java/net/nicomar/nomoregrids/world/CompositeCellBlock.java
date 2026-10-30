package net.nicomar.nomoregrids.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Technical, non-obtainable multipart cell. Queries use replicas even with owner section unloaded. */
public final class CompositeCellBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID=net.minecraft.world.level.block.state.properties.IntegerProperty.create("fluid",0,2);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SOURCE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("source");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty EMISSION=net.minecraft.world.level.block.state.properties.IntegerProperty.create("emission",0,15);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ATTENUATION=net.minecraft.world.level.block.state.properties.IntegerProperty.create("attenuation",0,15);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SEALED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("sealed");
    public static final MapCodec<CompositeCellBlock> CODEC=simpleCodec(CompositeCellBlock::new);
    public CompositeCellBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(EMISSION,0).setValue(ATTENUATION,0).setValue(SEALED,false).setValue(FLUID,0).setValue(SOURCE,false).setValue(FACING,net.minecraft.core.Direction.NORTH));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,EMISSION,ATTENUATION,SEALED,FLUID,SOURCE);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CompositeCellEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(false):Shapes.block();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
        return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.shape(true):Shapes.block();
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter world,BlockPos pos){return world.getBlockEntity(pos) instanceof CompositeCellEntity cell?cell.supportShape():Shapes.empty();}
    @Override protected boolean isSignalSource(BlockState state){return true;}
    @Override protected int getSignal(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.core.Direction direction){return net.nicomar.nomoregrids.platform.WorldCircuits.output(world,pos,direction);}
    @Override protected int getDirectSignal(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.core.Direction direction){return net.nicomar.nomoregrids.platform.WorldCircuits.output(world,pos,direction);}
    @Override protected int ownSignal(BlockState state,BlockGetter world,BlockPos pos){int value=0;for(var d:net.minecraft.core.Direction.values())value=Math.max(value,net.nicomar.nomoregrids.platform.WorldCircuits.output(world,pos,d));return value;}
    @Override public boolean isBed(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.world.entity.LivingEntity sleeper){return net.nicomar.nomoregrids.platform.WorldGroups.bedAt(world,pos).isPresent();}
    @Override public net.minecraft.core.Direction getBedDirection(BlockState state,LevelReader world,BlockPos pos){return net.nicomar.nomoregrids.platform.WorldGroups.bedAt(world,pos).map(net.nicomar.nomoregrids.platform.WorldGroups::bedDirection).orElse(net.minecraft.core.Direction.NORTH);}
    @Override public void setBedOccupied(BlockState state,Level world,BlockPos pos,net.minecraft.world.entity.LivingEntity sleeper,boolean occupied){net.nicomar.nomoregrids.platform.WorldGroups.occupied(world,pos,occupied);}
    @Override public java.util.Optional<net.minecraft.server.level.ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state,net.minecraft.world.entity.EntityType<?> type,LevelReader world,BlockPos pos,float orientation){if(!(world instanceof CollisionGetter collisions))return java.util.Optional.empty();return net.nicomar.nomoregrids.platform.WorldGroups.bedAt(world,pos).flatMap(p->BedBlock.findStandUpPosition(type,collisions,pos,net.nicomar.nomoregrids.platform.WorldGroups.bedDirection(p),orientation)).map(point->net.minecraft.server.level.ServerPlayer.RespawnPosAngle.of(point,pos,orientation));}
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
    @Override public boolean canPlaceLiquid(net.minecraft.world.entity.LivingEntity actor,BlockGetter world,BlockPos pos,BlockState state,net.minecraft.world.level.material.Fluid fluid){
        return actor==null&&(fluid==net.minecraft.world.level.material.Fluids.WATER||fluid==net.minecraft.world.level.material.Fluids.FLOWING_WATER||fluid==net.minecraft.world.level.material.Fluids.LAVA||fluid==net.minecraft.world.level.material.Fluids.FLOWING_LAVA)&&world.getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.valid()&&!cell.getBlockState().getValue(SEALED);
    }
    @Override public boolean placeLiquid(LevelAccessor world,BlockPos pos,BlockState state,net.minecraft.world.level.material.FluidState fluid){
        if(!(world instanceof net.minecraft.server.level.ServerLevel level))return true;var service=net.nicomar.nomoregrids.platform.NmgRuntime.existing(level);if(service==null||service.quarantined())return false;try{service.fluids().incoming(new net.nicomar.nomoregrids.core.CellPos(pos.getX(),pos.getY(),pos.getZ()),fluid);return true;}catch(java.io.IOException|RuntimeException ex){if(service.quarantined())net.nicomar.nomoregrids.platform.NmgRuntime.failed(level,ex);return false;}
    }
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state){
        if(state.getValue(FLUID)==0)return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
        var fluid=state.getValue(FLUID)==1?net.minecraft.world.level.material.Fluids.WATER:net.minecraft.world.level.material.Fluids.LAVA;return state.getValue(SOURCE)?fluid.getSource(false):fluid.getFlowing(1,false);
    }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face){return level.getBlockEntity(pos) instanceof CompositeCellEntity cell?net.nicomar.nomoregrids.platform.WorldMaterials.fire(cell,level,pos,face,false):0;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face){return level.getBlockEntity(pos) instanceof CompositeCellEntity cell?net.nicomar.nomoregrids.platform.WorldMaterials.fire(cell,level,pos,face,true):0;}
    @Override public boolean onCaughtFire(BlockState state,Level level,BlockPos pos,net.minecraft.core.Direction face,net.minecraft.world.entity.LivingEntity igniter){return level instanceof net.minecraft.server.level.ServerLevel server&&net.nicomar.nomoregrids.platform.WorldMaterials.burn(server,pos,face,igniter);}
    @Override public float getExplosionResistance(BlockState state,BlockGetter level,BlockPos pos,Explosion explosion){
        if(!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid())return 3600000;float min=3600000;
        for(var p:cell.view().replicas())try{if(!p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE))min=Math.min(min,net.nicomar.nomoregrids.platform.NativeStates.resolve(p).getExplosionResistance(level,pos,explosion));}catch(RuntimeException ignored){}return min;
    }
    @Override protected void onExplosionHit(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,Explosion explosion,java.util.function.BiConsumer<net.minecraft.world.item.ItemStack,BlockPos> drops){net.nicomar.nomoregrids.platform.WorldMaterials.explode(level,pos,explosion);}
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.Entity entity){
        if(level.getBlockEntity(pos) instanceof CompositeCellEntity cell)cell.ground(entity).ifPresent(p->{var nativeState=net.nicomar.nomoregrids.platform.NativeStates.resolve(p);nativeState.getBlock().stepOn(level,pos,nativeState,entity);});
    }
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,net.minecraft.world.entity.Entity entity,double distance){
        if(level.getBlockEntity(pos) instanceof CompositeCellEntity cell){var ground=cell.ground(entity);if(ground.isPresent()){var nativeState=net.nicomar.nomoregrids.platform.NativeStates.resolve(ground.orElseThrow());nativeState.getBlock().fallOn(level,nativeState,pos,entity,distance);return;}}super.fallOn(level,state,pos,entity,distance);
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
