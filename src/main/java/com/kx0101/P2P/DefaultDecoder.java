package com.kx0101.P2P;

import java.io.IOException;
import java.io.InputStream;

public class DefaultDecoder implements Decoder {
    @Override
    public void decode(InputStream in, RPC rpc) throws IOException {
        byte[] buf = new byte[1024];
        int n = in.read(buf);

        if (n == -1) {
            throw new IOException("stream closed");
        }

        rpc.data = new byte[n];
        System.arraycopy(buf, 0, rpc.data, 0, n);
    }
}
