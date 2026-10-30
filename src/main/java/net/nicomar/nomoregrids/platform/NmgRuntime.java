package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicomar.nomoregrids.NoMoreGrids;
import net.nicomar.nomoregrids.api.adapter.*;
import net.nicomar.nomoregrids.compat.vanilla.BoxAdapter;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter;
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Opt-in experimental world integration. Validation/certification remain separate from implementation. */
public final class NmgRuntime {
    private static final Map<ServerLevel,WorldService> SERVICES=new IdentityHashMap<>();
    private static final Set<ServerLevel> ENABLED=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ServerLevel,String> QUARANTINE=new IdentityHashMap<>();
    private static final Map<ServerLevel,Map<net.minecraft.world.level.ChunkPos,net.minecraft.world.level.chunk.LevelChunk>> LOADED=new IdentityHashMap<>();
    private static final Map<UUID,IntentLimiter> LIMITERS=new HashMap<>();
    private static final Map<UUID,Mining> MINING=new HashMap<>(),REPLACEMENTS=new HashMap<>();
    private record ChunkTask(ServerLevel level,net.minecraft.world.level.ChunkPos position){}
    private static final Set<ChunkTask> CHUNKS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<net.minecraft.server.MinecraftServer,net.nicomar.nomoregrids.diagnostic.TickWorkQueue<ChunkTask>> REPAIRS=new IdentityHashMap<>();
    private static final class Mining {
        final UUID piece;final long revision;final net.minecraft.world.item.ItemStack main,offhand;float progress;
        Mining(net.nicomar.nomoregrids.core.PieceRecord p,ServerPlayer actor,float initial){piece=p.pieceId();revision=p.revision();main=actor.getMainHandItem().copy();offhand=actor.getOffhandItem().copy();progress=initial;}
    }
    private NmgRuntime(){}
    public static void register(){NeoForge.EVENT_BUS.addListener(NmgRuntime::load);NeoForge.EVENT_BUS.addListener(NmgRuntime::unload);NeoForge.EVENT_BUS.addListener(NmgRuntime::chunkLoad);NeoForge.EVENT_BUS.addListener(NmgRuntime::chunkUnload);NeoForge.EVENT_BUS.addListener(NmgRuntime::login);NeoForge.EVENT_BUS.addListener(NmgRuntime::logout);NeoForge.EVENT_BUS.addListener(NmgRuntime::commands);NeoForge.EVENT_BUS.addListener(NmgRuntime::breaking);NeoForge.EVENT_BUS.addListener(NmgRuntime::rightClick);NeoForge.EVENT_BUS.addListener(NmgRuntime::leftClick);NeoForge.EVENT_BUS.addListener(NmgRuntime::playerTick);NeoForge.EVENT_BUS.addListener(NmgRuntime::serverTick);}
    public static WorldService require(ServerLevel level)throws IOException{
        if(!ENABLED.contains(level)&&!Boolean.getBoolean("nomoregrids.experimental"))throw new IOException("Experimental world editing is disabled");
        if(QUARANTINE.containsKey(level))throw new IOException(QUARANTINE.get(level));return open(level);
    }
    private static WorldService open(ServerLevel level)throws IOException{
        WorldService existing=SERVICES.get(level);if(existing!=null)return existing;
        var adapters=new AdapterRegistry();adapters.register(new BoxAdapter());
        if(ModList.get().isLoaded("paintableblocks"))try{adapters.register(new PaintableAdapter(NmgRuntime.class.getClassLoader()));}
        catch(ReflectiveOperationException|RuntimeException|LinkageError ex){NoMoreGrids.LOGGER.warn("PB adapter unavailable; data will be retained",ex);}
        adapters.freeze();var service=new WorldService(level,adapters);SERVICES.put(level,service);return service;
    }
    private static void load(LevelEvent.Load event){if(event.getLevel() instanceof ServerLevel level)level.getServer().execute(()->{
        try{open(level);}catch(IOException|RuntimeException ex){quarantine(level,ex);}
    });}
    private static void quarantine(ServerLevel level,Exception ex){QUARANTINE.put(level,Objects.toString(ex.getMessage(),"NMG recovery conflict"));NoMoreGrids.LOGGER.error("NMG recovery blocked; original data retained",ex);}
    private static void chunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level)CHUNKS.add(new ChunkTask(level,event.getChunk().getPos()));}
    private static void serverTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        long start=System.nanoTime();int done=0;for(var task:List.copyOf(CHUNKS)){
            if(task.level.getServer()!=event.getServer())continue;if(++done>32||System.nanoTime()-start>=2_000_000)break;CHUNKS.remove(task);
            var chunk=task.level.getChunkSource().getChunkNow(task.position.x(),task.position.z());if(chunk==null)continue;
            LOADED.computeIfAbsent(task.level,key->new HashMap<>()).put(task.position,chunk);
            if(QUARANTINE.containsKey(task.level))continue;
            try{
                var service=open(task.level);var cells=service.cellsInChunk(chunk).iterator();
                REPAIRS.computeIfAbsent(event.getServer(),key->new net.nicomar.nomoregrids.diagnostic.TickWorkQueue<>(4096)).submit(task,units->{
                    if(QUARANTINE.containsKey(task.level)||task.level.getChunkSource().getChunkNow(task.position.x(),task.position.z())!=chunk)return true;
                    try{for(int i=0;i<units&&cells.hasNext();i++)service.repairLoadedCells(List.of(cells.next()));return !cells.hasNext();}
                    catch(IOException|RuntimeException ex){quarantine(task.level,ex);return true;}
                });
            }catch(IOException|RuntimeException ex){quarantine(task.level,ex);}
        }
        var repairs=REPAIRS.get(event.getServer());if(repairs!=null){repairs.advance(2_000_000,16,System::nanoTime);if(repairs.pending()==0)REPAIRS.remove(event.getServer());}
    }
    private static void chunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)level.getServer().execute(()->{
        var task=new ChunkTask(level,event.getChunk().getPos());CHUNKS.remove(task);var repairs=REPAIRS.get(level.getServer());if(repairs!=null)repairs.cancel(task);var chunks=LOADED.get(level);if(chunks!=null)chunks.remove(event.getChunk().getPos());
    });}
    private static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player){NmgNetworking.joined(player);for(var level:List.copyOf(QUARANTINE.keySet())){
        var old=SERVICES.remove(level);if(old!=null)try{old.close();}catch(IOException ex){NoMoreGrids.LOGGER.error("Cannot close failed NMG service",ex);continue;}
        try{open(level);for(var chunk:LOADED.getOrDefault(level,Map.of()).keySet())CHUNKS.add(new ChunkTask(level,chunk));QUARANTINE.remove(level);}catch(IOException|RuntimeException ex){quarantine(level,ex);}
    }}}
    public static void action(ServerPlayer player,ActionPayload packet){
        if(!LIMITERS.computeIfAbsent(player.getUUID(),key->new IntentLimiter()).allow(System.nanoTime()))return;
        try{
            if(!NmgNetworking.ready(player.getUUID()))throw new IllegalArgumentException("INVALID_DATA: session handshake pending");
            var hit=LoadedRay.pick(player);if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)throw new IllegalArgumentException("No reachable surface");
            if(!player.level().isLoaded(block.getBlockPos()))throw new IllegalArgumentException("UNLOADED_REGION");
            String result="OK";var service=require(player.level());if(packet.intent()!=null)NmgNetworking.actionIntent(player.getUUID(),packet.intent());var mode=NmgNetworking.modes(player.getUUID());
            if(packet.intent()!=null){
                if(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell){var selected=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();if(packet.expectedPiece().isEmpty()||!packet.expectedPiece().orElseThrow().equals(selected.pieceId())||packet.expectedRevision()!=selected.revision())throw new IllegalArgumentException("STALE_REVISION");}
                else if(packet.expectedPiece().isPresent())throw new IllegalArgumentException("STALE_REVISION");
            }
            InteractionHand hand=packet.offhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            switch(packet.action()){
                case PLACE->{
                    if(mode.replace()&&!player.isCreative()){
                        if(!(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("No replacement component");
                        var p=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();var mining=REPLACEMENTS.get(player.getUUID());
                        if(mining==null||!mining.piece.equals(p.pieceId())||mining.revision!=p.revision()){mining=new Mining(p,player,0);REPLACEMENTS.put(player.getUUID(),mining);}
                        if(mining.progress<1)throw new IllegalArgumentException("PENDING_MINING: keep the target and correct main-hand tool");
                    }
                    service.place(player,hand,block,mode,packet.operation());REPLACEMENTS.remove(player.getUUID());
                }
                case PROBE->{if(!service.probe(player,hand,block,mode))result="PENDING_LOOT";}
                case REMOVE->{
                    if(!player.isCreative())throw new IllegalArgumentException("Survival removal uses native mining progress");
                    if(!(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("No component");
                    var piece=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();service.remove(player,piece.pieceId(),packet.operation());
                }
                case EDIT->{
                    if(!(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("No component");
                    var piece=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();service.editPaintable(player,hand,piece.pieceId(),block,packet.operation());
                }
                case LOCK->{
                    if(!(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("No component");
                    var piece=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();service.toggleConnections(player,piece.pieceId(),packet.operation());
                }
            }
            PacketDistributor.sendToPlayer(player,new ActionResultPayload(packet.operation(),true,result));
        }catch(IOException|RuntimeException ex){var service=SERVICES.get(player.level());if(service!=null&&service.quarantined())quarantine(player.level(),ex);String reason=reason(ex);PacketDistributor.sendToPlayer(player,new ActionResultPayload(packet.operation(),false,reason));if(packet.action()!=ActionPayload.Action.PROBE)player.sendOverlayMessage(Component.translatable("message.nomoregrids."+reason.toLowerCase(Locale.ROOT)));NoMoreGrids.LOGGER.debug("NMG rejected {}: {}",reason,ex.getMessage());}
    }
    private static String reason(Exception ex){
        if(ex instanceof net.nicomar.nomoregrids.world.IndexException index)return index.code().name();
        String text=Objects.toString(ex.getMessage(),"");for(String code:List.of("OVERLAP","NO_SUPPORT","UNLOADED_REGION","UNSUPPORTED_ADAPTER","STALE_REVISION","COMPLEXITY_LIMIT","ENTITY_BLOCKED","PROTECTED","INVENTORY_MISSING","INVALID_DATA","PENDING_MINING"))if(text.contains(code))return code;
        if(text.contains("disabled"))return "EXPERIMENTAL_DISABLED";if(text.contains("receipt")||text.contains("Recovery")||text.contains("quarantined"))return "RECOVERY_REQUIRED";
        if(text.contains("tool")||text.contains("Mining"))return "PENDING_MINING";if(text.contains("reachable")||text.contains("occluded")||text.contains("reach")||text.contains("line of sight"))return "UNREACHABLE";
        if(text.contains("adapter")||text.contains("metadata")||text.contains("construction item")||text.contains("component"))return "UNSUPPORTED_ADAPTER";return "INVALID_DATA";
    }
    private static void rightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;var level=player.level();
        if(QUARANTINE.containsKey(level)){event.setCanceled(true);player.sendOverlayMessage(Component.translatable("message.nomoregrids.recovery_required"));return;}
        var service=SERVICES.get(level);if(service==null)return;
        for(var pos:List.of(event.getPos(),event.getPos().relative(event.getHitVec().getDirection())))if(level.isLoaded(pos)&&!service.snapshot().at(new net.nicomar.nomoregrids.core.CellPos(pos.getX(),pos.getY(),pos.getZ())).isEmpty()&&!(level.getBlockEntity(pos) instanceof CompositeCellEntity)){
            event.setCanceled(true);try{service.repairLoadedCells(List.of(pos));}catch(IOException|RuntimeException ex){quarantine(level,ex);}return;
        }
    }
    private static void breaking(BreakBlockEvent event){
        if(event instanceof net.nicomar.nomoregrids.event.PieceBreakPermissionEvent)return;
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(QUARANTINE.containsKey(level)){event.setCanceled(true);return;}
        if(!(level.getBlockEntity(event.getPos()) instanceof CompositeCellEntity cell)){
            var service=SERVICES.get(level);if(service!=null&&!service.snapshot().at(new net.nicomar.nomoregrids.core.CellPos(event.getPos().getX(),event.getPos().getY(),event.getPos().getZ())).isEmpty()){
                event.setCanceled(true);try{service.repairLoadedCells(List.of(event.getPos()));}catch(IOException|RuntimeException ex){quarantine(level,ex);}}
            return;
        }
        event.setCanceled(true);
        if(!(event.getPlayer() instanceof ServerPlayer player))return;
        try{var picked=LoadedRay.pick(player);if(!(picked instanceof BlockHitResult hit)||!hit.getBlockPos().equals(event.getPos()))throw new IllegalArgumentException("No line of sight");
            var piece=ComponentSelection.select(player,cell.view(),NmgNetworking.modes(player.getUUID()).candidate()).orElseThrow();
            var mining=MINING.get(player.getUUID());
            if(!player.isCreative()&&(mining==null||!mining.piece.equals(piece.pieceId())||mining.revision!=piece.revision()||mining.progress<1))throw new IllegalArgumentException("Mining target/time changed");
            require(level).remove(player,piece.pieceId());MINING.remove(player.getUUID());
        }catch(IOException|RuntimeException ex){var service=SERVICES.get(level);if(service!=null&&service.quarantined())quarantine(level,ex);player.sendOverlayMessage(Component.translatable("message.nomoregrids."+reason(ex).toLowerCase(Locale.ROOT)));}
    }
    private static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)NmgNetworking.persist(player);UUID id=event.getEntity().getUUID();LIMITERS.remove(id);MINING.remove(id);REPLACEMENTS.remove(id);NmgNetworking.disconnected(id);}
    private static void leftClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(event.getAction()==net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.ABORT){MINING.remove(player.getUUID());return;}
        if(event.getAction()!=net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START)return;
        try{if(player.level().getBlockEntity(event.getPos()) instanceof CompositeCellEntity cell)ComponentSelection.select(player,cell.view(),NmgNetworking.modes(player.getUUID()).candidate()).filter(p->!p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE)).ifPresent(p->{
            float rate=NativeStates.resolve(p).getDestroyProgress(player,player.level(),event.getPos());MINING.put(player.getUUID(),new Mining(p,player,rate));});}
        catch(RuntimeException ex){MINING.remove(player.getUUID());}
    }
    private static void playerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        try{advanceMining(player,MINING,false);advanceMining(player,REPLACEMENTS,true);}catch(RuntimeException ex){MINING.remove(player.getUUID());REPLACEMENTS.remove(player.getUUID());}
    }
    private static void advanceMining(ServerPlayer player,Map<UUID,Mining> sessions,boolean replacement){
        Mining mining=sessions.get(player.getUUID());if(mining==null)return;
        var mode=NmgNetworking.modes(player.getUUID());var hit=LoadedRay.pick(player);
        if(replacement&&!mode.replace()||!net.minecraft.world.item.ItemStack.matches(mining.main,player.getMainHandItem())||!net.minecraft.world.item.ItemStack.matches(mining.offhand,player.getOffhandItem())){sessions.remove(player.getUUID());return;}
        if(!(hit instanceof BlockHitResult block)||!(player.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell)){sessions.remove(player.getUUID());return;}
        var piece=ComponentSelection.select(player,cell.view(),mode.candidate());
        if(piece.isEmpty()||!piece.orElseThrow().pieceId().equals(mining.piece)||piece.orElseThrow().revision()!=mining.revision){sessions.remove(player.getUUID());return;}
        float rate=NativeStates.resolve(piece.orElseThrow()).getDestroyProgress(player,player.level(),block.getBlockPos());if(Float.isFinite(rate)&&rate>=0)mining.progress=Math.min(1,mining.progress+rate);
    }
    private static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){CHUNKS.removeIf(task->task.level==level);var repairs=REPAIRS.get(level.getServer());if(repairs!=null){repairs.cancelIf(task->task.level==level);if(repairs.pending()==0)REPAIRS.remove(level.getServer());}ENABLED.remove(level);QUARANTINE.remove(level);LOADED.remove(level);var service=SERVICES.remove(level);if(service!=null)try{service.close();}catch(IOException ex){NoMoreGrids.LOGGER.error("Cannot close NMG journal",ex);}}}
    private static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg")
            .then(Commands.literal("status").executes(ctx->{var service=SERVICES.get(ctx.getSource().getLevel());ctx.getSource().sendSuccess(()->Component.literal("No More Grids: experimental, unverified; "+(service==null?"not opened":service.snapshot().records().size()+" pieces")),false);return 1;}))
            .then(Commands.literal("enable").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_OWNER)).executes(ctx->{ENABLED.add(ctx.getSource().getLevel());ctx.getSource().sendSuccess(()->Component.literal("NMG experimental editing enabled for this session; not certified"),true);return 1;}))
            .then(Commands.literal("inspect").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).executes(ctx->diagnostic(ctx.getSource(),"inspect")))
            .then(Commands.literal("validate").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).executes(ctx->diagnostic(ctx.getSource(),"validate")))
            .then(Commands.literal("report").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).executes(ctx->diagnostic(ctx.getSource(),"report")))
            .then(Commands.literal("recover").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).executes(ctx->diagnostic(ctx.getSource(),"recover")))
            .then(Commands.literal("export").requires(src->src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)).executes(ctx->diagnostic(ctx.getSource(),"export"))));
    }
    private static int diagnostic(net.minecraft.commands.CommandSourceStack source,String action){
        try{var service=SERVICES.get(source.getLevel());return switch(action){
            case "report"->NmgDiagnostics.report(source,service,QUARANTINE.containsKey(source.getLevel()));
            case "recover"->NmgDiagnostics.recoveryProposal(source,service,QUARANTINE.containsKey(source.getLevel()));
            case "inspect"->NmgDiagnostics.inspect(source,Objects.requireNonNull(service,"NMG authority unavailable"));
            case "validate"->NmgDiagnostics.validate(source,Objects.requireNonNull(service,"NMG authority unavailable"));
            case "export"->NmgDiagnostics.export(source,Objects.requireNonNull(service,"NMG authority unavailable"));
            default->throw new IllegalArgumentException("Unknown diagnostic");};
        }catch(IOException|RuntimeException ex){source.sendFailure(Component.literal("NMG diagnostic: "+Objects.toString(ex.getMessage(),"unavailable")));return 0;}
    }
}
