package net.nicomar.nomoregrids.network;

import java.util.*;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.world.PieceIndex;

/** Interpretation-relevant limits and adapter data versions. Immutable and compared before editing. */
public record ProtocolRules(int pieceSchema,int adapterApi,Map<String,Integer> adapters,PieceIndex.Limits limits,int recordBytes,int snapshotBytes,int operationCells){
    public ProtocolRules{
        adapters=Map.copyOf(adapters);Objects.requireNonNull(limits);
        if(pieceSchema!=1||adapterApi!=1||adapters.isEmpty()||adapters.size()>64||recordBytes<1||recordBytes>BinaryData.MAX_BYTES||snapshotBytes<1||snapshotBytes>SnapshotAssembler.MAX||operationCells<1||operationCells>512)throw new IllegalArgumentException("Protocol rules budget/version");
        adapters.forEach((id,version)->{new net.nicomar.nomoregrids.core.ResourceId(id);if(version<1)throw new IllegalArgumentException("Adapter version");});
    }
    public static ProtocolRules defaults(Map<String,Integer> adapters){return new ProtocolRules(1,1,adapters,PieceIndex.Limits.DEFAULT,BinaryData.MAX_BYTES,SnapshotAssembler.MAX,512);}
}
