package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;

import java.io.*;

public class TorrentClient implements Runnable {
    private final TorrentConnection tracker;
    private final ClientState state;
    private TorrentSeeder seeder;

    public TorrentClient(TorrentConnection tracker, ClientState state) {
        this.tracker = tracker;
        this.state = state;
        seeder = new TorrentSeeder(tracker, state);
    }

    @Override
    public void run() {
        System.out.println("Starting TorrentClient, files:");
        for (FileDescription f : state.getFiles().values()) {
            System.out.println(f);
        }

        for (FileDescription f : state.getFiles().values()) {
            new TorrentLeecher(tracker, state, f).start();
        }

        try {
            seeder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
