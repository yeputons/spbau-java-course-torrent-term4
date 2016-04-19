package net.yeputons.spbau.spring2016.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class StateFileHolder<T extends Serializable> implements StateHolder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(StateFileHolder.class);
    private final Path storage;
    private final T state;

    @SuppressWarnings("unchecked")
    public StateFileHolder(Path storage, T defaultValue) {
        T state = defaultValue;
        this.storage = storage;
        LOG.debug("Loading state of type {} from {}", defaultValue.getClass().getName(), storage);
        try (InputStream fin = Files.newInputStream(storage);
             ObjectInputStream in = new ObjectInputStream(fin)) {
            state = (T) in.readObject();
        } catch (NoSuchFileException | FileNotFoundException ignored) {
            LOG.debug("File not found, backing to default");
        } catch (IOException | ClassNotFoundException e) {
            LOG.warn("Unable to load state from file, backing to default", e);
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
            LOG.debug("Saving {} to {}", state.getClass().getName(), storage);
            try (OutputStream fout = Files.newOutputStream(storage);
                 ObjectOutputStream out = new ObjectOutputStream(fout)) {
                out.writeObject(state);
            }
        }
    }
}
