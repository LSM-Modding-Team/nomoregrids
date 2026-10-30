package net.nicomar.nomoregrids.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActionResultPayload(UUID operation,boolean accepted,String reason) implements CustomPacketPayload {
    public static final Type<ActionResultPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","action_result"));
    public ActionResultPayload{if(reason.length()>256)throw new IllegalArgumentException("Reason length");}
    public static final StreamCodec<RegistryFriendlyByteBuf,ActionResultPayload> CODEC=new StreamCodec<>(){
        @Override public ActionResultPayload decode(RegistryFriendlyByteBuf b){return new ActionResultPayload(b.readUUID(),b.readBoolean(),b.readUtf(256));}
        @Override public void encode(RegistryFriendlyByteBuf b,ActionResultPayload p){b.writeUUID(p.operation);b.writeBoolean(p.accepted);b.writeUtf(p.reason,256);}
    };
    @Override public Type<ActionResultPayload> type(){return TYPE;}
}
