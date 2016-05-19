package net.yeputons.spbau.spring2016.torrent.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

class SetDefaultPartSizeAction extends AbstractAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetDefaultPartSizeAction.class);
    private Component parentComponent;
    private PropertyDescription<Integer> defaultPartSizeProperty;

    SetDefaultPartSizeAction(Component parentComponent, PropertyDescription<Integer> defaultPartSizeProperty) {
        this.parentComponent = parentComponent;
        this.defaultPartSizeProperty = defaultPartSizeProperty;
        putValue(NAME, "Set part size");
        putValue(SHORT_DESCRIPTION, "Change default part size for uploaded/downloaded files");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        String newDefaultPartSizeStr = (String) JOptionPane.showInputDialog(
                parentComponent,
                "Enter new part size in bytes, should be greater than zero",
                Integer.toString(defaultPartSizeProperty.get())
        );
        if (newDefaultPartSizeStr == null) { // cancelled
            return;
        }
        int newDefaultPartSize;
        try {
            newDefaultPartSize = Integer.parseInt(newDefaultPartSizeStr);
            if (newDefaultPartSize <= 0) {
                throw new NumberFormatException("part size should be greater than zero");
            }
        } catch (NumberFormatException e) {
            LOG.error("Invalid part size", e);
            JOptionPane.showMessageDialog(
                    parentComponent,
                    "Invalid part size: " + e.getMessage(),
                    "Error while parsing part size",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        defaultPartSizeProperty.set(newDefaultPartSize);
    }
}
