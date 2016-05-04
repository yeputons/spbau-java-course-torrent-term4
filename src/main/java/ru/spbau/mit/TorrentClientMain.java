package ru.spbau.mit;

import net.yeputons.spbau.spring2016.torrent.client.ConsoleClient;

public final class TorrentClientMain {
    private TorrentClientMain() {
    }

    public static void main(String[] args) {
        new ConsoleClient(args).run();
    }
}
