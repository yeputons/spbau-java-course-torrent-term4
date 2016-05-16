package net.yeputons.spbau.spring2016.torrent.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

class SetUpdateIntervalAction extends AbstractAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetUpdateIntervalAction.class);
    private Component parentComponent;
    private PropertyDescription<Integer> updateIntervalProperty;

    SetUpdateIntervalAction(Component parentComponent, PropertyDescription<Integer> updateIntervalProperty) {
        this.parentComponent = parentComponent;
        this.updateIntervalProperty = updateIntervalProperty;
        putValue(NAME, "Set update interval");
        putValue(SHORT_DESCRIPTION, "Change interval for sending UpdateRequest to tracker");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        String newUpdateIntervalStr = (String) JOptionPane.showInputDialog(
                parentComponent,
                "Enter new update interval in ms, should be greater than zero",
                Integer.toString(updateIntervalProperty.get())
        );
        if (newUpdateIntervalStr == null) { // cancelled
            return;
        }
        int newUpdateInterval;
        try {
            newUpdateInterval = Integer.parseInt(newUpdateIntervalStr);
            if (newUpdateInterval <= 0) {
                throw new NumberFormatException("interval should be greater than zero");
            }
        } catch (NumberFormatException e) {
            LOG.error("Invalid update interval", e);
            JOptionPane.showMessageDialog(
                    parentComponent,
                    "Invalid update interval: " + e.getMessage(),
                    "Error while parsing update interval",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateIntervalProperty.set(newUpdateInterval);
    }
}
