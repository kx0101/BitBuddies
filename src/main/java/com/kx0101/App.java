package com.kx0101;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kx0101.P2P.DefaultDecoder;
import com.kx0101.P2P.TCPPeer;
import com.kx0101.P2P.TCPTransport;
import com.kx0101.P2P.TCPTransportOptions;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        executor.submit(() -> makeServer(":3000", "3000_storage", List.of()));

        makeServer(":4000", "4000_storage", List.of(":3000"));
    }

    public static void makeServer(String listenAddr, String storageRoot, List<String> bootstrapNodes) {
        int port = Integer.parseInt(listenAddr.replace(":", ""));

        TCPTransportOptions tcpOptions = new TCPTransportOptions();
        tcpOptions.listenPort = port;
        tcpOptions.decoder = new DefaultDecoder();
        tcpOptions.handshake = (peer) -> log
                .info("Handshake with peer: " + ((TCPPeer) peer).getSocket().getRemoteSocketAddress());
        TCPTransport tcpTransport = new TCPTransport(tcpOptions);

        ServerOptions options = new ServerOptions(listenAddr, storageRoot, tcpTransport, bootstrapNodes);
        Server fileServer = new Server(options);

        tcpOptions.onPeer = (peer) -> fileServer.onPeer(peer);

        try {
            fileServer.start();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
