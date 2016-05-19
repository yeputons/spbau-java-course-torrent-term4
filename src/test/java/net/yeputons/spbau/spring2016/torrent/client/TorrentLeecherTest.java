package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.FirmTorrentConnection;
import net.yeputons.spbau.spring2016.torrent.StateMemoryHolder;
import net.yeputons.spbau.spring2016.torrent.protocol.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TorrentLeecherTest {
    private static final FileEntry FILE = new FileEntry(1, "file1.bin", 9);
    private static final int PART_SIZE = 5;

    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder();

    private final FileDescription fileDescription = new FileDescription(FILE, PART_SIZE);

    @Test
    public void testLeecher() throws Exception {
        // CHECKSTYLE.OFF: MagicNumber
        SimpleSeeder[] seeders = new SimpleSeeder[]{
            new SimpleSeeder(1, 1, new byte[]{15, 16, 17, 18}),
            new SimpleSeeder(1, 0, new byte[]{10, 11, 12, 13, 14})
        };
        // CHECKSTYLE.ON: MagicNumber
        List<InetSocketAddress> addresses = new ArrayList<>();
        for (SimpleSeeder s : seeders) {
            addresses.add((InetSocketAddress) s.start());
        }
        FirmTorrentConnection tracker = mock(FirmTorrentConnection.class);

        AtomicInteger requestId = new AtomicInteger();
        when(tracker.makeRequest(new SourcesRequest(1))).thenAnswer((r) -> {
            if (requestId.incrementAndGet() % 2 == 0) {
                throw new IOException("Test exception");
            }
            return addresses;
        });

        Path downloadsDir = rootFolder.getRoot().toPath();
        ClientState state = new ClientState(downloadsDir);
        state.getFiles().put(fileDescription.getEntry().getId(), fileDescription);
        TorrentLeecher leecher = new TorrentLeecher(
                tracker,
                new StateMemoryHolder<>(state),
                fileDescription,
                // CHECKSTYLE.OFF: MagicNumber
                Executors.newScheduledThreadPool(10)
                // CHECKSTYLE.ON: MagicNumber
        );
        leecher.start();
        leecher.join();

        for (SimpleSeeder s : seeders) {
            s.shutdown();
            s.join();
        }

        byte[] downloaded = Files.readAllBytes(downloadsDir.resolve("file1.bin"));
        // CHECKSTYLE.OFF: MagicNumber
        assertArrayEquals(downloaded, new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18});
        // CHECKSTYLE.ON: MagicNumber
    }
}
