package net.nicomar.nomoregrids.simulation.movement;

import net.nicomar.nomoregrids.core.GridPoint;

public final class RailConnections {
    public record Endpoint(GridPoint position,GridPoint outwardTangent){}
    private RailConnections(){}
    public static boolean continuous(Endpoint a,Endpoint b){
        GridPoint t=a.outwardTangent,u=b.outwardTangent;
        return a.position.equals(b.position)&&t.x()==-u.x()&&t.y()==-u.y()&&t.z()==-u.z()&&(t.x()!=0||t.y()!=0||t.z()!=0);
    }
}
