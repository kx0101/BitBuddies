package com.kx0101;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryStream;

public class Storage {
    private final StorageOptions options;
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    public Storage(StorageOptions options) {
        this.options = options;
    }

    /**
     * Checks if a file exists in the storage
     */
    public boolean exists(String key) throws IOException {
        PathKey pathKey = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathKey.pathName);
        Path filePath = parentDir.resolve(pathKey.fileName);

        return Files.exists(filePath);
    }

    /**
     * Deletes a file from the storage
     */
    public void delete(String key) throws IOException {
        PathKey pathKey = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathKey.pathName);
        Path filePath = parentDir.resolve(pathKey.fileName);

        Files.delete(filePath);

        Path parent = filePath.getParent();

        while (!parent.equals(this.options.baseDir) && Files.isDirectory(parent) && isDirEmpty(parent)) {
            Files.delete(parent);
            parent = parent.getParent();
        }
    }

    public InputStream read(String key) throws IOException {
        return this.readStream(key);
    }

    public void write(String key, InputStream r) throws IOException {
        this.writeStream(key, r);
    }

    /**
     * Returns the full path string for a given PathKey
     */
    public String getFullPath(PathKey pathKey) {
        return Paths.get(pathKey.pathName, pathKey.fileName).toString();
    }

    /**
     * Reads a file stream from storage
     */
    private InputStream readStream(String key) throws IOException {
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
    private void writeStream(String key, InputStream r) throws IOException {
        PathKey pathKey = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathKey.pathName);
        Files.createDirectories(parentDir);

        Path filePath = parentDir.resolve(pathKey.fileName);

        try (OutputStream output = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int read;

            while ((read = r.read(buffer)) != -1) {
                output.write(buffer, 0, read);

                log.debug("written ({} bytes) to disk: {}", read, filePath);
            }
        }
    }

    private boolean isDirEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            return !entries.iterator().hasNext();
        }
    }
}
