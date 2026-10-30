package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Exact state capture/restoration. Non-integral vanilla shapes require a dedicated adapter. */
public final class NativeStates {
    private NativeStates(){}
    public static BlockState resolve(PieceRecord piece){return resolve(piece.sourceBlockId(),piece.sourceState());}
    public static BlockState resolve(ResourceId id,Map<String,String> values){
        BlockState state=BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id.value())).orElseThrow(()->new IllegalArgumentException("Missing block: "+id)).defaultBlockState();
        for(var entry:values.entrySet()){
            Property<?> property=state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if(property==null)throw new IllegalArgumentException("Missing state property");state=set(state,property,entry.getValue());
        }return state;
    }
    private static <T extends Comparable<T>> BlockState set(BlockState state,Property<T> property,String value){return state.setValue(property,property.getValue(value).orElseThrow());}
    public static Map<String,String> properties(BlockState state){var result=new TreeMap<String,String>();for(var property:state.getProperties())put(result,property,state.getValue(property));return Map.copyOf(result);}
    @SuppressWarnings({"unchecked","rawtypes"}) private static void put(Map<String,String> target,Property property,Comparable value){target.put(property.getName(),property.getName(value));}
    public static BoxUnion boxes(VoxelShape shape){
        var boxes=new ArrayList<Box16>();for(var box:shape.toAabbs())boxes.add(Box16.of(q(box.minX),q(box.minY),q(box.minZ),q(box.maxX),q(box.maxY),q(box.maxZ)));return new BoxUnion(boxes);
    }
    private static long q(double value){double scaled=value*16;long q=Math.round(scaled);if(!Double.isFinite(scaled)||Math.abs(q-scaled)>1e-8)throw new IllegalArgumentException("Non-integral shape requires an adapter");return q;}
    public static PieceRecord capture(BlockState state,BlockGetter world,BlockPos pos,UUID id){
        if(state.hasBlockEntity()||state.isRandomlyTicking()||!state.getFluidState().isEmpty())throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
        BoxUnion selection=boxes(state.getShape(world,pos)),collision=boxes(state.getCollisionShape(world,pos));
        if(selection.boxes().isEmpty())throw new IllegalArgumentException("No solid occupation");
        return new PieceRecord(id,0,new ResourceId("nomoregrids:box"),1,new ResourceId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),properties(state),
            new GridPoint(pos.getX()*16L,pos.getY()*16L,pos.getZ()*16L),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(selection,collision,selection),
            BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());
    }
}
