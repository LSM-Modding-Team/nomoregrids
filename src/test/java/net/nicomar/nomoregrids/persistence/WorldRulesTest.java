package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.nicomar.nomoregrids.network.ProtocolRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldRulesTest {
    @TempDir Path directory;
    @Test void missingAdapterKeepsItsVersionAndChangedVersionNeedsMigration()throws Exception{
        Path file=directory.resolve("rules.json");String hash="0".repeat(64);
        var original=ProtocolRules.defaults(Map.of("nomoregrids:box",1,"nomoregrids:paintableblocks",1));WorldRules.verifyOrCreate(file,hash,original);byte[] bytes=Files.readAllBytes(file);
        WorldRules.verifyOrCreate(file,hash,ProtocolRules.defaults(Map.of("nomoregrids:box",1)));assertArrayEquals(bytes,Files.readAllBytes(file));
        assertThrows(IOException.class,()->WorldRules.verifyOrCreate(file,hash,ProtocolRules.defaults(Map.of("nomoregrids:box",1,"nomoregrids:paintableblocks",2))));assertArrayEquals(bytes,Files.readAllBytes(file));
        assertThrows(IOException.class,()->WorldRules.verifyOrCreate(file,"1".repeat(64),original));assertArrayEquals(bytes,Files.readAllBytes(file));
    }
    @Test void unknownSchemaIsRetained()throws Exception{
        Path file=directory.resolve("future.json");String future="{\"schema\":99,\"payload\":\"retain\"}";Files.writeString(file,future);
        assertThrows(IOException.class,()->WorldRules.verifyOrCreate(file,"0".repeat(64),ProtocolRules.defaults(Map.of("nomoregrids:box",1))));assertEquals(future,Files.readString(file));
    }
}
