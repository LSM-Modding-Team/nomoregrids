package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.operation.PlacementSolver;
import net.nicomar.nomoregrids.network.ModePayload;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;

/** Shared client prediction/server planning. Never changes the world or inventory. */
public final class ConstructionCandidate {
    private ConstructionCandidate(){}
    public static PieceRecord build(Player player,InteractionHand hand,BlockHitResult hit,ModePayload mode,VanillaCatalog catalog,UUID id){
        var stack=player.getItemInHand(hand);var paintable=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().filter(b->b.placementItem(stack)).orElse(null);net.minecraft.world.level.block.Block block;
        if(stack.getItem() instanceof net.nicomar.nomoregrids.material.FragmentItem)block=net.nicomar.nomoregrids.material.FragmentItem.read(stack).block();
        else if(stack.getItem() instanceof BlockItem item){block=item.getBlock();if(paintable==null&&!net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack,item.getDefaultInstance()))throw new IllegalArgumentException("Item metadata requires a dedicated adapter");}
        else throw new IllegalArgumentException("Not a construction item");
        if(paintable==null&&!WorldFunctionals.eligible(block.defaultBlockState())&&!catalog.identifiers().contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString()))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
        var context=new BlockPlaceContext(player,hand,stack,hit);
        if(!player.level().isLoaded(context.getClickedPos()))throw new IllegalArgumentException("UNLOADED_REGION");
        for(var side:net.minecraft.core.Direction.values())if(!player.level().isLoaded(context.getClickedPos().relative(side)))throw new IllegalArgumentException("UNLOADED_REGION");
        var state=block.getStateForPlacement(context);
        if(state==null||paintable==null&&!WorldFunctionals.eligible(state)&&!catalog.eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
        if(mode.aligned()){
            var values=new HashMap<>(NativeStates.properties(state));mode.alignmentProperties().forEach((key,value)->{if(values.containsKey(key))values.put(key,value);});
            state=NativeStates.resolve(new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),values);
        }
        Direction6 normal=Direction6.valueOf(hit.getDirection().name());
        int orientation=0;
        boolean slab=state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock;
        boolean stair=state.getBlock() instanceof net.minecraft.world.level.block.StairBlock;
        if(slab&&"double".equals(NativeStates.properties(state).get("type"))){var values=new HashMap<>(NativeStates.properties(state));values.put("type","bottom".equals(NativeStates.properties(player.level().getBlockState(hit.getBlockPos())).get("type"))?"top":"bottom");state=NativeStates.resolve(new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),values);}
        if(mode.lateral()&&(slab||stair)){
            var values=new HashMap<>(NativeStates.properties(state));
            if(slab)values.put("type","bottom");
            if(stair){values.put("facing","north");values.put("half","bottom");values.put("shape","straight");}
            state=NativeStates.resolve(new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),values);
        }
        if(mode.expanded()&&!slab&&!stair&&NativeStates.properties(state).containsKey("axis")){var values=new HashMap<>(NativeStates.properties(state));values.put("axis","z");state=NativeStates.resolve(new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),values);}
        var piece=WorldFunctionals.eligible(state)?WorldFunctionals.placement(state,player.level(),context.getClickedPos(),id):paintable==null?NativeStates.capture(state,player.level(),context.getClickedPos(),id):paintable.placement(context,id);
        if(paintable!=null&&mode.aligned()&&mode.alignmentProperties().containsKey("pb_orientation"))piece=paintable.oriented(piece,Integer.parseInt(mode.alignmentProperties().get("pb_orientation")));
        if(mode.lateral()&&normal.axis()!=1&&(slab||stair)){
            if(stair){var look=player.getLookAngle();orientation=PlacementSolver.stairOrientation(normal,look.x,look.y,look.z,0);}
            else for(var rotation:Rotation.all())if(Direction6.DOWN.rotated(rotation.id())==normal.opposite()){orientation=rotation.id();break;}
        }else if(mode.expanded()&&!slab&&!stair){var look=player.getLookAngle();orientation=PlacementSolver.sixFronts(look.x,look.y,look.z);}
        double hx=hit.getLocation().x,hy=hit.getLocation().y,hz=hit.getLocation().z;
        if(mode.aligned()){
            for(var d:Direction6.values())if(d.axis()==mode.alignmentAxis()&&d.sign()==mode.alignmentSign())normal=d;
            orientation=mode.alignmentRotation();if(normal.axis()==0)hx=mode.alignmentPlane()/16.0;if(normal.axis()==1)hy=mode.alignmentPlane()/16.0;if(normal.axis()==2)hz=mode.alignmentPlane()/16.0;
        }
        boolean combineSlab=slab&&player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof net.minecraft.world.level.block.SlabBlock;
        GridPoint origin=mode.precise()||mode.aligned()||combineSlab?PlacementSolver.origin(piece.geometry().occupation(),piece.pivot16(),orientation,normal,hx,hy,hz,mode.offset()):piece.origin16();
        if(combineSlab&&!mode.precise()&&!mode.aligned()){
            var at=context.getClickedPos();origin=new GridPoint(normal.axis()==0?origin.x():at.getX()*16L,normal.axis()==1?origin.y():at.getY()*16L,normal.axis()==2?origin.z():at.getZ()*16L);
        }
        if(mode.replace()&&player.level().getBlockEntity(hit.getBlockPos()) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity cell){
            var target=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();if(paintable!=null&&target.adapterId().value().equals("nomoregrids:paintableblocks"))piece=paintable.oriented(piece,paintable.designOrientation(target));origin=target.origin16().add(target.pivot16()).subtract(piece.pivot16());orientation=target.orientation();
        }
        var geometry=piece.geometry();BinaryData itemData=piece.itemPayload();
        if(net.nicomar.nomoregrids.material.FragmentItem.cuttable(block)){
            int length=net.nicomar.nomoregrids.material.FragmentItem.length(stack);
            if(mode.adjusted()){
                Direction6 growth=Direction6.UP.rotated(orientation);GridPoint base=Rotation.of(orientation).transform(new GridPoint(8,0,8),piece.pivot16(),origin);
                length=mode.length()>0?mode.length():PlacementSolver.adjustedLength(growth.axis()==0?base.x():growth.axis()==1?base.y():base.z(),growth);
            }
            geometry=new BoxGeometrySnapshot(crop(geometry.occupation(),length),crop(geometry.collision(),length),crop(geometry.selection(),length));itemData=net.nicomar.nomoregrids.material.FragmentItem.descriptor(length);
            // Recompute contact using the final shape; pivot stays stable after cropping.
            if(!mode.replace()&&(mode.precise()||mode.aligned()))origin=PlacementSolver.origin(geometry.occupation(),piece.pivot16(),orientation,normal,hx,hy,hz,mode.offset());
        }
        var result=new PieceRecord(piece.pieceId(),0,piece.adapterId(),piece.adapterVersion(),piece.sourceBlockId(),piece.sourceState(),origin,orientation,piece.pivot16(),geometry,piece.geometryPayload(),piece.materialPayload(),itemData,Optional.empty(),Set.of());
        if((mode.precise()||mode.aligned())&&!mode.replace())for(var anchor:net.nicomar.nomoregrids.simulation.support.SupportPatches.required(result))if(anchor.normal()==normal.opposite()){
            long face=PlacementQuantizer.outward(normal.axis()==0?hx:normal.axis()==1?hy:hz,normal.sign()>0),offset=normal.axis()==0?mode.offset().x():normal.axis()==1?mode.offset().y():mode.offset().z();
            long shift=Math.subtractExact(Math.addExact(face,offset),anchor.plane());GridPoint delta=new GridPoint(normal.axis()==0?shift:0,normal.axis()==1?shift:0,normal.axis()==2?shift:0);
            result=new PieceRecord(result.pieceId(),0,result.adapterId(),result.adapterVersion(),result.sourceBlockId(),result.sourceState(),result.origin16().add(delta),result.orientation(),result.pivot16(),result.geometry(),result.geometryPayload(),result.materialPayload(),result.itemPayload(),result.groupId(),result.flags());break;
        }
        if(result.sourceBlockId().value().equals("minecraft:ladder")&&Direction6.UP.rotated(result.orientation())!=Direction6.UP)throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: ladder climbing requires a vertical frame");
        return result;
    }
    private static BoxUnion crop(BoxUnion shape,int length){var boxes=new ArrayList<Box16>();for(var b:shape.boxes())if(b.min().y()<length)boxes.add(Box16.of(b.min().x(),b.min().y(),b.min().z(),b.max().x(),Math.min(length,b.max().y()),b.max().z()));return new BoxUnion(boxes);}
}
