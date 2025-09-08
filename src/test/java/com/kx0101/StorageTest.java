package com.kx0101;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    private Path tempDir;
    private String rootDir;

    @BeforeEach
    void setup() throws IOException {
        rootDir = "liakosnetwork";
        tempDir = Paths.get("storage-test");
        Files.createDirectory(tempDir);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (!Files.exists(tempDir)) {
            return;
        }

        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        throw new RuntimeException("faile to delete: " + ex);
                    }
                });
    }

    @Test
    void testWriteStreamCreatesFileAndWritesData() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        Storage storage = new Storage(options);

        String key = "key";
        String expectedHashedKey = "2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683";
        String expectedHashedDir = this.rootDir
                + "/2c70e/12b7a/0646f/92279/f427c/7b38e/7334d/8e538/9cff1/67a1d/c30e7/3f826/b683";

        byte[] data = "pame ligo re magkes".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        // Act
        storage.writeStream(key, in);

        // Assert
        Path expectedDir = tempDir.resolve(expectedHashedDir);
        Path expectedFile = expectedDir.resolve(expectedHashedKey);

        assertTrue(Files.exists(expectedDir));
        assertTrue(Files.isDirectory(expectedDir));

        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.isRegularFile(expectedFile));

        byte[] fileContent = Files.readAllBytes(expectedFile);
        assertArrayEquals(data, fileContent);
    }

    @Test
    void testReadStream() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        Storage storage = new Storage(options);

        String key = "key";
        byte[] data = "pame ligo re magkes".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        // Act
        storage.writeStream(key, in);

        // Assert
        InputStream readStream = storage.readStream(key);
        byte[] readData = readStream.readAllBytes();

        readStream.close();

        assertArrayEquals(data, readData, "the read content should match the written content");
    }

    @Test
    void testDeleteFile() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        Storage storage = new Storage(options);

        String key = "key";
        String expectedHashedKey = "2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683";
        String expectedHashedDir = this.rootDir
                + "/2c70e/12b7a/0646f/92279/f427c/7b38e/7334d/8e538/9cff1/67a1d/c30e7/3f826/b683";

        byte[] data = "pame ligo re magkes".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        // Act
        storage.writeStream(key, in);

        Path expectedDir = tempDir.resolve(expectedHashedDir);
        Path expectedFile = expectedDir.resolve(expectedHashedKey);

        assertTrue(Files.exists(expectedDir));
        assertTrue(Files.isDirectory(expectedDir));

        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.isRegularFile(expectedFile));

        byte[] fileContent = Files.readAllBytes(expectedFile);
        assertArrayEquals(data, fileContent);

        // Assert
        storage.delete(key);

        assertFalse(Files.exists(expectedDir));
        assertFalse(Files.isDirectory(expectedDir));

        assertFalse(Files.exists(expectedFile));
        assertFalse(Files.isRegularFile(expectedFile));
    }

    @Test
    void testIfFileExists() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        Storage storage = new Storage(options);

        String key = "key";
        byte[] data = "pame ligo re magkes".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        // Act
        storage.writeStream(key, in);
        boolean exists = storage.exists(key);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testIfFileDoesntExist() throws IOException {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        Storage storage = new Storage(options);

        String key = "key";

        // Act
        boolean exists = storage.exists(key);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testHashPathTransformFunc() {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir, rootDir);
        options.pathTransformFunc = options::hashPathTransformFunc;

        String key = "key";

        // Act
        PathKey hashed = options.pathTransformFunc.apply(key);
        String expectedHashed = "2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683";
        assertEquals(hashed.fileName, expectedHashed);
    }
}
