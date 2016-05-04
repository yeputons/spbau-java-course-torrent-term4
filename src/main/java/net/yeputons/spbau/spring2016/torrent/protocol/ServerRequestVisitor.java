package net.yeputons.spbau.spring2016.torrent.protocol;

import java.io.IOException;

public interface ServerRequestVisitor {
    void accept(ListRequest r) throws IOException;
    void accept(UploadRequest r) throws IOException;
    void accept(SourcesRequest r) throws IOException;
    void accept(UpdateRequest r) throws IOException;
}
