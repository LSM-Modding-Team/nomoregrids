package net.nicomar.nomoregrids.diagnostic;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MeasurementWindowTest {
    @Test void warmupAndLaterFramesCannotAlterTheMeasuredInterval(){
        var samples=new MeasurementWindow(2,100);
        assertFalse(samples.add(999999));assertFalse(samples.add(999999));
        for(int i=1;i<=100;i++)assertEquals(i==100,samples.add(i));
        var report=samples.summary();assertEquals(100,report.samples());assertEquals(50L,report.p50Nanos());assertEquals(95L,report.p95Nanos());assertEquals(99L,report.p99Nanos());assertEquals(50.5,report.meanNanos());
        samples.add(0);assertEquals(report,samples.summary());
    }
    @Test void incompleteOrEmptyWindowsCannotMasqueradeAsFullSamples(){
        var samples=new MeasurementWindow(0,100);assertNull(samples.summary().p95Nanos());assertFalse(samples.summary().complete());
        samples.add(4);assertEquals(1,samples.summary().samples());assertFalse(samples.summary().complete());
        assertThrows(IllegalArgumentException.class,()->samples.add(-1));assertThrows(IllegalArgumentException.class,()->new MeasurementWindow(0,12001));
    }
    @Test void trafficSeparatesEncodedMessagesFromRecipientCopies(){
        var traffic=new TrafficMeter();traffic.prepared(70000,false);traffic.enqueued(65536,2);traffic.enqueued(4464,2);traffic.prepared(100,true);traffic.enqueued(100,0);
        assertEquals(new TrafficMeter.Snapshot(2,1,70100,4,140000),traffic.snapshot());
    }
}
