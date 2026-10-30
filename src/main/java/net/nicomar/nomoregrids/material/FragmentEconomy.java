package net.nicomar.nomoregrids.material;

import java.util.*;
import net.nicomar.nomoregrids.core.*;

/** Material-conserving cut/join planner; never mutates caller inventory or invents drops. */
public final class FragmentEconomy {
    public record Key(ResourceId material,ResourceId section,int textureOrientation,BinaryData data) {
        public Key{Objects.requireNonNull(material);Objects.requireNonNull(section);Objects.requireNonNull(data);}
    }
    public record Stack(Key key,int length,int count){
        public Stack{Objects.requireNonNull(key);if(length<1||length>16||count<1||count>64)throw new IllegalArgumentException("Invalid fragment stack");}
    }
    public record Cut(Stack placed,List<Optional<Stack>> inventory){public Cut{inventory=List.copyOf(inventory);}}
    private FragmentEconomy(){}
    public static Cut take(List<Optional<Stack>> inventory,Key material,int length,boolean creative){
        if(length<1||length>16)throw new IllegalArgumentException("Invalid cut length");
        if(creative)return new Cut(new Stack(material,length,1),inventory);
        int selected=-1,best=17;
        for(int i=0;i<inventory.size();i++)if(inventory.get(i).isPresent()){
            Stack stack=inventory.get(i).orElseThrow();
            if(stack.key.equals(material)&&stack.length>=length&&stack.length<best){selected=i;best=stack.length;}
        }
        if(selected<0)throw new IllegalArgumentException("INVENTORY_MISSING");
        var next=new ArrayList<>(inventory);Stack source=next.get(selected).orElseThrow();
        next.set(selected,source.count==1?Optional.empty():Optional.of(new Stack(source.key,source.length,source.count-1)));
        if(best>length)insert(next,new Stack(material,best-length,1));
        return new Cut(new Stack(material,length,1),next);
    }
    private static void insert(List<Optional<Stack>> inventory,Stack remainder){
        for(int i=0;i<inventory.size();i++)if(inventory.get(i).isPresent()){
            Stack s=inventory.get(i).orElseThrow();
            if(s.key.equals(remainder.key)&&s.length==remainder.length&&s.count<64){inventory.set(i,Optional.of(new Stack(s.key,s.length,s.count+1)));return;}
        }
        for(int i=0;i<inventory.size();i++)if(inventory.get(i).isEmpty()){inventory.set(i,Optional.of(remainder));return;}
        throw new IllegalArgumentException("No room for conserved remainder");
    }
    public static Stack join(Stack a,Stack b){
        if(a.count!=1||b.count!=1||!a.key.equals(b.key)||a.length+b.length>16)throw new IllegalArgumentException("Incompatible join");
        return new Stack(a.key,a.length+b.length,1);
    }
    public static Optional<Stack> recovered(Key key,int length,boolean creative,boolean lootAllowsRecovery){
        return creative||!lootAllowsRecovery?Optional.empty():Optional.of(new Stack(key,length,1));
    }
}
