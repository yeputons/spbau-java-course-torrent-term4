package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;

import java.io.Serializable;
import java.util.BitSet;

public class FileDescription implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FileEntry entry;
    private final int partSize;
    private final BitSet downloaded;

    public FileDescription(FileEntry entry, int partSize) {
        this.entry = entry;
        this.partSize = partSize;
        long partsCount = ((entry.getSize() + partSize - 1) / partSize);
        if (partsCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("partsCount is greater than Integer.MAX_VALUE");
        }
        downloaded = new BitSet((int) partsCount);
    }

    @Override
    public String toString() {
        return "FileDescription{"
                + "entry=" + entry
                + ", partSize=" + partSize
                + ", downloaded=" + downloaded.cardinality() + "/" + downloaded.size()
                + '}';
    }

    public FileEntry getEntry() {
        return entry;
    }

    public int getPartSize() {
        return partSize;
    }

    public BitSet getDownloaded() {
        return downloaded;
    }

    public int getPartStart(int partId) {
        return partId * partSize;
    }

    public int getPartSize(int partId) {
        if (partId + 1 < downloaded.size()) {
            return partSize;
        }
        return (int) (entry.getSize() - (downloaded.size() - 1) * partSize);
    }
}
