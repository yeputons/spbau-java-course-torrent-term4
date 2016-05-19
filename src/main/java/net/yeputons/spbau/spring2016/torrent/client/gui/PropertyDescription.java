package net.yeputons.spbau.spring2016.torrent.client.gui;

public interface PropertyDescription<T> {
    T get();
    void set(T newValue);
}
