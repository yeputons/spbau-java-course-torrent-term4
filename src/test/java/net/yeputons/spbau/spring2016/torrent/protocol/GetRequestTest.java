package net.yeputons.spbau.spring2016.torrent.protocol;

import java.nio.ByteBuffer;

public class GetRequestTest extends RequestTest<ByteBuffer> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected GetRequest getRequest() {
        return new GetRequest(10, 20, 7);
    }

    @Override
    protected ByteBuffer getAnswer() {
        return ByteBuffer.wrap(new byte[] {
            1, 2, 3, 4, 5, 6, 7
        });
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[] {
            2,
            0, 0, 0, 10,
            0, 0, 0, 20
        };
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[] {
            1, 2, 3, 4, 5, 6, 7
        };
    }
    // CHECKSTYLE.ON: MagicNumber
}
