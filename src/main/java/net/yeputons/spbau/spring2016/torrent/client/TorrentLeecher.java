package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.FirmTorrentConnection;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TorrentLeecher {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentLeecher.class);
    private final FirmTorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private final FileDescription fileDescription;
    private final ScheduledExecutorService executorService;
    private final CountDownLatch finishedLatch = new CountDownLatch(1);
    private final int retryDelay;

    public TorrentLeecher(FirmTorrentConnection tracker,
                          StateHolder<ClientState> stateHolder,
                          FileDescription fileDescription,
                          ScheduledExecutorService executorService) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        this.fileDescription = fileDescription;
        this.executorService = executorService;
        retryDelay = Integer.parseInt(System.getProperty("torrent.retry_delay", "1000"));
    }

    public void start() {
        LOG.info("Started downloading {}, retry delay is {}", fileDescription.getEntry(), retryDelay);
        this.executorService.submit(new LeechTask());
    }

    public void join() throws InterruptedException {
        finishedLatch.await();
    }

    private class LeechTask implements Runnable {
        private final FileEntry entry = fileDescription.getEntry();
        private final int fileId = entry.getId();
        private BitSet downloaded;

        LeechTask() {
            synchronized (stateHolder.getState()) {
                downloaded = (BitSet) fileDescription.getDownloaded().clone();
            }
        }

        @Override
        public void run() {
            if (isDownloadFinished()) {
                LOG.info("Downloading of {} is finished", entry);
                finishedLatch.countDown();
                return;
            }

            List<InetSocketAddress> sources = getSources();
            if (sources == null) {
                LOG.debug("Sleeping for {} msec", retryDelay);
                executorService.schedule(this, retryDelay, TimeUnit.MILLISECONDS);
                return;
            }

            if (downloadSomething(sources)) {
                LOG.debug("Starting next iteration right away");
                executorService.submit(this);
            } else {
                LOG.debug("Sleeping for {} msec", retryDelay);
                executorService.schedule(this, retryDelay, TimeUnit.MILLISECONDS);
            }
        }

        private boolean isDownloadFinished() {
            int partsCount = fileDescription.getPartsCount();
            if (downloaded.cardinality() >= partsCount) {
                return true;
            }
            LOG.debug("Downloaded: {}/{}", downloaded.cardinality(), partsCount);
            return false;
        }

        private List<InetSocketAddress> getSources() {
            List<InetSocketAddress> sources = null;
            try {
                sources = tracker.makeRequest(new SourcesRequest(fileId));
            } catch (IOException e) {
                LOG.error("Unable to request sources from tracker", e);
                return null;
            }
            Collections.shuffle(sources);
            LOG.debug("Sources: {}", sources);
            return sources;
        }

        private boolean downloadSomething(List<InetSocketAddress> sources) {
            for (InetSocketAddress source : sources) {
                try (TorrentConnection peer = TorrentConnection.connect(source)) {
                    List<Integer> partsAvailable = peer.makeRequest(new StatRequest(fileId));
                    LOG.debug("Peer {} has {} parts available", source, partsAvailable.size());
                    for (int partId : partsAvailable) {
                        if (downloaded.get(partId)) {
                            continue;
                        }
                        LOG.debug("Retrieving part {} from {}", partId, source);
                        ByteBuffer data = peer.makeRequest(
                                new GetRequest(fileId, partId, fileDescription.getPartSize(partId)));
                        try {
                            saveFile(data, partId);
                            return true;
                        } catch (IOException e) {
                            LOG.error("Error while saving file, will retry", e);
                            return false;
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Error while communicating with peer", e);
                }
            }
            return false;
        }

        private void saveFile(ByteBuffer data, int partId) throws IOException {
            ClientState state = stateHolder.getState();
            RandomAccessFile file = state.getFile(fileId);
            synchronized (file) {
                file.seek(fileDescription.getPartStart(partId));
                file.write(data.array());
            }
            synchronized (state) {
                downloaded.flip(partId);
                fileDescription.getDownloaded().flip(partId);
                try {
                    stateHolder.save();
                } catch (IOException e) {
                    downloaded.flip(partId);
                    fileDescription.getDownloaded().flip(partId);
                    throw e;
                }
            }
        }
    }
}
