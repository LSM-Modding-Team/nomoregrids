package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.nicomar.nomoregrids.core.SectionPos;

/** Durable private simulation checkpoint. Host must include the same changes in its committed journal. */
public final class SimulationStore {
    private final Path root;
    public SimulationStore(Path root)throws IOException{this.root=root;DurableDirectories.create(root);}
    private Path file(SectionPos section){return root.resolve(section.x()+"_"+section.y()+"_"+section.z()+".simulation");}
    public Optional<SimulationSnapshot> read(SectionPos section)throws IOException{
        Path path=file(section);if(!Files.exists(path))return Optional.empty();if(Files.size(path)>SimulationCodec.MAX_BYTES)throw new IOException("Simulation checkpoint budget; original retained");
        var snapshot=SimulationCodec.decode(Files.readAllBytes(path));if(!snapshot.section().equals(section))throw new IOException("Simulation owner section mismatch");return Optional.of(snapshot);
    }
    public void writeCommitted(SimulationSnapshot snapshot,long durableJournalSequence)throws IOException{
        if(snapshot.checkpoint()<1||snapshot.checkpoint()>durableJournalSequence)throw new IOException("Simulation checkpoint requires a durable journal commit");
        var previous=read(snapshot.section());if(previous.isPresent()){
            var old=previous.orElseThrow();if(old.equals(snapshot))return;if(old.checkpoint()>=snapshot.checkpoint())throw new IOException("Simulation checkpoint rollback/conflict; original retained");
        }
        SectionStore.atomic(file(snapshot.section()),SimulationCodec.encode(snapshot));
    }
}
