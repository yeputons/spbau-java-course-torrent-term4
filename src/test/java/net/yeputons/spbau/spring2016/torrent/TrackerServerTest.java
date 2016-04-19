package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.ListRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.UploadRequest;
import net.yeputons.spbau.spring2016.torrent.tracker.TrackerServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TrackerServerTest {
    private static final int SERVER_PORT = 12589;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testAutoSave() throws IOException, InterruptedException {
        Path storage = folder.newFile().toPath();
        Files.delete(storage);

        // CHECKSTYLE.OFF: MagicNumber
        List<FileEntry> expectedFiles = Arrays.asList(
                new FileEntry(1, "file1", 10),
                new FileEntry(2, "file2", 20)
        );
        // CHECKSTYLE.ON: MagicNumber

        TrackerServer server = new TrackerServer(SERVER_PORT, storage);
        server.start();
        try {
            try (TorrentConnection conn = TorrentConnection.connect(
                    new InetSocketAddress("127.0.0.1", SERVER_PORT))) {
                assertEquals(Collections.emptyList(), conn.makeRequest(new ListRequest()));
                for (FileEntry e : expectedFiles) {
                    assertEquals(Integer.valueOf(e.getId()),
                            conn.makeRequest(new UploadRequest(e.getName(), e.getSize())));
                }
                assertEquals(expectedFiles, conn.makeRequest(new ListRequest()));
            }
        } finally {
            server.shutdown();
            server.join();
        }

        server = new TrackerServer(SERVER_PORT, storage);
        server.start();
        try {
            try (TorrentConnection conn = TorrentConnection.connect(
                    new InetSocketAddress("127.0.0.1", SERVER_PORT))) {
                assertEquals(expectedFiles, conn.makeRequest(new ListRequest()));
            }
        } finally {
            server.shutdown();
            server.join();
        }
    }
}
