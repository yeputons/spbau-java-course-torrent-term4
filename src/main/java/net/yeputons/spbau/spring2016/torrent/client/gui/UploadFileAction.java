package net.yeputons.spbau.spring2016.torrent.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class UploadFileAction extends AbstractAction {
    private static final Logger LOG = LoggerFactory.getLogger(UploadFileAction.class);
    private final GuiClient guiClient;

    public UploadFileAction(GuiClient guiClient) {
        this.guiClient = guiClient;
        putValue(NAME, "Upload file");
        putValue(SHORT_DESCRIPTION, "Upload new file to server");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        List<Path> files;
        try {
            files =
                    Files.list(guiClient.getStateHolder().getState().getDownloadsDir())
                            .filter(Files::isRegularFile)
                            .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Cannot list downloads directory", e);
            JOptionPane.showMessageDialog(
                    guiClient,
                    "Cannot list downloads directory: " + e.getMessage(),
                    "Error while reading downloads directory",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path fileChosen = (Path) JOptionPane.showInputDialog(
                guiClient,
                "Select a file to upload to tracker:",
                "File uploading",
                JOptionPane.QUESTION_MESSAGE,
                null,
                files.toArray(),
                null
        );
        if (fileChosen == null) {
            return;
        }
        long size;
        try {
            size = Files.size(fileChosen);
        } catch (IOException e) {
            LOG.warn("Cannot get file size for " + fileChosen, e);
            JOptionPane.showMessageDialog(
                    guiClient,
                    "Cannot get file size: " + e.getMessage(),
                    "Unable to upload file",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        guiClient.uploadFile(fileChosen.getFileName().toString(), size);
    }
}
