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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TorrentLeecher {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentLeecher.class);
    private static final int RETRY_DELAY = 1000;
    private final TorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private final FileDescription fileDescription;
    private final ScheduledExecutorService executorService;
    private final CountDownLatch finishedLatch = new CountDownLatch(1);

    public TorrentLeecher(TorrentConnection tracker,
                          StateHolder<ClientState> stateHolder,
                          FileDescription fileDescription,
                          ScheduledExecutorService executorService) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        this.fileDescription = fileDescription;
        this.executorService = executorService;
    }

    public void start() {
        LOG.info("Started downloading {}", fileDescription.getEntry());
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
            int partsCount = fileDescription.getPartsCount();
            if (downloaded.cardinality() >= partsCount) {
                LOG.info("Downloading of {} is finished", entry);
                finishedLatch.countDown();
                return;
            }

            LOG.debug("Downloaded: {}/{}", downloaded.cardinality(), partsCount);
            List<InetSocketAddress> sources = null;
            try {
                sources = tracker.makeRequest(new SourcesRequest(fileId));
            } catch (IOException e) {
                LOG.error("Unable to request sources from tracker, will retry", e);
                executorService.schedule(this, RETRY_DELAY, TimeUnit.MILLISECONDS);
                return;
            }
            Collections.shuffle(sources);
            LOG.debug("Sources: {}", sources);

            boolean downloadedSomething = false;
            loopForSources:
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
                        ClientState state = stateHolder.getState();
                        try {
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
                                }
                            }
                            downloadedSomething = true;
                            break loopForSources;
                        } catch (IOException e) {
                            LOG.error("Error while saving file, will retry", e);
                            executorService.schedule(this, RETRY_DELAY, TimeUnit.MILLISECONDS);
                            return;
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Error while communicating with peer", e);
                }
            }
            if (!downloadedSomething) {
                LOG.debug("Sleeping until next iteration");
                executorService.schedule(this, RETRY_DELAY, TimeUnit.MILLISECONDS);
            } else {
                LOG.debug("Starting next iteration right away");
                executorService.submit(this);
            }
        }
    }
}
