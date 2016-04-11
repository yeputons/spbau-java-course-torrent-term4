package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StatRequest extends ClientRequest<List<Integer>> {
    public static final int REQUEST_ID = 1;

    private int fileId;

    private StatRequest() {
    }

    public StatRequest(int fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "StatRequest{" + "fileId=" + fileId + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatRequest that = (StatRequest) o;
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
    public List<Integer> makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);
        out.writeInt(fileId);

        int count = in.readInt();
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(in.readInt());
        }
        return result;
    }

    public static StatRequest readFrom(DataInputStream in) throws IOException {
        StatRequest r = new StatRequest();
        r.fileId = in.readInt();
        return r;
    }

    @Override
    public void answerTo(DataOutputStream out, List<Integer> data) throws IOException {
        out.writeInt(data.size());
        for (Integer id : data) {
            out.writeInt(id);
        }
    }
}
