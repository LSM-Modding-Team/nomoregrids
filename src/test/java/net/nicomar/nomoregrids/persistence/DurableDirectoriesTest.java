package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableDirectoriesTest {
    @TempDir Path root;
    @Test void directoryCreationJournalAndReplacementWorkOnTheHostPlatform()throws Exception{
        Path directory=root.resolve("world/nomoregrids/dimension");DurableDirectories.create(directory);DurableDirectories.force(directory);
        try(var journal=new DurableJournal(directory.resolve("operations.log"))){assertTrue(journal.committed().isEmpty());}
        Path file=directory.resolve("checkpoint");SectionStore.atomic(file,new byte[]{1});SectionStore.atomic(file,new byte[]{2});assertArrayEquals(new byte[]{2},Files.readAllBytes(file));
    }
    @Test void invalidTargetsAreStillErrors()throws Exception{
        Path missing=root.resolve("missing");assertThrows(IOException.class,()->DurableDirectories.force(missing));
        Path file=Files.writeString(root.resolve("file"),"data");assertThrows(NotDirectoryException.class,()->DurableDirectories.force(file));
    }
}
