package validation;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nicomar.nomoregrids.compat.paintableblocks.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.event.BeforeOperationEvent;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.platform.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Synchronous assertions against a real world. Player menus, visuals and multi-tick physics stay separate cases. */
final class WorldAcceptance {
    private static final Set<String> CASES=Set.of("eight_cells","mixed_slabs","protection","barrels","doors","support","pb_quarter","stale_revision");
    static void register(){NeoForge.EVENT_BUS.addListener(WorldAcceptance::commands);}
    private static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg_accept").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER))
            .then(Commands.literal("run").then(Commands.argument("case",StringArgumentType.word()).then(Commands.argument("anchor",BlockPosArgument.blockPos()).executes(ctx->{
                String name=StringArgumentType.getString(ctx,"case");var level=ctx.getSource().getLevel();Fixture fixture=null;Exception failure=null;
                try{ServerBenchmarks.checkProfile(level);if(!CASES.contains(name))throw new IllegalArgumentException("Unknown case: "+name);fixture=new Fixture(level,BlockPosArgument.getLoadedBlockPos(ctx,"anchor"));
                    switch(name){case "eight_cells"->eight(fixture);case "mixed_slabs"->slabs(fixture);case "protection"->protection(fixture);case "barrels"->barrels(fixture);case "doors"->doors(fixture);case "support"->support(fixture);case "pb_quarter"->pb(fixture);case "stale_revision"->stale(fixture);default->throw new IllegalArgumentException(name);}
                    fixture.clean();
                }catch(Exception ex){failure=ex;}
                var report=new JsonObject();report.addProperty("schema",1);report.addProperty("kind","world-acceptance-probe");report.addProperty("case",name);report.addProperty("passed",failure==null);report.addProperty("error",failure==null?null:failure.toString());report.addProperty("clientGuiTested",false);report.addProperty("fixtureCleanupComplete",fixture!=null&&fixture.cleaned);
                try{var file=level.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids/acceptance/"+name+"-"+UUID.randomUUID()+".json");SectionStore.atomic(file,new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));ctx.getSource().sendSuccess(()->Component.literal("NMG case report: "+file),false);}catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
                if(failure!=null){ctx.getSource().sendFailure(Component.literal("NMG case failed; fixtures retained: "+failure));return 0;}return 1;
            })))));
    }
    private static final class Fixture {
        final ServerLevel level;final WorldService service;final BlockPos at;final Set<UUID> created=new HashSet<>();boolean cleaned;
        Fixture(ServerLevel level,BlockPos at)throws Exception{
            this.level=level;this.at=at;service=NmgRuntime.require(level);
            require((at.getX()&15)==0&&(at.getY()&15)==0&&(at.getZ()&15)==0,"Case anchor must be section aligned (owner migration case)");
            require(service.snapshot().records().isEmpty()&&service.fluids().snapshot().isEmpty(),"Use a fresh empty authority per case");
            for(int x=-2;x<=7;x++)for(int y=-1;y<=4;y++)for(int z=-2;z<=7;z++){var p=at.offset(x,y,z);BenchmarkScenes.requirePosition(level,p);require(level.getBlockState(p).isAir(),"Load/clear fixture area first: "+p);}
        }
        PieceRecord box(Block block,BlockPos pos){return NativeStates.capture(block.defaultBlockState(),level,pos,UUID.randomUUID());}
        void put(PieceRecord... pieces)throws Exception{var changes=new ArrayList<OperationPlan.Change>();var groups=new HashSet<UUID>();for(var p:pieces){created.add(p.pieceId());p.groupId().ifPresent(groups::add);changes.add(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p)));}service.executeWorld(changes,List.of());for(var p:service.snapshot().records().values())if(p.groupId().filter(groups::contains).isPresent())created.add(p.pieceId());}
        void remove(UUID id)throws Exception{service.removeWorld(Set.of(id),false);}
        void clean()throws Exception{var ids=new HashSet<>(service.snapshot().records().keySet());require(created.containsAll(ids),"Unexpected authority; no cleanup");service.removeWorld(ids,false);require(service.snapshot().records().isEmpty(),"Cleanup did not remove fixture pieces");cleaned=true;}
        void replicas(PieceRecord p){require(p.equals(service.snapshot().records().get(p.pieceId())),"Canonical mismatch");for(var c:p.worldOccupation().cells(8)){var be=level.getBlockEntity(WorldService.pos(c));require(be instanceof CompositeCellEntity entity&&entity.valid()&&entity.view().equals(service.cellView(c,service.snapshot())),"Replica mismatch: "+c);}}
    }
    private static PieceRecord shifted(PieceRecord p,GridPoint offset){return BenchmarkScenes.move(p,p.origin16().add(offset),p.revision());}
    private static void eight(Fixture f)throws Exception{var p=shifted(f.box(Blocks.STONE,f.at),new GridPoint(8,8,8));f.put(p);require(p.worldOccupation().cells(8).size()==8,"Expected eight cells");f.replicas(p);var moved=BenchmarkScenes.move(p,p.origin16().add(new GridPoint(16,0,0)),1);f.service.executeWorld(List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(moved))),List.of());f.replicas(moved);f.remove(p.pieceId());var oldAndNew=new HashSet<>(p.worldOccupation().cells(8));oldAndNew.addAll(moved.worldOccupation().cells(8));for(var c:oldAndNew)require(f.level.getBlockState(WorldService.pos(c)).isAir(),"Old replica retained");}
    private static void slabs(Fixture f)throws Exception{
        var lower=NativeStates.capture(Blocks.STONE_SLAB.defaultBlockState(),f.level,f.at,UUID.randomUUID());
        var upper=NativeStates.capture(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),f.level,f.at,UUID.randomUUID());f.put(lower,upper);f.replicas(lower);f.replicas(upper);
        long bytes=f.service.journalBytes();boolean rejected=false;
        try{f.put(f.box(Blocks.STONE,f.at));}catch(IllegalArgumentException ex){rejected=true;}
        require(rejected&&f.service.journalBytes()==bytes&&f.service.snapshot().records().size()==2,"Overlap changed authority or journal");f.remove(upper.pieceId());require(f.service.snapshot().records().containsKey(lower.pieceId()),"Removed neighbor");require(f.level.getBlockState(f.at).getBlock()==Blocks.STONE_SLAB,"Exact native restoration failed");
    }
    private static void protection(Fixture f)throws Exception{
        var p=shifted(f.box(Blocks.STONE,f.at),new GridPoint(8,0,0));var observed=new java.util.concurrent.atomic.AtomicBoolean();Consumer<BeforeOperationEvent> deny=e->{if(e.level()==f.level&&e.plan().changes().stream().anyMatch(c->c.id().equals(p.pieceId()))){observed.set(true);e.setCanceled(true);}};
        long bytes=f.service.journalBytes();NeoForge.EVENT_BUS.addListener(BeforeOperationEvent.class,deny);boolean rejected=false;
        try{f.put(p);}catch(IllegalArgumentException ex){rejected=true;}finally{NeoForge.EVENT_BUS.unregister(deny);}
        require(rejected&&observed.get()&&f.service.snapshot().records().isEmpty()&&f.service.journalBytes()==bytes,"Denied operation left a commit or did not reach protection");
    }
    private static void barrels(Fixture f)throws Exception{
        var a=shifted(WorldFunctionals.placement(Blocks.BARREL.defaultBlockState(),f.level,f.at,UUID.randomUUID()),new GridPoint(8,0,0));
        var b=shifted(WorldFunctionals.placement(Blocks.BARREL.defaultBlockState(),f.level,f.at.east(),UUID.randomUUID()),new GridPoint(8,0,0));f.put(a,b);
        var port=f.service.transfers().items(f.at,Direction.UP);require(port!=null,"Missing exposed port");require(f.service.transfers().items(f.at.east(),Direction.UP)==null,"Ambiguous host combined inventories");
        var item=ItemResource.of(new ItemStack(Items.DIAMOND));
        try(var tx=Transaction.openRoot()){require(port.insert(0,item,3,tx)==3,"Insert planning failed");}
        require(f.service.functionals().detached(a).getItem(0).isEmpty(),"Aborted transfer committed");
        try(var tx=Transaction.openRoot()){require(port.insert(0,item,3,tx)==3,"Insert failed");tx.commit();}
        require(f.service.functionals().detached(a).getItem(0).getCount()==3,"Committed transfer missing");require(f.service.functionals().detached(b).getItem(0).isEmpty(),"Second authority was changed");
        var copy=f.service.functionals().detached(a);copy.setItem(0,ItemStack.EMPTY);require(f.service.functionals().detached(a).getItem(0).getCount()==3,"Detached copy was authoritative");
        var moved=BenchmarkScenes.move(a,a.origin16().add(new GridPoint(-16,0,0)),1);f.service.executeWorld(List.of(new OperationPlan.Change(a.pieceId(),Optional.of(a),Optional.of(moved))),List.of());f.replicas(moved);require(f.service.functionals().detached(moved).getItem(0).getCount()==3,"Owner migration lost slots");
    }
    private static void doors(Fixture f)throws Exception{
        var floor=f.box(Blocks.STONE,f.at);f.put(floor);var door=WorldGroups.capture(Blocks.OAK_DOOR.defaultBlockState(),f.level,f.at.above(),UUID.randomUUID(),UUID.randomUUID());f.put(door);
        var ids=WorldGroups.registry(f.service.snapshot()).members(door.pieceId());require(ids.size()==2,"Door group incomplete");WorldGroups.setOpen(f.service,null,f.service.snapshot().records().get(door.pieceId()),true,false);
        for(var id:ids)require(Boolean.parseBoolean(f.service.snapshot().records().get(id).sourceState().get("open")),"Half did not open");f.remove(door.pieceId());for(var id:ids)require(!f.service.snapshot().records().containsKey(id),"Group member survived removal");
    }
    private static void support(Fixture f)throws Exception{
        var vertical=NativeStates.capture(Blocks.STONE_SLAB.defaultBlockState(),f.level,f.at,UUID.randomUUID());vertical=new PieceRecord(vertical.pieceId(),0,vertical.adapterId(),1,vertical.sourceBlockId(),vertical.sourceState(),vertical.origin16(),8,vertical.pivot16(),vertical.geometry(),vertical.geometryPayload(),vertical.materialPayload(),vertical.itemPayload(),vertical.groupId(),vertical.flags());f.put(vertical);
        var carpet=shifted(f.box(Blocks.CARPET.red(),f.at.above()),new GridPoint(8,0,0));boolean rejected=false;try{f.put(carpet);}catch(IllegalArgumentException ex){rejected=true;}require(rejected,"Half support accepted full-footprint carpet");f.remove(vertical.pieceId());
        f.put(f.box(Blocks.STONE,f.at),f.box(Blocks.STONE,f.at.east()));f.put(carpet);f.replicas(carpet);
        require(f.level.setBlock(f.at,Blocks.AIR.defaultBlockState(),3),"Native support removal failed");f.service.externalChanged(Set.of(new CellPos(f.at.getX(),f.at.getY(),f.at.getZ())));
        require(!f.service.snapshot().records().containsKey(carpet.pieceId()),"Unsupported carpet survived");require(f.service.effects().keySet().stream().anyMatch(k->k.startsWith("loot:")),"Support loss did not queue durable loot");
    }
    private static void pb(Fixture f)throws Exception{
        var bridge=PaintableBridge.available().orElseThrow();var adapter=new PaintableAdapter(WorldAcceptance.class.getClassLoader());var p=bridge.create(f.level,f.at,4);Object design=adapter.restore(p.geometryPayload());var removed=new BitSet();for(int i=0;i<64;i++)if(i/4%4>=1)removed.set(i);design=design.getClass().getMethod("remove",BitSet.class).invoke(design,removed);var data=adapter.capture(design);var state=new HashMap<>(p.sourceState());state.put("cut","true");p=new PieceRecord(p.pieceId(),0,p.adapterId(),1,p.sourceBlockId(),state,p.origin16(),p.orientation(),p.pivot16(),bridge.snapshot(data),data,p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());f.put(p);
        require(p.worldOccupation().bounds().max().y()==p.origin16().y()+4,"PB quarter height degraded");var cube=shifted(f.box(Blocks.STONE,f.at),new GridPoint(0,4,0));f.put(cube);f.replicas(cube);require(f.service.snapshot().records().get(p.pieceId()).adapterId().value().equals("nomoregrids:paintableblocks"),"PB identity changed");
    }
    private static void stale(Fixture f)throws Exception{
        var p=shifted(f.box(Blocks.STONE,f.at),new GridPoint(8,0,0));f.put(p);var moved=BenchmarkScenes.move(p,p.origin16().add(new GridPoint(0,16,0)),1);f.service.executeWorld(List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(moved))),List.of());long bytes=f.service.journalBytes();boolean rejected=false;
        try{f.service.executeWorld(List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.empty())),List.of());}catch(IllegalArgumentException ex){rejected=true;}
        require(rejected&&f.service.journalBytes()==bytes&&f.service.snapshot().records().get(p.pieceId()).equals(moved),"Stale request changed committed state");
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
