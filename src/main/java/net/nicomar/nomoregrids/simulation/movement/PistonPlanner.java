package net.nicomar.nomoregrids.simulation.movement;

import java.util.*;
import java.util.function.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.world.*;

/** One-block orthogonal movement, bounded by twelve logical components and exact box sweeps. */
public final class PistonPlanner {
    private PistonPlanner(){}
    public static List<PieceChange> plan(PieceIndex index,Set<UUID> faceContacts,Direction6 direction,boolean pulling,
            GroupRegistry groups,Predicate<UUID> movable,Predicate<CellPos> loaded,Predicate<Box16> fixedObstacle,
            BiPredicate<PieceRecord,PieceRecord> adhesive){
        if(faceContacts.isEmpty())return List.of();
        GridPoint delta=new GridPoint(direction.x*16L,direction.y*16L,direction.z*16L);
        Set<UUID> closure=new TreeSet<>(groups.closure(faceContacts));boolean changed;
        do{
            changed=false;if(closure.size()>12)throw new IllegalArgumentException("Piston component limit");
            for(UUID id:List.copyOf(closure)){
                PieceRecord p=index.records().get(id);if(p==null||!movable.test(id)||p.flags().contains(PieceRecord.Flag.SUBSTITUTE))throw new IllegalArgumentException("Immovable component");
                for(var box:p.worldOccupation().boxes()){
                    Box16 sweep=sweep(box,delta);
                    Set<CellPos> swept=sweep.cells(32);Set<CellPos> candidates=new HashSet<>(swept);
                    for(var cell:swept){
                        if(!loaded.test(cell))throw new IllegalArgumentException("UNLOADED_REGION");
                        for(var side:Direction6.values())candidates.add(new CellPos(Math.addExact(cell.x(),side.x),Math.addExact(cell.y(),side.y),Math.addExact(cell.z(),side.z)));
                    }
                    for(var cell:candidates){
                        for(UUID other:index.at(cell))if(!closure.contains(other)){
                            PieceRecord neighbor=index.records().get(other);
                            boolean blocks=neighbor.worldOccupation().boxes().stream().anyMatch(sweep::overlaps);
                            boolean sticks=adhesive.test(p,neighbor)&&p.worldOccupation().boxes().stream().anyMatch(a->neighbor.worldOccupation().boxes().stream().anyMatch(b->touches(a,b)));
                            if(blocks&&pulling&&!sticks)throw new IllegalArgumentException("Pull trajectory blocked");
                            if(blocks||sticks)changed|=closure.addAll(groups.members(other));
                        }
                    }
                    if(fixedObstacle.test(sweep))throw new IllegalArgumentException("Trajectory intersects native solid/entity");
                }
            }
        }while(changed);
        if(closure.size()>12)throw new IllegalArgumentException("Piston component limit");
        var changes=new ArrayList<PieceChange>();
        for(UUID id:closure){var p=index.records().get(id);var moved=new PieceRecord(p.pieceId(),Math.incrementExact(p.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),
            p.origin16().add(delta),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());
            changes.add(new PieceChange(id,p.revision(),Optional.of(moved)));
        }
        return List.copyOf(changes);
    }
    public static Box16 sweep(Box16 box,GridPoint delta){
        int axes=(delta.x()!=0?1:0)+(delta.y()!=0?1:0)+(delta.z()!=0?1:0);if(axes!=1)throw new IllegalArgumentException("Orthogonal sweep only");
        return Box16.of(Math.min(box.min().x(),Math.addExact(box.min().x(),delta.x())),Math.min(box.min().y(),Math.addExact(box.min().y(),delta.y())),Math.min(box.min().z(),Math.addExact(box.min().z(),delta.z())),
            Math.max(box.max().x(),Math.addExact(box.max().x(),delta.x())),Math.max(box.max().y(),Math.addExact(box.max().y(),delta.y())),Math.max(box.max().z(),Math.addExact(box.max().z(),delta.z())));
    }
    public static boolean touches(Box16 a,Box16 b){
        boolean x=a.min().x()<b.max().x()&&a.max().x()>b.min().x(),y=a.min().y()<b.max().y()&&a.max().y()>b.min().y(),z=a.min().z()<b.max().z()&&a.max().z()>b.min().z();
        return (a.max().x()==b.min().x()||b.max().x()==a.min().x())&&y&&z
            ||(a.max().y()==b.min().y()||b.max().y()==a.min().y())&&x&&z||(a.max().z()==b.min().z()||b.max().z()==a.min().z())&&x&&y;
    }
}
