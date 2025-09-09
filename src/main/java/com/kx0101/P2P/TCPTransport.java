package com.kx0101.P2P;

import java.net.ServerSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPTransport implements Transport {
    private final TCPTransportOptions options;
    private ServerSocket serverSocket;
    private final BlockingQueue<RPC> rpcQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final Logger log = LoggerFactory.getLogger(TCPTransport.class);

    public TCPTransport(TCPTransportOptions options) {
        this.options = options;
    }

    @Override
    public void listenAndAccept() throws Exception {
        serverSocket = new ServerSocket(options.listenPort);

        executor.submit(this::listen);
    }

    @Override
    public void Dial(String addr) {
        String[] parts = addr.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try {
            Socket socket = new Socket(host, port);

            this.handleConnection(socket, true);
        } catch (Exception ex) {
            log.error("Error dialing {}: {}", addr, ex.getMessage());
        }
    }

    @Override
    public BlockingQueue<RPC> consume() {
        return rpcQueue;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    private void listen() {
        while (true) {
            try {
                Socket conn = serverSocket.accept();

                executor.submit(() -> handleConnection(conn, false));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket conn, boolean outbound) {
        try {
            TCPPeer peer = new TCPPeer(conn, outbound);
            log.info("New connection from " + conn.getRemoteSocketAddress());

            if (options.handshake != null) {
                options.handshake.accept(peer);
            }

            if (options.onPeer != null) {
                options.onPeer.accept(peer);
            }

            InputStream in = conn.getInputStream();
            RPC rpc = new RPC();

            while (true) {
                options.decoder.decode(in, rpc);
                rpc.from = conn.getRemoteSocketAddress();
                rpcQueue.put(rpc);

                rpc = new RPC();
            }
        } catch (Exception ex) {
            log.info("Closing peer connection: " + ex.getMessage());
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }
    }
}
