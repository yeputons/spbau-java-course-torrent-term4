package net.yeputons.spbau.spring2016.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.spbau.mit.TorrentClientMain;
import ru.spbau.mit.TorrentTrackerMain;

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
            TorrentTrackerMain.main(Arrays.copyOfRange(args, 1, args.length));
        } else if (args[0].equals("client")) {
            TorrentClientMain.main(Arrays.copyOfRange(args, 1, args.length));
        } else {
            help();
        }
    }

    private static void help() {
        System.err.println("Expected arguments: (server|client) [extra]");
        System.exit(1);
    }
}
