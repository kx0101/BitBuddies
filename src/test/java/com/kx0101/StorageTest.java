package com.kx0101;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    private Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        tempDir = Files.createTempDirectory("storage-test");
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void testWriteStreamCreatesFileAndWritesData() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions();
        options.pathTransformFunc = key -> tempDir.resolve(key).toString();

        Storage storage = new Storage(options);

        String key = "key";
        byte[] data = "pame ligo re magkes".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        // Act
        storage.writeStream(key, in);

        // Assert
        Path expectedDir = tempDir.resolve(key);
        Path expectedFile = expectedDir.resolve("someFilename");

        assertTrue(Files.exists(expectedDir));
        assertTrue(Files.isDirectory(expectedDir));

        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.isRegularFile(expectedFile));

        byte[] fileContent = Files.readAllBytes(expectedFile);
        assertArrayEquals(data, fileContent);
    }
}
