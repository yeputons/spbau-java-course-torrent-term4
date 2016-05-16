package net.yeputons.spbau.spring2016.torrent.client.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FileStartDownloadAction extends AbstractAction {
    private final PropertyDescription<GuiFileEntry.State> propertyDescription;

    public FileStartDownloadAction(PropertyDescription<GuiFileEntry.State> propertyDescription) {
        putValue(NAME, "Start download");
        this.propertyDescription = propertyDescription;
        updateEnabledState();
    }

    public void updateEnabledState() {
        GuiFileEntry.State state = propertyDescription.get();
        setEnabled(state == GuiFileEntry.State.NOT_STARTED || state == GuiFileEntry.State.STOPPED);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        propertyDescription.set(GuiFileEntry.State.DOWNLOADING);
    }
}
