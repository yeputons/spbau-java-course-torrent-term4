package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.GetRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.SourcesRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.StatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class TorrentLeecher {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentLeecher.class);
    private static final int RETRY_DELAY = 1000;
    private final TorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private final FileDescription fileDescription;
    private Thread thread;

    public TorrentLeecher(TorrentConnection tracker,
                          StateHolder<ClientState> stateHolder,
                          FileDescription fileDescription) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        this.fileDescription = fileDescription;
    }

    public void start() {
        thread = new Thread(this::downloadFile);
        thread.start();
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    private void downloadFile() {
        FileEntry entry = fileDescription.getEntry();
        int fileId = entry.getId();
        BitSet downloaded = fileDescription.getDownloaded();
        int partsCount = fileDescription.getPartsCount();

        while (downloaded.cardinality() < partsCount) {
            List<InetSocketAddress> sources = null;
            try {
                sources = tracker.makeRequest(new SourcesRequest(fileId));
            } catch (IOException e) {
                LOG.error("Unable to request sources from tracker", e);
                return;
            }
            for (InetSocketAddress source : sources) {
                try (TorrentConnection peer = TorrentConnection.connect(source)) {
                    List<Integer> partsAvailable = peer.makeRequest(new StatRequest(fileId));
                    for (int partId : partsAvailable) {
                        if (!downloaded.get(partId)) {
                            ByteBuffer data = peer.makeRequest(
                                    new GetRequest(fileId, partId, fileDescription.getPartSize(partId)));
                            ClientState state = stateHolder.getState();
                            try {
                                RandomAccessFile file = state.getFile(fileId);
                                synchronized (file) {
                                    file.seek(fileDescription.getPartStart(partId));
                                    file.write(data.array());
                                }
                                synchronized (state) {
                                    downloaded.flip(partId);
                                    try {
                                        stateHolder.save();
                                    } catch (IOException e) {
                                        downloaded.flip(partId);
                                    }
                                }
                            } catch (IOException e) {
                                LOG.error("Error while saving file", e);
                                return;
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Error while communicating with peer", e);
                }
            }
            try {
                Thread.sleep(RETRY_DELAY);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }
}
