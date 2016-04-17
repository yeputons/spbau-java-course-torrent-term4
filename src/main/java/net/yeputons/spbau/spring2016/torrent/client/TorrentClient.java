package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

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
            new Thread(() -> {
                try {
                    downloadFile(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        try {
            seeder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(FileDescription description) throws IOException {
        FileEntry entry = description.getEntry();
        int fileId = entry.getId();
        BitSet downloaded = description.getDownloaded();
        int partsCount = description.getPartsCount();

        while (downloaded.cardinality() < partsCount) {
            List<InetSocketAddress> sources = tracker.makeRequest(new SourcesRequest(fileId));
            for (InetSocketAddress source : sources) {
                try (TorrentConnection peer = new TorrentConnection(source)) {
                    List<Integer> partsAvailable = peer.makeRequest(new StatRequest(fileId));
                    for (int partId : partsAvailable) {
                        if (!downloaded.get(partId)) {
                            ByteBuffer data = peer.makeRequest(
                                    new GetRequest(fileId, partId, description.getPartSize(partId)));
                            RandomAccessFile file = state.getFile(fileId);
                            synchronized (file) {
                                file.seek(description.getPartStart(partId));
                                file.write(data.array());
                            }
                            downloaded.flip(partId);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            try {
                // CHECKSTYLE.OFF: MagicNumber
                Thread.sleep(1000);
                // CHECKSTYLE.ON: MagicNumber
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
