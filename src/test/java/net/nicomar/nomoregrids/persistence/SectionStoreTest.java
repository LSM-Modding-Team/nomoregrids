package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SectionStoreTest {
    @TempDir Path dir;
    @Test void authoritativeSectionRoundTripAndCorruptionRetention()throws Exception{
        var store=new SectionStore(dir);var p=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(-1,-1,-1));
        store.write(p.ownerSection(),7,List.of(p));assertEquals(new SectionStore.Section(7,List.of(p)),store.read(p.ownerSection()));
        Path file=dir.resolve("-1_-1_-1.nmg");byte[] bytes=Files.readAllBytes(file);bytes[18]^=1;Files.write(file,bytes);
        assertThrows(java.io.IOException.class,()->store.read(p.ownerSection()));assertArrayEquals(bytes,Files.readAllBytes(file));
    }
    @Test void emptyCollisionIsPreservedButOccupationCannotExceedOneBlock()throws Exception{
        var base=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);var geometry=new BoxGeometrySnapshot(ConstructionShapes.CUBE,new BoxUnion(List.of()),ConstructionShapes.CUBE);
        var p=new PieceRecord(base.pieceId(),0,base.adapterId(),1,base.sourceBlockId(),Map.of(),GridPoint.ZERO,0,base.pivot16(),geometry,BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());
        assertEquals(p,((BoxPieceCodec.Decoded)BoxPieceCodec.decode(BoxPieceCodec.encode(p))).piece());
        var tooLarge=new BoxUnion(List.of(Box16.of(0,0,0,32,16,16)));assertThrows(IllegalArgumentException.class,()->new BoxGeometrySnapshot(tooLarge,tooLarge,tooLarge));
    }
}
