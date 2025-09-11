package com.kx0101.P2P;

import java.net.Socket;

public interface Peer {
    Socket getSocket();

    String getId();
}
