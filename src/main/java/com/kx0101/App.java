package com.kx0101;

import java.util.concurrent.BlockingQueue;

import com.kx0101.P2P.DefaultDecoder;
import com.kx0101.P2P.RPC;
import com.kx0101.P2P.TCPPeer;
import com.kx0101.P2P.TCPTransport;
import com.kx0101.P2P.TCPTransportOptions;

public class App {
    public static void main(String[] args) {
        TCPTransportOptions options = new TCPTransportOptions();
        options.listenPort = 3000;
        options.decoder = new DefaultDecoder();
        options.handshake = (peer) -> System.out
                .println("handshake with " + ((TCPPeer) peer).getSocket().getRemoteSocketAddress());
        options.onPeer = (peer) -> System.out
                .println("onPeer hook for " + ((TCPPeer) peer).getSocket().getRemoteSocketAddress());

        TCPTransport transport = new TCPTransport(options);

        try {
            transport.listenAndAccept();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        BlockingQueue<RPC> queue = transport.consume();

        while (true) {
            try {
                RPC rpc = queue.take();
                System.out.println(rpc.from + ": " + new String(rpc.data));
            } catch (Exception ex) {
                System.out.println("RPC: " + ex.getMessage());
            }
        }
    }
}
