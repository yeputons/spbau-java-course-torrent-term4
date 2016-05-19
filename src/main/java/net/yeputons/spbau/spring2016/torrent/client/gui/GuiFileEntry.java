package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.client.ClientState;
import net.yeputons.spbau.spring2016.torrent.client.TorrentLeecher;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class GuiFileEntry implements Serializable {
    private static final long serialVersionUID = 4L;

    public enum State {
        NOT_STARTED,
        STOPPED,
        DOWNLOADING,
        DOWNLOADED
    }

    private final FileEntry entry;
    private State state = State.NOT_STARTED;
    private transient TorrentLeecher leecher;

    public GuiFileEntry(FileEntry entry) {
        this.entry = entry;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (state == State.DOWNLOADING) {
            state = State.STOPPED;
        }
    }

    public FileEntry getEntry() {
        return entry;
    }

    public State getState() {
        return state;
    }

    public TorrentLeecher getLeecher() {
        return leecher;
    }

    public void startDownload(GuiClient guiClient) {
        if (state == State.DOWNLOADING || state == State.DOWNLOADED) {
            return;
        }
        ClientState state = guiClient.getStateHolder().getState();
        FileDescription description;
        synchronized (state) {
            state.getFiles().putIfAbsent(getEntry().getId(),
                    new FileDescription(
                            getEntry(),
                            guiClient.getGuiStateHolder().getState().getDefaultPartSize()
                    )
            );
            description = state.getFileDescription(getEntry().getId());
        }
        leecher = new TorrentLeecher(
                guiClient.getGuiStateHolder().getState().getTrackerConnection(),
                guiClient.getStateHolder(),
                description,
                guiClient.getLeechersPool()
        );
        leecher.setCompleteListener(l -> {
            stopDownload();
            this.state = State.DOWNLOADED;
        });
        leecher.start();
        this.state = State.DOWNLOADING;
    }

    public void stopDownload() {
        if (state != State.DOWNLOADING) {
            return;
        }
        leecher.shutdown();
        leecher = null;
        state = State.STOPPED;
    }
}
