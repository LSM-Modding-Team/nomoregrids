package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Exact box geometry check after translating orientation into native state properties. No identity substitution. */
public final class NativeRepresentation {
    private NativeRepresentation(){}
    public static Optional<BlockState> restore(PieceRecord p,CellPos cell,BlockGetter world){
        if(!p.adapterId().value().equals("nomoregrids:box")||p.groupId().isPresent()||p.worldOccupation().cells(8).size()!=1)return Optional.empty();
        var values=new HashMap<>(p.sourceState());Direction6 up=Direction6.UP.rotated(p.orientation());
        if(p.orientation()!=0){
            boolean oriented=false;
            if(values.containsKey("axis")){var axis=switch(values.get("axis")){case "x"->Direction6.EAST;case "y"->Direction6.UP;case "z"->Direction6.SOUTH;default->throw new IllegalArgumentException("Native axis");};values.put("axis",new String[]{"x","y","z"}[axis.rotated(p.orientation()).axis()]);oriented=true;}
            if(values.containsKey("facing")){var facing=Direction6.valueOf(values.get("facing").toUpperCase(Locale.ROOT)).rotated(p.orientation());values.put("facing",facing.name().toLowerCase(Locale.ROOT));oriented=true;}
            if(values.containsKey("half")||values.containsKey("type")){if(up.axis()!=1)return Optional.empty();if(up==Direction6.DOWN){for(String key:List.of("half","type"))if(values.containsKey(key))values.put(key,switch(values.get(key)){case "bottom"->"top";case "top"->"bottom";case "upper"->"lower";case "lower"->"upper";default->values.get(key);});String shape=values.get("shape");if(shape!=null)values.put("shape",shape.endsWith("_left")?shape.replace("_left","_right"):shape.endsWith("_right")?shape.replace("_right","_left"):shape);}oriented=true;}
            if(!oriented)return Optional.empty();
        }
        try{var state=NativeStates.resolve(p.sourceBlockId(),values);if(state.hasBlockEntity()||state.isRandomlyTicking()||!state.getFluidState().isEmpty())return Optional.empty();var at=WorldService.pos(cell);var captured=NativeStates.capture(state,world,at,p.pieceId());
            if(!same(p.worldOccupation(),captured.worldOccupation(),cell)||!same(p.geometry().collision().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()),captured.geometry().collision().transform(Rotation.of(0),captured.pivot16(),captured.origin16()),cell)||!same(p.geometry().selection().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()),captured.geometry().selection().transform(Rotation.of(0),captured.pivot16(),captured.origin16()),cell))return Optional.empty();return Optional.of(state);
        }catch(RuntimeException ex){return Optional.empty();}
    }
    private static boolean same(BoxUnion a,BoxUnion b,CellPos cell){return !Shapes.joinIsNotEmpty(shape(a,cell),shape(b,cell),BooleanOp.NOT_SAME);}
    private static VoxelShape shape(BoxUnion union,CellPos cell){var shape=Shapes.empty();for(var b:union.boxes())shape=Shapes.or(shape,net.minecraft.world.level.block.Block.box(b.min().x()-cell.x()*16,b.min().y()-cell.y()*16,b.min().z()-cell.z()*16,b.max().x()-cell.x()*16,b.max().y()-cell.y()*16,b.max().z()-cell.z()*16));return shape;}
}
