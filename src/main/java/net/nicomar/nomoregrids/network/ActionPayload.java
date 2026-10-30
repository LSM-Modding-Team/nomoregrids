package net.nicomar.nomoregrids.network;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Atomic preferences and a stale-target guard. Server recomputes hit, geometry, costs and permissions. */
public record ActionPayload(UUID operation,Action action,boolean offhand,ModePayload intent,Optional<UUID> expectedPiece,long expectedRevision,boolean control) implements CustomPacketPayload {
    public enum Action { PLACE,REMOVE,PROBE,LOCK,EDIT,BUCKET }
    /** Native server-side fallback; clients always include their complete current intent. */
    public ActionPayload(UUID operation,Action action,boolean offhand,ModePayload intent,Optional<UUID> piece,long revision){this(operation,action,offhand,intent,piece,revision,false);}
    public ActionPayload(UUID operation,Action action,boolean offhand){this(operation,action,offhand,null,Optional.empty(),-1);}
    public ActionPayload{Objects.requireNonNull(operation);Objects.requireNonNull(action);Objects.requireNonNull(expectedPiece);if(expectedPiece.isEmpty()?expectedRevision!=-1:expectedRevision<0)throw new IllegalArgumentException("Target revision guard");}
    public static final Type<ActionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ActionPayload> CODEC=new StreamCodec<>(){
        @Override public ActionPayload decode(RegistryFriendlyByteBuf b){UUID id=b.readUUID();int kind=b.readUnsignedByte();Action action=switch(kind){case 0->Action.PLACE;case 1->Action.REMOVE;case 2->Action.PROBE;case 3->Action.LOCK;case 4->Action.EDIT;case 5->Action.BUCKET;default->throw new IllegalArgumentException("Action ID");};boolean offhand=b.readBoolean();ModePayload intent=b.readBoolean()?ModePayload.CODEC.decode(b):null;Optional<UUID> expected=b.readBoolean()?Optional.of(b.readUUID()):Optional.empty();long revision=expected.isPresent()?b.readLong():-1;return new ActionPayload(id,action,offhand,intent,expected,revision,b.readBoolean());}
        @Override public void encode(RegistryFriendlyByteBuf b,ActionPayload p){b.writeUUID(p.operation);b.writeByte(switch(p.action){case PLACE->0;case REMOVE->1;case PROBE->2;case LOCK->3;case EDIT->4;case BUCKET->5;});b.writeBoolean(p.offhand);b.writeBoolean(p.intent!=null);if(p.intent!=null)ModePayload.CODEC.encode(b,p.intent);b.writeBoolean(p.expectedPiece.isPresent());if(p.expectedPiece.isPresent()){b.writeUUID(p.expectedPiece.orElseThrow());b.writeLong(p.expectedRevision);}b.writeBoolean(p.control);}
    };
    @Override public Type<ActionPayload> type(){return TYPE;}
}
