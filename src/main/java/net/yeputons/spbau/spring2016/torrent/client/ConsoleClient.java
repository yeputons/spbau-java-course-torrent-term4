package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.StateFileHolder;
import net.yeputons.spbau.spring2016.torrent.TorrentConnection;
import net.yeputons.spbau.spring2016.torrent.TrackerServer;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import net.yeputons.spbau.spring2016.torrent.protocol.ListRequest;
import net.yeputons.spbau.spring2016.torrent.protocol.UploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

public class ConsoleClient implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleClient.class);
    private final int partSize;
    private final Path storage = Paths.get("torrent-client-state.bin");
    private final Deque<String> args;

    private final StateFileHolder<ClientState> stateHolder;

    public ConsoleClient(String[] args) {
        this.args = new LinkedList<>(Arrays.asList(args));
        stateHolder = new StateFileHolder<>(storage, new ClientState(Paths.get("./downloads")));
        partSize = Integer.parseInt(System.getProperty("torrent.part_size", "10485760"));
        LOG.info("Will use part size of {} bytes for new uploads and downloads", partSize);
    }

    @Override
    public void run() {
        if (args.size() < 2) {
            help();
        }
        String operation = args.removeFirst();
        InetSocketAddress addr = new InetSocketAddress(args.removeFirst(), TrackerServer.DEFAULT_PORT);
        try (TorrentConnection connection = new TorrentConnection(addr)) {
            switch (operation) {
                case "list":
                    doList(connection);
                    break;
                case "newfile":
                    doNewFile(connection);
                    break;
                case "get":
                    doGet(connection);
                    break;
                case "run":
                    doRun(connection);
                    break;
                default:
                    help();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void help() {
        System.err.println("Expected arguments: client (list|get|newfile|run) <tracker-address> [extra]");
        System.err.println("Extra arguments for 'get': <file-id>");
        System.err.println("Extra arguments for 'newfile': <file-path>");
        System.err.println(
                "Default file part size is 10485760 bytes, "
                + "can be modified with JVM property torrent.part_size");
        System.exit(1);
    }

    private void doList(TorrentConnection connection) throws IOException {
        if (args.size() != 0) {
            help();
        }
        System.out.printf("%9s %19s %s\n", "ID", "SIZE", "NAME");
        for (FileEntry e : connection.makeRequest(new ListRequest())) {
            System.out.printf("%9d %19d %s\n", e.getId(), e.getSize(), e.getName());
        }
    }

    private void doNewFile(TorrentConnection connection) throws IOException {
        if (args.size() != 1) {
            help();
        }
        Path p = Paths.get(args.removeFirst());
        String fileName = p.getFileName().toString();
        long size = Files.size(p);
        LOG.info("Adding file {} ({} bytes) to server", fileName, size);
        int id = connection.makeRequest(new UploadRequest(fileName, size));
        LOG.info("Server confirmed upload, file id is {}", id);

        ClientState state = stateHolder.getState();
        FileDescription description = new FileDescription(new FileEntry(id, fileName, size), partSize);
        description.getDownloaded().flip(0, description.getPartsCount());
        state.getFiles().put(id, description);
        stateHolder.save();
    }

    private void doGet(TorrentConnection connection) throws IOException {
        if (args.size() != 1) {
            help();
        }
        int id = Integer.parseInt(args.removeFirst());
        ClientState state = stateHolder.getState();
        if (state.getFiles().containsKey(id)) {
            LOG.warn("File is already in the list: {}", state.getFileDescription(id));
            return;
        }

        for (FileEntry e : connection.makeRequest(new ListRequest())) {
            if (e.getId() == id) {
                FileDescription description = new FileDescription(e, partSize);
                state.getFiles().put(id, description);
                stateHolder.save();
                return;
            }
        }
        LOG.error("File with id {} was not found on the server", id);
        System.exit(1);
    }

    private void doRun(TorrentConnection connection) throws IOException {
        if (args.size() != 0) {
            help();
        }
        TorrentClient client = new TorrentClient(connection, stateHolder);
        client.start();
        try {
            client.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
