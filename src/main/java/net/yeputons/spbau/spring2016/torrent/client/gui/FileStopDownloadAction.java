package net.yeputons.spbau.spring2016.torrent.client.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FileStopDownloadAction extends AbstractAction {
    private final PropertyDescription<GuiFileEntry.State> propertyDescription;

    public FileStopDownloadAction(PropertyDescription<GuiFileEntry.State> propertyDescription) {
        putValue(NAME, "Stop download");
        this.propertyDescription = propertyDescription;
        updateEnabledState();
    }

    public void updateEnabledState() {
        GuiFileEntry.State state = propertyDescription.get();
        setEnabled(state == GuiFileEntry.State.DOWNLOADING);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        propertyDescription.set(GuiFileEntry.State.STOPPED);
    }
}
