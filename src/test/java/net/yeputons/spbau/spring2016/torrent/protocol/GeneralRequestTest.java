package net.yeputons.spbau.spring2016.torrent.protocol;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class GeneralRequestTest {
    @Test(expected = NoRequestException.class)
    public void testRequestEof() throws IOException {
        ServerRequest.readRequest(new DataInputStream(new ByteArrayInputStream(new byte[0])));
    }
}
