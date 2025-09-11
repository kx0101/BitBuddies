package com.kx0101;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kx0101.P2P.DefaultDecoder;
import com.kx0101.P2P.TCPTransport;
import com.kx0101.P2P.TCPTransportOptions;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        SecretKey secret = Crypto.generateKey();
        log.info("Secret Key (Base64): {}", Base64.getEncoder().encodeToString(secret.getEncoded()));

        Server fileServer5000 = makeServer(":5000", "5000_storage", List.of(), secret);
        executor.submit(() -> {
            try {
                fileServer5000.start();
            } catch (Exception ex) {
                System.out.println(ex);
            }
        });

        Server fileServer4000 = makeServer(":4000", "4000_storage", List.of(":5000"), secret);
        fileServer4000.start();
    }

    public static Server makeServer(
            String listenAddr,
            String storageRoot,
            List<String> bootstrapNodes,
            SecretKey secret) {
        int port = Integer.parseInt(listenAddr.replace(":", ""));

        TCPTransportOptions tcpOptions = new TCPTransportOptions();
        tcpOptions.listenPort = port;
        tcpOptions.decoder = new DefaultDecoder();
        TCPTransport tcpTransport = new TCPTransport(tcpOptions);

        ServerOptions options = new ServerOptions(listenAddr, storageRoot, tcpTransport, bootstrapNodes);
        Server fileServer = new Server(options, secret);

        tcpOptions.onPeer = (peer) -> fileServer.onPeer(peer);
        tcpOptions.handshake = (peer) -> {
            try {
                tcpTransport.authHandshake(peer, secret);
            } catch (Exception ex) {
                log.error("Handshake failed: {}", ex.getMessage());

                try {
                    peer.getSocket().close();
                } catch (Exception ignored) {
                }
            }
        };

        return fileServer;
    }
}
