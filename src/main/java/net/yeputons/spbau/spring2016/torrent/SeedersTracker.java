package net.yeputons.spbau.spring2016.torrent;

import java.net.InetSocketAddress;
import java.util.*;

public class SeedersTracker {
    private final int seederTimeoutMsecs;

    private final Map<Integer, Map<InetSocketAddress, Long>> seeders = new HashMap<>();
    // seeders[fileId][seederAddress] -> lastUpdated

    public SeedersTracker(int seederTimeoutMsecs) {
        this.seederTimeoutMsecs = seederTimeoutMsecs;
    }

    public synchronized List<InetSocketAddress> getSources(int fileId) {
        Map<InetSocketAddress, Long> fileSeeders = seeders.get(fileId);
        if (fileSeeders == null) {
            return Collections.emptyList();
        }
        List<InetSocketAddress> result = new ArrayList<>();
        for (Iterator<Map.Entry<InetSocketAddress, Long>> it
                = fileSeeders.entrySet().iterator();
             it.hasNext();
             ) {
            Map.Entry<InetSocketAddress, Long> seeder = it.next();
            if (seeder.getValue() + seederTimeoutMsecs < System.currentTimeMillis()) {
                it.remove();
            } else {
                result.add(seeder.getKey());
            }
        }
        return result;
    }

    public synchronized void updateSource(int fileId, InetSocketAddress address) {
        Map<InetSocketAddress, Long> fileSeeders = seeders.get(fileId);
        if (fileSeeders == null) {
            fileSeeders = new HashMap<>();
            seeders.put(fileId, fileSeeders);
        }
        fileSeeders.put(address, System.currentTimeMillis());
    }
}
