package net.nicomar.nomoregrids.world;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import net.nicomar.nomoregrids.persistence.SectionStore;

/** Exercises actual immutable indices at F9 counts; no latency threshold or synthetic Minecraft host. */
class LargeSceneIndexTest {
    @Test void tenThousandPieceWallBuildsAndRemovesInBoundedTransactions(){
        var pieces=new ArrayList<PieceRecord>();for(int i=0;i<10000;i++)pieces.add(TestPieces.of(new UUID(1,i+1),ConstructionShapes.CUBE,new GridPoint((i%100)*16L+8,(i/100)*16L,0)));
        var index=PieceIndex.empty();for(int i=0;i<pieces.size();i+=64){var changes=pieces.subList(i,Math.min(i+64,pieces.size())).stream().map(p->new PieceChange(p.pieceId(),-1,Optional.of(p))).toList();index=index.changed(changes,PieceIndex.Limits.DEFAULT);}
        assertEquals(10000,index.records().size());for(var p:pieces)assertTrue(index.ownedBy(p.ownerSection()).contains(p.pieceId()));
        for(int i=0;i<pieces.size();i+=64){var changes=pieces.subList(i,Math.min(i+64,pieces.size())).stream().map(p->new PieceChange(p.pieceId(),0,Optional.<PieceRecord>empty())).toList();index=index.changed(changes,PieceIndex.Limits.DEFAULT);}
        assertTrue(index.records().isEmpty());assertTrue(index.at(new CellPos(1,0,0)).isEmpty());
    }
    @Test void denseSectionChecksTheRealUncompressedCodecBudget()throws Exception{
        var pieces=new ArrayList<PieceRecord>();for(int i=0;i<4096;i++)pieces.add(TestPieces.of(new UUID(2,i+1),ConstructionShapes.CUBE,new GridPoint(i%16*16L,i/16%16*16L,i/256*16L)));
        var index=PieceIndex.restored(pieces);var section=new SectionPos(0,0,0);assertEquals(4096,index.ownedBy(section).size());
        assertTrue(SectionStore.encode(section,1,pieces).length<=2*1024*1024);
        var overflow=TestPieces.of(new UUID(3,1),ConstructionShapes.CUBE,new GridPoint(0,0,0));var tooMany=new ArrayList<>(pieces);tooMany.add(overflow);assertThrows(java.io.IOException.class,()->SectionStore.encode(section,1,tooMany));
    }
}
