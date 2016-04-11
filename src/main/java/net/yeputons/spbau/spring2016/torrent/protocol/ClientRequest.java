package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientRequest<T> extends Request<T> {
    private static final Map<Integer, Method> REQUEST_TYPES = new HashMap<>();

    static {
        registerRequestType(REQUEST_TYPES, StatRequest.class);
    }

    public static Request<?> readRequest(DataInputStream in) throws IOException {
        return readRequest(REQUEST_TYPES, in);
    }
}
