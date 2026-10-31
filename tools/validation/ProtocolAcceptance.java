package validation;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.persistence.SectionStore;

/** Uses the real registered codec and registry-aware buffers. Does not claim socket or resync coverage. */
final class ProtocolAcceptance {
    static void register(){NeoForge.EVENT_BUS.addListener(ProtocolAcceptance::commands);}
    private static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("nmg_protocol_test").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER)).executes(ctx->{
        boolean passed=false;Exception failure=null;
        try{ServerBenchmarks.checkProfile(ctx.getSource().getLevel());
            for(boolean oversizedBody:new boolean[]{false,true}){
                var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(128),ctx.getSource().getLevel().registryAccess());
                try{
                    buffer.writeUtf("minecraft:overworld",256);buffer.writeBlockPos(BlockPos.ZERO);buffer.writeBoolean(false);buffer.writeUUID(UUID.randomUUID());buffer.writeVarInt(oversizedBody?1:SnapshotAssembler.MAX+1);buffer.writeVarInt(0);buffer.writeByteArray(new byte[32]);buffer.writeVarInt(oversizedBody?SnapshotAssembler.CHUNK+1:0);
                    boolean rejected=false;try{CellFragmentPayload.CODEC.decode(buffer);}catch(IllegalArgumentException|io.netty.handler.codec.DecoderException ex){rejected=true;}
                    if(!rejected||buffer.capacity()>1024)throw new IllegalStateException("Oversized fragment accepted or unexpectedly allocated");
                }finally{buffer.release();}
            }
            passed=true;
        }catch(Exception ex){failure=ex;}
        var json=new JsonObject();json.addProperty("schema",1);json.addProperty("kind","protocol-codec-case");json.addProperty("case","N04");json.addProperty("passed",passed);json.addProperty("socketTested",false);json.addProperty("error",failure==null?null:failure.toString());
        try{var file=ctx.getSource().getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids/acceptance/protocol-"+UUID.randomUUID()+".json");SectionStore.atomic(file,new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8));ctx.getSource().sendSuccess(()->Component.literal("NMG codec report: "+file),false);}catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
        return passed?1:0;
    }));}
}
