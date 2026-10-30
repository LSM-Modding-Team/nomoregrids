package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.simulation.movement.GroupRegistry;
import net.nicomar.nomoregrids.world.*;

/** Family-specific multipart authority. No adjacency-based grouping or temporary world installation. */
public final class WorldGroups {
    public static final String ADAPTER="nomoregrids:grouped";
    private WorldGroups(){}
    private static final Map<WorldService,Integer> BED_CURSORS=new WeakHashMap<>();
    public static boolean piece(PieceRecord p){return p.adapterId().value().equals(ADAPTER);}
    public static boolean eligible(BlockState s){return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals("minecraft")&&(s.getBlock() instanceof DoorBlock||s.getBlock() instanceof BedBlock||s.getBlock() instanceof TrapDoorBlock);}
    public static boolean bed(PieceRecord p){return piece(p)&&p.sourceState().containsKey("part");}
    public static BlockState placement(Block block,BlockPlaceContext context){var state=block.getStateForPlacement(context);if(state!=null)return state;var props=new HashMap<>(NativeStates.properties(block.defaultBlockState()));props.put("facing",context.getHorizontalDirection().getName());return NativeStates.resolve(new ResourceId(BuiltInRegistries.BLOCK.getKey(block).toString()),props);}
    public static PieceRecord capture(BlockState s,BlockGetter level,BlockPos at,UUID id,UUID group){
        if(!eligible(s))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: group");var shape=NativeStates.boxes(s.getShape(level,at));var collision=NativeStates.boxes(s.getCollisionShape(level,at));var source=new ResourceId(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString());
        return new PieceRecord(id,0,new ResourceId(ADAPTER),1,source,NativeStates.properties(s),new GridPoint(at.getX()*16L,at.getY()*16L,at.getZ()*16L),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(shape,collision,shape),new net.nicomar.nomoregrids.persistence.NativeStateImage(source,NativeStates.properties(s)).encode(),BinaryData.EMPTY,BinaryData.EMPTY,Optional.ofNullable(group),Set.of());
    }
    public static List<PieceRecord> captureNative(ServerLevel level,BlockPos at){
        var s=level.getBlockState(at);if(s.getBlock() instanceof TrapDoorBlock)return List.of(capture(s,level,at,UUID.randomUUID(),null));var group=UUID.randomUUID();var props=NativeStates.properties(s);var delta=props.containsKey("half")?new BlockPos(0,"lower".equals(props.get("half"))?1:-1,0):BlockPos.ZERO.relative(Direction.valueOf(props.get("facing").toUpperCase(Locale.ROOT)),"head".equals(props.get("part"))?-1:1);var other=at.offset(delta);
        if(!level.isLoaded(other))throw new IllegalArgumentException("UNLOADED_REGION: group");var pair=level.getBlockState(other);var values=NativeStates.properties(pair);var comparable=new HashMap<>(values);for(String key:List.of("half","part","occupied"))comparable.remove(key);var expected=new HashMap<>(props);for(String key:List.of("half","part","occupied"))expected.remove(key);if(!comparable.equals(expected)||pair.getBlock()!=s.getBlock()||props.containsKey("half")&&Objects.equals(props.get("half"),values.get("half"))||props.containsKey("part")&&Objects.equals(props.get("part"),values.get("part")))throw new IllegalArgumentException("INVALID_DATA: incomplete native group");
        return List.of(capture(s,level,at,UUID.randomUUID(),group),capture(pair,level,other,UUID.randomUUID(),group));
    }
    public static GroupRegistry registry(PieceIndex index){var members=new HashMap<UUID,Set<UUID>>();for(var p:index.records().values())p.groupId().ifPresent(g->members.computeIfAbsent(g,k->new HashSet<>()).add(p.pieceId()));var result=new GroupRegistry();members.forEach((g,m)->{if(m.size()>=2)result.register(g,m);});return result;}
    public static OperationPlan prepare(WorldService service,ServerLevel level,OperationPlan plan){
        if(plan.changes().isEmpty())return plan;var changes=new ArrayList<>(plan.changes());var groups=registry(service.snapshot());
        for(var c:List.copyOf(changes))if(c.before().isPresent()&&c.after().isEmpty())for(var id:groups.members(c.id()))if(changes.stream().noneMatch(x->x.id().equals(id))){var old=service.snapshot().records().get(id);changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.empty()));}
        for(var c:List.copyOf(changes))if(c.before().isEmpty()&&c.after().isPresent()){
            var p=c.after().orElseThrow();if(!piece(p)||p.groupId().isEmpty()||changes.stream().flatMap(x->x.after().stream()).filter(x->x.groupId().equals(p.groupId())).count()!=1)continue;
            var values=new HashMap<>(p.sourceState());GridPoint offset;
            if(values.containsKey("half")){boolean lower="lower".equals(values.get("half"));values.put("half",lower?"upper":"lower");offset=new GridPoint(0,lower?16:-16,0);}
            else{boolean foot="foot".equals(values.get("part"));values.put("part",foot?"head":"foot");var d=Direction6.valueOf(values.get("facing").toUpperCase(Locale.ROOT));offset=new GridPoint(d.x*(foot?16:-16),0,d.z*(foot?16:-16));}
            var source=NativeStates.resolve(p.sourceBlockId(),values);var other=capture(source,level,WorldService.pos(p.origin16().cell()),UUID.randomUUID(),p.groupId().orElseThrow());other=new PieceRecord(other.pieceId(),0,other.adapterId(),1,other.sourceBlockId(),other.sourceState(),p.origin16().add(Rotation.of(p.orientation()).apply(offset)),p.orientation(),p.pivot16(),other.geometry(),other.geometryPayload(),other.materialPayload(),other.itemPayload(),p.groupId(),p.flags());changes.add(new OperationPlan.Change(other.pieceId(),Optional.empty(),Optional.of(other)));
        }
        var touched=new HashSet<UUID>();changes.forEach(c->touched.add(c.id()));groups.requireComplete(touched);
        var after=service.snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);requireStructure(after,touched);
        return new OperationPlan(plan.id(),plan.actor(),plan.kind(),changes,plan.resourcesBefore(),plan.resourcesAfter(),plan.effects());
    }
    public static GridPoint roleOffset(PieceRecord p){if("upper".equals(p.sourceState().get("half")))return new GridPoint(0,16,0);if("head".equals(p.sourceState().get("part"))){var d=Direction6.valueOf(p.sourceState().get("facing").toUpperCase(Locale.ROOT));return new GridPoint(d.x*16L,0,d.z*16L);}return GridPoint.ZERO;}
    private static void requireStructure(PieceIndex index,Set<UUID> touched){for(var members:registry(index).snapshot().values()){if(Collections.disjoint(members,touched))continue;var parts=members.stream().map(index.records()::get).toList();if(parts.stream().anyMatch(p->!piece(p)))continue;if(parts.size()!=2)throw new IllegalArgumentException("INVALID_DATA: native group requires two members");var a=parts.getFirst();var b=parts.getLast();String role=a.sourceState().containsKey("part")?"part":"half";var values=new HashMap<>(a.sourceState());var other=new HashMap<>(b.sourceState());values.remove(role);other.remove(role);values.remove("occupied");other.remove("occupied");var firstBase=a.origin16().subtract(Rotation.of(a.orientation()).apply(roleOffset(a)));var secondBase=b.origin16().subtract(Rotation.of(b.orientation()).apply(roleOffset(b)));if(!a.sourceBlockId().equals(b.sourceBlockId())||a.orientation()!=b.orientation()||!a.pivot16().equals(b.pivot16())||!values.equals(other)||Objects.equals(a.sourceState().get(role),b.sourceState().get(role))||!firstBase.equals(secondBase))throw new IllegalArgumentException("INVALID_DATA: native group geometry/state conflict");}}
    public static void tick(WorldService service)throws IOException{var beds=service.snapshot().records().values().stream().filter(WorldGroups::bed).filter(p->"head".equals(p.sourceState().get("part"))).sorted(Comparator.comparing(PieceRecord::pieceId)).toList();for(int turn=0;turn<Math.min(32,beds.size());turn++){int cursor=BED_CURSORS.getOrDefault(service,0);var p=beds.get(Math.floorMod(cursor,beds.size()));BED_CURSORS.put(service,Math.floorMod(cursor+1,beds.size()));var pos=WorldService.pos(p.origin16().cell());if(!service.level().isLoaded(pos))continue;var b=p.worldOccupation().bounds();var area=new AABB(b.min().x()/16.0-1,b.min().y()/16.0-1,b.min().z()/16.0-1,b.max().x()/16.0+1,b.max().y()/16.0+2,b.max().z()/16.0+1);boolean sleeping=bedAt(service.level(),pos).filter(head->head.pieceId().equals(p.pieceId())).isPresent()&&!service.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,area,e->e.isSleeping()&&e.getSleepingPos().filter(pos::equals).isPresent()).isEmpty();if(sleeping!=Boolean.parseBoolean(p.sourceState().getOrDefault("occupied","false")))setOccupied(service,p,sleeping);}}
    private static void setOccupied(WorldService service,PieceRecord head,boolean occupied)throws IOException{var changes=new ArrayList<OperationPlan.Change>();for(var id:registry(service.snapshot()).members(head.pieceId())){var old=service.snapshot().records().get(id);var values=new HashMap<>(old.sourceState());values.put("occupied",Boolean.toString(occupied));var next=new PieceRecord(id,old.revision()+1,old.adapterId(),1,old.sourceBlockId(),values,old.origin16(),old.orientation(),old.pivot16(),old.geometry(),old.geometryPayload(),old.materialPayload(),old.itemPayload(),old.groupId(),old.flags());changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.of(next)));}if(changes.stream().anyMatch(c->!c.before().orElseThrow().sourceState().equals(c.after().orElseThrow().sourceState())))service.executeWorld(changes,List.of());}
    public static void complete(WorldService service,ServerLevel level,ArrayList<OperationPlan.Change> changes){
        var plan=prepare(service,level,new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,changes,BinaryData.EMPTY,BinaryData.EMPTY));changes.clear();changes.addAll(plan.changes());
    }
    public static List<ItemStack> drops(PieceRecord p){boolean primary=!"upper".equals(p.sourceState().get("half"))&&!"head".equals(p.sourceState().get("part"));return primary?List.of(new ItemStack(NativeStates.resolve(p).getBlock().asItem())):List.of();}
    public static void use(WorldService service,ServerPlayer player,PieceRecord p)throws IOException{
        if(bed(p)){sleep(service,player,p);return;}
        if((p.sourceBlockId().value().equals("minecraft:iron_door")||p.sourceBlockId().value().equals("minecraft:iron_trapdoor")))return;
        boolean open=!Boolean.parseBoolean(p.sourceState().getOrDefault("open","false"));setOpen(service,player,p,open,false);
    }
    public static void setOpen(WorldService service,ServerPlayer player,PieceRecord p,boolean open,boolean powered)throws IOException{
        var changes=new ArrayList<OperationPlan.Change>();for(var id:registry(service.snapshot()).members(p.pieceId())){
            var old=service.snapshot().records().get(id);var values=new HashMap<>(old.sourceState());values.put("open",Boolean.toString(open));values.put("powered",Boolean.toString(powered));if(values.equals(old.sourceState()))continue;var s=NativeStates.resolve(old.sourceBlockId(),values);var nativePiece=capture(s,service.level(),WorldService.pos(old.origin16().cell()),old.pieceId(),old.groupId().orElse(null));var next=new PieceRecord(old.pieceId(),Math.incrementExact(old.revision()),old.adapterId(),1,old.sourceBlockId(),values,old.origin16(),old.orientation(),old.pivot16(),nativePiece.geometry(),old.geometryPayload(),old.materialPayload(),old.itemPayload(),old.groupId(),old.flags());
            PivotSweep.validate(service,old,next,registry(service.snapshot()).members(old.pieceId()));changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.of(next)));
        }
        if(changes.isEmpty())return;if(player==null)service.executeWorld(changes,List.of());else{var inventory=NativeInventory.encode(player,NativeInventory.copy(player));service.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.EDIT,changes,inventory,inventory));}
    }
    public static Optional<PieceRecord> bedAt(BlockGetter world,BlockPos pos){if(!(world.getBlockEntity(pos) instanceof CompositeCellEntity cell)||!cell.valid())return Optional.empty();var beds=cell.view().replicas().stream().filter(WorldGroups::bed).filter(p->"head".equals(p.sourceState().get("part"))&&Direction6.UP.rotated(p.orientation())==Direction6.UP&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)).toList();return beds.size()==1?Optional.of(beds.getFirst()):Optional.empty();}
    public static Direction bedDirection(PieceRecord p){return Direction.valueOf(Direction6.valueOf(p.sourceState().get("facing").toUpperCase(Locale.ROOT)).rotated(p.orientation()).name());}
    public static void occupied(Level world,BlockPos pos,boolean occupied){if(!(world instanceof ServerLevel level))return;var service=NmgRuntime.existing(level);var found=bedAt(world,pos);if(service==null||found.isEmpty())return;try{setOccupied(service,found.orElseThrow(),occupied);}catch(IOException ex){throw new UncheckedIOException(ex);}}
    private static void sleep(WorldService service,ServerPlayer player,PieceRecord selected)throws IOException{
        var head=registry(service.snapshot()).members(selected.pieceId()).stream().map(service.snapshot().records()::get).filter(p->"head".equals(p.sourceState().get("part"))).findFirst().orElseThrow();var pos=WorldService.pos(head.origin16().cell());if(bedAt(player.level(),pos).filter(p->p.pieceId().equals(head.pieceId())).isEmpty())throw new IllegalArgumentException("Bed has no accessible horizontal sleeping surface");
        var rule=player.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.BED_RULE,pos);if(rule.explodes()){service.removeWorld(Set.of(head.pieceId()),false);var center=new Vec3(head.origin16().x()/16.0+0.5,head.origin16().y()/16.0+0.5,head.origin16().z()/16.0+0.5);player.level().explode(null,player.level().damageSources().badRespawnPointExplosion(center),null,center,5,true,Level.ExplosionInteraction.BLOCK);return;}
        if(Boolean.parseBoolean(head.sourceState().getOrDefault("occupied","false"))){player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("block.minecraft.bed.occupied"));return;}
        player.startSleepInBed(pos).ifLeft(problem->{if(problem.message()!=null)player.sendOverlayMessage(problem.message());}).ifRight(unit->player.setPos(head.origin16().x()/16.0+0.5,head.origin16().y()/16.0+0.6875,head.origin16().z()/16.0+0.5));
    }
}
