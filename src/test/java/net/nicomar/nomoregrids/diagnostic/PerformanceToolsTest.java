package net.nicomar.nomoregrids.diagnostic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PerformanceToolsTest {
    @Test void weightedEvictionHonorsRecencyAndHardByteBudget(){
        var cache=new WeightedLru<String,String>(10);cache.put("a","a",4);cache.put("b","b",4);cache.get("a");cache.put("c","c",4);
        assertTrue(cache.get("b").isEmpty());assertTrue(cache.get("a").isPresent());assertEquals(8,cache.stats().bytes());cache.put("oversize","x",20);assertEquals(8,cache.stats().bytes());
    }
    @Test void unfinishedWorkSurvivesBudgetAndCoalescingKeepsNewest(){
        var queue=new TickWorkQueue<String>(2);var calls=new AtomicLong();queue.submit("region",n->{calls.incrementAndGet();return false;});
        var time=new AtomicLong();queue.advance(3,1,time::getAndIncrement);assertEquals(1,queue.pending());
        queue.submit("region",n->true);queue.advance(3,1,time::getAndIncrement);assertEquals(0,queue.pending());
    }
}
