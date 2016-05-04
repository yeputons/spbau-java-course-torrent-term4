package net.yeputons.spbau.spring2016.torrent.protocol;

import java.util.Arrays;
import java.util.List;

public class ListRequestTest extends RequestTest<List<FileEntry>> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected ListRequest getRequest() {
        return new ListRequest();
    }

    @Override
    protected List<FileEntry> getAnswer() {
        return Arrays.asList(new FileEntry(1, "file1", 12345678), new FileEntry(2, "file2", 87654321));
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[] {1};
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[] {
                0, 0, 0, 2,
                0, 0, 0, 1,
                0, 5, 'f', 'i', 'l', 'e', '1',
                0, 0, 0, 0, 0, (byte) 0xBC, 0x61, 0x4E,
                0, 0, 0, 2,
                0, 5, 'f', 'i', 'l', 'e', '2',
                0, 0, 0, 0, 0x05, 0x39, 0x7F, (byte) 0xB1
        };
    }
    // CHECKSTYLE.ON: MagicNumber
}
