package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SourcesRequest extends ServerRequest<List<InetSocketAddress>> {
    public static final int REQUEST_ID = 3;

    private int fileId;

    private SourcesRequest() {
    }

    public SourcesRequest(int fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "SourcesRequest{" + "fileId=" + fileId + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SourcesRequest that = (SourcesRequest) o;
        return fileId == that.fileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId);
    }

    public int getFileId() {
        return fileId;
    }

    @Override
    public List<InetSocketAddress> makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);
        out.writeInt(fileId);
        int count = in.readInt();
        List<InetSocketAddress> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // CHECKSTYLE.OFF: MagicNumber
            byte[] addr = new byte[4];
            // CHECKSTYLE.ON: MagicNumber
            in.readFully(addr);
            int port = in.readUnsignedShort();
            result.add(new InetSocketAddress(Inet4Address.getByAddress(addr), port));
        }
        return result;
    }

    public static SourcesRequest readFrom(DataInputStream in) throws IOException {
        SourcesRequest r = new SourcesRequest();
        r.fileId = in.readInt();
        return r;
    }

    @Override
    public void answerTo(DataOutputStream out, List<InetSocketAddress> data) throws IOException {
        out.writeInt(data.size());
        for (InetSocketAddress addr : data) {
            out.write(((Inet4Address) addr.getAddress()).getAddress());
            out.writeShort(addr.getPort());
        }
    }

    @Override
    public void visit(ServerRequestVisitor visitor) throws IOException {
        visitor.accept(this);
    }
}
