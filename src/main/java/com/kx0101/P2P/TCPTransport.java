package com.kx0101.P2P;

import java.net.ServerSocket;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPTransport implements Transport {
    private final TCPTransportOptions options;
    private ServerSocket serverSocket;
    private final BlockingQueue<RPC> rpcQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public TCPTransport(TCPTransportOptions options) {
        this.options = options;
    }

    @Override
    public void listenAndAccept() throws Exception {
        serverSocket = new ServerSocket(options.listenPort);

        executor.submit(this::listen);
    }

    private void listen() {
        while (true) {
            try {
                Socket conn = serverSocket.accept();

                executor.submit(() -> handleConnection(conn));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket conn) {
        try {
            TCPPeer peer = new TCPPeer(conn, true);
            System.out.println("New incoming connection: " + conn.getRemoteSocketAddress());

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
            System.out.println("Closing peer connection: " + ex.getMessage());
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public BlockingQueue<RPC> consume() {
        return rpcQueue;
    }
}
