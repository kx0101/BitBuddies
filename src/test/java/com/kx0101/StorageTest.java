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
        StorageOptions options = new StorageOptions(tempDir);
        Storage storage = new Storage(options);

        String key = "key";
        String expectedHashedKey = "2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683";
        String expectedHashedDir = "2c70e/12b7a/0646f/92279/f427c/7b38e/7334d/8e538/9cff1/67a1d/c30e7/3f826/b683";

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
    void testHashPathTransformFunc() {
        // Arrange
        StorageOptions options = new StorageOptions(tempDir);
        options.pathTransformFunc = options::hashPathTransformFunc;

        String key = "key";

        // Act
        PathKey hashed = options.pathTransformFunc.apply(key);
        String expectedHashed = "2c70e12b7a0646f92279f427c7b38e7334d8e5389cff167a1dc30e73f826b683";
        assertEquals(hashed.fileName, expectedHashed);
    }
}
