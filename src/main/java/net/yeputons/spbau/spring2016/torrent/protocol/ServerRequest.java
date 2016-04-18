package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class ServerRequest<T> extends Request<T> {
    private static final Map<Integer, Method> REQUEST_TYPES = new HashMap<>();

    private static void initRequestTypes() {
        if (!REQUEST_TYPES.isEmpty()) {
            return;
        }
        registerRequestType(REQUEST_TYPES, ListRequest.class);
        registerRequestType(REQUEST_TYPES, UploadRequest.class);
        registerRequestType(REQUEST_TYPES, SourcesRequest.class);
        registerRequestType(REQUEST_TYPES, UpdateRequest.class);
    }

    public abstract void visit(ServerRequestVisitor visitor) throws IOException;

    public static ServerRequest<?> readRequest(DataInputStream in) throws IOException {
        initRequestTypes();
        return (ServerRequest<?>) readRequest(REQUEST_TYPES, in);
    }
}
