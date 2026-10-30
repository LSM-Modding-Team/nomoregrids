package net.nicomar.nomoregrids.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Identity and intention only. Hit, piece, transform, costs and permissions are recomputed server-side. */
public record ActionPayload(UUID operation,Action action,boolean offhand) implements CustomPacketPayload {
    public enum Action { PLACE,REMOVE,PROBE,LOCK }
    public static final Type<ActionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ActionPayload> CODEC=new StreamCodec<>(){
        @Override public ActionPayload decode(RegistryFriendlyByteBuf b){UUID id=b.readUUID();int kind=b.readUnsignedByte();Action a=switch(kind){case 0->Action.PLACE;case 1->Action.REMOVE;case 2->Action.PROBE;case 3->Action.LOCK;default->throw new IllegalArgumentException("Action ID");};return new ActionPayload(id,a,b.readBoolean());}
        @Override public void encode(RegistryFriendlyByteBuf b,ActionPayload p){b.writeUUID(p.operation);b.writeByte(switch(p.action){case PLACE->0;case REMOVE->1;case PROBE->2;case LOCK->3;});b.writeBoolean(p.offhand);}
    };
    @Override public Type<ActionPayload> type(){return TYPE;}
}
