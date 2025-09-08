package com.kx0101;

import com.kx0101.P2P.Transport;

public class ServerOptions {
    public String listenAddr;
    public String storageRoot;
    public Transport transport;

    public ServerOptions(String listenAddr, String storageRoot, Transport transport) {
        this.listenAddr = listenAddr;
        this.storageRoot = storageRoot;
        this.transport = transport;
    }
}
