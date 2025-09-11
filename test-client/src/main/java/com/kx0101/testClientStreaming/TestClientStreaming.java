package com.kx0101.testClientStreaming;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TestClientStreaming {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        File fileToUpload = new File("bigfile.bin");
        long totalSize = fileToUpload.length();

        try (Socket socket = new Socket(host, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                InputStream fileStream = new FileInputStream(fileToUpload)) {

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

            out.writeByte(1);
            byte[] filenameBytes = "bigfile.bin".getBytes();
            out.writeInt(filenameBytes.length);
            out.write(filenameBytes);
            out.writeLong(totalSize);
            out.flush();

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fileStream.read(buffer)) != -1) {
                System.out.println("Sending " + read + " bytes...");
                out.write(buffer, 0, read);
            }

            out.flush();
            System.out.println("Upload finished!");

            String fetchCmd = "FETCH bigfile.bin";
            byte[] cmdBytes = fetchCmd.getBytes();

            out.writeByte(0);
            out.writeInt(cmdBytes.length);
            out.write(cmdBytes);
            out.flush();
            System.out.println("Sent fetch command");

            int respFlag = in.readUnsignedByte();
            if (respFlag != 1) {
                throw new RuntimeException("unexpected flag: " + respFlag);
            }

            int respFnameLen = in.readInt();
            byte[] respFname = new byte[respFnameLen];
            in.readFully(respFname);
            String remoteFilename = new String(respFname);

            long fileSize = in.readLong();
            System.out.printf("Start reading %s (%d bytes)\n", remoteFilename, fileSize);

            File outFile = new File("downloaded_" + remoteFilename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[4096];
                int nr;

                while ((nr = in.read(buf)) != -1) {
                    fos.write(buf, 0, nr);
                }

                fos.flush();
            }

            System.out.println("Download finished!");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
