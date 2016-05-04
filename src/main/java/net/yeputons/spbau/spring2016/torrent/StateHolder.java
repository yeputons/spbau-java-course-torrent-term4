package net.yeputons.spbau.spring2016.torrent;

import java.io.IOException;

public interface StateHolder<T> {
    T getState();
    void save() throws IOException;
}
