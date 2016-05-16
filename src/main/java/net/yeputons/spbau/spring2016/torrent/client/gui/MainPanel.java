package net.yeputons.spbau.spring2016.torrent.client.gui;

import net.yeputons.spbau.spring2016.torrent.FirmTorrentConnection;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.net.SocketAddress;
import java.text.DateFormat;
import java.util.Calendar;

public class MainPanel extends JPanel {
    private final GuiClient guiClient;

    private JTable table;
    private DownloadsTableModel tableModel;
    private JLabel trackerAddress;
    private JLabel trackerStatus;
    private JLabel lastUpdateInfo;

    public MainPanel(GuiClient guiClient) {
        this.guiClient = guiClient;
        setLayout(new BorderLayout());
        addDownloadsTable();
        addMenu();
        addStatusBar();
    }

    public DownloadsTableModel getTableModel() {
        return tableModel;
    }

    private GuiFileEntry getSelectedFileInfo() {
        int row = table.getSelectedRow();
        if (row == -1) {
            return null;
        }
        GuiState state = guiClient.getGuiStateHolder().getState();
        synchronized (state) {
            if (row >= state.getFileList().size()) {
                return null;
            }
            return state.getFileList().get(row);
        }
    }

    private void addDownloadsTable() {
        tableModel = new DownloadsTableModel(guiClient.getStateHolder(), guiClient.getGuiStateHolder());

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }

    private void addMenu() {
        final JMenuBar menuBar = new JMenuBar();

        PropertyDescription<GuiFileEntry.State> currentFileStateDescription =
                new PropertyDescription<GuiFileEntry.State>() {
                    @Override
                    public GuiFileEntry.State get() {
                        GuiFileEntry guiEntry = getSelectedFileInfo();
                        if (guiEntry == null) {
                            return null;
                        }
                        return guiEntry.getState();
                    }

                    @Override
                    public void set(GuiFileEntry.State newValue) {
                        GuiFileEntry guiEntry = getSelectedFileInfo();
                        if (guiEntry != null) {
                            guiClient.setFileState(guiEntry, newValue);
                        }
                    }
                };

        JMenu fileMenu = new JMenu("File");
        FileStartDownloadAction fileStartDownloadAction = new FileStartDownloadAction(currentFileStateDescription);
        FileStopDownloadAction fileStopDownloadAction = new FileStopDownloadAction(currentFileStateDescription);
        table.getSelectionModel().addListSelectionListener(e -> {
            fileStartDownloadAction.updateEnabledState();
            fileStopDownloadAction.updateEnabledState();
        });
        tableModel.addTableModelListener(e -> {
            fileStartDownloadAction.updateEnabledState();
            fileStopDownloadAction.updateEnabledState();
        });
        fileMenu.add(new UploadFileAction(guiClient));
        fileMenu.add(fileStartDownloadAction);
        fileMenu.add(fileStopDownloadAction);
        menuBar.add(fileMenu);

        JMenu trackerMenu = new JMenu("Tracker");
        UpdateListAction updateListAction = new UpdateListAction(guiClient);
        guiClient.getGuiStateHolder().getState().addPropertyChangeListener(e -> {
            if (e.getPropertyName().equals("trackerConnection")) {
                updateListAction.updateEnabledState();
            }
        });
        trackerMenu.add(updateListAction);
        menuBar.add(trackerMenu);

        JMenu configMenu = new JMenu("Configure");
        configMenu.add(new SetTrackerAction(guiClient, new PropertyDescription<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return guiClient.getGuiStateHolder().getState().getTrackerAddress();
            }

            @Override
            public void set(SocketAddress newValue) {
                guiClient.setTrackerAddress(newValue);
            }
        }));
        configMenu.add(new SetUpdateIntervalAction(guiClient, new PropertyDescription<Integer>() {
            @Override
            public Integer get() {
                return guiClient.getGuiStateHolder().getState().getUpdateInterval();
            }

            @Override
            public void set(Integer newValue) {
                guiClient.setUpdateInterval(newValue);
            }
        }));
        configMenu.add(new SetDefaultPartSizeAction(guiClient, new PropertyDescription<Integer>() {
            @Override
            public Integer get() {
                return guiClient.getGuiStateHolder().getState().getDefaultPartSize();
            }

            @Override
            public void set(Integer newValue) {
                guiClient.setDefaultPartSize(newValue);
            }
        }));
        menuBar.add(configMenu);

        add(menuBar, BorderLayout.NORTH);
    }

    private void addStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        add(statusBar, BorderLayout.SOUTH);

        // CHECKSTYLE.OFF: MagicNumber
        statusBar.setLayout(new GridLayout(
                1, // rows
                3, // cols
                32, //hgap
                0 // vgap
        ));
        // CHECKSTYLE.ON: MagicNumber
        trackerAddress = new JLabel();
        trackerStatus = new JLabel();
        lastUpdateInfo = new JLabel();
        statusBar.add(trackerAddress);
        statusBar.add(trackerStatus);
        statusBar.add(lastUpdateInfo);

        GuiState state = guiClient.getGuiStateHolder().getState();
        synchronized (state) {
            state.addPropertyChangeListener((e) -> {
                if (e.getPropertyName().equals("trackerConnection")) {
                    ((FirmTorrentConnection) e.getNewValue()).addPropertyChangeListener(e1 -> updateStatusBar());
                } else {
                    updateStatusBar();
                }
            });
            FirmTorrentConnection connection = state.getTrackerConnection();
            if (connection != null) {
                connection.addPropertyChangeListener(e -> updateStatusBar());
            }
        }

        updateStatusBar();
    }

    private void updateStatusBar() {
        SwingUtilities.invokeLater(() -> {
            GuiState state = guiClient.getGuiStateHolder().getState();
            SocketAddress address;
            FirmTorrentConnection connection;
            synchronized (state) {
                address = state.getTrackerAddress();
                connection = state.getTrackerConnection();
            }
            trackerAddress.setText("Tracker address: " + (address == null ? "undefined" : address.toString()));

            String status = "N/A";
            if (connection != null) {
                status = connection.getState().toString().toLowerCase();
            }
            trackerStatus.setText("Tracker status: " + status);

            Calendar lastUpdate = state.getFileListLastUpdated();
            String lastUpdateStr = "none";
            if (lastUpdate != null) {
                lastUpdateStr = DateFormat.getDateTimeInstance().format(lastUpdate.getTime());
            }
            lastUpdateInfo.setText("Last update: " + lastUpdateStr);
        });
    }
}
