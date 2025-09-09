package com.kx0101.P2P;

import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPPeer implements Peer {
    private final Socket socket;
    private final boolean outbound;

    public TCPPeer(Socket socket, boolean outbound) {
        this.socket = socket;
        this.outbound = outbound;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isOutBound() {
        return outbound;
    }

    @Override
    public void Send(byte[] data) throws Exception {
        OutputStream out = this.socket.getOutputStream();

        out.write(data);
        out.flush();
    }

    @Override
    public SocketAddress remoteAddr() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
