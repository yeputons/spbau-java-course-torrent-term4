package net.yeputons.spbau.spring2016.torrent.protocol;

import java.util.Arrays;
import java.util.List;

public class StatRequestTest extends RequestTest<List<Integer>> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected StatRequest getRequest() {
        return new StatRequest(987);
    }

    @Override
    protected List<Integer> getAnswer() {
        return Arrays.asList(2, 5, 8, 7);
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[]{
                1,
                0, 0, 0x03, (byte) 0xDB
        };
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[]{
                0, 0, 0, 4,
                0, 0, 0, 2,
                0, 0, 0, 5,
                0, 0, 0, 8,
                0, 0, 0, 7
        };
    }
    // CHECKSTYLE.ON: MagicNumber
}
