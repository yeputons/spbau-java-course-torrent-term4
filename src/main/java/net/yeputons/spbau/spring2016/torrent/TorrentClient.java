package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class TorrentClient implements Runnable {
    private final TorrentConnection tracker;
    private final ClientState state;
    private final Map<Integer, RandomAccessFile> files = new HashMap<>();

    public TorrentClient(TorrentConnection tracker, ClientState state) {
        this.tracker = tracker;
        this.state = state;
    }

    @Override
    public void run() {
        System.out.println("Starting TorrentClient, files:");
        for (FileDescription f : state.getFiles().values()) {
            System.out.println(f);

            try {
                files.put(f.getEntry().getId(), new RandomAccessFile(f.getEntry().getName(), "rw"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
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

        try (ServerSocket listener = new ServerSocket()) {
            new Thread(() -> {
                while (true) {
                    try {
                        updateTracker(listener.getLocalPort());
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    try {
                        // CHECKSTYLE.OFF: MagicNumber
                        Thread.sleep(1000);
                        // CHECKSTYLE.ON: MagicNumber
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (true) {
                final Socket peer = listener.accept();
                new Thread(() -> {
                    try (DataInputStream in = new DataInputStream(peer.getInputStream());
                         DataOutputStream out = new DataOutputStream(peer.getOutputStream())) {
                        processPeer(peer, in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            peer.close();
                        } catch (IOException ignored) {
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(FileDescription description) throws IOException {
        FileEntry entry = description.getEntry();
        int fileId = entry.getId();
        BitSet downloaded = description.getDownloaded();

        while (downloaded.cardinality() < downloaded.size()) {
            List<InetSocketAddress> sources = tracker.makeRequest(new SourcesRequest(fileId));
            for (InetSocketAddress source : sources) {
                try (TorrentConnection peer = new TorrentConnection(source)) {
                    List<Integer> partsAvailable = peer.makeRequest(new StatRequest(fileId));
                    for (int partId : partsAvailable) {
                        if (!downloaded.get(partId)) {
                            ByteBuffer data = peer.makeRequest(
                                    new GetRequest(fileId, partId, description.getPartSize(partId)));
                            RandomAccessFile file = files.get(fileId);
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

    private void updateTracker(int seedingPort) throws IOException {
        tracker.makeRequest(new UpdateRequest(
                seedingPort, files.keySet().stream().collect(Collectors.toList())));
    }

    private void processPeer(final Socket peer, DataInputStream in, final DataOutputStream out)
            throws IOException {
        while (true) {
            ClientRequest.readRequest(in).visit(new ClientRequestVisitor() {
                @Override
                public void accept(StatRequest r) throws IOException {
                    synchronized (state) {
                        FileDescription description = state.getFiles().get(r.getFileId());
                        if (description != null) {
                            r.answerTo(out,
                                description.getDownloaded().stream().boxed().collect(Collectors.toList()));
                        } else {
                            r.answerTo(out, Collections.emptyList());
                        }
                    }
                }

                @Override
                public void accept(GetRequest r) throws IOException {
                    int fileId = r.getFileId();
                    int partId = r.getPartId();
                    FileDescription description;
                    synchronized (state) {
                        description = state.getFiles().get(fileId);
                        if (description == null || !description.getDownloaded().get(r.getPartId())) {
                            peer.close();
                            return;
                        }
                    }
                    RandomAccessFile file = files.get(r.getFileId());
                    byte[] data = new byte[description.getPartSize(partId)];
                    synchronized (file) {
                        file.seek(description.getPartStart(partId));
                        file.readFully(data);
                    }
                    r.answerTo(out, ByteBuffer.wrap(data));
                }
            });
        }
    }
}
