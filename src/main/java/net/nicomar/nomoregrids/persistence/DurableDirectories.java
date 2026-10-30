package net.nicomar.nomoregrids.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Forces directory metadata where the platform supports opening directory channels. */
public final class DurableDirectories {
    private DurableDirectories(){}
    public static void create(Path directory)throws IOException{
        var missing=new ArrayList<Path>();Path current=directory.toAbsolutePath();while(current!=null&&!Files.exists(current)){missing.add(current);current=current.getParent();}
        Files.createDirectories(directory);Collections.reverse(missing);
        for(Path created:missing){force(created);if(created.getParent()!=null)force(created.getParent());}
    }
    private static final java.util.concurrent.atomic.AtomicBoolean WINDOWS_NOTICE=new java.util.concurrent.atomic.AtomicBoolean();
    public static void force(Path directory)throws IOException{
        var attributes=Files.readAttributes(directory,java.nio.file.attribute.BasicFileAttributes.class);
        if(!attributes.isDirectory())throw new NotDirectoryException(directory.toString());
        // Windows NIO cannot open directory FileChannels. Never suppress file flush or real I/O failures.
        if(directory.getFileSystem().provider().getClass().getName().equals("sun.nio.fs.WindowsFileSystemProvider")){
            if(WINDOWS_NOTICE.compareAndSet(false,true))System.getLogger(DurableDirectories.class.getName()).log(System.Logger.Level.WARNING,"Windows NIO does not support directory fsync; files remain forced and replacements atomic, but directory metadata durability across power loss is not guaranteed.");
            return;
        }
        try(var channel=FileChannel.open(directory,StandardOpenOption.READ)){channel.force(true);}
    }
}
