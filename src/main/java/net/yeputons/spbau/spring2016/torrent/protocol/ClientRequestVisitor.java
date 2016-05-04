package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.IOException;

public interface ClientRequestVisitor {
    void accept(StatRequest r) throws IOException;
    void accept(GetRequest r) throws IOException;
}
