package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EditingStatePayload(String dimension,boolean enabled) implements CustomPacketPayload {
    public static final Type<EditingStatePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","editing_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EditingStatePayload> CODEC=new StreamCodec<>(){
        @Override public EditingStatePayload decode(RegistryFriendlyByteBuf b){return new EditingStatePayload(b.readUtf(256),b.readBoolean());}
        @Override public void encode(RegistryFriendlyByteBuf b,EditingStatePayload p){b.writeUtf(p.dimension,256);b.writeBoolean(p.enabled);}
    };
    @Override public Type<EditingStatePayload> type(){return TYPE;}
}
