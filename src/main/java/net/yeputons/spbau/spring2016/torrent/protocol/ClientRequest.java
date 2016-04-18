package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientRequest<T> extends Request<T> {
    private static final Map<Integer, Method> REQUEST_TYPES = new HashMap<>();

    public abstract void visit(ClientRequestVisitor visitor) throws IOException;

    public static ClientRequest<?> readRequest(DataInputStream in) throws IOException {
        initRequestTypes();
        return (ClientRequest<?>) readRequest(REQUEST_TYPES, in);
    }

    private static void initRequestTypes() {
        if (!REQUEST_TYPES.isEmpty()) {
            return;
        }
        registerRequestType(REQUEST_TYPES, StatRequest.class);
        registerRequestType(REQUEST_TYPES, GetRequest.class);
    }
}
