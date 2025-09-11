package com.kx0101.P2P;

import java.net.ServerSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

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
    public void dial(String addr) {
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

            while (!conn.isClosed()) {
                RPC rpc = new RPC();
                this.options.decoder.decode(in, rpc);

                rpc.from = peer.getSocket().getRemoteSocketAddress().toString();

                if (rpc.isStream()) {
                    peer.startStream();

                    rpc.stream = in;
                    rpcQueue.put(rpc);

                    peer.waitForStreamEnd();

                    continue;
                }

                rpcQueue.put(rpc);
            }
        } catch (Exception ex) {
            log.info("Closing peer connection: " + ex);
            try {
                conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void authHandshake(Peer peer, SecretKey SECRET_KEY) throws Exception {
        DataInputStream in = new DataInputStream(peer.getSocket().getInputStream());
        DataOutputStream out = new DataOutputStream(peer.getSocket().getOutputStream());

        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        out.writeInt(nonce.length);
        out.write(nonce);
        out.flush();

        int hmacLen = in.readInt();
        byte[] clientHmac = new byte[hmacLen];
        in.readFully(clientHmac);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(SECRET_KEY);
        byte[] expectedHmac = mac.doFinal(nonce);

        if (!Arrays.equals(clientHmac, expectedHmac)) {
            throw new RuntimeException("Invalid handshake: client authentication failed");
        }

        log.info("Handshake successful with peer {}", peer.getSocket().getRemoteSocketAddress());
    }
}
