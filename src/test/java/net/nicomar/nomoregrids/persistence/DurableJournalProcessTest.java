package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import net.nicomar.nomoregrids.operation.OperationPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Abruptly terminates a real writer JVM: no close(), shutdown hook or in-memory replay. */
class DurableJournalProcessTest {
    @TempDir Path dir;

    @Test void committedOperationSurvivesForciblyKilledWriterWithoutDoubleCommit() throws Exception {
        var piece=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        var plan=new OperationPlan(UUID.randomUUID(),UUID.randomUUID(),OperationPlan.Kind.PLACE,
            List.of(new OperationPlan.Change(piece.pieceId(),Optional.empty(),Optional.of(piece))),BinaryData.EMPTY,BinaryData.EMPTY);
        var log=dir.resolve("ops.log");var ready=dir.resolve("committed");var output=dir.resolve("writer.log");
        String classpath=Path.of(DurableJournalProcessTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            +File.pathSeparator+Path.of(DurableJournal.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String executable=System.getProperty("os.name").startsWith("Windows")?"java.exe":"java";
        var writer=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",executable).toString(),"-cp",classpath,
            Writer.class.getName(),log.toString(),ready.toString(),Base64.getEncoder().encodeToString(OperationCodec.encode(plan)))
            .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
            while(!Files.exists(ready)&&writer.isAlive()&&System.nanoTime()<deadline)Thread.sleep(10);
            assertTrue(Files.exists(ready),()->"Writer failed before commit: "+read(output));
            assertTrue(writer.isAlive(),"Writer must still hold the unclosed journal before termination");
            writer.destroyForcibly();assertTrue(writer.waitFor(10,TimeUnit.SECONDS));
            try(var recovered=new DurableJournal(log)) {
                assertEquals(List.of(plan),recovered.committed());long bytes=Files.size(log);
                recovered.commit(plan);assertEquals(bytes,Files.size(log));
            }
        } finally {
            if(writer.isAlive()){writer.destroyForcibly();writer.waitFor(10,TimeUnit.SECONDS);}
        }
    }
    private static String read(Path path){try{return Files.readString(path);}catch(Exception ex){return ex.toString();}}

    public static class Writer {
        public static void main(String[] args)throws Exception {
            var journal=new DurableJournal(Path.of(args[0]));
            journal.commit(OperationCodec.decode(Base64.getDecoder().decode(args[2])));
            Files.writeString(Path.of(args[1]),"committed");
            Thread.sleep(TimeUnit.MINUTES.toMillis(2));
            journal.close();
        }
    }
}
