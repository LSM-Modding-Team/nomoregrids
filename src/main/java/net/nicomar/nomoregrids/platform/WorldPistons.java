package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** Mixed native/component closure and exact convex segment sweeps, committed with the base/head. */
public final class WorldPistons {
    private WorldPistons(){}
    public static void register(){NeoForge.EVENT_BUS.addListener(WorldPistons::pre);}
    private static CellPos cell(BlockPos p){return new CellPos(p.getX(),p.getY(),p.getZ());}
    private static void pre(PistonEvent.Pre event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var service=NmgRuntime.existing(level);if(service==null||service.writing())return;boolean extending=event.getPistonMoveType().isExtend;
        if(!relevant(service,event.getPos(),event.getDirection(),extending))return;event.setCanceled(true);if(service.quarantined())return;
        try{new Plan(service,event.getPos(),event.getDirection(),extending).execute();NeoForge.EVENT_BUS.post(new PistonEvent.Post(level,event.getPos(),event.getDirection(),event.getPistonMoveType()));}
        catch(IOException|RuntimeException ex){if(service.quarantined())NmgRuntime.failed(level,ex);else net.nicomar.nomoregrids.NoMoreGrids.LOGGER.debug("NMG piston blocked: {}",ex.toString());}
    }
    private static boolean relevant(WorldService service,BlockPos base,Direction facing,boolean extending){
        var pending=new ArrayDeque<BlockPos>();pending.add(base.relative(facing,extending?1:2));var seen=new HashSet<BlockPos>();
        while(!pending.isEmpty()&&seen.size()<512){var pos=pending.removeFirst();if(!seen.add(pos)||pos.equals(base)||!service.level().isLoaded(pos)||pos.distManhattan(base)>13)continue;if(!service.snapshot().at(cell(pos)).isEmpty())return true;var state=service.level().getBlockState(pos);if(state.isAir()||state.getBlock() instanceof LiquidBlock)continue;pending.add(pos.relative(facing));if(state.is(Blocks.SLIME_BLOCK)||state.is(Blocks.HONEY_BLOCK))for(var side:Direction.values())pending.add(pos.relative(side));}
        return false;
    }
    private static final class Plan {
        final WorldService service;final ServerLevel level;final BlockPos base,head;final Direction facing;final boolean extending;final GridPoint delta;final VanillaCatalog catalog=new VanillaCatalog();
        final Map<UUID,PieceRecord> records=new HashMap<>();final Map<UUID,CellPos> captured=new HashMap<>();final Set<UUID> moving=new TreeSet<>();final Map<CellPos,OperationPlan.Effect> nativeEffects=new LinkedHashMap<>();final List<OperationPlan.Effect> loot=new ArrayList<>();final Set<CellPos> destroyed=new HashSet<>();final Map<UUID,List<ConvexVolume>> geometry=new HashMap<>();
        Plan(WorldService service,BlockPos base,Direction facing,boolean extending){this.service=service;this.level=service.level();this.base=base;this.head=base.relative(facing);this.facing=facing;this.extending=extending;var d=extending?facing:facing.getOpposite();this.delta=new GridPoint(d.getStepX()*16L,d.getStepY()*16L,d.getStepZ()*16L);records.putAll(service.snapshot().records());}
        void execute()throws IOException{
            var state=level.getBlockState(base);if(!(state.getBlock() instanceof PistonBaseBlock))throw new IllegalArgumentException("Piston changed");boolean sticky=state.is(Blocks.STICKY_PISTON);
            var start=cell(extending?head:head.relative(facing));if(extending||sticky){ensureNative(start,false);for(var p:at(start)){if(!extending&&!touchesHead(p))continue;add(p);}}
            boolean changed;do{changed=false;for(var id:List.copyOf(moving)){
                var p=records.get(id);if(!service.available(p)||!service.adapter(p).capabilities().contains(PieceAdapter.Capability.MOVE)||Boolean.parseBoolean(p.sourceState().getOrDefault("occupied","false")))throw new IllegalArgumentException("Immovable component");var reaction=NativeStates.resolve(p).getPistonPushReaction();if(reaction==PushReaction.BLOCK&&!WorldFunctionals.piece(p)&&!WorldGroups.piece(p)&&!WorldDecorations.piece(p)&&!p.adapterId().value().equals("nomoregrids:paintableblocks")||reaction==PushReaction.PUSH_ONLY&&!extending)throw new IllegalArgumentException("Immovable native profile");
                for(var volume:volumes(p)){var sweep=volume.swept(new ConvexVolume.Vec(delta.x(),delta.y(),delta.z()));var bounds=bounds(sweep);var touched=integral(bounds).cells(32);var search=new HashSet<CellPos>(touched);for(var c:touched)for(var side:Direction6.values())search.add(new CellPos(c.x()+side.x,c.y()+side.y,c.z()+side.z));
                    for(var c:search){var pos=WorldService.pos(c);if(!level.isLoaded(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))throw new IllegalArgumentException("UNLOADED_REGION: piston");if(pos.equals(head)&&!extending)continue;if(pos.equals(base)){if(intersectsNative(sweep,c))throw new IllegalArgumentException("Piston base blocks trajectory");continue;}
                        if(at(c).isEmpty()&&!destroyed.contains(c)){
                            var nativeState=level.getBlockState(pos);if(nativeState.isAir()||nativeState.getBlock() instanceof LiquidBlock)continue;var nativeShape=nativeVolumes(c);boolean blocks=nativeShape.stream().anyMatch(v->overlap(sweep,v));boolean sticks=adhesive(p.sourceBlockId().value(),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(nativeState.getBlock()).toString())&&nativeShape.stream().anyMatch(v->volumes(p).stream().anyMatch(w->w.contact(v)==ConvexVolume.Contact.TOUCH));
                            if(!blocks&&!sticks)continue;if(nativeState.getPistonPushReaction()==PushReaction.DESTROY&&!WorldGroups.eligible(nativeState)){if(!blocks)continue;destroy(c);continue;}ensureNative(c,true);
                        }
                        for(var q:at(c))if(!moving.contains(q.pieceId())){boolean blocks=volumes(q).stream().anyMatch(v->overlap(sweep,v));boolean sticks=adhesive(p.sourceBlockId().value(),q.sourceBlockId().value())&&volumes(q).stream().anyMatch(v->volumes(p).stream().anyMatch(w->w.contact(v)==ConvexVolume.Contact.TOUCH));if(blocks&&!extending&&!sticks)throw new IllegalArgumentException("Pull trajectory blocked");if(blocks||sticks){add(q);changed=true;}}
                    }
                    for(var shape:level.getEntityCollisions(null,bounds))for(var b:shape.toAabbs())if(overlap(sweep,ConvexVolume.box(b.minX*16,b.minY*16,b.minZ*16,b.maxX*16,b.maxY*16,b.maxZ*16)))throw new IllegalArgumentException("ENTITY_BLOCKED");
                }
            }}while(changed);
            var changes=new ArrayList<OperationPlan.Change>();for(var id:moving){var p=records.get(id);var old=service.snapshot().records().get(id);var moved=new PieceRecord(id,old==null?0:Math.incrementExact(old.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16().add(delta),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());changes.add(new OperationPlan.Change(id,Optional.ofNullable(old),Optional.of(moved),Optional.ofNullable(captured.get(id))));if(captured.containsKey(id))putNative(captured.get(id),Blocks.AIR.defaultBlockState());}
            putNative(cell(base),state.setValue(PistonBaseBlock.EXTENDED,extending));var headState=extending?Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,facing).setValue(PistonHeadBlock.TYPE,sticky?PistonType.STICKY:PistonType.DEFAULT):Blocks.AIR.defaultBlockState();putNative(cell(head),headState);
            // A represented native goal yields to a new component in its destination; AIR is a vacancy receipt.
            var after=service.snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),net.nicomar.nomoregrids.world.PieceIndex.Limits.DEFAULT);for(var c:changes)for(var destination:c.after().orElseThrow().worldOccupation().cells(8))if(!nativeEffects.containsKey(destination)&&service.nativeGoal(destination).isPresent()&&!service.nativeGoal(destination).orElseThrow().isAir())nativeEffects.put(destination,new OperationPlan.Effect(WorldService.nativeKey(destination),destination,Optional.of(service.effects().get(WorldService.nativeKey(destination)).data()),Optional.empty()));
            if(extending&&!after.at(cell(head)).isEmpty())throw new IllegalArgumentException("Piston head destination occupied");var effects=new ArrayList<>(nativeEffects.values());effects.addAll(loot);service.executeWorld(changes,effects);
        }
        void putNative(CellPos c,net.minecraft.world.level.block.state.BlockState state){if(service.nativeGoal(c).filter(state::equals).isEmpty())nativeEffects.put(c,service.nativeChange(c,state));}
        void add(PieceRecord p){moving.add(p.pieceId());p.groupId().ifPresent(g->{for(var q:records.values())if(q.groupId().equals(Optional.of(g)))moving.add(q.pieceId());});if(moving.size()>12)throw new IllegalArgumentException("Piston component limit");}
        List<PieceRecord> at(CellPos c){var result=new ArrayList<PieceRecord>();for(var id:service.snapshot().at(c))result.add(records.get(id));for(var id:captured.keySet()){var p=records.get(id);if(p.worldOccupation().cells(8).contains(c))result.add(p);}return result;}
        List<ConvexVolume> volumes(PieceRecord p){return geometry.computeIfAbsent(p.pieceId(),id->service.adapter(p).occupation(p).stream().map(v->v.transformed(p.orientation(),p.pivot16(),p.origin16())).toList());}
        List<ConvexVolume> nativeVolumes(CellPos c){var pos=WorldService.pos(c);var state=level.getBlockState(pos);var shape=state.getShape(level,pos);return shape.toAabbs().stream().map(b->ConvexVolume.box((b.minX+c.x())*16,(b.minY+c.y())*16,(b.minZ+c.z())*16,(b.maxX+c.x())*16,(b.maxY+c.y())*16,(b.maxZ+c.z())*16)).toList();}
        boolean intersectsNative(ConvexVolume v,CellPos c){return nativeVolumes(c).stream().anyMatch(q->overlap(v,q));}
        void ensureNative(CellPos c,boolean needed){if(!at(c).isEmpty()||destroyed.contains(c))return;var pos=WorldService.pos(c);if(!level.isLoaded(pos))throw new IllegalArgumentException("UNLOADED_REGION");var state=level.getBlockState(pos);if(state.isAir()||state.getBlock() instanceof LiquidBlock)return;if(!needed&&state.getPistonPushReaction()==PushReaction.DESTROY&&!WorldGroups.eligible(state)){destroy(c);return;}var pb=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().filter(b->b.nativeBlock(state));List<PieceRecord> pieces;
            if(WorldGroups.eligible(state))pieces=WorldGroups.captureNative(level,pos);else if(WorldFunctionals.eligible(state))pieces=List.of(WorldFunctionals.capture(state,level,pos,UUID.randomUUID()));else if(WorldDecorations.eligible(state))pieces=List.of(WorldDecorations.capture(state,level,pos,UUID.randomUUID(),null));else if(pb.isPresent())pieces=List.of(pb.orElseThrow().captureNative(level,pos,UUID.randomUUID()));else if(catalog.eligible(state))pieces=List.of(NativeStates.capture(state,level,pos,UUID.randomUUID()));else throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: native piston member");
            for(var p:pieces){records.put(p.pieceId(),p);captured.put(p.pieceId(),p.origin16().cell());}if(captured.size()+moving.size()>24)throw new IllegalArgumentException("Piston capture budget");
        }
        void destroy(CellPos c){var pos=WorldService.pos(c);var state=level.getBlockState(pos);if(state.hasBlockEntity()||!catalog.eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: piston destruction");WorldLoot.queue(level,NativeStates.capture(state,level,pos,UUID.randomUUID()),loot);putNative(c,Blocks.AIR.defaultBlockState());destroyed.add(c);}
        boolean touchesHead(PieceRecord p){var cube=ConvexVolume.box(head.getX()*16,head.getY()*16,head.getZ()*16,(head.getX()+1)*16,(head.getY()+1)*16,(head.getZ()+1)*16);return volumes(p).stream().anyMatch(v->v.contact(cube)==ConvexVolume.Contact.TOUCH);}
    }
    private static boolean adhesive(String a,String b){if(a.equals("minecraft:slime_block")&&b.equals("minecraft:honey_block")||a.equals("minecraft:honey_block")&&b.equals("minecraft:slime_block"))return false;return Set.of("minecraft:slime_block","minecraft:honey_block").contains(a)||Set.of("minecraft:slime_block","minecraft:honey_block").contains(b);}
    private static boolean overlap(ConvexVolume a,ConvexVolume b){var c=a.contact(b);return c==ConvexVolume.Contact.OVERLAP||c==ConvexVolume.Contact.INDETERMINATE;}
    private static AABB bounds(ConvexVolume v){double x=Double.POSITIVE_INFINITY,y=x,z=x,X=Double.NEGATIVE_INFINITY,Y=X,Z=X;for(var p:v.vertices()){x=Math.min(x,p.x());y=Math.min(y,p.y());z=Math.min(z,p.z());X=Math.max(X,p.x());Y=Math.max(Y,p.y());Z=Math.max(Z,p.z());}return new AABB(x/16,y/16,z/16,X/16,Y/16,Z/16);}
    private static Box16 integral(AABB b){return Box16.of((long)Math.floor(b.minX*16),(long)Math.floor(b.minY*16),(long)Math.floor(b.minZ*16),(long)Math.ceil(b.maxX*16),(long)Math.ceil(b.maxY*16),(long)Math.ceil(b.maxZ*16));}
}
