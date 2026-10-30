package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Physical manifest hash plus persisted modes; transient target/alignment data is reset on entry. */
public record SessionPayload(int schema,String profileHash,boolean precise,boolean expanded,boolean adjusted) implements CustomPacketPayload {
    public static final Type<SessionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","session"));
    public SessionPayload{if(schema!=1||!profileHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Session schema/hash");}
    public static final StreamCodec<RegistryFriendlyByteBuf,SessionPayload> CODEC=new StreamCodec<>(){
        @Override public SessionPayload decode(RegistryFriendlyByteBuf b){int schema=b.readUnsignedByte();String hash=b.readUtf(64);int bits=b.readUnsignedByte();if((bits&~7)!=0)throw new IllegalArgumentException("Session modes");return new SessionPayload(schema,hash,(bits&1)!=0,(bits&2)!=0,(bits&4)!=0);}
        @Override public void encode(RegistryFriendlyByteBuf b,SessionPayload p){b.writeByte(p.schema);b.writeUtf(p.profileHash,64);b.writeByte((p.precise?1:0)|(p.expanded?2:0)|(p.adjusted?4:0));}
    };
    public static String localHash(){
        try(var input=SessionPayload.class.getResourceAsStream("/data/nomoregrids/piece_profiles/vanilla.json")){
            if(input==null)throw new java.io.IOException("Missing physics manifest");return java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(input.readAllBytes()));
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot hash physical profiles",ex);}
    }
    @Override public Type<SessionPayload> type(){return TYPE;}
}
