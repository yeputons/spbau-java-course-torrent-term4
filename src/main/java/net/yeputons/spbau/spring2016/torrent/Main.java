package net.yeputons.spbau.spring2016.torrent;

import java.nio.file.Paths;
import java.util.Arrays;

public final class Main {
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
            server.run();
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
