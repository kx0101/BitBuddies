package com.kx0101.P2P;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DefaultDecoder implements Decoder {
    @Override
    public void decode(InputStream in, RPC rpc) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);

        int flag = dataIn.readUnsignedByte();
        if (flag == 1) {
            int filenameLen = dataIn.readInt();
            byte[] fnameBytes = new byte[filenameLen];
            dataIn.readFully(fnameBytes);
            String filename = new String(fnameBytes);

            long totalSize = dataIn.readLong();

            String header = "STREAM " + filename + " " + totalSize;

            rpc.data = header.getBytes();
            rpc.streamSize = totalSize;
            rpc.stream = dataIn;
            rpc.setStream(true);

            return;
        }

        int length = dataIn.readInt();
        byte[] data = new byte[length];

        dataIn.readFully(data);
        rpc.data = data;
    }
}
