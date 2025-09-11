package com.kx0101.P2P;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public interface Transport {
    void listenAndAccept() throws Exception;

    BlockingQueue<RPC> consume();

    void close() throws IOException;

    void dial(String addr);
}
