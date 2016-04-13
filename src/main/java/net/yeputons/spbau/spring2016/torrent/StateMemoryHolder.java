package net.yeputons.spbau.spring2016.torrent;

import java.io.IOException;

public class StateMemoryHolder<T> implements StateHolder<T> {
    private final T state;

    public StateMemoryHolder(T state) {
        this.state = state;
    }

    @Override
    public T getState() {
        return state;
    }

    @Override
    public void save() throws IOException {
    }
}
