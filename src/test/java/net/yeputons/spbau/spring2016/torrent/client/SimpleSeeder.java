package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.SocketDataStreamsWrapper;
import net.yeputons.spbau.spring2016.torrent.protocol.GetRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.StatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;

public class SimpleSeeder {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleSeeder.class);
    private final List<byte[]> requests = new ArrayList<>();
    private final List<byte[]> answers = new ArrayList<>();
    private ServerSocket listener;
    private Thread thread;
    private AtomicReference<Exception> exceptionCaught;

    public SimpleSeeder(int fileId, int partId, byte[] data) {
        // CHECKSTYLE.OFF: MagicNumber
        requests.add(new byte[] {
                StatRequest.REQUEST_ID,
                (byte) (fileId >> 24),
                (byte) (fileId >> 16),
                (byte) (fileId >>  8),
                (byte) (fileId >>  0)
        });
        answers.add(new byte[] {
                0, 0, 0, 1,
                (byte) (partId >> 24),
                (byte) (partId >> 16),
                (byte) (partId >>  8),
                (byte) (partId >>  0)
        });
        requests.add(new byte[] {
                GetRequest.REQUEST_ID,
                (byte) (fileId >> 24),
                (byte) (fileId >> 16),
                (byte) (fileId >>  8),
                (byte) (fileId >>  0),
                (byte) (partId >> 24),
                (byte) (partId >> 16),
                (byte) (partId >>  8),
                (byte) (partId >>  0)
        });
        // CHECKSTYLE.ON: MagicNumber
        answers.add(data);
    }

    SocketAddress start() throws IOException {
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress("0.0.0.0", 0));
        exceptionCaught = new AtomicReference<>();
        thread = new Thread(() -> {
            while (true) {
                Socket peer;
                try {
                    peer = listener.accept();
                } catch (IOException ignored) {
                    break;
                }
                try (SocketDataStreamsWrapper wrapper = new SocketDataStreamsWrapper(peer)) {
                    for (int i = 0; i < requests.size(); i++) {
                        byte[] data = new byte[requests.get(i).length];
                        wrapper.getInputStream().readFully(data);
                        assertArrayEquals(requests.get(i), data);
                        wrapper.getOutputStream().write(answers.get(i));
                    }
                } catch (IOException e) {
                    LOG.warn("Exception while processing client", e);
                }
            }
        });
        thread.start();
        return listener.getLocalSocketAddress();
    }

    public void shutdown() throws IOException {
        listener.close();
    }

    public void join() throws Exception {
        thread.join();
        Exception e = exceptionCaught.get();
        if (e != null) {
            throw e;
        }
    }
}
