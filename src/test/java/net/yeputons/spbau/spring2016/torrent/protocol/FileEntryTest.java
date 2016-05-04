package net.yeputons.spbau.spring2016.torrent.protocol;

public class FileEntryTest extends ProtocolEntityTest {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected ProtocolEntity getObject() {
        return new FileEntry(123456789, "some_file", 123456789012345L);
    }

    @Override
    protected byte[] getSerializedObject() {
        return new byte[] {
                0x07, 0x5B, (byte) 0xCD, 0x15,
                0, 9, 's', 'o', 'm', 'e', '_', 'f', 'i', 'l', 'e',
                0, 0, 0x70, 0x48, (byte) 0x86, 0x0D, (byte) 0xDF, 0x79
        };
    }
    // CHECKSTYLE.ON: MagicNumber
}
