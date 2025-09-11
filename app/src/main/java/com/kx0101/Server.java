package com.kx0101;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.kx0101.P2P.Peer;
import com.kx0101.P2P.RPC;
import com.kx0101.P2P.TCPPeer;

public class Server {
    public ServerOptions options;
    public Storage storage;
    public ConcurrentHashMap<String, Peer> peers;

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private volatile boolean running = false;
    private SecretKey SECRET_KEY;

    public Server(ServerOptions options, SecretKey secret) {
        this.options = options;
        this.storage = new Storage(new StorageOptions(Paths.get(options.storageRoot)));
        this.peers = new ConcurrentHashMap<>();
        this.SECRET_KEY = secret;
    }

    public void start() throws Exception {
        log.info("Server starting on {}", options.listenAddr);

        try {
            this.options.transport.listenAndAccept();
        } catch (Exception e) {
            log.error("Failed to bind transport on {}", options.listenAddr, e);

            throw e;
        }

        this.bootstrapNetwork();

        this.running = true;
        BlockingQueue<RPC> queue = this.options.transport.consume();

        while (this.running) {
            try {
                RPC rpc = queue.take();
                this.handleRPC(rpc);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void bootstrapNetwork() {
        for (String addr : this.options.bootstrapNodes) {
            try {
                this.options.transport.dial(addr);
                log.info("Bootstrapped to {}", addr);
            } catch (Exception e) {
                log.warn("Failed to connect to bootstrap node {}", addr, e);
            }
        }
    }

    public void onPeer(Peer peer) {
        String peerId = peer.getId();
        this.peers.put(peerId, peer);

        log.info("New peer connected: {}", peerId);
    }

    public void removePeer(Peer peer) {
        String peerId = peer.getId();
        this.peers.remove(peerId);

        log.info("Peer disconnected: {}", peerId);
    }

    private void handleRPC(RPC rpc) throws Exception {
        log.info("Received RPC from {}: {}", rpc.from, new String(rpc.data));

        String command = new String(rpc.data).trim();
        Peer peer = this.peers.get(rpc.from);

        if (!command.startsWith("STORE") && !command.startsWith("FETCH") && !command.startsWith("STREAM")) {
            log.warn("Unknown RPC command: {}", command);
            return;
        }

        if (peer == null) {
            log.warn("Peer not found for address: {}", rpc.from);
            return;
        }

        if (command.startsWith("STORE")) {
            String[] parts = new String(rpc.data).split(" ", 2);
            if (parts.length < 2) {
                log.warn("Invalid STORE command format");
                return;
            }

            String filename = parts[1].split(" ", 2)[0];
            int headerLength = ("STORE " + filename + " ").getBytes().length;

            byte[] fileData = new byte[rpc.data.length - headerLength];
            System.arraycopy(rpc.data, headerLength, fileData, 0, fileData.length);

            this.store(peer, new ByteArrayInputStream(fileData), rpc.isStream(), filename);
            return;
        }

        if (command.startsWith("FETCH")) {
            this.fetch(peer, command.split(" ")[1]);
            return;
        }

        if (rpc.isStream()) {
            String[] parts = command.split(" ");
            String filename = parts[1];

            try (OutputStream out = this.storage.openStream(filename)) {
                byte[] iv = new byte[Crypto.GCM_IV_LENGTH];
                new SecureRandom().nextBytes(iv);

                out.write(iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, this.SECRET_KEY, new GCMParameterSpec(Crypto.GCM_TAG_LENGTH * 8, iv));

                try (CipherOutputStream cipherOut = new CipherOutputStream(out, cipher)) {
                    byte[] buffer = new byte[8192];
                    long remaining = rpc.streamSize;
                    InputStream in = rpc.stream;

                    log.info("Server handling stream {} ({} bytes)", filename, remaining);

                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int read = in.read(buffer, 0, toRead);

                        if (read == -1) {
                            throw new IOException("stream ended prematurely");
                        }

                        cipherOut.write(buffer, 0, read);
                        remaining -= read;

                        log.info("Received {} bytes, {} bytes remaining", read, remaining);
                    }

                    cipherOut.flush();
                }
            } catch (Exception e) {
                log.error("Error writing stream: {}", e.getMessage(), e);
            } finally {
                if (peer != null && peer instanceof TCPPeer) {
                    try {
                        ((TCPPeer) peer).stopStream();
                    } catch (Exception ex) {
                        log.warn("Failed to stop stream for peer {}: {}", rpc.from, ex.getMessage());
                    }
                }
            }

            return;
        }
    }

    public void store(Peer origin, InputStream data, boolean streamed, String key) throws Exception {
        if (streamed) {
            log.info("Storing streamed data for key {}", key);
        } else {
            log.info("Storing small data for key {}", key);
        }

        try (InputStream encryptedStream = Crypto.encryptStream(data, this.SECRET_KEY)) {
            this.storage.write(key, encryptedStream);
        }

        log.info("Stored data with key: {}", key);

        this.broadcast(key, origin.getId());
    }

    private void broadcast(String key, String originPeerId) {
        this.peers.forEach((peerId, peer) -> {
            if (peerId.equals(originPeerId)) {
                return;
            }

            try (InputStream in = this.storage.read(key)) {
                DataOutputStream out = new DataOutputStream(peer.getSocket().getOutputStream());
                long size = new File(this.storage.getPath(key)).length();

                String filename = key;
                byte[] filenameBytes = filename.getBytes();

                out.writeByte(1);
                out.writeInt(filenameBytes.length);
                out.write(filenameBytes);
                out.writeLong(size);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                out.flush();
                log.info("Broadcasted key {} to peer {}", key, peerId);

            } catch (Exception ex) {
                log.error("Failed to send STORE command to peer {}: {}", peerId, ex.getMessage());
            }
        });
    }

    private void fetch(Peer peer, String key) throws Exception {
        try (InputStream in = this.storage.read(key);
                OutputStream peerOut = peer.getSocket().getOutputStream()) {

            DataOutputStream out = new DataOutputStream(peerOut);
            long size = new File(this.storage.getPath(key)).length();
            byte[] filenameBytes = key.getBytes();

            out.writeByte(1);
            out.writeInt(filenameBytes.length);
            out.write(filenameBytes);
            out.writeLong(size);
            out.flush();

            Crypto.decryptStream(in, peerOut, this.SECRET_KEY);

            out.flush();
            log.info("Sent key {} to peer {}", key, peer.getId());
        } catch (Exception ex) {
            log.error("Failed to fetch key {}: {}", key, ex.getMessage());
        }
    }

    public void stop() {
        this.running = false;

        this.peers.forEach((addr, peer) -> {
            try {
                peer.getSocket().close();
            } catch (IOException e) {
                log.error("Failed to close peer connection: {}", addr, e);
            }
        });

        this.peers.clear();
        log.info("Server stopped");
    }
}
