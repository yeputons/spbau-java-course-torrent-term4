package net.yeputons.spbau.spring2016.torrent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClientState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, FileDescription> files = new HashMap<>();

    public Map<Integer, FileDescription> getFiles() {
        return files;
    }
}
