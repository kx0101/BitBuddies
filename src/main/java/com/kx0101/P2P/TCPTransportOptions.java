package com.kx0101.P2P;

import java.util.function.Consumer;

public class TCPTransportOptions {
    public int listenPort;
    public Decoder decoder;
    public Consumer<Peer> onPeer;
    public Consumer<Peer> handshake;
}
