package net.yeputons.spbau.spring2016.torrent.protocol;

public class UnknownRequestIdException extends RuntimeException {
    public UnknownRequestIdException(int id) {
        super("Client send request with unknown id: " + id);
    }
}
