package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TorrentClient {
    private final TorrentConnection tracker;
    private final StateHolder<ClientState> stateHolder;
    private TorrentSeeder seeder;
    private List<TorrentLeecher> leechers;

    public TorrentClient(TorrentConnection tracker, StateHolder<ClientState> stateHolder) {
        this.tracker = tracker;
        this.stateHolder = stateHolder;
        seeder = new TorrentSeeder(tracker, stateHolder.getState());
    }

    public void start() {
        System.out.println("Starting TorrentClient, files:");
        leechers = new ArrayList<>();
        for (FileDescription f : stateHolder.getState().getFiles().values()) {
            System.out.println(f);
            TorrentLeecher leecher = new TorrentLeecher(tracker, stateHolder, f);
            leecher.start();
            leechers.add(leecher);
        }

        try {
            seeder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void join() throws InterruptedException {
        seeder.join();
        for (TorrentLeecher leecher : leechers) {
            leecher.join();
        }
    }
}
