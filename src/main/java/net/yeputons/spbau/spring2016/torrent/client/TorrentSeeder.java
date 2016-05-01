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
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class TorrentSeeder {
    private static final int DEFAULT_UPDATE_INTERVEL = 10 * 1000;
    private static final Logger LOG = LoggerFactory.getLogger(TorrentSeeder.class);

    private final TorrentConnection tracker;
    private final ClientState state;
    private ServerSocket listener;
    private Timer updatingTimer;
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
        LOG.info("Started seeder on {}", listener.getLocalSocketAddress());

        updatingTimer = new Timer("updatingTimer");
        updatingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (listener.isClosed()) {
                    LOG.info("Listener socket is closed, do not send updates to tracker anymore");
                    updatingTimer.cancel();
                    return;
                }
                try {
                    updateTracker(listener.getLocalPort());
                } catch (IOException e) {
                    LOG.warn("Error while making update to tracker", e);
                }
            }
        }, 0, updateInterval);

        listeningThread = new Thread(() -> {
            while (true) {
                final Socket peer;
                try {
                    peer = listener.accept();
                } catch (IOException ignored) {
                    break;
                }
                LOG.info("New client from {}", peer.getRemoteSocketAddress());
                new Thread(() -> {
                    try (SocketDataStreamsWrapper wrapper = new SocketDataStreamsWrapper(peer)) {
                        processPeer(peer, wrapper.getInputStream(), wrapper.getOutputStream());
                    } catch (IOException e) {
                        LOG.warn("Error while processing connection from peer", e);
                    }
                    LOG.info("Client disconnected");
                }).start();
            }
        });
        listeningThread.start();
    }

    public SocketAddress getAddress() {
        return listener.getLocalSocketAddress();
    }

    public void shutdown() throws IOException {
        updatingTimer.cancel();
        listener.close();
    }

    public void join() throws InterruptedException {
        updatingTimer.cancel();
        listeningThread.join();
    }

    private void updateTracker(int seedingPort) throws IOException {
        UpdateRequest request = new UpdateRequest(
                seedingPort, state.getFiles().keySet().stream().collect(Collectors.toList()));
        LOG.debug("Making {}", request);
        boolean result = tracker.makeRequest(request);
        LOG.debug("Updated tracker, result={}", result);
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
            LOG.debug("Incoming request: {}", request);
            request.visit(new ClientRequestVisitor() {
                @Override
                public void accept(StatRequest r) throws IOException {
                    FileDescription description = state.getFileDescription(r.getFileId());
                    if (description != null) {
                        synchronized (state) {
                            LOG.debug("Answering: {}/{} parts are downloaded",
                                    description.getDownloaded().cardinality(),
                                    description.getPartsCount()
                                    );
                            r.answerTo(out,
                                    description.getDownloaded().stream().boxed().collect(Collectors.toList()));
                        }
                    } else {
                        LOG.warn("Unknown file, answering with empty list");
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
                            if (description == null) {
                                LOG.warn("Unknown file, disconnecting");
                            } else {
                                LOG.warn("Part is not downloaded, disconnecting");
                            }
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
                    LOG.debug("Answering with {} bytes", data.length);
                    r.answerTo(out, ByteBuffer.wrap(data));
                }
            });
        }
    }
}
