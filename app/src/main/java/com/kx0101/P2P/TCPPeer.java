package com.kx0101.P2P;

import java.net.Socket;

public class TCPPeer implements Peer {
    private final Socket socket;
    private final boolean outbound;

    private final Object streamLock = new Object();
    private volatile boolean streaming = false;

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

    public void waitForStreamEnd() throws InterruptedException {
        synchronized (streamLock) {
            while (streaming) {
                streamLock.wait();
            }
        }
    }

    public void startStream() {
        synchronized (streamLock) {
            this.streaming = true;
        }
    }

    public void stopStream() {
        synchronized (streamLock) {
            this.streaming = false;
            this.streamLock.notifyAll();
        }
    }

    @Override
    public String getId() {
        return this.socket.getRemoteSocketAddress().toString();
    }
}
