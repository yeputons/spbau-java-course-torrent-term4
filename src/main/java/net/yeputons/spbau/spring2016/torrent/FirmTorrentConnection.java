package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

public class FirmTorrentConnection implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(FirmTorrentConnection.class);
    private final SocketAddress address;
    private TorrentConnection connection;

    public FirmTorrentConnection(SocketAddress address) {
        this.address = address;
        LOG.debug("Created FirmTorrentConnection with address " + address.toString());
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            connection.close();
        }
    }

    private void ensureConnection() throws IOException {
        if (connection != null && !connection.getSocket().isClosed()) {
            return;
        }
        if (connection != null) {
            LOG.debug("Reconnecting to " + address.toString());
        }
        try {
            connection = TorrentConnection.connect(address);
        } catch (IOException e) {
            if (connection != null) {
                LOG.warn("Unable to reconnect to " + address.toString(), e);
            } else {
                LOG.warn("Unable to connect to " + address.toString(), e);
            }
            throw e;
        }
    }

    public synchronized <T> T makeRequest(Request<T> r) throws IOException {
        ensureConnection();
        try {
            return connection.makeRequest(r);
        } catch (IOException e) {
            LOG.warn("Caught IOException, closing connection", e);
            connection.close();
            throw e;
        }
    }
}
