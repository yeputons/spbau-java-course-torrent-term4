package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.FirmTorrentConnection;
import net.yeputons.spbau.spring2016.torrent.client.TorrentSeeder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.*;

public class GuiState implements Serializable {
    private static final long serialVersionUID = 9L;
    private static final int DEFAULT_PART_SIZE = 1024 * 1024;

    private SocketAddress trackerAddress;
    private final List<GuiFileEntry> fileList = new ArrayList<>();
    private Calendar fileListLastUpdated;
    private int updateInterval = TorrentSeeder.DEFAULT_UPDATE_INTERVAL;
    private int defaultPartSize = DEFAULT_PART_SIZE;

    private transient FirmTorrentConnection trackerConnection;
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public GuiState() {
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void firePropertyChange(String key, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(key, oldValue, newValue);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public synchronized FirmTorrentConnection getTrackerConnection() {
        return trackerConnection;
    }

    public synchronized SocketAddress getTrackerAddress() {
        return trackerAddress;
    }

    public List<GuiFileEntry> getFileList() {
        return fileList;
    }

    public Calendar getFileListLastUpdated() {
        return fileListLastUpdated;
    }

    public void setFileListLastUpdated(Calendar fileListLastUpdated) {
        Calendar oldValue = this.fileListLastUpdated;
        this.fileListLastUpdated = fileListLastUpdated;
        firePropertyChange("fileListLastUpdated", oldValue, fileListLastUpdated);
    }

    public synchronized void setTrackerAddress(SocketAddress trackerAddress) {
        SocketAddress oldAddress = this.trackerAddress;
        final FirmTorrentConnection oldConnection = this.trackerConnection;

        this.trackerAddress = trackerAddress;
        if (trackerAddress == null) {
            this.trackerConnection = null;
        } else {
            this.trackerConnection = new FirmTorrentConnection(trackerAddress);
        }
        firePropertyChange("trackerAddress", oldAddress, this.trackerAddress);
        firePropertyChange("trackerConnection", oldConnection, this.trackerConnection);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        propertyChangeSupport = new PropertyChangeSupport(this);
        setTrackerAddress(trackerAddress);
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        int oldValue = this.updateInterval;
        this.updateInterval = updateInterval;
        firePropertyChange("updateInterval", oldValue, updateInterval);
    }

    public int getDefaultPartSize() {
        return defaultPartSize;
    }

    public void setDefaultPartSize(int defaultPartSize) {
        this.defaultPartSize = defaultPartSize;
    }
}
