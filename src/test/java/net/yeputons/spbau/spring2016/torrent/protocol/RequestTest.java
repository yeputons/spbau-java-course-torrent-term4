package net.yeputons.spbau.spring2016.torrent.protocol;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public abstract class RequestTest<T> {
    protected abstract Request<T> getRequest();
    protected abstract T getAnswer();
    protected abstract byte[] getSerializedRequest();
    protected abstract byte[] getSerializedAnswer();

    @Test
    public void testAnswerTo() throws IOException {
        Request<T> r = getRequest();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (DataOutputStream dataOut = new DataOutputStream(out)) {
                r.answerTo(dataOut, getAnswer());
            }
            assertArrayEquals(getSerializedAnswer(), out.toByteArray());
        }
    }

    @Test
    public void testReadFrom() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(getSerializedRequest())) {
            Request<T> request = getRequest();
            if (request instanceof ServerRequest) {
                assertEquals(request, ServerRequest.readRequest(new DataInputStream(in)));
            } else  if (request instanceof ClientRequest) {
                assertEquals(request, ClientRequest.readRequest(new DataInputStream(in)));
            } else {
                throw new AssertionError("request is neither ServerRequest nor ClientRequest");
            }
        }
    }

    @Test
    public void testMakeRequest() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(getSerializedAnswer());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (DataOutputStream dataOut = new DataOutputStream(out)) {
                assertEquals(getAnswer(), getRequest().makeRequest(new DataInputStream(in), dataOut));
            }
            assertArrayEquals(getSerializedRequest(), out.toByteArray());
        }
    }
}
