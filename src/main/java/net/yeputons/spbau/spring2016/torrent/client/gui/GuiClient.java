package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.FirmTorrentConnection;
import net.yeputons.spbau.spring2016.torrent.StateFileHolder;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.client.ClientState;
import net.yeputons.spbau.spring2016.torrent.client.TorrentSeeder;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.UploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class GuiClient extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(GuiClient.class);
    private final StateHolder<ClientState> stateHolder;
    private final StateHolder<GuiState> guiStateHolder;
    private final Timer failingTasksTimer = new Timer();
    private final MainPanel mainPanel;

    private TorrentSeeder seeder;
    private final ScheduledExecutorService leechersPool = Executors.newScheduledThreadPool(1);

    public GuiClient() {
        stateHolder = new StateFileHolder<>(
                Paths.get("torrent-client-state.bin"),
                new ClientState(Paths.get("./downloads"))
        );
        guiStateHolder = new StateFileHolder<>(Paths.get("torrent-gui-state.bin"), new GuiState());

        Path downloadsDir = stateHolder.getState().getDownloadsDir();
        if (!Files.isDirectory(downloadsDir)) {
            try {
                Files.createDirectories(downloadsDir);
            } catch (IOException e) {
                LOG.error("Unable to create downloads directory (" + downloadsDir + ")", e);
            }
        }

        GuiState guiState = guiStateHolder.getState();
        guiState.addPropertyChangeListener(ev -> {
            switch (ev.getPropertyName()) {
                case "trackerConnection":
                case "updateInterval":
                    stopSeeder();
                    scheduleSeederStart();
                    break;
                default: break;
            }
        });
        scheduleSeederStart();
        scheduleGuiStateSave();

        setTitle("Torrent client");
        mainPanel = new MainPanel(this);
        add(mainPanel);
        pack();
    }

    public StateHolder<ClientState> getStateHolder() {
        return stateHolder;
    }

    StateHolder<GuiState> getGuiStateHolder() {
        return guiStateHolder;
    }

    public void setTrackerAddress(SocketAddress newAddress) {
        GuiState guiState = getGuiStateHolder().getState();
        synchronized (guiState) {
            guiState.setTrackerAddress(newAddress);
            guiState.getFileList().clear();
            guiState.setFileListLastUpdated(null);
        }
        mainPanel.getTableModel().fireTableDataChanged();
        scheduleGuiStateSave();
    }

    public void setUpdateInterval(int newUpdateInterval) {
        GuiState guiState = guiStateHolder.getState();
        synchronized (guiState) {
            guiState.setUpdateInterval(newUpdateInterval);
        }
        scheduleGuiStateSave();
    }

    public void setDefaultPartSize(int defaultPartSize) {
        GuiState guiState = guiStateHolder.getState();
        synchronized (guiState) {
            guiState.setDefaultPartSize(defaultPartSize);
        }
        scheduleGuiStateSave();
    }

    public void updateFileList(List<FileEntry> result) {
        GuiState guiState = guiStateHolder.getState();
        synchronized (guiState) {
            guiState.getFileList().clear();
            guiState.getFileList().addAll(result.stream().map(GuiFileEntry::new).collect(Collectors.toList()));
            guiState.setFileListLastUpdated(Calendar.getInstance());
        }
        mainPanel.getTableModel().fireTableDataChanged();
        scheduleGuiStateSave();
    }

    public void setFileState(GuiFileEntry guiEntry, GuiFileEntry.State newValue) {
        GuiState guiState = guiStateHolder.getState();
        synchronized (guiState) {
            if (newValue == GuiFileEntry.State.DOWNLOADING) {
                guiEntry.startDownload(this);
                guiEntry.getLeecher().setProgressListener(l -> {
                    int id;
                    synchronized (guiState) {
                        id = guiState.getFileList().indexOf(guiEntry);
                    }
                    if (id != -1) {
                        mainPanel.getTableModel().fireTableRowsUpdated(id, id);
                    }
                });
            }
            if (newValue == GuiFileEntry.State.STOPPED) {
                guiEntry.stopDownload();
            }
        }
        mainPanel.getTableModel().fireTableDataChanged();
        scheduleGuiStateSave();
    }

    public ScheduledExecutorService getLeechersPool() {
        return leechersPool;
    }

    public void uploadFile(String fileName, long size) {
        SwingUtilities.invokeLater(() -> {
            int id;
            try {
                id = guiStateHolder.getState().getTrackerConnection().makeRequest(
                        new UploadRequest(fileName, size));
            } catch (IOException e) {
                LOG.warn("Unable to upload file", e);
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to upload file: " + e.getMessage(),
                        "Unable to upload file",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "File was successfully uploaded with id " + id + "\n"
                            + "Update files list to see it",
                    "File uploaded",
                    JOptionPane.INFORMATION_MESSAGE
            );

            FileDescription description = new FileDescription(new FileEntry(id, fileName, size),
                    guiStateHolder.getState().getDefaultPartSize());
            description.getDownloaded().flip(0, description.getPartsCount());
            ClientState state = stateHolder.getState();
            synchronized (state) {
                state.getFiles().put(id, description);
            }
            scheduleStateSave();
        });
    }

    private class StateSaveTask extends TimerTask {
        private static final int RETRY_DELAY = 1000;

        @Override
        public void run() {
            try {
                stateHolder.save();
                LOG.debug("Saved client state");
            } catch (IOException e) {
                LOG.error("Cannot save client state, will retry after " + RETRY_DELAY + " msec", e);
                failingTasksTimer.schedule(new StateSaveTask(), RETRY_DELAY);
            }
        }
    }

    private void scheduleStateSave() {
        failingTasksTimer.schedule(new StateSaveTask(), 0);
    }

    private class GuiStateSaveTask extends TimerTask {
        private static final int RETRY_DELAY = 1000;

        @Override
        public void run() {
            try {
                guiStateHolder.save();
                LOG.debug("Saved GUI state");
            } catch (IOException e) {
                LOG.error("Cannot save GUI state, will retry after " + RETRY_DELAY + " msec", e);
                failingTasksTimer.schedule(new GuiStateSaveTask(), RETRY_DELAY);
            }
        }
    }

    private void scheduleGuiStateSave() {
        failingTasksTimer.schedule(new GuiStateSaveTask(), 0);
    }

    private class SeederStartTask extends TimerTask {
        private static final int RETRY_DELAY = 1000;

        @Override
        public void run() {
            if (seeder != null) {
                return;
            }
            GuiState guiState = guiStateHolder.getState();
            FirmTorrentConnection connection;
            synchronized (guiState) {
                connection = guiState.getTrackerConnection();
            }
            if (connection == null) {
                return;
            }
            seeder = new TorrentSeeder(connection, stateHolder.getState(), guiState.getUpdateInterval());
            try {
                seeder.start();
            } catch (IOException e) {
                LOG.error("Cannot start seeder, will retry after " + RETRY_DELAY + " msec", e);
                failingTasksTimer.schedule(new SeederStartTask(), RETRY_DELAY);
            }
        }
    }

    private void scheduleSeederStart() {
        failingTasksTimer.schedule(new SeederStartTask(), 0);
    }

    private void stopSeeder() {
        if (seeder != null) {
            try {
                seeder.shutdown();
            } catch (IOException e) {
                LOG.warn("Exception during seeder shut down", e);
            }
            seeder = null;
        }
    }
}
