package net.yeputons.spbau.spring2016.torrent;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            help();
        }
        if (args[0].equals("server")) {
            if (args.length < 2) {
                help();
            }
            new TrackerServer(Integer.parseInt(args[1])).run();
        }
    }

    private static void help() {
        System.err.println("Expected arguments: server [other-arguments]");
        System.err.println("Server's argument: <port>");
        System.exit(1);
    }
}
