package com.kx0101;

import java.nio.file.Paths;

import com.kx0101.P2P.RPC;

public class Server {
    public ServerOptions options;
    public Storage storage;

    public Server(ServerOptions options) {
        this.options = options;
        this.storage = new Storage(new StorageOptions(Paths.get(options.storageRoot)));
    }

    public void start() throws Exception {
        System.out.println("Server starting on " + options.listenAddr);
        this.options.transport.listenAndAccept();

        while (true) {
            RPC rpc = this.options.transport.consume().take();
            System.out.printf("%s: %s%n", rpc.from, new String(rpc.data));
        }
    }
}
