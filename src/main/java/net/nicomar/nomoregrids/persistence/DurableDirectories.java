package net.nicomar.nomoregrids.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Persists every newly created directory entry before a receipt can depend on its child file. */
public final class DurableDirectories {
    private DurableDirectories(){}
    public static void create(Path directory)throws IOException{
        var missing=new ArrayList<Path>();Path current=directory.toAbsolutePath();while(current!=null&&!Files.exists(current)){missing.add(current);current=current.getParent();}
        Files.createDirectories(directory);Collections.reverse(missing);
        for(Path created:missing){force(created);if(created.getParent()!=null)force(created.getParent());}
    }
    public static void force(Path directory)throws IOException{try(var channel=FileChannel.open(directory,StandardOpenOption.READ)){channel.force(true);}}
}
