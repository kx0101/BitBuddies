package com.kx0101.testClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TestClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(host, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())) {

            int nonceLen = in.readInt();
            byte[] nonce = new byte[nonceLen];
            in.readFully(nonce);

            byte[] keyBytes = Base64.getDecoder().decode("V/NJwmlStMEjgyg0VXb4jNobaaabo3WyF+ssczHp0Bg=");
            SecretKey secret = new SecretKeySpec(keyBytes, "AES");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secret);
            byte[] hmac = mac.doFinal(nonce);

            out.writeInt(hmac.length);
            out.write(hmac);
            out.flush();

            System.out.println("Handshake completed, ready to upload/fetch files");

            byte[] payload = "STORE test.txt lets try one more time offf\n".getBytes(StandardCharsets.UTF_8);

            out.writeByte(0);
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();

            byte[] fetchCmd = "FETCH test.txt".getBytes(StandardCharsets.UTF_8);
            out.writeByte(0);
            out.writeInt(fetchCmd.length);
            out.write(fetchCmd);
            out.flush();

            try (FileOutputStream fos = new FileOutputStream("fetched_test.txt")) {
                byte consumeFlag = in.readByte();

                int filenameLen = in.readInt();
                byte[] filenameBytes = new byte[filenameLen];
                in.readFully(filenameBytes);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();

                System.out.println("Fetched file saved as fetched_test.txt");
            }

            Thread.sleep(5000);

            // streamed RPC
            // out.writeByte(1);
            // out.flush();
            // write a stream of bytes using socket.getOutputStream()

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
