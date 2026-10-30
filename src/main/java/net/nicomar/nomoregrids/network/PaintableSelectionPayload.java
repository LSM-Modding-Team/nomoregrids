package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.nicomar.nomoregrids.core.GridPoint;

public record PaintableSelectionPayload(String dimension,int phase,GridPoint min,GridPoint max) implements CustomPacketPayload {
    public static final Type<PaintableSelectionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","paintable_selection"));
    public PaintableSelectionPayload{if(dimension.length()>256||phase<0||phase>2)throw new IllegalArgumentException("Selection bounds");}
    public static final StreamCodec<RegistryFriendlyByteBuf,PaintableSelectionPayload> CODEC=new StreamCodec<>(){
        @Override public PaintableSelectionPayload decode(RegistryFriendlyByteBuf b){return new PaintableSelectionPayload(b.readUtf(256),b.readUnsignedByte(),new GridPoint(b.readLong(),b.readLong(),b.readLong()),new GridPoint(b.readLong(),b.readLong(),b.readLong()));}
        @Override public void encode(RegistryFriendlyByteBuf b,PaintableSelectionPayload p){b.writeUtf(p.dimension,256);b.writeByte(p.phase);b.writeLong(p.min.x());b.writeLong(p.min.y());b.writeLong(p.min.z());b.writeLong(p.max.x());b.writeLong(p.max.y());b.writeLong(p.max.z());}
    };
    @Override public Type<PaintableSelectionPayload> type(){return TYPE;}
}
