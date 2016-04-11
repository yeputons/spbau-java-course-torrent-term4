package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class Request<T> {
    private static final Map<Integer, Method> REQUEST_TYPES = new HashMap<>();

    static {
        registerRequestType(ListRequest.class);
        registerRequestType(UploadRequest.class);
        registerRequestType(SourcesRequest.class);
        registerRequestType(UpdateRequest.class);
    }

    static synchronized void registerRequestType(Class<? extends Request> klass) {
        try {
            int requestId = getRequestId(klass);
            if (REQUEST_TYPES.containsKey(getRequestId(klass))) {
                throw new IllegalArgumentException("Request with id " + requestId + " is already present");
            }
            REQUEST_TYPES.put(requestId, klass.getMethod("readFrom", DataInputStream.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static Request<?> readRequest(DataInputStream in) throws IOException {
        int id = in.read();
        if (id == -1) {
            throw new EOFException();
        }
        Method readFromMethod = REQUEST_TYPES.get(id);
        if (readFromMethod == null) {
            throw new UnknownRequestIdException();
        }
        try {
            return (Request<?>) readFromMethod.invoke(null, in);
        } catch (InvocationTargetException e) {
            throw (IOException) e.getCause();
        } catch (IllegalAccessException  e) {
            throw new RuntimeException(e);
        }
    }

    public static int getRequestId(Class<? extends Request> klass) {
        try {
            return klass.getField("REQUEST_ID").getInt(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public final int getRequestId() {
        return getRequestId(getClass());
    }

    public abstract T makeRequest(DataInputStream in, DataOutputStream out) throws IOException;
    //public static abstract Request<T> readFrom(DataInputStream in) throws IOException;
    public abstract void answerTo(DataOutputStream out, T data) throws IOException;
}
