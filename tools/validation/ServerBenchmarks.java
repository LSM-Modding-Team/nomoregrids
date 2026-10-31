package validation;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.diagnostic.MeasurementWindow;
import net.nicomar.nomoregrids.network.CellReplication;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.platform.*;

/** Explicit test-world benchmark controller. One bounded transaction per build/cleanup turn. No chunk forcing. */
final class ServerBenchmarks {
    private enum Phase { ENCLOSURE,BUILD,WET,SOURCES,WARMUP,MEASURE,COMPLETE,CLEAN_SOURCES,CLEAN,CLEAN_ENCLOSURE,FAILED,CLEANED }
    private static final Map<ServerLevel,Run> RUNS=new IdentityHashMap<>();
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final class Run {
        final ServerLevel level;final WorldService service;final BenchmarkScenes.Plan plan;
        final MeasurementWindow window;
        final Map<BlockPos,net.nicomar.nomoregrids.core.BinaryData> nativePbHashes=new HashMap<>();
        Phase phase=Phase.BUILD;int cursor;long tickStart,started,startedEpochMillis,finished,finishedEpochMillis;boolean fixturesVerified;JsonObject result;net.nicomar.nomoregrids.diagnostic.TrafficMeter.Snapshot trafficStart;
        Run(ServerLevel level,BenchmarkScenes.Plan plan)throws Exception{this.level=level;this.service=NmgRuntime.require(level);this.plan=plan;window=new MeasurementWindow(plan.scene().contains("wall10000")||plan.scene().contains("dense4096")?1200:200,1200);if(!plan.enclosure().isEmpty())phase=Phase.ENCLOSURE;}
    }
    static void register(){
        NeoForge.EVENT_BUS.addListener(ServerBenchmarks::commands);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,ServerBenchmarks::pre);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,ServerBenchmarks::post);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e)->{
            for(var run:List.copyOf(RUNS.values()))if(run.level.getServer()==e.getServer()){if(run.phase!=Phase.COMPLETE&&run.phase!=Phase.CLEANED)fail(run,new IllegalStateException("Server stopped before completion"));RUNS.remove(run.level);}
        });
    }
    private static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg_bench").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER))
            .then(Commands.literal("start").then(Commands.argument("scene",StringArgumentType.word()).then(Commands.argument("anchor",BlockPosArgument.blockPos()).executes(ctx->{
                try{var level=ctx.getSource().getLevel();checkProfile(level);if(RUNS.containsKey(level))throw new IllegalStateException("Clean the previous run first");var service=NmgRuntime.require(level);
                    if(!service.snapshot().records().isEmpty()||!service.effects().isEmpty()||service.journalBytes()!=0)throw new IllegalStateException("Use a fresh disposable world per scene");
                    var plan=BenchmarkScenes.create(level,StringArgumentType.getString(ctx,"scene"),UUID.randomUUID(),BlockPosArgument.getLoadedBlockPos(ctx,"anchor"));
                    for(var p:plan.pieces()){
                        for(var c:p.worldOccupation().cells(8))if(!level.getBlockState(WorldService.pos(c)).isAir())throw new IllegalStateException("Scene must be clear native air: "+c);
                        if(fluidScene(plan.scene())){var above=WorldService.pos(p.origin16().cell()).above();BenchmarkScenes.requirePosition(level,above);if(!level.getBlockState(above).isAir())throw new IllegalStateException("Fluid source fixture needs clear air above");}
                    }
                    for(var wall:plan.enclosure())if(!level.getBlockState(wall).isAir())throw new IllegalStateException("Enclosure must be clear air: "+wall);
                    var run=new Run(level,plan);RUNS.put(level,run);manifest(run);ctx.getSource().sendSuccess(()->Component.literal("NMG benchmark queued: "+plan.run()+". BUILD is excluded; scene-dependent warmup + 1200 measured ticks."),false);return 1;
                }catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
            }))))
            .then(Commands.literal("status").executes(ctx->{var run=RUNS.get(ctx.getSource().getLevel());ctx.getSource().sendSuccess(()->Component.literal(run==null?"No active benchmark":run.plan.run()+" "+run.phase+" "+run.cursor+"/"+run.plan.pieces().size()),false);return 1;}))
            .then(Commands.literal("disk").executes(ctx->{
                try{var level=ctx.getSource().getLevel();checkProfile(level);var run=RUNS.get(level);if(run==null||run.phase!=Phase.COMPLETE)throw new IllegalStateException("A complete measurement is required before the separate save/disk observation");disk(run);ctx.getSource().sendSuccess(()->Component.literal("Save flushed and logical disk sizes recorded outside the tick window"),false);return 1;}
                catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
            }))
            .then(Commands.literal("clean").executes(ctx->{var run=RUNS.get(ctx.getSource().getLevel());if(run==null)return 0;run.phase=fluidScene(run.plan.scene())?Phase.CLEAN_SOURCES:Phase.CLEAN;run.cursor=0;return 1;}))
            .then(Commands.literal("resume_clean").then(Commands.argument("run",StringArgumentType.word()).executes(ctx->{
                try{var level=ctx.getSource().getLevel();checkProfile(level);if(RUNS.containsKey(level))throw new IllegalStateException("Run already attached");var id=UUID.fromString(StringArgumentType.getString(ctx,"run"));var path=root(level).resolve(id+"-manifest.json");if(Files.size(path)>2*1024*1024)throw new IllegalArgumentException("Manifest size");var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    if(json.get("schema").getAsInt()!=1||!json.get("run").getAsString().equals(id.toString()))throw new IllegalArgumentException("Manifest identity/schema");var a=json.getAsJsonArray("anchor");var plan=BenchmarkScenes.create(level,json.get("scene").getAsString(),id,new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()));var run=new Run(level,plan);run.phase=fluidScene(plan.scene())?Phase.CLEAN_SOURCES:Phase.CLEAN;
                    if(json.has("nativePbHashes"))for(var row:json.getAsJsonArray("nativePbHashes")){var v=row.getAsJsonObject();run.nativePbHashes.put(new BlockPos(v.get("x").getAsInt(),v.get("y").getAsInt(),v.get("z").getAsInt()),new BinaryData(java.util.HexFormat.of().parseHex(v.get("hash").getAsString())));}
                    RUNS.put(level,run);return 1;
                }catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
            }))));
    }
    static void checkProfile(ServerLevel level){
        if(!Boolean.getBoolean("nomoregrids.validation"))throw new IllegalStateException("Validation profile is disabled");
        var path=level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if(java.util.stream.StreamSupport.stream(path.spliterator(),false).noneMatch(p->p.toString().startsWith("validation-")))throw new IllegalStateException("World must be under a validation-* directory");
    }
    private static void pre(ServerTickEvent.Pre e){for(var run:RUNS.values())if(run.level.getServer()==e.getServer())run.tickStart=System.nanoTime();}
    private static void post(ServerTickEvent.Post e){
        for(var run:List.copyOf(RUNS.values()))if(run.level.getServer()==e.getServer())try{
            if(run.phase==Phase.ENCLOSURE||run.phase==Phase.CLEAN_ENCLOSURE){enclosure(run,run.phase==Phase.CLEAN_ENCLOSURE);continue;}
            if(run.phase==Phase.BUILD){build(run);continue;}
            if(run.phase==Phase.WET){water(run);continue;}
            if(run.phase==Phase.SOURCES||run.phase==Phase.CLEAN_SOURCES){sources(run,run.phase==Phase.CLEAN_SOURCES);continue;}
            if(run.phase==Phase.CLEAN){clean(run);continue;}
            if(run.phase!=Phase.WARMUP&&run.phase!=Phase.MEASURE)continue;
            for(var chunk:run.plan.loadedChunks())if(!run.level.isLoaded(new BlockPos(chunk.x()*16,run.plan.anchor().getY(),chunk.z()*16)))throw new IllegalStateException("Benchmark chunk unloaded: "+chunk);
            if(!run.window.warmingUp()&&run.started==0){run.started=run.tickStart;run.startedEpochMillis=System.currentTimeMillis();run.trafficStart=CellReplication.traffic(run.level);}
            boolean complete=run.window.add(System.nanoTime()-run.tickStart);
            run.phase=run.window.warmingUp()?Phase.WARMUP:Phase.MEASURE;
            if(complete){run.finished=System.nanoTime();run.finishedEpochMillis=System.currentTimeMillis();verifyFixtures(run);run.fixturesVerified=true;run.phase=Phase.COMPLETE;result(run,null);}
        }catch(Exception ex){fail(run,ex);}
    }
    private static boolean nativeScene(Run run){return run.plan.scene().startsWith("native_");}
    private static boolean fluidScene(String name){return name.contains("fluids");}
    private static void build(Run run)throws Exception{
        var changes=new ArrayList<OperationPlan.Change>();int end=Math.min(run.cursor+64,run.plan.pieces().size());
        for(int i=run.cursor;i<end;i++){
            var p=run.plan.pieces().get(i);
            if(!nativeScene(run)){changes.add(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p)));continue;}
            var at=WorldService.pos(p.origin16().cell());var state=NativeStates.resolve(p);if(fluidScene(run.plan.scene()))state=state.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,true);
            if(!run.level.getBlockState(at).isAir()||!run.level.setBlock(at,state,3))throw new IllegalStateException("Native fixture changed/rejected: "+at);
            if(run.plan.scene().equals("native_pb256")){
                var be=run.level.getBlockEntity(at);var adapter=new net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter(ServerBenchmarks.class.getClassLoader());Object design=adapter.restore(p.geometryPayload());
                be.getClass().getMethod("setDesign",design.getClass()).invoke(be,design);run.nativePbHashes.put(at,WorldFunctionals.nativeDigest(run.level,be));
            }
        }
        if(!changes.isEmpty())run.service.executeWorld(changes,List.of());run.cursor=end;manifest(run);
        if(end==run.plan.pieces().size()){
            run.cursor=0;run.phase=fluidScene(run.plan.scene())?(nativeScene(run)?Phase.SOURCES:Phase.WET):Phase.WARMUP;
            net.nicomar.nomoregrids.NoMoreGrids.LOGGER.info("NMG benchmark {} built: {} fixtures, {} chunks",run.plan.run(),end,run.plan.chunks());
        }
    }
    private static void water(Run run)throws Exception{
        int end=Math.min(run.cursor+64,run.plan.pieces().size());var effects=new ArrayList<OperationPlan.Effect>();
        for(int i=run.cursor;i<end;i++){
            var p=run.plan.pieces().get(i);var cell=p.origin16().cell();var mask=net.nicomar.nomoregrids.simulation.fluid.CavityCell.blocked(p.geometry().occupation());var water=new byte[4096];for(int n=0;n<4096;n++)if(!mask.get(n))water[n]=1;
            var previous=run.service.fluids().at(cell);if(previous.filter(c->c.amount(net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.LAVA)>0).isPresent())throw new IllegalStateException("Foreign lava in fixture; original retained");
            var next=new net.nicomar.nomoregrids.simulation.fluid.CavityCell(mask,water,net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.EMPTY);
            effects.add(new OperationPlan.Effect(WorldFluids.key(cell),cell,previous.map(net.nicomar.nomoregrids.persistence.CavityCodec::encode),Optional.of(net.nicomar.nomoregrids.persistence.CavityCodec.encode(next))));
        }
        run.service.executeWorld(List.of(),effects);run.cursor=end;manifest(run);if(end==run.plan.pieces().size()){run.cursor=0;run.phase=Phase.SOURCES;}
    }
    private static void sources(Run run,boolean clean)throws Exception{
        int end=Math.min(run.cursor+64,run.plan.pieces().size());
        for(int i=run.cursor;i<end;i++){
            var at=WorldService.pos(run.plan.pieces().get(i).origin16().cell()).above();if(!run.level.isLoaded(at))throw new IllegalStateException("Source chunk unloaded");
            if(clean&&run.level.getBlockState(at).isAir())continue;
            var expected=clean?net.minecraft.world.level.block.Blocks.WATER.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            if(!run.service.snapshot().at(new CellPos(at.getX(),at.getY(),at.getZ())).isEmpty()||!run.level.getBlockState(at).equals(expected))throw new IllegalStateException("Native source fixture changed; retained: "+at);
            if(!run.level.setBlock(at,(clean?net.minecraft.world.level.block.Blocks.AIR:net.minecraft.world.level.block.Blocks.WATER).defaultBlockState(),3))throw new IllegalStateException("Source write rejected");
        }
        run.cursor=end;manifest(run);if(end==run.plan.pieces().size()){run.cursor=0;run.phase=clean?Phase.CLEAN:Phase.WARMUP;}
    }
    private static void clean(Run run)throws Exception{
        int end=Math.min(run.cursor+32,run.plan.pieces().size());var changes=new ArrayList<OperationPlan.Change>();
        for(int i=run.cursor;i<end;i++){
            var expected=run.plan.pieces().get(i);
            if(nativeScene(run)){
                var cell=expected.origin16().cell();var at=WorldService.pos(cell);var state=NativeStates.resolve(expected);if(fluidScene(run.plan.scene()))state=state.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,true);
                if(run.level.getBlockState(at).isAir())continue;
                if(!run.service.snapshot().at(cell).isEmpty()||!run.level.getBlockState(at).equals(state))throw new IllegalStateException("Foreign native fixture edit; retained: "+at);
                if(run.plan.scene().equals("native_pb256")){var be=run.level.getBlockEntity(at);var hash=run.nativePbHashes.get(at);if(hash==null||be==null||!WorldFunctionals.matchesDigest(run.level,be,hash))throw new IllegalStateException("PB image changed or unrecorded; retained: "+at);}
                if(!run.level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3))throw new IllegalStateException("Native cleanup rejected");continue;
            }
            var actual=run.service.snapshot().records().get(expected.pieceId());if(actual==null)continue;if(!actual.equals(expected))throw new IllegalStateException("Fixture changed externally; original retained: "+expected.pieceId());changes.add(new OperationPlan.Change(actual.pieceId(),Optional.of(actual),Optional.empty()));
        }
        if(!changes.isEmpty())run.service.executeWorld(changes,List.of());run.cursor=end;manifest(run);
        if(end==run.plan.pieces().size()){if(!run.plan.enclosure().isEmpty()){run.phase=Phase.CLEAN_ENCLOSURE;run.cursor=0;}else{run.phase=Phase.CLEANED;RUNS.remove(run.level);}manifest(run);}
    }
    private static void enclosure(Run run,boolean clean)throws Exception{
        int end=Math.min(run.cursor+64,run.plan.enclosure().size());
        for(int i=run.cursor;i<end;i++){
            var at=run.plan.enclosure().get(i);var current=run.level.getBlockState(at);if(clean&&current.isAir())continue;
            var expected=(clean?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState();
            if(!run.level.isLoaded(at)||!run.service.snapshot().at(new CellPos(at.getX(),at.getY(),at.getZ())).isEmpty()||!current.equals(expected))throw new IllegalStateException("Enclosure changed; retained: "+at);
            if(!run.level.setBlock(at,(clean?net.minecraft.world.level.block.Blocks.AIR:net.minecraft.world.level.block.Blocks.STONE).defaultBlockState(),3))throw new IllegalStateException("Enclosure write rejected");
        }
        run.cursor=end;manifest(run);if(end==run.plan.enclosure().size()){run.cursor=0;run.phase=clean?Phase.CLEANED:Phase.BUILD;manifest(run);if(clean)RUNS.remove(run.level);}
    }
    private static Path root(ServerLevel level){return level.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids/benchmarks");}
    private static JsonObject header(Run run){var json=new JsonObject();json.addProperty("schema",1);json.addProperty("run",run.plan.run().toString());json.addProperty("scene",run.plan.scene());json.addProperty("phase",run.phase.name());json.addProperty("fixtures",run.plan.pieces().size());json.addProperty("authoritativePieces",run.service.snapshot().records().size());json.addProperty("profileHash",net.nicomar.nomoregrids.network.SessionPayload.fingerprint());json.addProperty("minecraft","26.2");json.addProperty("neoforge","26.2.0.88");var mods=new TreeMap<String,String>();net.neoforged.fml.ModList.get().getMods().forEach(m->mods.put(m.getModId(),m.getVersion().toString()));json.add("mods",GSON.toJsonTree(mods));json.addProperty("chunks",run.plan.chunks());var a=new JsonArray();a.add(run.plan.anchor().getX());a.add(run.plan.anchor().getY());a.add(run.plan.anchor().getZ());json.add("anchor",a);return json;}
    private static void manifest(Run run)throws Exception{var json=header(run);json.addProperty("cursor",run.cursor);var hashes=new JsonArray();for(var e:run.nativePbHashes.entrySet()){var row=new JsonObject();row.addProperty("x",e.getKey().getX());row.addProperty("y",e.getKey().getY());row.addProperty("z",e.getKey().getZ());row.addProperty("hash",java.util.HexFormat.of().formatHex(e.getValue().copy()));hashes.add(row);}json.add("nativePbHashes",hashes);SectionStore.atomic(root(run.level).resolve(run.plan.run()+"-manifest.json"),GSON.toJson(json).getBytes(StandardCharsets.UTF_8));}
    private static void result(Run run,Exception error)throws Exception{
        var json=header(run);json.add("tickWindow",GSON.toJsonTree(run.window.summary()));json.addProperty("elapsedNanos",run.started==0?0:(run.finished==0?System.nanoTime():run.finished)-run.started);json.addProperty("error",error==null?null:error.toString());json.addProperty("acceptanceCertified",false);json.addProperty("startedEpochMillis",run.startedEpochMillis);json.addProperty("finishedEpochMillis",run.finishedEpochMillis==0?System.currentTimeMillis():run.finishedEpochMillis);json.addProperty("fixturesVerified",run.fixturesVerified);json.add("hardware",GSON.toJsonTree(net.nicomar.nomoregrids.diagnostic.MeasurementContext.snapshot()));
        json.addProperty("java",System.getProperty("java.version"));json.addProperty("os",System.getProperty("os.name"));json.addProperty("processors",Runtime.getRuntime().availableProcessors());json.addProperty("heapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());json.addProperty("heapMaxBytes",Runtime.getRuntime().maxMemory());json.addProperty("players",run.level.getServer().getPlayerCount());json.addProperty("activeCavitiesAtEnd",run.service.fluids().snapshot().size());
        json.add("publicCellTrafficStart",GSON.toJsonTree(run.trafficStart));json.add("publicCellTrafficEnd",GSON.toJsonTree(CellReplication.traffic(run.level)));json.addProperty("journalBytes",run.service.journalBytes());json.add("subsystems",run.service.metrics());json.add("checkpoint",GSON.toJsonTree(run.service.compactionStatus()));
        SectionStore.atomic(root(run.level).resolve(run.plan.run()+"-result.json"),GSON.toJson(json).getBytes(StandardCharsets.UTF_8));run.result=json;
    }
    private static void verifyFixtures(Run run)throws Exception{
        var records=run.service.snapshot().records();if(records.size()!=(nativeScene(run)?0:run.plan.pieces().size()))throw new IllegalStateException("Authority count changed during measurement");
        for(var expected:run.plan.pieces()){
            if(!nativeScene(run)){if(!expected.equals(records.get(expected.pieceId())))throw new IllegalStateException("Fixture identity/state changed: "+expected.pieceId());continue;}
            var at=WorldService.pos(expected.origin16().cell());var state=NativeStates.resolve(expected);if(fluidScene(run.plan.scene()))state=state.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,true);
            if(!run.level.getBlockState(at).equals(state))throw new IllegalStateException("Native fixture state changed: "+at);
            if(run.plan.scene().equals("native_pb256")){var hash=run.nativePbHashes.get(at);var be=run.level.getBlockEntity(at);if(hash==null||be==null||!WorldFunctionals.matchesDigest(run.level,be,hash))throw new IllegalStateException("Native PB fixture changed: "+at);}
        }
        for(var at:run.plan.enclosure())if(!run.level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.STONE))throw new IllegalStateException("Enclosure changed: "+at);
        if(fluidScene(run.plan.scene()))for(var p:run.plan.pieces())if(!run.level.getBlockState(WorldService.pos(p.origin16().cell()).above()).equals(net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()))throw new IllegalStateException("Fluid source changed");
    }
    private static void disk(Run run)throws Exception{
        // Same save call as vanilla /save-all flush; never included in the timed interval.
        if(!run.level.getServer().saveEverything(true,true,true))throw new IllegalStateException("World flush did not succeed");
        var world=run.level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();var reports=root(run.level).toAbsolutePath().normalize();
        long total=0,nmg=0,files=0;var nmgRoot=world.resolve("nomoregrids");
        try(var paths=Files.walk(world)){
            var iterator=paths.iterator();while(iterator.hasNext()){
                var path=iterator.next();if(path.startsWith(reports)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))continue;
                if(++files>200000)throw new IllegalStateException("Disk observation file budget exceeded");
                long size=Files.size(path);total=Math.addExact(total,size);if(path.startsWith(nmgRoot))nmg=Math.addExact(nmg,size);
            }
        }
        var json=header(run);json.addProperty("kind","world-disk-observation");json.addProperty("flushed",true);json.addProperty("observedEpochMillis",System.currentTimeMillis());json.addProperty("worldLogicalBytes",total);json.addProperty("nmgLogicalBytes",nmg);json.addProperty("files",files);json.addProperty("reportsExcluded",true);json.addProperty("filesystemAllocatedBytesMeasured",false);json.add("checkpoint",GSON.toJsonTree(run.service.compactionStatus()));
        SectionStore.atomic(reports.resolve(run.plan.run()+"-disk.json"),GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }
    private static void fail(Run run,Exception ex){run.phase=Phase.FAILED;try{result(run,ex);manifest(run);}catch(Exception saving){ex.addSuppressed(saving);}net.nicomar.nomoregrids.NoMoreGrids.LOGGER.error("NMG benchmark failed; fixtures and journal retained",ex);}
}
