package net.yeputons.spbau.spring2016.torrent.protocol;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public abstract class ProtocolEntityTest<T extends ProtocolEntity> {
    protected abstract T getObject();
    protected abstract byte[] getSerializedObject();

    @Test
    public void testWriteTo() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (DataOutputStream dataOut = new DataOutputStream(out)) {
                getObject().writeTo(dataOut);
            }
            assertArrayEquals(getSerializedObject(), out.toByteArray());
        }
    }

    @Test
    public void testReadFrom() throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(getSerializedObject())) {
            assertEquals(getObject(), ProtocolEntity.readFrom(getObject().getClass(), new DataInputStream(in)));
        }
    }
}
