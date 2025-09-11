package com.kx0101.P2P;

import java.io.InputStream;

public class RPC {
    public String from;
    public byte[] data;
    private boolean isStream;

    public transient InputStream stream;
    public long streamSize;

    public RPC() {
    }

    public RPC(byte[] data, boolean isStream) {
        this.data = data;
        this.isStream = isStream;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean isStream) {
        this.isStream = isStream;
    }
}
