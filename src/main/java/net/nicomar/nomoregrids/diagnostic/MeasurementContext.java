package net.nicomar.nomoregrids.diagnostic;

import java.nio.file.*;
import java.util.*;

/** Whitelisted non-secret environment facts plus operator-declared hardware/revision labels. */
public final class MeasurementContext {
    private MeasurementContext(){}
    public static Map<String,String> snapshot(){
        var facts=new TreeMap<String,String>();
        for(String key:List.of("java.version","java.vm.name","java.vendor","os.name","os.version","os.arch"))facts.put(key,System.getProperty(key,"unknown"));
        facts.put("processors",Integer.toString(Runtime.getRuntime().availableProcessors()));facts.put("heapMaxBytes",Long.toString(Runtime.getRuntime().maxMemory()));
        facts.put("hardwareTag",System.getProperty("nomoregrids.profileHardware",""));facts.put("revisionTag",System.getProperty("nomoregrids.profileRevision",""));
        facts.put("cgroupCpuMax",read("/sys/fs/cgroup/cpu.max"));facts.put("cgroupMemoryMax",read("/sys/fs/cgroup/memory.max"));return Map.copyOf(facts);
    }
    private static String read(String name){try{var path=Path.of(name);return Files.isRegularFile(path)?Files.readString(path).strip():"unavailable";}catch(Exception ex){return "unavailable";}}
}
