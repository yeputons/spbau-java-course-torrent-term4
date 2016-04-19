package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.client.ConsoleClient;
import net.yeputons.spbau.spring2016.torrent.tracker.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            help();
        }
        if (args[0].equals("server")) {
            if (args.length != 1) {
                System.err.println("No extra arguments expected for server");
            }

            TrackerServer server = new TrackerServer(Paths.get("tracker-server-data.bin"));
            try {
                server.start();
            } catch (IOException e) {
                LOG.error("Cannot start server", e);
            }
        } else if (args[0].equals("client")) {
            new ConsoleClient(Arrays.copyOfRange(args, 1, args.length)).run();
        } else {
            help();
        }
    }

    private static void help() {
        System.err.println("Expected arguments: (server|client) [extra]");
        System.exit(1);
    }
}
