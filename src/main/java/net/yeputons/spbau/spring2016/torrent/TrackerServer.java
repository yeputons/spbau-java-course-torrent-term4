package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;

public class TrackerServer implements Runnable {
    public static final int DEFAULT_PORT = 8081;

    private static class State implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<FileEntry> files = new ArrayList<>();
        private int freeFileId = 1;
    }

    private final StateHolder<State> stateHolder;
    private final SeedersTracker seeders = new SeedersTracker(60 * 1000);

    private final int port;

    private ServerSocket server;
    private boolean shuttedDown = false;

    public TrackerServer() {
        this(DEFAULT_PORT, null);
    }
    public TrackerServer(int port) {
        this(port, null);
    }
    public TrackerServer(Path storage) {
        this(DEFAULT_PORT, storage);
    }
    public TrackerServer(int port, Path storage) {
        this.port = port;
        if (storage != null) {
            stateHolder = new StateFileHolder<State>(storage, new State());
        } else {
            stateHolder = new StateMemoryHolder<State>(new State());
        }
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                if (shuttedDown) {
                    return;
                }
                server = new ServerSocket();
                server.bind(new InetSocketAddress("0.0.0.0", port));
            }
            while (!Thread.interrupted()) {
                final Socket client = server.accept();
                new Thread(() -> {
                    try (DataInputStream in = new DataInputStream(client.getInputStream());
                         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
                        for (;;) {
                            ServerRequest.readRequest(in).visit(new ServerRequestVisitor() {
                                @Override
                                public void accept(ListRequest r) throws IOException {
                                    State s = stateHolder.getState();
                                    synchronized (s) {
                                        r.answerTo(out, s.files);
                                    }
                                }

                                @Override
                                public void accept(UploadRequest r) throws IOException {
                                    int id;
                                    State s = stateHolder.getState();
                                    synchronized (s) {
                                        id = s.freeFileId;
                                        s.freeFileId++;
                                        s.files.add(new FileEntry(id, r.getFileName(), r.getSize()));
                                        stateHolder.save();
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
                    } catch (EOFException ignored) {
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

    public void shutdown() throws IOException {
        synchronized (this) {
            if (server != null) {
                server.close();
            }
            shuttedDown = true;
        }
    }
}
