package ru.spbau.mit;

import net.yeputons.spbau.spring2016.torrent.client.ConsoleClient;
import net.yeputons.spbau.spring2016.torrent.client.gui.GuiClient;

import javax.swing.*;

public final class TorrentClientMain {
    private TorrentClientMain() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            GuiClient guiClient = new GuiClient();
            guiClient.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            guiClient.setVisible(true);
        } else {
            new ConsoleClient(args).run();
        }
    }
}
