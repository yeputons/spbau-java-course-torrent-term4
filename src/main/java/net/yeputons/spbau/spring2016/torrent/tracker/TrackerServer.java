package net.yeputons.spbau.spring2016.torrent.tracker;

import net.yeputons.spbau.spring2016.torrent.SocketDataStreamsWrapper;
import net.yeputons.spbau.spring2016.torrent.StateFileHolder;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.StateMemoryHolder;
import net.yeputons.spbau.spring2016.torrent.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrackerServer {
    public static final int DEFAULT_PORT = 8081;
    private static final Logger LOG = LoggerFactory.getLogger(TrackerServer.class);

    private static class State implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<FileEntry> files = new ArrayList<>();
        private int freeFileId = 1;
    }

    private final StateHolder<State> stateHolder;
    private final SeedersTracker seeders = new SeedersTracker(60 * 1000);

    private ServerSocket server;
    private Thread thread;
    private ExecutorService clientThreads;
    private final int port;
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

    public void start() throws IOException {
        synchronized (this) {
            if (thread != null || server != null) {
                throw new IllegalStateException("Already started");
            }
            if (shuttedDown) {
                return;
            }
            server = new ServerSocket();
            server.bind(new InetSocketAddress("0.0.0.0", port));
            clientThreads = Executors.newCachedThreadPool();
        }
        LOG.info("Started server on {}", server.getLocalSocketAddress());
        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                final Socket client;
                try {
                    client = server.accept();
                } catch (IOException ignored) {
                    break;
                }
                LOG.info("New client from {}", client.getRemoteSocketAddress());
                clientThreads.submit(() -> {
                    try (SocketDataStreamsWrapper wrapper = new SocketDataStreamsWrapper(client)) {
                        final DataInputStream in = wrapper.getInputStream();
                        final DataOutputStream out = wrapper.getOutputStream();
                        for (;;) {
                            ServerRequest<?> request;
                            try {
                                request = ServerRequest.readRequest(in);
                            } catch (NoRequestException ignored) {
                                break;
                            }
                            LOG.debug("Incoming request: {}", request);
                            request.visit(new ServerRequestVisitor() {
                                @Override
                                public void accept(ListRequest r) throws IOException {
                                    State s = stateHolder.getState();
                                    synchronized (s) {
                                        LOG.debug("Answering with {}", s.files);
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
                                    LOG.debug("Answering with {}", id);
                                    r.answerTo(out, id);
                                }

                                @Override
                                public void accept(SourcesRequest r) throws IOException {
                                    List<InetSocketAddress> answer = seeders.getSources(r.getFileId());
                                    LOG.debug("Answering with {}", answer);
                                    r.answerTo(out, answer);
                                }

                                @Override
                                public void accept(UpdateRequest r) throws IOException {
                                    InetSocketAddress address =
                                            new InetSocketAddress(client.getInetAddress(), r.getSeedPort());
                                    for (int fileId : r.getSeedingFiles()) {
                                        seeders.updateSource(fileId, address);
                                    }
                                    LOG.debug("Answering with {}", true);
                                    r.answerTo(out, true);
                                }
                            });
                        }
                    } catch (IOException e) {
                        LOG.warn("Error while processing client request", e);
                    }
                    LOG.info("Client disconnected");
                });
            }
            LOG.info("Server on {} is terminated", server.getLocalSocketAddress());
        });
        thread.start();
    }

    public void shutdown() throws IOException {
        LOG.info("Shutting down server on {}", server.getLocalSocketAddress());
        synchronized (this) {
            if (server != null) {
                server.close();
            }
            clientThreads.shutdown();
            shuttedDown = true;
        }
    }

    public void join() throws InterruptedException {
        if (thread != null) {
            thread.join();
        }
        if (clientThreads != null) {
            clientThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }
}
