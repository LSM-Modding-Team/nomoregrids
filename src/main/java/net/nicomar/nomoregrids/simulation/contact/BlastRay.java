package net.nicomar.nomoregrids.simulation.contact;

import java.util.*;

/** Continuous material thickness on a ray. Repeated primitives of one authority never absorb twice. */
public final class BlastRay {
    public record Span(UUID material,double near,double far,double resistance){public Span{Objects.requireNonNull(material);if(!Double.isFinite(near)||!Double.isFinite(far)||!Double.isFinite(resistance)||near>far||resistance<0)throw new IllegalArgumentException("Invalid blast interval");}}
    private BlastRay(){}
    public static boolean reaches(UUID target,List<Span> input,double energy,double reach){
        if(!Double.isFinite(energy)||!Double.isFinite(reach)||reach<0)throw new IllegalArgumentException("Invalid blast energy");var grouped=new HashMap<UUID,List<Span>>();for(var span:input)if(span.far>0&&span.near<reach)grouped.computeIfAbsent(span.material,k->new ArrayList<>()).add(new Span(span.material,Math.max(0,span.near),Math.min(reach,span.far),span.resistance));var merged=new ArrayList<Span>();
        for(var spans:grouped.values()){spans.sort(Comparator.comparingDouble(Span::near));Span current=null;for(var next:spans){if(current==null)current=next;else if(next.near<=current.far+1e-7)current=new Span(current.material,current.near,Math.max(current.far,next.far),Math.max(current.resistance,next.resistance));else{merged.add(current);current=next;}}if(current!=null)merged.add(current);}
        merged.sort(Comparator.comparingDouble(Span::near).thenComparing(Span::material));double position=0;
        for(var span:merged){energy-=Math.max(0,span.near-position)*0.75;double thickness=span.far-span.near;energy-=(span.resistance+0.3)*thickness+Math.max(0,span.far-Math.max(position,span.near))*0.75;if(energy<=0)return false;if(span.material.equals(target))return true;position=Math.max(position,span.far);}
        return false;
    }
}
