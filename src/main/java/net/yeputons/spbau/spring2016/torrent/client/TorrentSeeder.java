package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.SocketDataStreamsWrapper;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.Collectors;

public class TorrentSeeder {
    private static final int DEFAULT_UPDATE_INTERVEL = 10 * 1000;
    private static final Logger LOG = LoggerFactory.getLogger(TorrentSeeder.class);

    private final TorrentConnection tracker;
    private final ClientState state;
    private ServerSocket listener;
    private Thread updatingThread;
    private Thread listeningThread;
    private final int updateInterval;

    public TorrentSeeder(TorrentConnection tracker, ClientState state) {
        this(tracker, state, DEFAULT_UPDATE_INTERVEL);
    }

    public TorrentSeeder(TorrentConnection tracker, ClientState state, int updateInterval) {
        this.tracker = tracker;
        this.state = state;
        this.updateInterval = updateInterval;
    }

    public void start() throws IOException {
        if (listener != null) {
            throw new IllegalStateException("Already started");
        }
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress("0.0.0.0", 0));

        updatingThread = new Thread(() -> {
            while (!listener.isClosed()) {
                try {
                    updateTracker(listener.getLocalPort());
                } catch (IOException e) {
                    LOG.warn("Error while making update to tracker", e);
                }
                try {
                    Thread.sleep(updateInterval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updatingThread.start();

        listeningThread = new Thread(() -> {
            while (true) {
                final Socket peer;
                try {
                    peer = listener.accept();
                } catch (IOException ignored) {
                    break;
                }
                new Thread(() -> {
                    try (SocketDataStreamsWrapper wrapper = new SocketDataStreamsWrapper(peer)) {
                        processPeer(peer, wrapper.getInputStream(), wrapper.getOutputStream());
                    } catch (IOException e) {
                        LOG.warn("Error while processing connection from peer", e);
                    }
                }).start();
            }
        });
        listeningThread.start();
    }

    public SocketAddress getAddress() {
        return listener.getLocalSocketAddress();
    }

    public void shutdown() throws IOException {
        updatingThread.interrupt();
        listener.close();
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
            ClientRequest<?> request;
            try {
                request = ClientRequest.readRequest(in);
            } catch (NoRequestException ignored) {
                break;
            }
            request.visit(new ClientRequestVisitor() {
                @Override
                public void accept(StatRequest r) throws IOException {
                    FileDescription description = state.getFileDescription(r.getFileId());
                    if (description != null) {
                        synchronized (state) {
                            r.answerTo(out,
                                    description.getDownloaded().stream().boxed().collect(Collectors.toList()));
                        }
                    } else {
                        r.answerTo(out, Collections.emptyList());
                    }
                }

                @Override
                public void accept(GetRequest r) throws IOException {
                    int fileId = r.getFileId();
                    int partId = r.getPartId();
                    FileDescription description = state.getFileDescription(fileId);
                    synchronized (state) {
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
