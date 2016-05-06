package net.yeputons.spbau.spring2016.torrent;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketDataStreamsWrapper implements Closeable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SocketDataStreamsWrapper(Socket socket) throws IOException {
        this.socket = socket;
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

    public Socket getSocket() {
        return socket;
    }

    public DataInputStream getInputStream() {
        return in;
    }

    public DataOutputStream getOutputStream() {
        return out;
    }

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
