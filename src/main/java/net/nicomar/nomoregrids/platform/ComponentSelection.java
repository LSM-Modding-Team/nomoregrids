package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.world.CellView;

/** Caller supplies only the first visible cell from the authoritative player ray. */
public final class ComponentSelection {
    private ComponentSelection(){}
    public static Optional<PieceRecord> select(Player player,CellView view,int candidate){
        var eye=player.getEyePosition();var look=player.getLookAngle();
        var end=eye.add(look.scale(player.blockInteractionRange()));
        record Found(PieceRecord piece,double distance){}
        var found=new ArrayList<Found>();
        for(var p:view.replicas())PieceQueries.ray(p,eye,end).ifPresent(hit->found.add(new Found(p,hit.distance16())));
        found.sort(Comparator.comparingDouble(Found::distance).thenComparing(f->f.piece.pieceId()));if(found.isEmpty())return Optional.empty();
        double nearest=found.getFirst().distance;var accessible=found.stream().filter(f->f.distance<=nearest+1e-7).toList();
        return Optional.of(accessible.get(Math.floorMod(candidate,accessible.size())).piece);
    }
}
