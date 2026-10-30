package net.nicomar.nomoregrids.simulation.connection;

import java.util.*;
import java.util.function.Function;
import net.nicomar.nomoregrids.core.*;

/** Deterministic fixed-point resolution. Caller proposals include geometry/overlap validation. */
public final class ConnectionSolver {
    public record Port(UUID piece,String name,GridPoint point,Direction6 normal,int width,int height,ResourceId type){
        public Port{if(width<1||height<1)throw new IllegalArgumentException("Invalid port");}
    }
    public record Link(UUID first,String firstPort,UUID second,String secondPort){}
    private ConnectionSolver(){}
    public static List<Link> matching(Collection<Port> ports){
        var sorted=ports.stream().sorted(Comparator.comparing(Port::point,Comparator.comparingLong(GridPoint::x).thenComparingLong(GridPoint::y).thenComparingLong(GridPoint::z))
            .thenComparing(Port::piece).thenComparing(Port::name)).toList();
        var result=new ArrayList<Link>();
        for(int i=0;i<sorted.size();i++)for(int j=i+1;j<sorted.size();j++){
            Port a=sorted.get(i),b=sorted.get(j);
            if(!a.piece.equals(b.piece)&&a.point.equals(b.point)&&a.normal==b.normal.opposite()&&a.width==b.width&&a.height==b.height&&a.type.equals(b.type))
                result.add(new Link(a.piece,a.name,b.piece,b.name));
        }
        return List.copyOf(result);
    }
    public static <T> T converge(T initial,Function<T,T> proposeAndValidate){
        T current=initial;Set<T> seen=new HashSet<>();
        for(int pass=0;pass<16;pass++){
            if(!seen.add(current))throw new IllegalArgumentException("Connection cycle");
            T next=Objects.requireNonNull(proposeAndValidate.apply(current));if(next.equals(current))return next;current=next;
        }
        throw new IllegalArgumentException("Connections did not converge in 16 passes");
    }
}
