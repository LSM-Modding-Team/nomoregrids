package net.nicomar.nomoregrids.platform;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.world.*;

/** Administrator diagnostics. Reports and public design exports exclude resource/inventory receipts. */
public final class NmgDiagnostics {
    private NmgDiagnostics(){}
    public static int inspect(CommandSourceStack source,WorldService service){
        PieceRecord p=selected(source,service);source.sendSuccess(()->Component.literal(describe(p).toString()),false);return 1;
    }
    private static PieceRecord selected(CommandSourceStack source,WorldService service){
        var actor=source.getPlayer();if(actor==null)throw new IllegalArgumentException("Inspect/export requires a player target");
        var hit=LoadedRay.pick(actor);if(!(hit instanceof BlockHitResult block)||!actor.level().isLoaded(block.getBlockPos())||!(actor.level().getBlockEntity(block.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("No loaded NMG component in reach");
        var selected=ComponentSelection.select(actor,cell.view(),net.nicomar.nomoregrids.network.NmgNetworking.modes(actor.getUUID()).candidate()).orElseThrow();
        var piece=service.snapshot().records().get(selected.pieceId());if(piece==null||piece.revision()!=selected.revision())throw new IllegalArgumentException("STALE_REVISION");return piece;
    }
    private static JsonObject describe(PieceRecord p){
        var json=new JsonObject();json.addProperty("piece",p.pieceId().toString());json.addProperty("revision",p.revision());json.addProperty("source",p.sourceBlockId().value());json.addProperty("adapter",p.adapterId().value());json.addProperty("adapterVersion",p.adapterVersion());json.addProperty("orientation",p.orientation());
        var origin=new JsonArray();origin.add(p.origin16().x());origin.add(p.origin16().y());origin.add(p.origin16().z());json.add("origin16",origin);json.addProperty("occupationBoxes",p.geometry().occupation().boxes().size());json.addProperty("cells",p.worldOccupation().cells(8).size());json.addProperty("substitute",p.flags().contains(PieceRecord.Flag.SUBSTITUTE));return json;
    }
    public static int validate(CommandSourceStack source,WorldService service){
        int checked=0,invalid=0,unloaded=0;var index=service.snapshot();
        var ordered=index.records().values().stream().limit(4096).sorted(Comparator.comparing(PieceRecord::pieceId)).toList();
        for(var p:ordered){checked++;try{
            for(var cell:p.worldOccupation().cells(8)){
                if(!index.at(cell).contains(p.pieceId())||!index.referencedBy(cell.section()).contains(p.pieceId())||!index.ownedBy(p.ownerSection()).contains(p.pieceId()))throw new IllegalArgumentException("Reference conflict");
                BlockPos pos=WorldService.pos(cell);if(!source.getLevel().isLoaded(pos)){unloaded++;continue;}
                if(service.nativeProjection(cell).filter(source.getLevel().getBlockState(pos)::equals).isPresent())continue;if(!(source.getLevel().getBlockEntity(pos) instanceof CompositeCellEntity entity)||!entity.valid())throw new IllegalArgumentException("Missing/invalid replica");
                if(service.nativeProjection(cell).filter(source.getLevel().getBlockState(pos)::equals).isPresent())continue;var expected=service.cellView(cell,index);if(!expected.equals(entity.view()))throw new IllegalArgumentException("Replica revision conflict");
            }
            NativeStates.resolve(p);NativeStates.original(p);
        }catch(RuntimeException ex){invalid++;}}
        String result="NMG structural validation: checked="+checked+", conflicts="+invalid+", unloaded references="+unloaded+", deferred="+Math.max(0,index.records().size()-checked)+". Does not compile or run acceptance tests.";
        source.sendSuccess(()->Component.literal(result),false);return invalid==0?1:0;
    }
    public static int report(CommandSourceStack source,WorldService service,boolean quarantined)throws IOException{
        var json=header(source,"report");json.addProperty("quarantined",quarantined);json.addProperty("pieces",service==null?0:service.snapshot().records().size());
        var counts=new TreeMap<String,Integer>();if(service!=null)service.snapshot().records().values().forEach(p->counts.merge(p.adapterId().value(),1,Integer::sum));var adapters=new JsonObject();counts.forEach(adapters::addProperty);json.add("adapterPieceCounts",adapters);
        var limits=PieceIndex.Limits.DEFAULT;json.addProperty("perCell",limits.perCell());json.addProperty("ownersPerSection",limits.ownedPerSection());json.addProperty("referencesPerSection",limits.referencesPerSection());json.addProperty("journalCompaction",true);if(service!=null){json.addProperty("journalBytes",service.journalBytes());var status=service.compactionStatus();var checkpoint=new JsonObject();checkpoint.addProperty("running",status.running());checkpoint.addProperty("copiedSequence",status.copied());checkpoint.addProperty("failures",status.failures());checkpoint.addProperty("lastFailure",status.lastFailure());json.add("checkpoint",checkpoint);json.add("timings",service.metrics());var cache=net.nicomar.nomoregrids.diagnostic.GeometryCache.stats();json.addProperty("geometryCacheBytes",cache.bytes());json.addProperty("geometryCacheHits",cache.hits());json.addProperty("paintableCacheBytes",net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter.cacheStats().bytes());json.addProperty("circuitCacheBytes",service.circuits().cacheStats().bytes());}json.addProperty("javaValidated",false);
        return write(source,"report",json);
    }
    public static int recoveryProposal(CommandSourceStack source,WorldService service,boolean quarantined)throws IOException{
        var json=header(source,"recovery-proposal");json.addProperty("quarantined",quarantined||service!=null&&service.quarantined());json.addProperty("automaticMutation",false);
        var actions=new JsonArray();actions.add("Back up the complete world, including playerdata and nomoregrids, together.");actions.add("Keep original journal and section files; never truncate or delete an unknown tail.");actions.add("Reconnect the actor with a pending resource receipt; replay validates the native saved inventory receipt.");actions.add("Compare loaded replicas with authority using /nmg validate; unloaded chunks are never forced.");actions.add("Resolve incompatible adapter versions before enabling new edits.");json.add("proposal",actions);return write(source,"recovery-proposal",json);
    }
    public static int export(CommandSourceStack source,WorldService service)throws IOException{
        var p=selected(source,service);CellPos cell=p.worldOccupation().cells(8).first();var publicPiece=CellView.from(cell,service.snapshot()).replicas().stream().filter(q->q.pieceId().equals(p.pieceId())).findFirst().orElseThrow();
        var json=header(source,"public-design");json.add("component",describe(p));json.addProperty("format","nmg-public-cell-v1");json.addProperty("payload",Base64.getEncoder().encodeToString(new CellView(List.of(publicPiece)).encode()));json.addProperty("containsInventory",false);return write(source,"design",json);
    }
    private static JsonObject header(CommandSourceStack source,String kind){var json=new JsonObject();json.addProperty("schema",1);json.addProperty("kind",kind);json.addProperty("minecraft","26.2");json.addProperty("neoforge","26.2.0.88");json.addProperty("dimension",source.getLevel().dimension().identifier().toString());return json;}
    private static int write(CommandSourceStack source,String prefix,JsonObject json)throws IOException{
        Path file=source.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids/diagnostics").resolve(prefix+"-"+UUID.randomUUID()+".json");byte[] bytes=new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>2*1024*1024)throw new IOException("Diagnostic budget");SectionStore.atomic(file,bytes);source.sendSuccess(()->Component.literal("NMG "+prefix+": "+file.toAbsolutePath()),false);return 1;
    }
}
