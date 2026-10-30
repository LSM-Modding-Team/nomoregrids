package net.nicomar.nomoregrids.persistence;

import java.util.*;
import net.nicomar.nomoregrids.core.PieceRecord;

/** Legacy SUBSTITUTE was derived availability, never material data. Keep it out of journal authority comparisons. */
public final class AuthorityRecords {
    private AuthorityRecords(){}
    public static PieceRecord legacy(PieceRecord p){
        if(!p.flags().contains(PieceRecord.Flag.SUBSTITUTE))return p;var flags=new HashSet<>(p.flags());flags.remove(PieceRecord.Flag.SUBSTITUTE);
        return new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),flags);
    }
}
