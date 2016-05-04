package net.yeputons.spbau.spring2016.torrent.protocol;

import java.util.Arrays;

public class UpdateRequestTest extends RequestTest<Boolean> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected UpdateRequest getRequest() {
        return new UpdateRequest(8081, Arrays.asList(1, 5, 2, 3));
    }

    @Override
    protected Boolean getAnswer() {
        return true;
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[] {
                4,
                0x1F, (byte) 0x91,
                0, 0, 0, 4,
                0, 0, 0, 1,
                0, 0, 0, 5,
                0, 0, 0, 2,
                0, 0, 0, 3
        };
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[] {1};
    }
    // CHECKSTYLE.ON: MagicNumber
}
