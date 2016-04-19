package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.EOFException;

public class NoRequestException extends EOFException {
    public NoRequestException() {
        super("No request was received from the client");
    }
}
