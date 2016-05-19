package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.tracker.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

class SetTrackerAction extends AbstractAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetTrackerAction.class);
    private final Component parentComponent;
    private final PropertyDescription<SocketAddress> addressProperty;

    SetTrackerAction(Component parentComponent, PropertyDescription<SocketAddress> addressProperty) {
        this.parentComponent = parentComponent;
        this.addressProperty = addressProperty;
        putValue(NAME, "Set tracker");
        putValue(SHORT_DESCRIPTION, "Change tracker address and clear downloads list");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        SocketAddress address = addressProperty.get();
        String newAddressStr = (String) JOptionPane.showInputDialog(
                parentComponent,
                "Enter new tracker's address as <host>[:<port>]\n"
                + "Default port is " + TrackerServer.DEFAULT_PORT + "\n"
                + "All downloads will be discarded",
                addressToString(address)
                );
        if (newAddressStr == null) { // cancelled
            return;
        }
        newAddressStr = newAddressStr.trim();
        SocketAddress newAddress;
        try {
            newAddress = parseAddress(newAddressStr);
        } catch (IllegalArgumentException e) {
            LOG.error("Cannot parse tracker address", e);
            JOptionPane.showMessageDialog(
                    parentComponent,
                    "Cannot parse tracker address: " + e.getMessage(),
                    "Error while parsing tracker address",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        addressProperty.set(newAddress);
    }

    private String addressToString(SocketAddress address) {
        if (address == null) {
            return "";
        }
        if (!(address instanceof InetSocketAddress)) {
            return address.toString();
        }
        InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
        return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
    }

    private SocketAddress parseAddress(String newAddress) throws IllegalArgumentException {
        String[] tokens = newAddress.split(":", -1);
        if (tokens.length == 1) {
            return new InetSocketAddress(tokens[0], TrackerServer.DEFAULT_PORT);
        } else if (tokens.length == 2) {
            return new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1]));
        } else {
            throw new IllegalArgumentException(
                    "Expected one or two parts of address separated by colon, got " + tokens.length);
        }
    }
}
