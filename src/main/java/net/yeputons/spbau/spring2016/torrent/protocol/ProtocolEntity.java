package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface ProtocolEntity {
    void writeTo(DataOutputStream out) throws IOException;

    static ProtocolEntity readFrom(Class<? extends ProtocolEntity> klass, DataInputStream in) throws IOException {
        try {
            return (ProtocolEntity) klass.getMethod("readFrom", DataInputStream.class).invoke(null, in);
        } catch (InvocationTargetException e) {
            throw (IOException) e.getCause();
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
