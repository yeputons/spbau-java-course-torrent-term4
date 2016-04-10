package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ListRequest extends Request<List<FileEntry>> {
    public static final int REQUEST_ID = 1;

    static {
        Request.registerRequestType(ListRequest.class);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public List<FileEntry> makeRequest(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeByte(REQUEST_ID);

        int count = in.readInt();
        List<FileEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(FileEntry.readFrom(in));
        }
        return entries;
    }

    public static ListRequest readFrom(DataInputStream in) throws IOException {
        return new ListRequest();
    }

    @Override
    public void answerTo(DataOutputStream out, List<FileEntry> data) throws IOException {
        out.writeInt(data.size());
        for (FileEntry e : data) {
            e.writeTo(out);
        }
    }
}
