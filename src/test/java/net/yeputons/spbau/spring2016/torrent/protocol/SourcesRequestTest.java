package net.yeputons.spbau.spring2016.torrent.protocol;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class SourcesRequestTest extends RequestTest<List<InetSocketAddress>> {
    // CHECKSTYLE.OFF: MagicNumber
    @Override
    protected SourcesRequest getRequest() {
        return new SourcesRequest(123);
    }

    @Override
    protected List<InetSocketAddress> getAnswer() {
        return Arrays.asList(
                new InetSocketAddress("127.0.0.1", 80),
                new InetSocketAddress("192.168.10.35", 8080)
        );
    }

    @Override
    protected byte[] getSerializedRequest() {
        return new byte[] {
                3,
                0, 0, 0, 123
        };
    }

    @Override
    protected byte[] getSerializedAnswer() {
        return new byte[] {
                0, 0, 0, 2,
                127, 0, 0, 1,
                0, 80,
                (byte) 192, (byte) 168, 10, 35,
                0x1F, (byte) 0x90
        };
    }
    // CHECKSTYLE.ON: MagicNumber
}
