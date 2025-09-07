package com.kx0101;

import java.util.function.Function;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StorageOptions {
    private final Path baseDir;
    public Function<String, PathKey> pathTransformFunc;

    public StorageOptions(Function<String, PathKey> pathTransformFunc, Path baseDir) {
        this.baseDir = baseDir;
        this.pathTransformFunc = pathTransformFunc;
    }

    public StorageOptions(Path baseDir) {
        this.baseDir = baseDir;
        this.pathTransformFunc = this::hashPathTransformFunc;
    }

    public PathKey hashPathTransformFunc(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        String hashed = hash(key);

        int blockSize = 5;
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < hashed.length(); i += blockSize) {
            int end = Math.min(i + blockSize, hashed.length());
            pathBuilder.append(hashed, i, end);

            if (end < hashed.length()) {
                pathBuilder.append("/");
            }
        }

        Path fullPath = baseDir.resolve(pathBuilder.toString());
        return new PathKey(fullPath.toString(), hashed);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 not supported", ex);
        }
    }
}
