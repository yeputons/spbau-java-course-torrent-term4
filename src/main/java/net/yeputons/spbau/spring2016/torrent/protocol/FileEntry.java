package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.util.Objects;

public class FileEntry implements ProtocolEntity, Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private long size;

    private FileEntry() {
    }

    public FileEntry(int id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    @Override
    public String toString() {
        return "FileEntry{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", size=" + size
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
        FileEntry fileEntry = (FileEntry) o;
        return id == fileEntry.id
                && size == fileEntry.size
                && Objects.equals(name, fileEntry.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, size);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public static FileEntry readFrom(DataInputStream in) throws IOException {
        FileEntry e = new FileEntry();
        e.id = in.readInt();
        e.name = in.readUTF();
        e.size = in.readLong();
        return e;
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeUTF(name);
        out.writeLong(size);
    }
}
