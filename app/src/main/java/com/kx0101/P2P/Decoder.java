package com.kx0101.P2P;

import java.io.InputStream;
import java.io.IOException;

public interface Decoder {
    void decode(InputStream in, RPC rpc) throws IOException;
}
