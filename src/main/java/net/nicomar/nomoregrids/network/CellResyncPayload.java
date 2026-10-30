package net.nicomar.nomoregrids.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CellResyncPayload(BlockPos position) implements CustomPacketPayload {
    public static final Type<CellResyncPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","cell_resync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CellResyncPayload> CODEC=new StreamCodec<>(){
        @Override public CellResyncPayload decode(RegistryFriendlyByteBuf b){return new CellResyncPayload(b.readBlockPos());}
        @Override public void encode(RegistryFriendlyByteBuf b,CellResyncPayload p){b.writeBlockPos(p.position);}
    };
    @Override public Type<CellResyncPayload> type(){return TYPE;}
}
