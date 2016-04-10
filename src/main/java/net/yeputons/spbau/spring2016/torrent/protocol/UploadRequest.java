package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class UploadRequest extends Request<Integer> {
    public static final int REQUEST_ID = 2;

    static {
        Request.registerRequestType(UploadRequest.class);
    }

    private String fileName;
    private long size;

    private UploadRequest() {
    }

    public UploadRequest(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;
    }

    @Override
    public String toString() {
        return "UploadRequest{" + "fileName='" + fileName + '\'' + ", size=" + size + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UploadRequest that = (UploadRequest) o;
        return size == that.size && Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, size);
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    @Override
    public Integer makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);
        out.writeUTF(fileName);
        out.writeLong(size);
        return in.readInt();
    }

    public static UploadRequest readFrom(DataInputStream in) throws IOException {
        UploadRequest r = new UploadRequest();
        r.fileName = in.readUTF();
        r.size = in.readLong();
        return r;
    }

    @Override
    public void answerTo(DataOutputStream out, Integer data) throws IOException {
        out.writeInt(data);
    }
}
