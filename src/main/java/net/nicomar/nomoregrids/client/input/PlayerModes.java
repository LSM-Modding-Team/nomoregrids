package net.nicomar.nomoregrids.client.input;

import java.util.Optional;
import net.nicomar.nomoregrids.core.*;

/** Per server/player mode state; transient selection/alignment never leaks across dimensions. */
public final class PlayerModes {
    public boolean precise,expanded,adjusted;
    private int axis,candidate;
    private GridPoint offset=GridPoint.ZERO;
    private Optional<GridPoint> alignment=Optional.empty();
    public int axis(){return axis;}public int candidate(){return candidate;}public GridPoint offset(){return offset;}
    public Optional<GridPoint> alignment(){return alignment;}
    public void cycleAxis(boolean screenOpen){if(!screenOpen)axis=(axis+1)%3;}
    public void scroll(int steps,boolean shiftPosition,boolean cycleCandidates,boolean screenOpen){
        if(screenOpen)return;
        if(shiftPosition)offset=offset.add(new GridPoint(axis==0?steps:0,axis==1?steps:0,axis==2?steps:0));
        else if(cycleCandidates)candidate=Math.addExact(candidate,steps);
    }
    public void align(GridPoint reference){alignment=alignment.isPresent()?Optional.empty():Optional.of(reference);}
    public void changedDimension(){alignment=Optional.empty();candidate=0;offset=GridPoint.ZERO;}
}
