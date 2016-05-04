package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientState implements Serializable {
    private static final long serialVersionUID = 3L;

    private final String downloadsDir;
    private final ConcurrentMap<Integer, FileDescription> files = new ConcurrentHashMap<>();
    private transient ConcurrentMap<Integer, RandomAccessFile> filesOpened = new ConcurrentHashMap<>();

    public ClientState(Path downloadsDir) {
        this.downloadsDir = downloadsDir.toAbsolutePath().toString();
    }

    public Path getDownloadsDir() {
        return Paths.get(downloadsDir);
    }

    Map<Integer, FileDescription> getFiles() {
        return files;
    }

    public FileDescription getFileDescription(int id) {
        return files.get(id);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        filesOpened = new ConcurrentHashMap<>();
    }

    public RandomAccessFile getFile(int id) throws IOException {
        FileDescription description = getFileDescription(id);
        if (description == null) {
            throw new RuntimeException("Requested to open unknown file with id " + id);
        }
        RandomAccessFile result;
        synchronized (description) {
            result = filesOpened.get(id);
            if (result == null) {
                result = new RandomAccessFile(
                        Paths.get(downloadsDir, description.getEntry().getName()).toFile(),
                        "rw");
                filesOpened.put(id, result);
            }
        }
        synchronized (result) {
            result.setLength(description.getEntry().getSize());
        }
        return result;
    }
}
