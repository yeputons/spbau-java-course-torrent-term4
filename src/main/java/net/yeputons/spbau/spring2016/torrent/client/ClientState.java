package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientState implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String downloadsDir;
    private final Map<Integer, FileDescription> files = new HashMap<>();
    private transient Map<Integer, RandomAccessFile> filesOpened;

    public ClientState(Path downloadsDir) {
        this.downloadsDir = downloadsDir.toAbsolutePath().toString();
    }

    Map<Integer, FileDescription> getFiles() {
        return files;
    }

    public FileDescription getFileDescription(int id) {
        synchronized (files) {
            return files.get(id);
        }
    }

    public RandomAccessFile getFile(int id) {
        synchronized (files) {
            FileDescription description = getFileDescription(id);
            if (description == null) {
                throw new RuntimeException("Requested to open unknown file with id " + id);
            }
            if (filesOpened == null) {
                filesOpened = new HashMap<>();
            }
            RandomAccessFile result = filesOpened.get(id);
            if (result == null) {
                try {
                    result = new RandomAccessFile(
                            Paths.get(downloadsDir, description.getEntry().getName()).toFile(),
                            "rw");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Requested to open unknown file with id " + id, e);
                }
                filesOpened.put(id, result);
            }
            try {
                result.setLength(description.getEntry().getSize());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }
}
