package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SessionAckPayload(String fingerprint) implements CustomPacketPayload {
    public static final Type<SessionAckPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","session_ack"));
    public SessionAckPayload{if(!fingerprint.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Session acknowledgement");}
    public static final StreamCodec<RegistryFriendlyByteBuf,SessionAckPayload> CODEC=new StreamCodec<>(){
        @Override public SessionAckPayload decode(RegistryFriendlyByteBuf b){return new SessionAckPayload(b.readUtf(64));}
        @Override public void encode(RegistryFriendlyByteBuf b,SessionAckPayload p){b.writeUtf(p.fingerprint,64);}
    };
    @Override public Type<SessionAckPayload> type(){return TYPE;}
}
