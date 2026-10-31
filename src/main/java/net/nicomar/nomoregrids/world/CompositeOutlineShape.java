package net.nicomar.nomoregrids.world;

import it.unimi.dsi.fastutil.doubles.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.nicomar.nomoregrids.platform.PieceQueries;

/** Exact outline raycast; a hit beyond this cell is deferred to traversal so intervening walls win. */
public final class CompositeOutlineShape extends VoxelShape {
    private static final DoubleList COORDS=DoubleLists.unmodifiable(DoubleArrayList.wrap(new double[]{0,1}));
    private final VoxelShape snapshot;
    private final CellView view;
    public CompositeOutlineShape(VoxelShape snapshot,CellView view){super(BitSetDiscreteVoxelShape.withFilledBounds(1,1,1,0,0,0,1,1,1));this.snapshot=snapshot;this.view=view;}
    @Override public DoubleList getCoords(Direction.Axis axis){return COORDS;}
    @Override public void forAllEdges(Shapes.DoubleLineConsumer consumer){snapshot.forAllEdges(consumer);}
    @Override public void forAllBoxes(Shapes.DoubleLineConsumer consumer){snapshot.forAllBoxes(consumer);}
    @Override public java.util.List<AABB> toAabbs(){return snapshot.toAabbs();}
    @Override public AABB bounds(){return snapshot.bounds();}
    @Override public BlockHitResult clip(Vec3 start,Vec3 end,BlockPos cell){
        // A fluid-only container has no component ray to intersect. Its full-cell
        // outline is still needed to target it with a bucket.
        if(view.replicas().isEmpty()&&view.cavity().isPresent())return snapshot.clip(start,end,cell);
        PieceQueries.Hit closest=null;
        for(var piece:view.replicas()){
            var result=PieceQueries.ray(piece,start,end);if(result.isEmpty())continue;var hit=result.orElseThrow();var p=hit.point();
            if(p.x<cell.getX()-1e-7||p.x>cell.getX()+1+1e-7||p.y<cell.getY()-1e-7||p.y>cell.getY()+1+1e-7||p.z<cell.getZ()-1e-7||p.z>cell.getZ()+1+1e-7)continue;
            if(closest==null||hit.distance16()<closest.distance16())closest=hit;
        }
        return closest==null?null:new BlockHitResult(closest.point(),closest.face(),cell,false);
    }
}
