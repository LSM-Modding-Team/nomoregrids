package net.nicomar.nomoregrids.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.nicomar.nomoregrids.core.GridPoint;

/** Preferences only. Carries neither accepted geometry nor inventory/world authority. */
public record ModePayload(boolean precise,boolean expanded,boolean adjusted,boolean lateral,boolean replace,int axis,GridPoint offset,int candidate,
        boolean aligned,int alignmentAxis,int alignmentSign,long alignmentPlane,int alignmentRotation,int length,java.util.Map<String,String> alignmentProperties) implements CustomPacketPayload {
    public static final Type<ModePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("nomoregrids","modes"));
    public ModePayload(boolean precise,boolean expanded,boolean adjusted,boolean lateral,boolean replace,int axis,GridPoint offset,int candidate){this(precise,expanded,adjusted,lateral,replace,axis,offset,candidate,false,1,1,0,0,0,java.util.Map.of());}
    public ModePayload(boolean precise,boolean expanded,boolean adjusted,boolean lateral,boolean replace,int axis,GridPoint offset,int candidate,boolean aligned,int alignmentAxis,int alignmentSign,long alignmentPlane,int alignmentRotation){this(precise,expanded,adjusted,lateral,replace,axis,offset,candidate,aligned,alignmentAxis,alignmentSign,alignmentPlane,alignmentRotation,0,java.util.Map.of());}
    public ModePayload{if(axis<0||axis>2||offset.x()< -256||offset.x()>256||offset.y()< -256||offset.y()>256||offset.z()< -256||offset.z()>256||candidate<0||candidate>63
        ||alignmentAxis<0||alignmentAxis>2||(alignmentSign!=1&&alignmentSign!=-1)||alignmentPlane< -512000000||alignmentPlane>512000000||alignmentRotation<0||alignmentRotation>23||length<0||length>16)throw new IllegalArgumentException("Invalid mode intent");
        alignmentProperties=java.util.Map.copyOf(alignmentProperties);if(alignmentProperties.size()>4||!aligned&&!alignmentProperties.isEmpty())throw new IllegalArgumentException("Alignment properties");
        for(var entry:alignmentProperties.entrySet())if(!java.util.Set.of("axis","facing","half","type").contains(entry.getKey())||!entry.getValue().matches("[a-z_]{1,16}"))throw new IllegalArgumentException("Alignment property");}
    public static final StreamCodec<RegistryFriendlyByteBuf,ModePayload> CODEC=new StreamCodec<>(){
        @Override public ModePayload decode(RegistryFriendlyByteBuf b){int bits=b.readUnsignedByte();if((bits&~63)!=0)throw new IllegalArgumentException("Mode bits");int axis=b.readUnsignedByte();GridPoint offset=new GridPoint(b.readShort(),b.readShort(),b.readShort());int candidate=b.readUnsignedByte();int length=b.readUnsignedByte();boolean aligned=(bits&32)!=0;
            int alignmentAxis=aligned?b.readUnsignedByte():1,sign=aligned?b.readByte():1;long plane=aligned?b.readLong():0;int rotation=aligned?b.readUnsignedByte():0;
            var properties=new java.util.HashMap<String,String>();int count=aligned?b.readUnsignedByte():0;if(count>4)throw new IllegalArgumentException("Alignment count");
            for(int i=0;i<count;i++)if(properties.putIfAbsent(b.readUtf(16),b.readUtf(16))!=null)throw new IllegalArgumentException("Duplicate alignment property");
            return new ModePayload((bits&1)!=0,(bits&2)!=0,(bits&4)!=0,(bits&8)!=0,(bits&16)!=0,axis,offset,candidate,aligned,alignmentAxis,sign,plane,rotation,length,properties);}
        @Override public void encode(RegistryFriendlyByteBuf b,ModePayload p){b.writeByte((p.precise?1:0)|(p.expanded?2:0)|(p.adjusted?4:0)|(p.lateral?8:0)|(p.replace?16:0)|(p.aligned?32:0));b.writeByte(p.axis);b.writeShort((int)p.offset.x());b.writeShort((int)p.offset.y());b.writeShort((int)p.offset.z());b.writeByte(p.candidate);b.writeByte(p.length);
            if(p.aligned){b.writeByte(p.alignmentAxis);b.writeByte(p.alignmentSign);b.writeLong(p.alignmentPlane);b.writeByte(p.alignmentRotation);b.writeByte(p.alignmentProperties.size());for(var e:new java.util.TreeMap<>(p.alignmentProperties).entrySet()){b.writeUtf(e.getKey(),16);b.writeUtf(e.getValue(),16);}}}
    };
    @Override public Type<ModePayload> type(){return TYPE;}
}
