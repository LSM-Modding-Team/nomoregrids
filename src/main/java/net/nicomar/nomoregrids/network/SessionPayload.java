package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Physical manifest hash plus persisted modes; transient target/alignment data is reset on entry. */
public record SessionPayload(int schema,String profileHash,ProtocolRules rules,boolean precise,boolean expanded,boolean adjusted) implements CustomPacketPayload {
    public static final Type<SessionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","session"));
    public SessionPayload{java.util.Objects.requireNonNull(rules);if(schema!=1||!profileHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Session schema/hash");}
    public static final StreamCodec<RegistryFriendlyByteBuf,SessionPayload> CODEC=new StreamCodec<>(){
        @Override public SessionPayload decode(RegistryFriendlyByteBuf b){
            int schema=b.readUnsignedByte();String hash=b.readUtf(64);int piece=b.readUnsignedByte(),api=b.readUnsignedByte(),count=b.readVarInt();if(count<1||count>64)throw new IllegalArgumentException("Adapter manifest budget");
            var adapters=new java.util.HashMap<String,Integer>();for(int i=0;i<count;i++)if(adapters.putIfAbsent(b.readUtf(256),b.readVarInt())!=null)throw new IllegalArgumentException("Duplicate adapter");
            var limits=new net.nicomar.nomoregrids.world.PieceIndex.Limits(b.readVarInt(),b.readVarInt(),b.readVarInt());
            var rules=new ProtocolRules(piece,api,adapters,limits,b.readVarInt(),b.readVarInt(),b.readVarInt());
            int bits=b.readUnsignedByte();if((bits&~7)!=0)throw new IllegalArgumentException("Session modes");return new SessionPayload(schema,hash,rules,(bits&1)!=0,(bits&2)!=0,(bits&4)!=0);
        }
        @Override public void encode(RegistryFriendlyByteBuf b,SessionPayload p){
            b.writeByte(p.schema);b.writeUtf(p.profileHash,64);b.writeByte(p.rules.pieceSchema());b.writeByte(p.rules.adapterApi());b.writeVarInt(p.rules.adapters().size());
            new java.util.TreeMap<>(p.rules.adapters()).forEach((id,version)->{b.writeUtf(id,256);b.writeVarInt(version);});
            b.writeVarInt(p.rules.limits().perCell());b.writeVarInt(p.rules.limits().ownedPerSection());b.writeVarInt(p.rules.limits().referencesPerSection());
            b.writeVarInt(p.rules.recordBytes());b.writeVarInt(p.rules.snapshotBytes());b.writeVarInt(p.rules.operationCells());b.writeByte((p.precise?1:0)|(p.expanded?2:0)|(p.adjusted?4:0));
        }
    };
    public static String localHash(){
        try(var input=SessionPayload.class.getResourceAsStream("/data/nomoregrids/piece_profiles/vanilla.json")){
            if(input==null)throw new java.io.IOException("Missing physics manifest");return java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(input.readAllBytes()));
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot hash physical profiles",ex);}
    }
    public static ProtocolRules localRules(){
        java.util.Map<String,Integer> versions=new java.util.HashMap<>();versions.put("nomoregrids:box",1);
        if(net.neoforged.fml.ModList.get().isLoaded("paintableblocks"))try{var adapter=new net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter(SessionPayload.class.getClassLoader());versions.put(adapter.id().value(),adapter.dataVersion());}catch(ReflectiveOperationException|RuntimeException ignored){}
        return ProtocolRules.defaults(versions);
    }
    public static String fingerprint(){
        var rules=localRules();String canonical=localHash()+"|"+rules.pieceSchema()+"|"+rules.adapterApi()+"|"+new java.util.TreeMap<>(rules.adapters())+"|"+rules.limits()+"|"+rules.recordBytes()+"|"+rules.snapshotBytes()+"|"+rules.operationCells();
        return java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Override public Type<SessionPayload> type(){return TYPE;}
}
