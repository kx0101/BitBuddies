package com.kx0101.P2P;

import java.net.SocketAddress;

public interface Peer {
    void Send(byte[] data) throws Exception;

    SocketAddress remoteAddr();

    void close() throws Exception;
}
