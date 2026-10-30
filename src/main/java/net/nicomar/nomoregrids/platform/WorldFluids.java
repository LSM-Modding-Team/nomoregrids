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
    private CavitySimulation.FlowJob job;private Map<CellPos,CavityCell> input;private Map<BlockPos,net.minecraft.world.level.block.state.BlockState> nativeImages;private PieceIndex generation;
    private Map<String,WorldEffectStore.Value> effectGeneration;
    public WorldFluids(ServerLevel level,WorldService service){this.level=level;this.service=service;}
    public static String key(CellPos c){return "fluid:"+c.x()+":"+c.y()+":"+c.z();}
    public Optional<CavityCell> at(CellPos c){var value=service.effects().get(key(c));if(value==null)return Optional.empty();try{return Optional.of(CavityCodec.decode(value.data()));}catch(IOException ex){throw new IllegalStateException("INVALID_DATA: cavity",ex);}}
    public Map<CellPos,CavityCell> snapshot(){var result=new TreeMap<CellPos,CavityCell>();for(var entry:service.effects().entrySet())if(entry.getKey().startsWith("fluid:"))try{result.put(entry.getValue().owner(),CavityCodec.decode(entry.getValue().data()));}catch(IOException ex){throw new IllegalStateException("INVALID_DATA: cavity",ex);}return result;}
    public static Kind kind(FluidState f){if(f.isEmpty())return Kind.EMPTY;if(f.getType()==Fluids.WATER||f.getType()==Fluids.FLOWING_WATER)return Kind.WATER;if(f.getType()==Fluids.LAVA||f.getType()==Fluids.FLOWING_LAVA)return Kind.LAVA;throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: external fluid");}
    private CavityCell nativeCell(CellPos c,BitSet mask){
        var fluid=level.getFluidState(WorldService.pos(c));var type=kind(fluid);byte[] values=new byte[4096];if(type!=Kind.EMPTY){int height=fluid.isSource()?16:Math.max(1,fluid.getAmount()*2);for(int i=0;i<4096;i++)if(i/16%16<height&&!mask.get(i))values[i]=CavityCell.code(type);}
        return new CavityCell(mask,values,type!=Kind.EMPTY&&fluid.isSource()&&mask.isEmpty()?type:Kind.EMPTY);
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
        if(boxes.isEmpty()&&solids.isEmpty()&&index.at(c).isEmpty()&&!edited.contains(c)){
            var state=level.getBlockState(WorldService.pos(c));if(!(state.getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock)&&!(state.getBlock() instanceof LiquidBlock)&&!state.isAir()){
                try{var clipped=new ArrayList<Box16>();for(var b:NativeStates.boxes(state.getCollisionShape(level,WorldService.pos(c))).boxes()){long x0=Math.max(0,b.min().x()),y0=Math.max(0,b.min().y()),z0=Math.max(0,b.min().z()),x1=Math.min(16,b.max().x()),y1=Math.min(16,b.max().y()),z1=Math.min(16,b.max().z());if(x0<x1&&y0<y1&&z0<z1)clipped.add(Box16.of(x0,y0,z0,x1,y1,z1));}return CavityCell.blocked(new BoxUnion(clipped));}catch(IllegalArgumentException nonIntegral){var closed=new BitSet(4096);closed.set(0,4096);return closed;}
            }
        }
        if(solids.size()>4096)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cavity geometry");var direct=CavityCell.blocked(new BoxUnion(boxes));if(solids.isEmpty())return direct;var build=new CavityMaskJob(solids);build.advance(4096);direct.or(build.result());return direct;
    }
    public OperationPlan prepare(OperationPlan plan)throws IOException{
        if(plan.changes().isEmpty()||plan.actor().equals(OperationPlan.WORLD_ACTOR)&&plan.effects().stream().anyMatch(e->e.key().startsWith("fluid:")))return plan;var before=service.snapshot();var after=before.changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);
        Set<CellPos> touched=plan.pieceCells();boolean wet=false;for(var c:touched)if(at(c).isPresent()||!level.getFluidState(WorldService.pos(c)).isEmpty())wet=true;if(!wet)return plan;
        var previous=new TreeMap<CellPos,CavityCell>();var masks=new TreeMap<CellPos,BitSet>();var region=new LinkedHashSet<CellPos>();var pending=new ArrayDeque<CellPos>();touched.stream().sorted().forEach(c->{region.add(c);pending.add(c);});
        while(!pending.isEmpty()){
            var c=pending.removeFirst();if(region.size()>256)throw new IllegalArgumentException("COMPLEXITY_LIMIT: fluid redistribution region");
            var old=at(c).orElseGet(()->nativeCell(c,mask(c,before,Set.of())));var nextMask=mask(c,after,touched);
            // A complete source obstructed by construction becomes conserved flowing volume, never a fractional bucket source.
            if(old.source()!=Kind.EMPTY&&!nextMask.isEmpty())old=new CavityCell(old.blocked(),old.fluids(),Kind.EMPTY);
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
        var bytes=old.fluids();int h=fluid.isSource()?16:Math.max(1,fluid.getAmount()*2);for(int i=0;i<4096;i++)if(old.free(i)&&i/16%16<h)bytes[i]=CavityCell.code(type);
        var next=new CavityCell(old.blocked(),bytes,fluid.isSource()&&old.blocked().isEmpty()?type:Kind.EMPTY);var effects=new ArrayList<OperationPlan.Effect>();addEffect(c,next,effects);service.executeWorld(List.of(),effects);
    }
    public void bucket(ServerPlayer player,InteractionHand hand,CellPos c,UUID operation)throws IOException{
        if(service.hasOperation(operation,player.getUUID()))return;
        var old=at(c).orElseThrow(()->new IllegalArgumentException("INVENTORY_MISSING: full fluid cavity required"));var held=player.getItemInHand(hand);boolean collect=held.is(Items.BUCKET);Kind type=collect?old.source():held.is(Items.WATER_BUCKET)?Kind.WATER:held.is(Items.LAVA_BUCKET)?Kind.LAVA:Kind.EMPTY;
        var next=old.bucket(type,collect);var slots=NativeInventory.copy(player);var before=NativeInventory.encode(player,slots);int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
        if(!player.isCreative()){slots.get(slot).shrink(1);NativeInventory.insert(slots,new ItemStack(collect?(type==Kind.WATER?Items.WATER_BUCKET:Items.LAVA_BUCKET):Items.BUCKET));}
        var changes=new ArrayList<OperationPlan.Effect>();addEffect(c,next,changes);service.executePrepared(new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.CLAIM,List.of(),before,NativeInventory.encode(player,slots),changes));
    }
    public void tick()throws IOException{
        long time=level.getGameTime();if(job==null){if(time%5!=0)return;
            var all=snapshot();if(all.isEmpty())return;var region=new TreeSet<CellPos>();for(var c:all.keySet())if(level.isLoaded(WorldService.pos(c))){region.add(c);for(var d:Direction6.values()){var n=new CellPos(c.x()+d.x,c.y()+d.y,c.z()+d.z);if(level.isLoaded(WorldService.pos(n)))region.add(n);}if(region.size()>256)break;}
            if(region.size()>256)throw new IllegalArgumentException("COMPLEXITY_LIMIT: active fluid region");input=new TreeMap<>();nativeImages=new HashMap<>();
            for(var c:region){var state=level.getBlockState(WorldService.pos(c));nativeImages.put(WorldService.pos(c),state);var existing=all.get(c);input.put(c,existing!=null?existing:nativeCell(c,mask(c,service.snapshot(),Set.of())));}
            generation=service.snapshot();effectGeneration=Map.copyOf(service.effects());job=new CavitySimulation.FlowJob(input,time%30==0?Set.of(Kind.WATER,Kind.LAVA):Set.of(Kind.WATER));
        }
        if(service.snapshot()!=generation||!service.effects().equals(effectGeneration)||nativeImages.entrySet().stream().anyMatch(e->!level.isLoaded(e.getKey())||!level.getBlockState(e.getKey()).equals(e.getValue()))){job=null;return;}
        if(!job.advance(8192))return;var next=job.result();job=null;var changes=new ArrayList<OperationPlan.Effect>();
        for(var entry:next.entrySet()){
            var c=entry.getKey();if(!at(c).isPresent()&&!level.getFluidState(WorldService.pos(c)).isEmpty())continue;
            addEffect(c,entry.getValue(),changes);
        }
        var reactions=CavitySimulation.react(next);var pieces=new ArrayList<OperationPlan.Change>();
        for(var product:reactions.products().entrySet()){
            var c=product.getKey();if(!service.snapshot().at(c).isEmpty())continue;var state=NativeStates.resolve(product.getValue(),Map.of());var p=NativeStates.capture(state,level,WorldService.pos(c),UUID.randomUUID());pieces.add(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p)));addEffect(c,reactions.fluids().get(c),changes);
        }
        service.executeWorld(pieces,changes);
    }
}
