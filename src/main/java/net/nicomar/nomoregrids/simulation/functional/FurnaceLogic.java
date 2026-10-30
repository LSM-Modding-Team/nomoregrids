package net.nicomar.nomoregrids.simulation.functional;

import java.util.*;
import net.nicomar.nomoregrids.core.ResourceId;

/** Explicit furnace adapter logic. Recipe/fuel lookup is supplied by the server recipe registry. */
public final class FurnaceLogic {
    public record Stack(ResourceId item,int count){public Stack{if(count<1||count>64)throw new IllegalArgumentException("Stack count");}}
    public record Recipe(ResourceId input,Stack result,int ticks,float experience){public Recipe{if(ticks<1||!Float.isFinite(experience)||experience<0)throw new IllegalArgumentException("Recipe");}}
    public record Fuel(int ticks,Optional<Stack> remainder){public Fuel{if(ticks<1)throw new IllegalArgumentException("Fuel");}}
    public record State(Optional<Stack> input,Optional<Stack> fuel,Optional<Stack> output,int burning,int progress,float experience){
        public State{if(burning<0||progress<0||experience<0)throw new IllegalArgumentException("Furnace state");}
    }
    public record Tick(State state,Optional<Stack> fuelRemainder){}
    private FurnaceLogic(){}
    private static Optional<Stack> take(Optional<Stack> slot){var s=slot.orElseThrow();return s.count==1?Optional.empty():Optional.of(new Stack(s.item,s.count-1));}
    public static Tick tick(State old,Optional<Recipe> recipe,java.util.function.Function<ResourceId,Optional<Fuel>> fuels){
        int burning=Math.max(0,old.burning-1),progress=old.progress;float experience=old.experience;
        Optional<Stack> input=old.input,fuel=old.fuel,output=old.output,remainder=Optional.empty();
        boolean can=recipe.isPresent()&&input.isPresent()&&input.orElseThrow().item.equals(recipe.orElseThrow().input)
            &&(output.isEmpty()||output.orElseThrow().item.equals(recipe.orElseThrow().result.item))
            &&output.map(Stack::count).orElse(0)+recipe.orElseThrow().result.count<=64;
        if(can&&burning==0&&fuel.isPresent()){
            var candidate=fuels.apply(fuel.orElseThrow().item);
            if(candidate.isPresent()){
                Fuel f=candidate.orElseThrow();
                // A container remainder cannot be discarded or overwrite a fuel stack.
                if(f.remainder.isEmpty()||fuel.orElseThrow().count==1){burning=f.ticks;fuel=take(fuel);if(f.remainder.isPresent())fuel=f.remainder;}
            }
        }
        if(can&&burning>0){
            Recipe r=recipe.orElseThrow();progress++;
            if(progress>=r.ticks){input=take(input);output=Optional.of(new Stack(r.result.item,output.map(Stack::count).orElse(0)+r.result.count));progress=0;experience+=r.experience;}
        }else progress=Math.max(0,progress-2);
        return new Tick(new State(input,fuel,output,burning,progress,experience),remainder);
    }
}
