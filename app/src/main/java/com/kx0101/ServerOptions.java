package com.kx0101;

import java.util.ArrayList;
import java.util.List;

import com.kx0101.P2P.Transport;

public class ServerOptions {
    public String listenAddr;
    public String storageRoot;
    public Transport transport;
    public List<String> bootstrapNodes = new ArrayList<>();

    public ServerOptions(String listenAddr, String storageRoot, Transport transport, List<String> bootstrapNodes) {
        this.listenAddr = listenAddr;
        this.storageRoot = storageRoot;
        this.transport = transport;
        this.bootstrapNodes = bootstrapNodes;
    }
}
