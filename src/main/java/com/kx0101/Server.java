package com.kx0101;

import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.kx0101.P2P.Peer;
import com.kx0101.P2P.RPC;

public class Server {
    public ServerOptions options;
    public Storage storage;
    public ConcurrentHashMap<SocketAddress, Peer> peers;

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public Server(ServerOptions options) {
        this.options = options;
        this.storage = new Storage(new StorageOptions(Paths.get(options.storageRoot)));
        this.peers = new ConcurrentHashMap<>();
    }

    public void start() throws Exception {
        log.info("Server starting on {}", options.listenAddr);

        try {
            this.options.transport.listenAndAccept();
        } catch (Exception e) {
            log.error("Failed to bind transport on {}", options.listenAddr, e);

            throw e;
        }

        this.bootstrapNetwork();

        while (true) {
            RPC rpc = this.options.transport.consume().take();
            log.debug("Received RPC from {}: {}", rpc.from, new String(rpc.data));
        }
    }

    public void bootstrapNetwork() {
        for (String addr : this.options.bootstrapNodes) {
            this.options.transport.Dial(addr);
        }
    }

    public void onPeer(Peer peer) {
        SocketAddress peerId = peer.remoteAddr();
        this.peers.put(peerId, peer);

        log.info("New peer connected: {}", peer.remoteAddr());
    }
}
