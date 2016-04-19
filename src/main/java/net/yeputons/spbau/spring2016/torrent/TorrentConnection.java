package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.Request;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class TorrentConnection implements Closeable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public TorrentConnection(SocketAddress address) throws IOException {
        socket = new Socket();
        socket.connect(address);
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e.addSuppressed(e);
            }
            throw e;
        }
    }

    public synchronized <T> T makeRequest(Request<T> r) throws IOException {
        return r.makeRequest(in, out);
    }

    @Override
    public void close() throws IOException {
        IOException e = null;
        try {
            // should close OutputStream first in order to flush buffers to socket
            // before it's automatically closed by closing OutputStream
            out.close();
        } catch (IOException e1) {
            e = e1;
        }
        try {
            in.close();
        } catch (IOException e1) {
            if (e != null) {
                e.addSuppressed(e1);
            } else {
                e = e1;
            }
        }
        try {
            socket.close();
        } catch (IOException e1) {
            if (e != null) {
                e.addSuppressed(e1);
            } else {
                e = e1;
            }
        }
        if (e != null) {
            throw e;
        }
    }
}
