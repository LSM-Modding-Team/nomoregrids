package net.nicomar.nomoregrids.network;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Menu identity/tier only. Slot contents continue through the authorized native menu channel. */
public record FunctionalMenuPayload(UUID piece,BlockPos position,String block,Map<String,String> state) implements CustomPacketPayload {
    public static final Type<FunctionalMenuPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","functional_menu"));
    public FunctionalMenuPayload{position=position.immutable();state=Map.copyOf(state);if(block.length()>256||state.size()>32||state.entrySet().stream().anyMatch(e->e.getKey().length()>128||e.getValue().length()>128))throw new IllegalArgumentException("Menu context budget");}
    public static final StreamCodec<RegistryFriendlyByteBuf,FunctionalMenuPayload> CODEC=new StreamCodec<>(){
        public FunctionalMenuPayload decode(RegistryFriendlyByteBuf b){var id=b.readUUID();var pos=b.readBlockPos();var block=b.readUtf(256);int n=b.readVarInt();if(n<0||n>32)throw new IllegalArgumentException("Menu state budget");var props=new HashMap<String,String>();for(int i=0;i<n;i++)if(props.putIfAbsent(b.readUtf(128),b.readUtf(128))!=null)throw new IllegalArgumentException("Duplicate menu property");return new FunctionalMenuPayload(id,pos,block,props);}
        public void encode(RegistryFriendlyByteBuf b,FunctionalMenuPayload p){b.writeUUID(p.piece);b.writeBlockPos(p.position);b.writeUtf(p.block,256);b.writeVarInt(p.state.size());new TreeMap<>(p.state).forEach((key,value)->{b.writeUtf(key,128);b.writeUtf(value,128);});}
    };
    public Type<FunctionalMenuPayload> type(){return TYPE;}
}
