package net.nicomar.nomoregrids.persistence;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.nicomar.nomoregrids.network.ProtocolRules;

/** A world never silently changes its adapter interpretation or physical manifest. */
public final class WorldRules {
    private WorldRules(){}
    public static void verifyOrCreate(Path path,String profileHash,ProtocolRules rules)throws IOException{
        var expected=new JsonObject();expected.addProperty("schema",1);expected.addProperty("profileHash",profileHash);expected.addProperty("pieceSchema",rules.pieceSchema());expected.addProperty("adapterApi",rules.adapterApi());
        var adapters=new JsonObject();new java.util.TreeMap<>(rules.adapters()).forEach(adapters::addProperty);expected.add("adapters",adapters);
        var limits=new JsonObject();limits.addProperty("perCell",rules.limits().perCell());limits.addProperty("ownedPerSection",rules.limits().ownedPerSection());limits.addProperty("referencesPerSection",rules.limits().referencesPerSection());limits.addProperty("recordBytes",rules.recordBytes());limits.addProperty("snapshotBytes",rules.snapshotBytes());limits.addProperty("operationCells",rules.operationCells());expected.add("limits",limits);
        if(Files.exists(path)){
            if(Files.size(path)>16384)throw new IOException("World rules budget; original retained");
            try{
                var existing=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();var previous=existing.getAsJsonObject("adapters");
                var comparison=existing.deepCopy();comparison.remove("adapters");var interpretation=expected.deepCopy();interpretation.remove("adapters");
                if(!comparison.equals(interpretation)||previous.size()>64)throw new IOException("World rules mismatch; migration required and original retained");
                for(var e:previous.entrySet()){
                    new net.nicomar.nomoregrids.core.ResourceId(e.getKey());int version=e.getValue().getAsBigDecimal().intValueExact();if(version<1)throw new IOException("Invalid retained adapter version");
                    if(adapters.has(e.getKey())&&adapters.get(e.getKey()).getAsInt()!=version)throw new IOException("Adapter migration required; original retained");
                }
                // Missing dependencies keep their recorded version for conservative substitutes.
                // Newly installed adapters may extend the catalogue without reinterpreting old payloads.
                boolean changed=false;for(var e:adapters.entrySet())if(!previous.has(e.getKey())){previous.add(e.getKey(),e.getValue());changed=true;}
                if(previous.size()>64)throw new IOException("World adapter catalogue budget");
                if(changed)SectionStore.atomic(path,existing.toString().getBytes(StandardCharsets.UTF_8));
            }catch(RuntimeException ex){throw new IOException("Invalid world rules; original retained",ex);}
        }else SectionStore.atomic(path,expected.toString().getBytes(StandardCharsets.UTF_8));
    }
}
