package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.*;
import net.nicomar.nomoregrids.simulation.fluid.*;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind;
import net.nicomar.nomoregrids.world.PieceIndex;

/** One server authority for partial cavities. Native fluid cells are boundary inputs, never a second writer. */
public final class WorldFluids {
    private final ServerLevel level;private final WorldService service;
    private CavityRelaxation job;
    private record Due(CellPos cell,Kind kind,long tick){}
    private final PriorityQueue<Due> due=new PriorityQueue<>(Comparator.comparingLong(Due::tick).thenComparing(Due::cell).thenComparing(Due::kind));
    private final Map<String,Long> scheduled=new HashMap<>();private boolean initialized;private Kind workingKind;
    private Map<CellPos,Set<UUID>> memberships;private Map<UUID,Long> revisions;private Map<CellPos,CavityCell> input;private Map<BlockPos,net.minecraft.world.level.block.state.BlockState> nativeImages;private PieceIndex generation;
    private Map<String,WorldEffectStore.Value> effectGeneration;
    public WorldFluids(ServerLevel level,WorldService service){this.level=level;this.service=service;}
    public static String key(CellPos c){return "fluid:"+c.x()+":"+c.y()+":"+c.z();}
    public Optional<CavityCell> at(CellPos c){var value=service.effects().get(key(c));if(value==null)return Optional.empty();try{return Optional.of(CavityCodec.decode(value.data()));}catch(IOException ex){throw new IllegalStateException("INVALID_DATA: cavity",ex);}}
    public Map<CellPos,CavityCell> snapshot(){var result=new TreeMap<CellPos,CavityCell>();for(var entry:service.effects().entrySet())if(entry.getKey().startsWith("fluid:"))try{result.put(entry.getValue().owner(),CavityCodec.decode(entry.getValue().data()));}catch(IOException ex){throw new IllegalStateException("INVALID_DATA: cavity",ex);}return result;}
    public static Kind kind(FluidState f){if(f.isEmpty())return Kind.EMPTY;if(f.getType()==Fluids.WATER||f.getType()==Fluids.FLOWING_WATER)return Kind.WATER;if(f.getType()==Fluids.LAVA||f.getType()==Fluids.FLOWING_LAVA)return Kind.LAVA;throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: external fluid");}
    private CavityCell nativeCell(CellPos c,BitSet mask){
        var fluid=level.getFluidState(WorldService.pos(c));var type=kind(fluid);byte[] values=new byte[4096];if(type!=Kind.EMPTY){int height=fluid.isSource()?16:Math.max(1,fluid.getAmount()*2);for(int i=0;i<4096;i++)if(i/16%16<height&&!mask.get(i))values[i]=CavityCell.code(type);}
        byte[] distance=new byte[4096];for(int i=0;i<4096;i++)distance[i]=values[i]==0?(byte)127:(byte)Math.min(type==Kind.LAVA?48:112,(8-fluid.getAmount())*16);return new CavityCell(mask,values,type!=Kind.EMPTY&&fluid.isSource()&&mask.isEmpty()?type:Kind.EMPTY,distance);
    }
    private BitSet mask(CellPos c,PieceIndex index,Set<CellPos> edited){
        var boxes=new ArrayList<Box16>();var solids=new ArrayList<ConvexVolume>();long x=c.x()*16,y=c.y()*16,z=c.z()*16;
        for(var id:index.at(c)){
            var p=index.records().get(id);if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: retained fluid geometry");
            if(p.adapterId().value().equals("nomoregrids:box")||WorldFunctionals.piece(p)){
                for(var box:p.worldOccupation().boxes()){long x0=Math.max(x,box.min().x()),y0=Math.max(y,box.min().y()),z0=Math.max(z,box.min().z()),x1=Math.min(x+16,box.max().x()),y1=Math.min(y+16,box.max().y()),z1=Math.min(z+16,box.max().z());if(x0<x1&&y0<y1&&z0<z1)boxes.add(Box16.of(x0-x,y0-y,z0-z,x1-x,y1-y,z1-z));}continue;
            }
            for(var volume:service.adapter(p).occupation(p)){
                var v=volume.transformed(p.orientation(),p.pivot16(),p.origin16());
                for(var plane:List.of(new ConvexVolume.Plane(new ConvexVolume.Vec(-1,0,0),-x),new ConvexVolume.Plane(new ConvexVolume.Vec(1,0,0),x+16),new ConvexVolume.Plane(new ConvexVolume.Vec(0,-1,0),-y),new ConvexVolume.Plane(new ConvexVolume.Vec(0,1,0),y+16),new ConvexVolume.Plane(new ConvexVolume.Vec(0,0,-1),-z),new ConvexVolume.Plane(new ConvexVolume.Vec(0,0,1),z+16)))v=v.clipped(plane);
                if(v.hasVolume())solids.add(v.transformed(0,GridPoint.ZERO,new GridPoint(-x,-y,-z)));
            }
        }
        if(boxes.isEmpty()&&solids.isEmpty()&&index.at(c).isEmpty()&&(!edited.contains(c)||index!=service.snapshot()&&WorldNativeView.at(c).isPresent())){
            var state=index!=service.snapshot()?WorldNativeView.at(c).orElseGet(()->level.getBlockState(WorldService.pos(c))):level.getBlockState(WorldService.pos(c));if(!(state.getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock)&&!(state.getBlock() instanceof LiquidBlock)&&!state.isAir()){
                try{var clipped=new ArrayList<Box16>();for(var b:NativeStates.boxes(state.getCollisionShape(level,WorldService.pos(c))).boxes()){long x0=Math.max(0,b.min().x()),y0=Math.max(0,b.min().y()),z0=Math.max(0,b.min().z()),x1=Math.min(16,b.max().x()),y1=Math.min(16,b.max().y()),z1=Math.min(16,b.max().z());if(x0<x1&&y0<y1&&z0<z1)clipped.add(Box16.of(x0,y0,z0,x1,y1,z1));}return CavityCell.blocked(new BoxUnion(clipped));}catch(IllegalArgumentException nonIntegral){var closed=new BitSet(4096);closed.set(0,4096);return closed;}
            }
        }
        if(solids.size()>4096)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cavity geometry");var direct=CavityCell.blocked(new BoxUnion(boxes));if(solids.isEmpty())return direct;var build=new CavityMaskJob(solids);build.advance(4096);direct.or(build.result());return direct;
    }
    public OperationPlan prepare(OperationPlan plan)throws IOException{
        if(plan.changes().isEmpty()&&plan.effects().stream().noneMatch(e->e.key().startsWith("native:"))||plan.actor().equals(OperationPlan.WORLD_ACTOR)&&plan.effects().stream().anyMatch(e->e.key().startsWith("fluid:")))return plan;var before=service.snapshot();var after=before.changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);
        Set<CellPos> touched=new HashSet<>(plan.pieceCells());plan.effects().stream().filter(e->e.key().startsWith("native:")).forEach(e->touched.add(e.owner()));boolean wet=false;for(var c:touched)if(at(c).isPresent()||!level.getFluidState(WorldService.pos(c)).isEmpty())wet=true;if(!wet)return plan;
        var previous=new TreeMap<CellPos,CavityCell>();var masks=new TreeMap<CellPos,BitSet>();var region=new LinkedHashSet<CellPos>();var pending=new ArrayDeque<CellPos>();touched.stream().sorted().forEach(c->{region.add(c);pending.add(c);});
        while(!pending.isEmpty()){
            var c=pending.removeFirst();if(region.size()>256)throw new IllegalArgumentException("COMPLEXITY_LIMIT: fluid redistribution region");
            var old=at(c).orElseGet(()->nativeCell(c,mask(c,before,Set.of())));var nextMask=mask(c,after,touched);
            // A complete source obstructed by construction becomes conserved flowing volume, never a fractional bucket source.
            if(old.source()!=Kind.EMPTY&&!nextMask.isEmpty())old=new CavityCell(old.blocked(),old.fluids(),Kind.EMPTY,old.distances());
            previous.put(c,old);masks.put(c,nextMask);
            if(old.amount(Kind.WATER)+old.amount(Kind.LAVA)==0&&!touched.contains(c))continue;
            for(var d:Direction6.values()){
                var n=new CellPos(c.x()+d.x,c.y()+d.y,c.z()+d.z);if(region.contains(n)||!level.isLoaded(WorldService.pos(n)))continue;
                if(touched.stream().noneMatch(t->Math.abs(t.x()-n.x())+Math.abs(t.y()-n.y())+Math.abs(t.z()-n.z())<=4))continue;
                var m=mask(n,before,Set.of());if(m.cardinality()==4096)continue;region.add(n);pending.add(n);
            }
        }
        var displaced=CavitySimulation.displace(previous,masks,262144);var effects=new ArrayList<>(plan.effects());
        for(var entry:displaced.entrySet()){
            var c=entry.getKey();if(!touched.contains(c)&&!at(c).isPresent()&&!level.getFluidState(WorldService.pos(c)).isEmpty())continue;
            addEffect(c,entry.getValue(),effects);
        }
        return new OperationPlan(plan.id(),plan.actor(),plan.kind(),plan.changes(),plan.resourcesBefore(),plan.resourcesAfter(),effects);
    }
    private void addEffect(CellPos c,CavityCell next,List<OperationPlan.Effect> changes){
        var before=at(c).map(CavityCodec::encode);var after=next.amount(Kind.WATER)+next.amount(Kind.LAVA)==0?Optional.<BinaryData>empty():Optional.of(CavityCodec.encode(next));
        if(!before.equals(after)){changes.removeIf(e->e.key().equals(key(c)));changes.add(new OperationPlan.Effect(key(c),c,before,after));}
    }
    public void incoming(CellPos c,FluidState fluid)throws IOException{
        var old=at(c).orElseGet(()->new CavityCell(mask(c,service.snapshot(),Set.of()),new byte[4096],Kind.EMPTY));Kind type=kind(fluid);if(type==Kind.EMPTY)return;
        if(old.amount(type==Kind.WATER?Kind.LAVA:Kind.WATER)>0)return;
        var bytes=old.fluids();var distances=old.distances();int distance=Math.max(0,Math.min(type==Kind.WATER?112:48,(8-fluid.getAmount())*16));int h=fluid.isSource()?16:Math.max(1,fluid.getAmount()*2);for(int i=0;i<4096;i++)if(old.free(i)&&i/16%16<h){bytes[i]=CavityCell.code(type);distances[i]=(byte)Math.min(Byte.toUnsignedInt(distances[i]),distance);}
        var next=new CavityCell(old.blocked(),bytes,fluid.isSource()&&old.blocked().isEmpty()?type:Kind.EMPTY,distances);var effects=new ArrayList<OperationPlan.Effect>();addEffect(c,next,effects);service.executeWorld(List.of(),effects);
    }
    public void bucket(ServerPlayer player,InteractionHand hand,CellPos c,UUID operation)throws IOException{
        if(service.hasOperation(operation,player.getUUID()))return;
        var old=at(c).orElseThrow(()->new IllegalArgumentException("INVENTORY_MISSING: full fluid cavity required"));var held=player.getItemInHand(hand);boolean collect=held.is(Items.BUCKET);Kind type=collect?old.source():held.is(Items.WATER_BUCKET)?Kind.WATER:held.is(Items.LAVA_BUCKET)?Kind.LAVA:Kind.EMPTY;
        var next=old.bucket(type,collect);var slots=NativeInventory.copy(player);var before=NativeInventory.encode(player,slots);int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
        if(!player.isCreative()){slots.get(slot).shrink(1);NativeInventory.insert(slots,new ItemStack(collect?(type==Kind.WATER?Items.WATER_BUCKET:Items.LAVA_BUCKET):Items.BUCKET));}
        var changes=new ArrayList<OperationPlan.Effect>();addEffect(c,next,changes);service.executePrepared(new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.CLAIM,List.of(),before,NativeInventory.encode(player,slots),changes));
    }
    private boolean authority(CellPos c){return !service.snapshot().at(c).isEmpty()||service.effects().containsKey(key(c));}
    private boolean adjacentAuthority(CellPos c){if(authority(c))return true;for(var d:Direction6.values())if(authority(new CellPos(c.x()+d.x,c.y()+d.y,c.z()+d.z)))return true;return false;}
    /** Native changes far from NMG are boundary observations, never a reason to claim vanilla fluids. */
    public void changed(CellPos c){if(!adjacentAuthority(c))return;for(var kind:List.of(Kind.WATER,Kind.LAVA)){schedule(c,kind,level.getGameTime()+delay(kind));for(var d:Direction6.values())schedule(new CellPos(c.x()+d.x,c.y()+d.y,c.z()+d.z),kind,level.getGameTime()+delay(kind));}}
    private int delay(Kind kind){return (kind==Kind.WATER?Fluids.WATER:Fluids.LAVA).getTickDelay(level);}
    private String ticket(CellPos c,Kind kind){return key(c)+":"+kind;}
    private void schedule(CellPos c,Kind kind,long time){if(!level.isLoaded(WorldService.pos(c)))return;String k=ticket(c,kind);Long old=scheduled.get(k);if(old!=null&&old<=time)return;scheduled.put(k,time);due.add(new Due(c,kind,time));}
    private boolean target(CellPos c){var state=level.getBlockState(WorldService.pos(c));return at(c).isPresent()||state.isAir()||state.getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock;}
    private CavityCell cell(CellPos c){return at(c).orElseGet(()->nativeCell(c,mask(c,service.snapshot(),Set.of())));}
    public void tick()throws IOException{
        if(!initialized){for(var c:snapshot().keySet())changed(c);initialized=true;}
        long now=level.getGameTime();if(job==null){var targets=new LinkedHashSet<CellPos>();while(!due.isEmpty()&&targets.size()<16){var next=due.peek();if(next.tick>now)break;due.remove();if(!Objects.equals(scheduled.get(ticket(next.cell,next.kind)),next.tick))continue;if(!targets.isEmpty()&&workingKind!=next.kind){due.add(next);break;}scheduled.remove(ticket(next.cell,next.kind));if(!level.isLoaded(WorldService.pos(next.cell))||!target(next.cell))continue;workingKind=next.kind;targets.add(next.cell);}
            if(targets.isEmpty())return;var region=new HashSet<>(targets);for(var c:targets)for(var d:Direction6.values()){var n=new CellPos(c.x()+d.x,c.y()+d.y,c.z()+d.z);if(level.isLoaded(WorldService.pos(n)))region.add(n);}
            input=new TreeMap<>();nativeImages=new HashMap<>();memberships=new HashMap<>();revisions=new HashMap<>();for(var c:region){var at=WorldService.pos(c);nativeImages.put(at,level.getBlockState(at));input.put(c,cell(c));memberships.put(c,service.snapshot().at(c));for(var id:service.snapshot().at(c))revisions.put(id,service.snapshot().records().get(id).revision());}job=new CavityRelaxation(input,targets,workingKind);effectGeneration=new HashMap<>();for(var c:region){var v=service.effects().get(key(c));if(v!=null)effectGeneration.put(key(c),v);}
        }
        boolean stale=nativeImages.entrySet().stream().anyMatch(e->!level.isLoaded(e.getKey())||!level.getBlockState(e.getKey()).equals(e.getValue()))||memberships.entrySet().stream().anyMatch(e->!service.snapshot().at(e.getKey()).equals(e.getValue()))||revisions.entrySet().stream().anyMatch(e->service.snapshot().records().get(e.getKey())==null||service.snapshot().records().get(e.getKey()).revision()!=e.getValue());
        if(!stale)for(var c:input.keySet())if(!Objects.equals(effectGeneration.get(key(c)),service.effects().get(key(c)))){stale=true;break;}
        if(stale){for(var c:input.keySet())schedule(c,workingKind,now+delay(workingKind));job=null;return;}
        if(!job.advance(65536))return;var result=job.result();job=null;var effects=new ArrayList<OperationPlan.Effect>();var combined=new HashMap<>(input);combined.putAll(result);var reactions=CavitySimulation.react(combined);var pieces=new ArrayList<OperationPlan.Change>();
        for(var e:result.entrySet()){addEffect(e.getKey(),reactions.fluids().get(e.getKey()),effects);if(e.getValue().amount(Kind.WATER)+e.getValue().amount(Kind.LAVA)>0)schedule(e.getKey(),workingKind,now+delay(workingKind));}
        for(var product:reactions.products().entrySet())if(result.containsKey(product.getKey())&&service.snapshot().at(product.getKey()).isEmpty()){
            var p=NativeStates.capture(NativeStates.resolve(product.getValue(),Map.of()),level,WorldService.pos(product.getKey()),UUID.randomUUID());pieces.add(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p)));addEffect(product.getKey(),reactions.fluids().get(product.getKey()),effects);
        }service.executeWorld(pieces,effects);
    }
}
