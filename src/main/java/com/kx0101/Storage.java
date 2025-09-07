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

    public void writeStream(String key, InputStream r) throws IOException {
        String pathName = this.options.pathTransformFunc.apply(key);

        Path parentDir = Paths.get(pathName);
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        String fileName = "someFilename";
        Path filePath = parentDir.resolve(fileName);

        try (OutputStream output = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int read;

            while ((read = r.read(buffer)) != -1) {
                output.write(buffer, 0, read);

                System.out.printf("written (%d) bytes to disk: %s\n", read, filePath);
            }
        }
    }
}
