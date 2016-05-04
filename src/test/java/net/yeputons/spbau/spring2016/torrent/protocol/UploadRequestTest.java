package net.yeputons.spbau.spring2016.torrent.protocol;

public class UploadRequestTest extends RequestTest<Integer> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected UploadRequest getRequest() {
        return new UploadRequest("my_file", 456);
    }

    @Override
    protected Integer getAnswer() {
        return 100;
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[] {
                2,
                0, 7, 'm', 'y', '_', 'f', 'i', 'l', 'e',
                0, 0, 0, 0, 0, 0, 0x01, (byte) 0xC8
        };
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[] {0, 0, 0, 100};
    }
    // CHECKSTYLE.ON: MagicNumber
}
