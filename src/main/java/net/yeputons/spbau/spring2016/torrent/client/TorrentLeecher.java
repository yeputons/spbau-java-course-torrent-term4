package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.GetRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.SourcesRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.StatRequest;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class TorrentLeecher {
    private final TorrentConnection tracker;
    private final ClientState state;
    private final FileDescription fileDescription;
    private Thread thread;

    public TorrentLeecher(TorrentConnection tracker, ClientState state, FileDescription fileDescription) {
        this.tracker = tracker;
        this.state = state;
        this.fileDescription = fileDescription;
    }

    public void start() {
        thread = new Thread(() -> {
            try {
                downloadFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    private void downloadFile() throws IOException {
        FileEntry entry = fileDescription.getEntry();
        int fileId = entry.getId();
        BitSet downloaded = fileDescription.getDownloaded();
        int partsCount = fileDescription.getPartsCount();

        while (downloaded.cardinality() < partsCount) {
            List<InetSocketAddress> sources = tracker.makeRequest(new SourcesRequest(fileId));
            for (InetSocketAddress source : sources) {
                try (TorrentConnection peer = new TorrentConnection(source)) {
                    List<Integer> partsAvailable = peer.makeRequest(new StatRequest(fileId));
                    for (int partId : partsAvailable) {
                        if (!downloaded.get(partId)) {
                            ByteBuffer data = peer.makeRequest(
                                    new GetRequest(fileId, partId, fileDescription.getPartSize(partId)));
                            RandomAccessFile file = state.getFile(fileId);
                            synchronized (file) {
                                file.seek(fileDescription.getPartStart(partId));
                                file.write(data.array());
                            }
                            downloaded.flip(partId);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            try {
                // CHECKSTYLE.OFF: MagicNumber
                Thread.sleep(1000);
                // CHECKSTYLE.ON: MagicNumber
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
