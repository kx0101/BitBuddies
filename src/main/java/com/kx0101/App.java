package com.kx0101;

import java.util.List;

import com.kx0101.P2P.DefaultDecoder;
import com.kx0101.P2P.TCPPeer;
import com.kx0101.P2P.TCPTransport;
import com.kx0101.P2P.TCPTransportOptions;

public class App {
    public static void main(String[] args) throws Exception {
        TCPTransportOptions tcpOptions = new TCPTransportOptions();
        tcpOptions.listenPort = 3000;
        tcpOptions.decoder = new DefaultDecoder();
        tcpOptions.handshake = (peer) -> System.out
                .println("handshake with " + ((TCPPeer) peer).getSocket().getRemoteSocketAddress());
        tcpOptions.onPeer = (peer) -> System.out
                .println("onPeer hook for " + ((TCPPeer) peer).getSocket().getRemoteSocketAddress());

        TCPTransport tcpTransport = new TCPTransport(tcpOptions);

        List<String> bootstrapNodes = List.of(":4000");
        ServerOptions options = new ServerOptions(":3000", "3000_network", tcpTransport, bootstrapNodes);
        Server fileServer = new Server(options);

        try {
            fileServer.start();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
