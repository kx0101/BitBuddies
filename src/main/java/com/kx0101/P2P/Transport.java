package com.kx0101.P2P;

import java.util.concurrent.BlockingQueue;

public interface Transport {
    void listenAndAccept() throws Exception;

    BlockingQueue<RPC> consume();
}
