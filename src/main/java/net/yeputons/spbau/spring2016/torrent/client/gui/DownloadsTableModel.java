package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateHolder;
import net.yeputons.spbau.spring2016.torrent.client.ClientState;

import javax.swing.table.AbstractTableModel;

public class DownloadsTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {
            "ID",
            "Name",
            "Size",
            "Part size",
            "Status",
            "Downloaded",
    };
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_SIZE = 2;
    private static final int COLUMN_PART_SIZE = 3;
    private static final int COLUMN_STATUS = 4;
    private static final int COLUMN_DOWNLOADED = 5;

    private final StateHolder<ClientState> stateHolder;
    private final StateHolder<GuiState> guiStateHolder;

    public DownloadsTableModel(StateHolder<ClientState> stateHolder, StateHolder<GuiState> guiStateHolder) {
        this.stateHolder = stateHolder;
        this.guiStateHolder = guiStateHolder;
    }

    @Override
    public int getRowCount() {
        GuiState guiState = guiStateHolder.getState();
        synchronized (guiState) {
            return guiState.getFileList().size();
        }
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        GuiState guiState = guiStateHolder.getState();
        GuiFileEntry guiEntry;
        synchronized (guiState) {
            guiEntry = guiState.getFileList().get(rowIndex);
        }
        if (guiEntry == null) {
            return "";
        }

        switch (columnIndex) {
            case COLUMN_ID: return guiEntry.getEntry().getId();
            case COLUMN_NAME: return guiEntry.getEntry().getName();
            case COLUMN_SIZE: return guiEntry.getEntry().getSize();
            default: break;
        }

        ClientState state = stateHolder.getState();
        FileDescription description;
        int downloaded;
        int partSize;
        int partsCount;
        synchronized (state) {
            description = state.getFileDescription(guiEntry.getEntry().getId());
            if (description == null) {
                downloaded = -1;
                partSize = -1;
                partsCount = -1;
            } else {
                downloaded = description.getDownloaded().cardinality();
                partSize = description.getPartSize();
                partsCount = description.getPartsCount();
            }
        }

        switch (columnIndex) {
            case COLUMN_PART_SIZE:
                return partSize < 0 ? "N/A" : Integer.toString(partSize);
            case COLUMN_STATUS:
                switch (guiEntry.getState()) {
                    case NOT_STARTED: return "Not started";
                    case STOPPED: return "Stopped";
                    case DOWNLOADING: return "Downloading";
                    case DOWNLOADED: return "Downloaded";
                    default: return "";
                }
            case COLUMN_DOWNLOADED:
                if (partsCount < 0) {
                    return "N/A";
                }
                if (partsCount == 0) {
                    downloaded = 1;
                    partsCount = 1;
                }
                // CHECKSTYLE.OFF: MagicNumber
                return String.format("%.1f%%", 100.0 * downloaded / partsCount);
                // CHECKSTYLE.ON: MagicNumber
            default: break;
        }
        return null;
    }
}
