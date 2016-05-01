package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class GetRequest extends ClientRequest<ByteBuffer> {
    public static final int REQUEST_ID = 2;

    private int fileId;
    private int partId;
    private int partSize;

    private GetRequest() {
    }

    public GetRequest(int fileId, int partId, int partSize) {
        this.fileId = fileId;
        this.partId = partId;
        this.partSize = partSize;
    }

    @Override
    public String toString() {
        return "GetRequest{"
                + "fileId=" + fileId
                + ", partId=" + partId
                + ", partSize=" + partSize
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GetRequest that = (GetRequest) o;
        return fileId == that.fileId
                && partId == that.partId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, partId);
    }

    public int getFileId() {
        return fileId;
    }

    public int getPartId() {
        return partId;
    }

    @Override
    public ByteBuffer makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);
        out.writeInt(fileId);
        out.writeInt(partId);
        byte[] result = new byte[partSize];
        in.readFully(result);
        return ByteBuffer.wrap(result);
    }

    public static GetRequest readFrom(DataInputStream in) throws IOException {
        GetRequest r = new GetRequest();
        r.fileId = in.readInt();
        r.partId = in.readInt();
        r.partSize = 0;
        return r;
    }

    @Override
    public void answerTo(DataOutputStream out, ByteBuffer data) throws IOException {
        out.write(data.array(), data.arrayOffset(), data.limit() - data.arrayOffset());
    }

    @Override
    public void visit(ClientRequestVisitor visitor) throws IOException {
        visitor.accept(this);
    }
}
