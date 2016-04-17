package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.Collectors;

public class TorrentSeeder {
    private final TorrentConnection tracker;
    private final ClientState state;
    private ServerSocket listener;
    private Thread updatingThread;
    private Thread listeningThread;

    public TorrentSeeder(TorrentConnection tracker, ClientState state) {
        this.tracker = tracker;
        this.state = state;
    }

    public void start() throws IOException {
        if (listener != null) {
            throw new IllegalStateException("Already started");
        }
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress("0.0.0.0", 0));

        updatingThread = new Thread(() -> {
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
        });
        updatingThread.start();

        listeningThread = new Thread(() -> {
            while (true) {
                final Socket peer;
                try {
                    peer = listener.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
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
        });
        listeningThread.start();
    }

    public void shutdown() {
        try {
            listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void join() throws InterruptedException {
        updatingThread.join();
        listeningThread.join();
    }

    private void updateTracker(int seedingPort) throws IOException {
        tracker.makeRequest(new UpdateRequest(
                seedingPort, state.getFiles().keySet().stream().collect(Collectors.toList())));
    }

    private void processPeer(final Socket peer, DataInputStream in, final DataOutputStream out)
            throws IOException {
        while (true) {
            ClientRequest.readRequest(in).visit(new ClientRequestVisitor() {
                @Override
                public void accept(StatRequest r) throws IOException {
                    synchronized (state) {
                        FileDescription description = state.getFileDescription(r.getFileId());
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
                        description = state.getFileDescription(fileId);
                        if (description == null || !description.getDownloaded().get(r.getPartId())) {
                            peer.close();
                            return;
                        }
                    }
                    RandomAccessFile file = state.getFile(r.getFileId());
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
