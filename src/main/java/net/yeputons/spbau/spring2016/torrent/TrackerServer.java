package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackerServer implements Runnable {
    public static final int DEFAULT_PORT = 8081;

    private final List<FileEntry> files = new ArrayList<>();
    private final AtomicInteger freeFileId = new AtomicInteger(1);
    private final SeedersTracker seeders = new SeedersTracker(60 * 1000);

    private final int port;

    public TrackerServer() {
        this(DEFAULT_PORT);
    }
    public TrackerServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port);
            while (!Thread.interrupted()) {
                final Socket client = server.accept();
                new Thread(() -> {
                    try (DataInputStream in = new DataInputStream(client.getInputStream());
                         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
                        for (;;) {
                            ServerRequest.readRequest(in).visit(new ServerRequestVisitor() {
                                @Override
                                public void accept(ListRequest r) throws IOException {
                                    synchronized (files) {
                                        r.answerTo(out, files);
                                    }
                                }

                                @Override
                                public void accept(UploadRequest r) throws IOException {
                                    int id = freeFileId.getAndIncrement();
                                    synchronized (files) {
                                        files.add(new FileEntry(id, r.getFileName(), r.getSize()));
                                    }
                                    r.answerTo(out, id);
                                }

                                @Override
                                public void accept(SourcesRequest r) throws IOException {
                                    r.answerTo(out, seeders.getSources(r.getFileId()));
                                }

                                @Override
                                public void accept(UpdateRequest r) throws IOException {
                                    InetSocketAddress address =
                                            new InetSocketAddress(client.getInetAddress(), r.getSeedPort());
                                    for (int fileId : r.getSeedingFiles()) {
                                        seeders.updateSource(fileId, address);
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
