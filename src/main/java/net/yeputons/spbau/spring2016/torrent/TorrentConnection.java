package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.Request;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class TorrentConnection extends SocketDataStreamsWrapper {
    public TorrentConnection(Socket socket) throws IOException {
        super(socket);
    }

    public static TorrentConnection connect(SocketAddress address) throws IOException {
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
        return new TorrentConnection(socket);
    }

    public synchronized <T> T makeRequest(Request<T> r) throws IOException {
        return r.makeRequest(getInputStream(), getOutputStream());
    }
}
