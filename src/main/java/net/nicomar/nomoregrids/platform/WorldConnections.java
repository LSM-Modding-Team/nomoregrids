package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.material.FragmentItem;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.simulation.connection.FamilyPorts;
import net.nicomar.nomoregrids.simulation.connection.FamilyPorts.Family;
import net.nicomar.nomoregrids.simulation.support.*;
import net.nicomar.nomoregrids.world.PieceIndex;

/** Plans connection topology and 3-D stair corners before any inventory or world mutation. */
public final class WorldConnections {
    private WorldConnections(){}
    public static void complete(ServerLevel level,PieceIndex before,ArrayList<OperationPlan.Change> changes,VanillaCatalog catalog){
        complete(level,before,changes,catalog,Set.of());
    }
    public static void complete(ServerLevel level,PieceIndex before,ArrayList<OperationPlan.Change> changes,VanillaCatalog catalog,Set<CellPos> observed){
        // External notifications on plain solids cannot change family topology. Avoid
        // rebuilding the whole authority; support and fluid observers run separately.
        Set<CellPos> relevant=new HashSet<>(observed);boolean changedFamily=false;
        for(var change:changes){change.nativeSource().ifPresent(relevant::add);for(var side:List.of(change.before(),change.after()))if(side.isPresent()){var p=side.orElseThrow();relevant.addAll(p.worldOccupation().cells(8));changedFamily|=FamilyPorts.family(p)!=Family.NONE;}}
        if(!changedFamily&&WorldSupport.nearby(before,relevant).stream().map(before.records()::get).noneMatch(p->FamilyPorts.family(p)!=Family.NONE))return;
        UUID primary=changes.reversed().stream().flatMap(c->c.after().stream()).map(PieceRecord::pieceId).findFirst().orElse(null);
        Map<UUID,PieceRecord> work=new HashMap<>(before.records());for(var c:changes){if(c.after().isPresent())work.put(c.id(),c.after().orElseThrow());else work.remove(c.id());}
        Set<CellPos> touched=new HashSet<>();for(var c:changes){c.nativeSource().ifPresent(touched::add);c.before().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));}
        for(UUID id:WorldSupport.nearby(before,observed))touched.addAll(before.records().get(id).worldOccupation().cells(8));
        boolean stable=false;for(int pass=0;pass<16;pass++){
            // Drafts may overlap before redundant arms are removed. They never escape this planner.
            var draft=PieceIndex.restored(work.values(),(a,b)->false);Set<UUID> nearby=WorldSupport.nearby(draft,touched);boolean captured=false;
            for(UUID id:nearby){var p=work.get(id);if(FamilyPorts.family(p)==Family.NONE)continue;
                Set<CellPos> contacts=contacts(p);for(var cell:contacts){var at=WorldService.pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION: connection");
                    if(!draft.at(cell).isEmpty()||touched.contains(cell))continue;var state=WorldNativeView.at(cell).orElseGet(()->level.getBlockState(at));if(!catalog.eligible(state))continue;
                    var nativePiece=NativeStates.capture(state,level,at,UUID.randomUUID());if(!related(p,nativePiece))continue;
                    work.put(nativePiece.pieceId(),nativePiece);touched.add(cell);captured=true;
                    if(diffCount(before,work)>128)throw new IllegalArgumentException("COMPLEXITY_LIMIT: connected native region");
                }
            }
            if(captured)continue;
            Map<UUID,PieceRecord> next=new HashMap<>(work);for(UUID id:nearby){var p=work.get(id);if(FamilyPorts.family(p)==Family.NONE||p.flags().contains(PieceRecord.Flag.CONNECTIONS_LOCKED))continue;
                Map<String,String> values=new HashMap<>(p.sourceState());
                if(FamilyPorts.family(p)==Family.STAIR)values.put("shape",stairShape(p,draft));
                else{
                    Set<Direction6> links=EnumSet.noneOf(Direction6.class);for(var port:FamilyPorts.ports(p))if(connected(level,draft,touched,p,port,catalog))links.add(port.local());
                    for(var side:List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST))values.put(side.name().toLowerCase(Locale.ROOT),FamilyPorts.family(p)==Family.WALL?(links.contains(side)?"low":"none"):Boolean.toString(links.contains(side)));
                    if(FamilyPorts.family(p)==Family.WALL){
                        boolean straight=links.size()==2&&(links.containsAll(Set.of(Direction6.NORTH,Direction6.SOUTH))||links.containsAll(Set.of(Direction6.EAST,Direction6.WEST)));
                        for(var side:links){long u0=side==Direction6.WEST?0:5,u1=side==Direction6.EAST?16:11,v0=side==Direction6.NORTH?0:5,v1=side==Direction6.SOUTH?16:11;
                            var above=SupportPatches.transform(new SupportResolver.Patch(Direction6.UP,16,u0,v0,u1,v1),p);if(solidContact(level,draft,touched,p,above,catalog))values.put(side.name().toLowerCase(Locale.ROOT),"tall");
                        }
                        var center=SupportPatches.transform(new SupportResolver.Patch(Direction6.UP,16,4,4,12,12),p);
                        values.put("up",Boolean.toString(!straight||solidContact(level,draft,touched,p,center,catalog)));
                    }
                }
                if(!values.equals(p.sourceState()))next.put(id,reshaped(p,values,before.records().get(id)));
            }
            if(next.equals(work)){stable=true;break;}work=next;
        }
        if(!stable)throw new IllegalArgumentException("COMPLEXITY_LIMIT: connections did not stabilize");
        var sources=new HashMap<UUID,CellPos>();for(var c:changes)c.nativeSource().ifPresent(source->sources.put(c.id(),source));
        Set<UUID> ids=new TreeSet<>(before.records().keySet());ids.addAll(work.keySet());changes.clear();
        for(UUID id:ids){var old=before.records().get(id);var next=work.get(id);if(!Objects.equals(old,next))changes.add(new OperationPlan.Change(id,Optional.ofNullable(old),Optional.ofNullable(next),Optional.ofNullable(sources.get(id))));}
        if(primary!=null){var selected=changes.stream().filter(c->c.id().equals(primary)).findFirst();selected.ifPresent(c->{changes.remove(c);changes.add(c);});}
        if(changes.size()>128)throw new IllegalArgumentException("COMPLEXITY_LIMIT: connection update batch");
        // Final overlap checks run with the actual adapter resolver in OperationEngine.
    }
    private static int diffCount(PieceIndex before,Map<UUID,PieceRecord> work){int count=0;for(var e:work.entrySet())if(!Objects.equals(before.records().get(e.getKey()),e.getValue()))count++;return count;}
    private static Set<CellPos> contacts(PieceRecord p){
        Set<CellPos> result=new HashSet<>();if(FamilyPorts.family(p)==Family.STAIR){var center=FamilyPorts.center(p);for(var side:List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST))result.add(center.add(side.rotated(p.orientation()).vector().multiply(16)).cell());}
        else for(var port:FamilyPorts.ports(p))result.addAll(SupportPatches.contactCells(port.area()).cells(8));return result;
    }
    private static boolean related(PieceRecord a,PieceRecord b){
        if(FamilyPorts.family(a)==Family.STAIR&&FamilyPorts.family(b)==Family.STAIR){GridPoint delta=FamilyPorts.center(b).subtract(FamilyPorts.center(a));return FamilyPorts.stairUp(a)==FamilyPorts.stairUp(b)&&List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST).stream().anyMatch(d->d.rotated(a.orientation()).vector().multiply(16).equals(delta));}
        for(var pa:FamilyPorts.ports(a))for(var pb:FamilyPorts.ports(b))if(FamilyPorts.match(pa,pb))return true;return false;
    }
    private static boolean connected(ServerLevel level,PieceIndex draft,Set<CellPos> touched,PieceRecord piece,FamilyPorts.Port port,VanillaCatalog catalog){
        List<SupportResolver.Patch> surfaces=new ArrayList<>();Set<UUID> seen=new HashSet<>();for(var cell:SupportPatches.contactCells(port.area()).cells(8)){
            var at=WorldService.pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION: connection");
            if(!draft.at(cell).isEmpty()||touched.contains(cell)){
                for(UUID id:draft.at(cell))if(!id.equals(piece.pieceId())&&seen.add(id)){
                    var other=draft.records().get(id);for(var peer:FamilyPorts.ports(other))if(FamilyPorts.match(port,peer))return true;
                    if(FamilyPorts.family(other)==Family.NONE&&(other.adapterId().value().equals("nomoregrids:box")||WorldFunctionals.piece(other))&&!other.flags().contains(PieceRecord.Flag.SUBSTITUTE))surfaces.addAll(SupportPatches.surfaces(other.worldOccupation()));
                }
            }else{var state=WorldNativeView.at(cell).orElseGet(()->level.getBlockState(at));if(catalog.eligible(state)){var other=NativeStates.capture(state,level,at,UUID.randomUUID());if(FamilyPorts.family(other)==Family.NONE)surfaces.addAll(SupportPatches.surfaces(other.worldOccupation()));}}
        }return SupportResolver.covered(port.area(),surfaces);
    }
    private static PieceRecord reshaped(PieceRecord p,Map<String,String> properties,PieceRecord original){
        var state=NativeStates.resolve(p.sourceBlockId(),properties);var captured=NativeStates.capture(state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,p.pieceId());var geometry=captured.geometry();int length=FragmentItem.length(p.itemPayload());
        if(FamilyPorts.family(p)==Family.PANEL&&length<16)geometry=new BoxGeometrySnapshot(crop(geometry.occupation(),length),crop(geometry.collision(),length),crop(geometry.selection(),length));
        return new PieceRecord(p.pieceId(),original==null?0:Math.incrementExact(original.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),properties,p.origin16(),p.orientation(),p.pivot16(),geometry,p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());
    }
    private static boolean solidContact(ServerLevel level,PieceIndex draft,Set<CellPos> touched,PieceRecord piece,SupportResolver.Patch patch,VanillaCatalog catalog){
        List<SupportResolver.Patch> surfaces=new ArrayList<>();Set<UUID> seen=new HashSet<>();for(var cell:SupportPatches.contactCells(patch).cells(8)){
            var at=WorldService.pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION: wall cap");
            if(touched.contains(cell)||!draft.at(cell).isEmpty())for(UUID id:draft.at(cell)){
                if(!id.equals(piece.pieceId())&&seen.add(id)){var p=draft.records().get(id);if((p.adapterId().value().equals("nomoregrids:box")||WorldFunctionals.piece(p))&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE))surfaces.addAll(SupportPatches.surfaces(p.worldOccupation()));}
            }else{var state=WorldNativeView.at(cell).orElseGet(()->level.getBlockState(at));if(catalog.eligible(state))surfaces.addAll(SupportPatches.surfaces(NativeStates.capture(state,level,at,UUID.randomUUID()).worldOccupation()));}
        }return SupportResolver.covered(patch,surfaces);
    }
    private static BoxUnion crop(BoxUnion shape,int length){var out=new ArrayList<Box16>();for(var b:shape.boxes())if(b.min().y()<length)out.add(Box16.of(b.min().x(),b.min().y(),b.min().z(),b.max().x(),Math.min(length,b.max().y()),b.max().z()));return new BoxUnion(out);}
    private static PieceRecord stairAt(PieceIndex index,PieceRecord p,Direction6 direction){
        GridPoint center=FamilyPorts.center(p).add(direction.vector().multiply(16));var cell=center.cell();Set<UUID> seen=new HashSet<>();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)for(UUID id:index.at(new CellPos(cell.x()+x,cell.y()+y,cell.z()+z)))if(seen.add(id)){var q=index.records().get(id);if(FamilyPorts.family(q)==Family.STAIR&&FamilyPorts.center(q).equals(center)&&FamilyPorts.stairUp(q)==FamilyPorts.stairUp(p))return q;}return null;
    }
    private static String stairShape(PieceRecord p,PieceIndex index){
        Direction6 front=FamilyPorts.front(p),up=Direction6.UP.rotated(p.orientation());GridPoint a=up.vector(),b=front.vector();Direction6 left=Direction6.of(new GridPoint(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x()));
        var neighbor=stairAt(index,p,front);if(neighbor!=null){var facing=FamilyPorts.front(neighbor);if(facing!=front&&facing!=front.opposite()&&canTake(index,p,facing.opposite()))return facing==left?"outer_left":"outer_right";}
        neighbor=stairAt(index,p,front.opposite());if(neighbor!=null){var facing=FamilyPorts.front(neighbor);if(facing!=front&&facing!=front.opposite()&&canTake(index,p,facing))return facing==left?"inner_left":"inner_right";}
        return "straight";
    }
    private static boolean canTake(PieceIndex index,PieceRecord p,Direction6 direction){var neighbor=stairAt(index,p,direction);return neighbor==null||FamilyPorts.front(neighbor)!=FamilyPorts.front(p);}
}
