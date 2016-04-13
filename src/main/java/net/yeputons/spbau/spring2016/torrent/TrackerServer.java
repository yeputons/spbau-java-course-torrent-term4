package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TrackerServer implements Runnable {
    public static final int DEFAULT_PORT = 8081;

    private final List<FileEntry> files = new ArrayList<>();
    private int freeFileId = 1;
    private final SeedersTracker seeders = new SeedersTracker(60 * 1000);

    private final int port;

    private Path autoSaveStorage;

    private ServerSocket server;
    private boolean shuttedDown = false;

    public TrackerServer() {
        this(DEFAULT_PORT);
    }
    public TrackerServer(int port) {
        this.port = port;
    }

    public Path getAutoSaveStorage() {
        return autoSaveStorage;
    }

    public void setAutoSaveStorage(Path autoSaveStorage) {
        this.autoSaveStorage = autoSaveStorage;
    }

    private void autoSave() {
        Path storage = autoSaveStorage;
        if (storage != null) {
            try {
                saveTo(storage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void restoreFrom(Path storage) throws IOException {
        synchronized (files) {
            try (DataInputStream in = new DataInputStream(Files.newInputStream(storage))) {
                freeFileId = in.readInt();
                int count = in.readInt();
                files.clear();
                for (int i = 0; i < count; i++) {
                    files.add(FileEntry.readFrom(in));
                }
            }
        }
    }

    public void saveTo(Path storage) throws IOException {
        synchronized (files) {
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(storage))) {
                out.writeInt(freeFileId);
                out.writeInt(files.size());
                for (FileEntry e : files) {
                    e.writeTo(out);
                }
            }
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
                                    synchronized (files) {
                                        r.answerTo(out, files);
                                    }
                                }

                                @Override
                                public void accept(UploadRequest r) throws IOException {
                                    int id;
                                    synchronized (files) {
                                        id = freeFileId;
                                        freeFileId++;
                                        files.add(new FileEntry(id, r.getFileName(), r.getSize()));
                                        autoSave();
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
