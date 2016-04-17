package net.yeputons.spbau.spring2016.torrent.client;

import net.yeputons.spbau.spring2016.torrent.FileDescription;
import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ClientStateTest {
    private static final FileDescription FILE = new FileDescription(new FileEntry(1, "file1.txt", 100), 35);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testGetFileDescription() {
        ClientState state = new ClientState(folder.getRoot().toPath());
        assertTrue(state.getFiles().isEmpty());
        assertNull(state.getFileDescription(1));
        state.getFiles().put(FILE.getEntry().getId(), FILE);
        assertSame(FILE, state.getFileDescription(1));
    }

    @Test
    public void testRandomAccessFileCreation() throws IOException {
        ClientState state = new ClientState(folder.getRoot().toPath());
        state.getFiles().put(FILE.getEntry().getId(), FILE);

        Path filePath = folder.getRoot().toPath().resolve("file1.txt");
        assertFalse(Files.exists(filePath));
        RandomAccessFile file = state.getFile(1);
        // CHECKSTYLE.OFF: MagicNumber
        assertEquals(100, file.length());
        assertTrue(Files.exists(filePath));
        assertEquals(100, Files.size(filePath));
        // CHECKSTYLE.ON: MagicNumber
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ClientState state = new ClientState(folder.getRoot().toPath());
        state.getFiles().put(FILE.getEntry().getId(), FILE);
        RandomAccessFile file1 = state.getFile(1);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out  = new ObjectOutputStream(bout)) {
            out.writeObject(state);
        }

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        try (ObjectInputStream in = new ObjectInputStream(bin)) {
            state = (ClientState) in.readObject();
        }

        RandomAccessFile file2 = state.getFile(1);
        assertNotSame(file1, file2);

        // CHECKSTYLE.OFF: MagicNumber
        file1.seek(0);
        assertEquals(0, file1.read());

        file2.seek(0);
        file2.write(10);

        file1.seek(0);
        assertEquals(10, file1.read());
        // CHECKSTYLE.ON: MagicNumber
    }
}
