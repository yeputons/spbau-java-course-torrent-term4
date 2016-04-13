package net.yeputons.spbau.spring2016.torrent;

import net.yeputons.spbau.spring2016.torrent.protocol.FileEntry;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileDescriptionTest {
    @Test
    public void testEvenParts() {
        FileDescription description = new FileDescription(new FileEntry(1, "test", 80), 40);
        assertEquals(2, description.getPartsCount());
        assertEquals(0, description.getPartStart(0));
        assertEquals(40, description.getPartSize(0));
        assertEquals(40, description.getPartStart(1));
        assertEquals(40, description.getPartSize(1));
    }

    @Test
    public void testMultipleParts() {
        FileDescription description = new FileDescription(new FileEntry(1, "test", 100), 40);
        assertEquals(3, description.getPartsCount());
        assertEquals(0, description.getPartStart(0));
        assertEquals(40, description.getPartSize(0));
        assertEquals(40, description.getPartStart(1));
        assertEquals(40, description.getPartSize(1));
        assertEquals(80, description.getPartStart(2));
        assertEquals(20, description.getPartSize(2));
    }

    @Test
    public void testSinglePart() {
        FileDescription description = new FileDescription(new FileEntry(1, "test", 30), 40);
        assertEquals(1, description.getPartsCount());
        assertEquals(0, description.getPartStart(0));
        assertEquals(30, description.getPartSize(0));
    }
}