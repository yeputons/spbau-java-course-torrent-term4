package net.yeputons.spbau.spring2016.torrent;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class SeedersTrackerTest {
    private <T> void assertEqualsNoOrder(List<T> a, List<T> b) {
        assertEquals(new HashSet<T>(a), new HashSet<T>(b));
    }

    @Test
    public void testSeedersTracker() throws InterruptedException {
        // CHECKSTYLE.OFF: MagicNumber
        SeedersTracker tracker = new SeedersTracker(100);
        assertEqualsNoOrder(Collections.emptyList(), tracker.getSources(1));
        assertEqualsNoOrder(Collections.emptyList(), tracker.getSources(2));

        final InetSocketAddress client1 = new InetSocketAddress("127.0.0.1", 123);
        final InetSocketAddress client2 = new InetSocketAddress("127.0.0.1", 456);
        final InetSocketAddress client3 = new InetSocketAddress("127.0.0.2", 123);
        final InetSocketAddress client4 = new InetSocketAddress("127.0.0.2", 456);

        // Step 1: add some clients
        tracker.updateSource(1, client1);
        tracker.updateSource(2, client1);
        tracker.updateSource(1, client3);
        tracker.updateSource(1, client2);
        tracker.updateSource(2, client3);
        assertEqualsNoOrder(Arrays.asList(client1, client2, client3), tracker.getSources(1));
        assertEqualsNoOrder(Arrays.asList(client1, client3), tracker.getSources(2));

        // Step 2: nothing should expire yet, let's update some clients
        Thread.sleep(60);
        assertEqualsNoOrder(Arrays.asList(client1, client2, client3), tracker.getSources(1));
        assertEqualsNoOrder(Arrays.asList(client1, client3), tracker.getSources(2));
        tracker.updateSource(1, client1);
        tracker.updateSource(2, client1);
        tracker.updateSource(1, client3);
        tracker.updateSource(2, client4);
        assertEqualsNoOrder(Arrays.asList(client1, client2, client3), tracker.getSources(1));
        assertEqualsNoOrder(Arrays.asList(client1, client3, client4), tracker.getSources(2));

        // Step 3: clients not updated on step 2 should expire
        Thread.sleep(60);
        assertEqualsNoOrder(Arrays.asList(client1, client3), tracker.getSources(1));
        assertEqualsNoOrder(Arrays.asList(client1, client4), tracker.getSources(2));

        // Step 4: all clients should expire
        Thread.sleep(60);
        assertEqualsNoOrder(Collections.emptyList(), tracker.getSources(1));
        assertEqualsNoOrder(Collections.emptyList(), tracker.getSources(2));
        // CHECKSTYLE.ON: MagicNumber
    }
}
