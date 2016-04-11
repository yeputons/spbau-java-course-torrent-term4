package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UpdateRequest extends Request<Boolean> {
    public static final int REQUEST_ID = 4;

    static {
        Request.registerRequestType(UpdateRequest.class);
    }

    private int seedPort;
    private List<Integer> seedingFiles;

    private UpdateRequest() {
    }

    public UpdateRequest(int seedPort, List<Integer> seedingFiles) {
        this.seedPort = seedPort;
        this.seedingFiles = seedingFiles;
    }

    @Override
    public String toString() {
        return "UpdateRequest{" + "seedPort=" + seedPort + ", seedingFiles=" + seedingFiles + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateRequest that = (UpdateRequest) o;
        return seedPort == that.seedPort && Objects.equals(seedingFiles, that.seedingFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seedPort, seedingFiles);
    }

    public int getSeedPort() {
        return seedPort;
    }

    public List<Integer> getSeedingFiles() {
        return Collections.unmodifiableList(seedingFiles);
    }

    @Override
    public Boolean makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);
        out.writeShort(seedPort);
        out.writeInt(seedingFiles.size());
        for (Integer id : seedingFiles) {
            out.writeInt(id);
        }
        return in.readBoolean();
    }

    public static UpdateRequest readFrom(DataInputStream in) throws IOException {
        UpdateRequest r = new UpdateRequest();
        r.seedPort = in.readUnsignedShort();

        int count = in.readInt();
        r.seedingFiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            r.seedingFiles.add(in.readInt());
        }
        return r;
    }

    @Override
    public void answerTo(DataOutputStream out, Boolean data) throws IOException {
        out.writeBoolean(data);
    }
}
