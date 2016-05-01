package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class TorrentClient {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentClient.class);
    private static final int LEECHER_TASKS_POOL_SIZE = 10;
    private final TorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private TorrentSeeder seeder;
    private List<TorrentLeecher> leechers;
    private ScheduledExecutorService leechersPool;

    public TorrentClient(TorrentConnection tracker, StateHolder<ClientState> stateHolder) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        seeder = new TorrentSeeder(tracker, stateHolder.getState());
    }

    public void start() {
        Collection<FileDescription> files = stateHolder.getState().getFiles().values();
        LOG.info("Starting TorrentClient, files are:\n"
                + files.stream()
                        .map(FileDescription::toString)
                        .collect(Collectors.joining("\n"))
        );
        leechers = new ArrayList<>();
        leechersPool = Executors.newScheduledThreadPool(LEECHER_TASKS_POOL_SIZE);
        for (FileDescription f : files) {
            TorrentLeecher leecher = new TorrentLeecher(tracker, stateHolder, f, leechersPool);
            leecher.start();
            leechers.add(leecher);
        }

        try {
            seeder.start();
        } catch (IOException e) {
            LOG.error("Cannot start seeder", e);
        }
    }

    public void join() throws InterruptedException {
        seeder.join();
        for (TorrentLeecher leecher : leechers) {
            leecher.join();
        }
    }
}
