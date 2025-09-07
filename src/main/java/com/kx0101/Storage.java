package com.kx0101;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Storage {
    private final StorageOptions options;

    public Storage(StorageOptions options) {
        this.options = options;
    }

    /**
     * Reads a file stream from storage
     */
    public InputStream readStream(String key) throws IOException {
        PathKey pathKey = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathKey.pathName);
        Path filePath = parentDir.resolve(pathKey.fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("file not found: " + filePath);
        }

        return Files.newInputStream(filePath);
    }

    /**
     * Writes the InputStream to storage
     */
    public void writeStream(String key, InputStream r) throws IOException {
        PathKey pathKey = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathKey.pathName);
        Files.createDirectories(parentDir);

        Path filePath = parentDir.resolve(pathKey.fileName);

        try (OutputStream output = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int read;

            while ((read = r.read(buffer)) != -1) {
                output.write(buffer, 0, read);

                System.out.printf("written (%d) bytes to disk: %s\n", read, filePath);
            }
        }
    }

    /**
     * Returns the full path string for a given PathKey
     */
    public String getFullPath(PathKey pathKey) {
        return Paths.get(pathKey.pathName, pathKey.fileName).toString();
    }
}
