package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.UpdateRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static org.mockito.Mockito.*;

public class TorrentSeederTest {
    private static final FileDescription FILE = new FileDescription(new FileEntry(1, "file1.txt", 100), 35);
    private static final FileDescription FILE2 = new FileDescription(new FileEntry(2, "file2.txt", 50), 35);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ClientState state;

    @Before
    public void initState() {
        state = new ClientState(folder.getRoot().toPath());
        state.getFiles().put(1, FILE);
    }

    @Test
    public void testUpdateTracker() throws IOException, InterruptedException {
        TorrentConnection tracker = mock(TorrentConnection.class);
        // CHECKSTYLE.OFF: MagicNumber
        TorrentSeeder seeder = new TorrentSeeder(tracker, state, 100);
        // CHECKSTYLE.ON: MagicNumber
        seeder.start();

        Semaphore waitingForRequest = new Semaphore(1);
        waitingForRequest.acquire();

        when(tracker.makeRequest(new UpdateRequest(anyInt(), Arrays.asList(1)))).thenAnswer((r) -> {
            waitingForRequest.release();
            return true;
        });
        waitingForRequest.acquire();
        waitingForRequest.acquire();

        state.getFiles().put(2, FILE2);
        when(tracker.makeRequest(new UpdateRequest(anyInt(), Arrays.asList(1, 2)))).thenAnswer((r) -> {
            waitingForRequest.release();
            return true;
        });
        waitingForRequest.acquire();
        waitingForRequest.acquire();

        seeder.shutdown();
        seeder.join();
    }
}
