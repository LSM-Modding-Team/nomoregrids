package net.nicomar.nomoregrids.network;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SignEditorPayload(UUID piece,long revision,boolean front,List<String> lines) implements CustomPacketPayload {
    public static final Type<SignEditorPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","sign_editor"));
    public SignEditorPayload{lines=List.copyOf(lines);if(revision<0||lines.size()!=4||lines.stream().anyMatch(s->s.length()>384))throw new IllegalArgumentException("Sign text bounds");}
    public static final StreamCodec<RegistryFriendlyByteBuf,SignEditorPayload> CODEC=new StreamCodec<>(){
        @Override public SignEditorPayload decode(RegistryFriendlyByteBuf b){var id=b.readUUID();long revision=b.readLong();boolean front=b.readBoolean();var lines=new ArrayList<String>();for(int i=0;i<4;i++)lines.add(b.readUtf(384));return new SignEditorPayload(id,revision,front,lines);}
        @Override public void encode(RegistryFriendlyByteBuf b,SignEditorPayload p){b.writeUUID(p.piece);b.writeLong(p.revision);b.writeBoolean(p.front);for(var line:p.lines)b.writeUtf(line,384);}
    };
    @Override public Type<SignEditorPayload> type(){return TYPE;}
}
