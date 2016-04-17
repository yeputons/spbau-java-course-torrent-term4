package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;

import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClientState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, FileDescription> files = new HashMap<>();

    public Map<Integer, FileDescription> getFiles() {
        return files;
    }

    public FileDescription getFileDescription(int id) {
        synchronized (files) {
            return files.get(id);
        }
    }
}
