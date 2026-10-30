package net.nicomar.nomoregrids.world;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import org.junit.jupiter.api.Test;

class CellViewTest {
    @Test void publicReplicaNeverExportsPrivateOrUnknownAdapterPayload()throws Exception{
        var base=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);var secret=new BinaryData(new byte[]{42,43,44});
        var p=new PieceRecord(base.pieceId(),0,new ResourceId("external:unknown"),1,base.sourceBlockId(),base.sourceState(),base.origin16(),0,base.pivot16(),base.geometry(),secret,secret,secret,Optional.empty(),Set.of(PieceRecord.Flag.SUBSTITUTE));
        var index=PieceIndex.retained(List.of(p),(a,b)->a.worldOccupation().overlaps(b.worldOccupation()));var view=CellView.from(new CellPos(0,0,0),index);
        var copy=CellView.decode(view.encode()).replicas().getFirst();assertEquals(0,copy.geometryPayload().size());assertEquals(0,copy.materialPayload().size());assertEquals(0,copy.itemPayload().size());assertEquals(secret,index.records().get(p.pieceId()).itemPayload());
    }
}
