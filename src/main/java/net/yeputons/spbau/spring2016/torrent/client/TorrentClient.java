package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;

import java.io.*;

public class TorrentClient implements Runnable {
    private final TorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private TorrentSeeder seeder;

    public TorrentClient(TorrentConnection tracker, StateHolder<ClientState> stateHolder) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        seeder = new TorrentSeeder(tracker, stateHolder.getState());
    }

    @Override
    public void run() {
        System.out.println("Starting TorrentClient, files:");
        for (FileDescription f : stateHolder.getState().getFiles().values()) {
            System.out.println(f);
            new TorrentLeecher(tracker, stateHolder, f).start();
        }

        try {
            seeder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
