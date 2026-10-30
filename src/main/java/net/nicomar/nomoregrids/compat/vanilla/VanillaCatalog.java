package net.nicomar.nomoregrids.compat.vanilla;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Generated explicit vanilla IDs plus runtime restrictions, never class inheritance alone. */
public final class VanillaCatalog {
    private final Set<String> ids;
    public VanillaCatalog(){
        try(var input=VanillaCatalog.class.getResourceAsStream("/data/nomoregrids/piece_profiles/vanilla.json")){
            if(input==null)throw new IOException("Missing vanilla manifest");var array=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("blocks");
            Set<String> values=new HashSet<>();for(var id:array)if(!values.add(id.getAsString()))throw new IOException("Duplicate vanilla ID");ids=Set.copyOf(values);
        }catch(IOException|RuntimeException ex){throw new IllegalStateException("Invalid vanilla manifest",ex);}
    }
    public boolean eligible(BlockState state){return ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())&&!state.hasBlockEntity()&&!state.isRandomlyTicking()&&state.getFluidState().isEmpty();}
    public Set<String> identifiers(){return ids;}
}
