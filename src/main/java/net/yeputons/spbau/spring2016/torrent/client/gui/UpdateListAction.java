package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.ListRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

class UpdateListAction extends AbstractAction {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateListAction.class);
    private GuiClient guiClient;

    UpdateListAction(GuiClient guiClient) {
        this.guiClient = guiClient;
        putValue(NAME, "Update files list");
        putValue(SHORT_DESCRIPTION, "Re-downloads files list from tracker");
        updateEnabledState();
    }

    public void updateEnabledState() {
        setEnabled(guiClient.getGuiStateHolder().getState()
                .getTrackerConnection() != null);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<FileEntry> result;
                try {
                    result = guiClient.getGuiStateHolder().getState()
                            .getTrackerConnection().makeRequest(new ListRequest());
                } catch (IOException e) {
                    LOG.error("Cannot make request to tracker", e);
                    return null;
                }
                guiClient.updateFileList(result);
                return null;
            }
        }.execute();
    }
}
