package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        TorrentConnection tracker = mock(TorrentConnection.class);
        when(tracker.makeRequest(new SourcesRequest(1))).thenReturn(addresses);

        Path downloadsDir = rootFolder.getRoot().toPath();
        ClientState state = new ClientState(downloadsDir);
        state.getFiles().put(fileDescription.getEntry().getId(), fileDescription);
        TorrentLeecher leecher = new TorrentLeecher(tracker, state, fileDescription);
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
