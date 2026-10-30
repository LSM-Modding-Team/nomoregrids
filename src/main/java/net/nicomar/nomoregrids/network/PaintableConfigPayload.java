package net.nicomar.nomoregrids.network;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PaintableConfigPayload(UUID session,int action,boolean create,int width,int height,int depth,int resolution,int direction,boolean upper) implements CustomPacketPayload {
    public static final Type<PaintableConfigPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","paintable_config"));
    public PaintableConfigPayload{if(action<0||action>2||width<1||width>16||height<1||height>16||depth<1||depth>16||!Set.of(1,2,4,8,16).contains(resolution)||direction<0||direction>3)throw new IllegalArgumentException("PB configuration bounds");}
    public static final StreamCodec<RegistryFriendlyByteBuf,PaintableConfigPayload> CODEC=new StreamCodec<>(){
        @Override public PaintableConfigPayload decode(RegistryFriendlyByteBuf b){return new PaintableConfigPayload(b.readUUID(),b.readUnsignedByte(),b.readBoolean(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readUnsignedByte(),b.readBoolean());}
        @Override public void encode(RegistryFriendlyByteBuf b,PaintableConfigPayload p){b.writeUUID(p.session);b.writeByte(p.action);b.writeBoolean(p.create);b.writeByte(p.width);b.writeByte(p.height);b.writeByte(p.depth);b.writeByte(p.resolution);b.writeByte(p.direction);b.writeBoolean(p.upper);}
    };
    @Override public Type<PaintableConfigPayload> type(){return TYPE;}
}
