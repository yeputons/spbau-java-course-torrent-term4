package net.yeputons.spbau.spring2016.torrent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class StateFileHolder<T extends Serializable> implements StateHolder<T> {
    private final Path storage;
    private final T state;

    @SuppressWarnings("unchecked")
    public StateFileHolder(Path storage, T defaultValue) {
        T state = defaultValue;
        this.storage = storage;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(storage))) {
            state = (T) in.readObject();
        } catch (NoSuchFileException | FileNotFoundException ignored) {
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.state = state;
    }

    @Override
    public T getState() {
        return state;
    }

    @Override
    public void save() throws IOException {
        synchronized (state) {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(storage))) {
                out.writeObject(state);
            }
        }
    }
}
