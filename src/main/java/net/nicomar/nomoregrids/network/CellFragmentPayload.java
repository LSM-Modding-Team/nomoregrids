package net.nicomar.nomoregrids.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A bounded fragment tied to one dimension/cell. SHA-256 authenticates the assembled public bytes. */
public record CellFragmentPayload(String dimension,BlockPos position,boolean delta,SnapshotAssembler.Fragment fragment) implements CustomPacketPayload {
    public static final Type<CellFragmentPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","cell_fragment"));
    public CellFragmentPayload{if(dimension.length()>256)throw new IllegalArgumentException("Dimension length");position=position.immutable();}
    public static final StreamCodec<RegistryFriendlyByteBuf,CellFragmentPayload> CODEC=new StreamCodec<>(){
        @Override public CellFragmentPayload decode(RegistryFriendlyByteBuf b){return new CellFragmentPayload(b.readUtf(256),b.readBlockPos(),b.readBoolean(),new SnapshotAssembler.Fragment(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(32),b.readByteArray(SnapshotAssembler.CHUNK)));}
        @Override public void encode(RegistryFriendlyByteBuf b,CellFragmentPayload p){b.writeUtf(p.dimension,256);b.writeBlockPos(p.position);b.writeBoolean(p.delta);var f=p.fragment;b.writeUUID(f.message());b.writeVarInt(f.totalBytes());b.writeVarInt(f.index());b.writeByteArray(f.digest());b.writeByteArray(f.data());}
    };
    @Override public Type<CellFragmentPayload> type(){return TYPE;}
}
