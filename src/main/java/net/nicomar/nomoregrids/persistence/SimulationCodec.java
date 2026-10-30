package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell;
import net.nicomar.nomoregrids.simulation.functional.FunctionalStore;
import net.nicomar.nomoregrids.simulation.redstone.SignalNetwork;

/** Private checkpoint codec. A future schema is rejected without rewriting its original file. */
public final class SimulationCodec {
    public static final int MAX_BYTES=2*1024*1024;
    private SimulationCodec(){}
    public static byte[] encode(SimulationSnapshot state)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(new Limited(bytes));out.writeInt(2);out.writeLong(state.section().x());out.writeLong(state.section().y());out.writeLong(state.section().z());out.writeLong(state.checkpoint());
        out.writeInt(state.cavities().size());for(var pos:state.cavities().keySet().stream().sorted().toList()){
            out.writeLong(pos.x());out.writeLong(pos.y());out.writeLong(pos.z());var cell=state.cavities().get(pos);out.writeByte(CavityCell.code(cell.source()));
            long[] mask=cell.blocked().toLongArray();for(int i=0;i<64;i++)out.writeLong(i<mask.length?mask[i]:0);
            byte[] fluids=cell.fluids();for(int i=0;i<4096;i+=4)out.writeByte(fluids[i]|fluids[i+1]<<2|fluids[i+2]<<4|fluids[i+3]<<6);
        }
        out.writeInt(state.functionals().size());for(UUID id:state.functionals().keySet().stream().sorted().toList()){
            var machine=state.functionals().get(id);OperationCodec.uuid(out,id);out.writeLong(machine.revision());out.writeLong(machine.energy());out.writeInt(machine.slots().size());for(var slot:machine.slots())OperationCodec.blob(out,slot.copy());OperationCodec.blob(out,machine.machine().copy());
        }
        out.writeInt(state.groups().size());for(UUID id:state.groups().keySet().stream().sorted().toList()){OperationCodec.uuid(out,id);var members=state.groups().get(id);out.writeInt(members.size());for(UUID member:members.stream().sorted().toList())OperationCodec.uuid(out,member);}
        out.writeInt(state.circuits().size());for(UUID id:state.circuits().keySet().stream().sorted().toList()){OperationCodec.uuid(out,id);circuit(out,state.circuits().get(id));}
        out.flush();CRC32C crc=new CRC32C();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());return bytes.toByteArray();
    }
    private static final class Limited extends FilterOutputStream{
        int written;Limited(OutputStream output){super(output);}
        @Override public void write(int value)throws IOException{if(++written>MAX_BYTES)throw new IOException("Simulation checkpoint byte budget");out.write(value);}
        @Override public void write(byte[] bytes,int offset,int length)throws IOException{if(length>MAX_BYTES-written)throw new IOException("Simulation checkpoint byte budget");written+=length;out.write(bytes,offset,length);}
    }
    public static SimulationSnapshot decode(byte[] bytes)throws IOException{
        if(bytes.length<56||bytes.length>MAX_BYTES)throw new IOException("Simulation checkpoint size");CRC32C crc=new CRC32C();crc.update(bytes,0,bytes.length-4);if((int)crc.getValue()!=ByteBuffer.wrap(bytes).getInt(bytes.length-4))throw new IOException("Simulation checksum; original retained");
        var in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-4));try{
            int version=in.readInt();if(version!=1&&version!=2)throw new IOException("Future simulation schema; original retained");var section=new SectionPos(in.readLong(),in.readLong(),in.readLong());long checkpoint=in.readLong();
            var cavities=new HashMap<CellPos,CavityCell>();int count=count(in,256);for(int i=0;i<count;i++){
                var pos=new CellPos(in.readLong(),in.readLong(),in.readLong());var source=CavityCell.kind(in.readByte());long[] mask=new long[64];for(int j=0;j<64;j++)mask[j]=in.readLong();byte[] fluids=new byte[4096];for(int j=0;j<4096;j+=4){int packed=in.readUnsignedByte();for(int k=0;k<4;k++)fluids[j+k]=(byte)((packed>>>(2*k))&3);}
                if(cavities.putIfAbsent(pos,new CavityCell(BitSet.valueOf(mask),fluids,source))!=null)throw new IOException("Duplicate cavity authority");
            }
            var machines=new HashMap<UUID,FunctionalStore.State>();count=count(in,4096);for(int i=0;i<count;i++){
                UUID id=OperationCodec.uuid(in);long revision=in.readLong(),energy=in.readLong();int slots=count(in,256);var inventory=new ArrayList<BinaryData>();for(int j=0;j<slots;j++)inventory.add(new BinaryData(OperationCodec.blob(in,BinaryData.MAX_BYTES)));var machine=new BinaryData(OperationCodec.blob(in,BinaryData.MAX_BYTES));
                if(machines.putIfAbsent(id,new FunctionalStore.State(id,revision,inventory,energy,machine))!=null)throw new IOException("Duplicate functional authority");
            }
            var groups=new HashMap<UUID,Set<UUID>>();count=count(in,4096);for(int i=0;i<count;i++){UUID id=OperationCodec.uuid(in);int n=count(in,128);Set<UUID> members=new HashSet<>();for(int j=0;j<n;j++)if(!members.add(OperationCodec.uuid(in)))throw new IOException("Duplicate group member");if(groups.putIfAbsent(id,members)!=null)throw new IOException("Duplicate group authority");}
            var circuits=new HashMap<UUID,SignalNetwork.Snapshot>();count=count(in,256);for(int i=0;i<count;i++)if(circuits.putIfAbsent(OperationCodec.uuid(in),circuit(in))!=null)throw new IOException("Duplicate circuit authority");
            if(in.available()!=0)throw new IOException("Trailing simulation data");return new SimulationSnapshot(section,checkpoint,cavities,machines,groups,circuits);
        }catch(IllegalArgumentException|ArithmeticException ex){throw new IOException("Invalid simulation data; original retained",ex);}
    }
    private static int count(DataInputStream in,int limit)throws IOException{int n=in.readInt();if(n<0||n>limit)throw new IOException("Simulation collection budget");return n;}
    private static void circuit(DataOutputStream out,SignalNetwork.Snapshot state)throws IOException{
        out.writeLong(state.tick());out.writeInt(state.nodes().size());for(var node:state.nodes()){OperationCodec.uuid(out,node.id());out.writeUTF(node.kind().name());out.writeByte(node.delayTicks());out.writeBoolean(node.subtract());}
        out.writeInt(state.edges().size());for(var edge:state.edges()){OperationCodec.uuid(out,edge.from());OperationCodec.uuid(out,edge.to());out.writeUTF(edge.input().name());out.writeInt(edge.qLength());}
        levels(out,state.values());levels(out,state.sources());out.writeInt(state.pending().size());for(var event:state.pending()){out.writeLong(event.tick());OperationCodec.uuid(out,event.id());out.writeByte(event.value());}
    }
    private static SignalNetwork.Snapshot circuit(DataInputStream in)throws IOException{
        long tick=in.readLong();int count=count(in,65536);var nodes=new ArrayList<SignalNetwork.Node>();for(int i=0;i<count;i++)nodes.add(new SignalNetwork.Node(OperationCodec.uuid(in),SignalNetwork.Kind.valueOf(in.readUTF()),in.readUnsignedByte(),flag(in)));
        count=count(in,262144);var edges=new ArrayList<SignalNetwork.Edge>();for(int i=0;i<count;i++)edges.add(new SignalNetwork.Edge(OperationCodec.uuid(in),OperationCodec.uuid(in),SignalNetwork.Input.valueOf(in.readUTF()),in.readInt()));
        var values=levels(in);var sources=levels(in);count=count(in,SignalNetwork.MAX_PENDING);var pending=new ArrayList<SignalNetwork.Pending>();for(int i=0;i<count;i++)pending.add(new SignalNetwork.Pending(in.readLong(),OperationCodec.uuid(in),in.readUnsignedByte()));
        return new SignalNetwork.Snapshot(tick,nodes,edges,values,sources,pending);
    }
    private static boolean flag(DataInputStream in)throws IOException{int value=in.readUnsignedByte();if(value>1)throw new IOException("Invalid simulation flag");return value==1;}
    private static void levels(DataOutputStream out,Map<UUID,Integer> levels)throws IOException{out.writeInt(levels.size());for(UUID id:levels.keySet().stream().sorted().toList()){OperationCodec.uuid(out,id);out.writeByte(levels.get(id));}}
    private static Map<UUID,Integer> levels(DataInputStream in)throws IOException{int count=count(in,65536);Map<UUID,Integer> result=new HashMap<>();for(int i=0;i<count;i++)if(result.putIfAbsent(OperationCodec.uuid(in),in.readUnsignedByte())!=null)throw new IOException("Duplicate signal level");return result;}
}
