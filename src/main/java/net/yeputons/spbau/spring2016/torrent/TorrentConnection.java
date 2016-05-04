package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class TorrentConnection extends SocketDataStreamsWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TorrentConnection.class);
    public TorrentConnection(Socket socket) throws IOException {
        super(socket);
    }

    public static TorrentConnection connect(SocketAddress address) throws IOException {
        LOG.debug("Connecting to {}", address);
        Socket socket = new Socket();
        try {
            socket.connect(address);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }
        LOG.debug("Connected to {}, local endpoint is {}", address, socket.getLocalSocketAddress());
        return new TorrentConnection(socket);
    }

    public synchronized <T> T makeRequest(Request<T> r) throws IOException {
        return r.makeRequest(getInputStream(), getOutputStream());
    }
}
