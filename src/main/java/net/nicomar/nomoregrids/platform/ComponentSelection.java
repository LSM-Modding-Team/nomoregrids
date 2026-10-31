package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.world.CellView;

/** Caller supplies only the first visible cell from the authoritative player ray. */
public final class ComponentSelection {
    private static volatile int clientCandidate;
    private ComponentSelection(){}
    public static void clientCandidate(int candidate){clientCandidate=Math.floorMod(candidate,64);}
    public static int candidate(Player player){return player.level().isClientSide()?clientCandidate:net.nicomar.nomoregrids.network.NmgNetworking.modes(player.getUUID()).candidate();}
    public static Optional<PieceRecord> select(Player player,CellView view,int candidate){
        var accessible=accessible(player,view);return accessible.isEmpty()?Optional.empty():Optional.of(accessible.get(Math.floorMod(candidate,accessible.size())));
    }
    public static int accessibleCount(Player player,CellView view){return accessible(player,view).size();}
    private static List<PieceRecord> accessible(Player player,CellView view){
        var eye=player.getEyePosition();var look=player.getLookAngle();
        var end=eye.add(look.scale(player.blockInteractionRange()));
        record Found(PieceRecord piece,double distance){}
        var found=new ArrayList<Found>();
        for(var p:view.replicas())PieceQueries.ray(p,eye,end).ifPresent(hit->found.add(new Found(p,hit.distance16())));
        found.sort(Comparator.comparingDouble(Found::distance).thenComparing(f->f.piece.pieceId()));if(found.isEmpty())return List.of();
        double nearest=found.getFirst().distance;return found.stream().filter(f->f.distance<=nearest+1e-7).map(Found::piece).toList();
    }
}
