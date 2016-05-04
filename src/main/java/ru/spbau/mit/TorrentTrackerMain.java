package ru.spbau.mit;

import net.yeputons.spbau.spring2016.torrent.tracker.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public final class TorrentTrackerMain {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentTrackerMain.class);

    private TorrentTrackerMain() {
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("No arguments expected for the server");
            System.exit(1);
        }

        TrackerServer server = new TrackerServer(Paths.get("tracker-server-data.bin"));
        try {
            server.start();
        } catch (IOException e) {
            LOG.error("Cannot start server", e);
        }
    }
}
