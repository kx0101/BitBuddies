package com.kx0101;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Crypto {
    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_IV_LENGTH = 16;
    public static final int GCM_TAG_LENGTH = 16;

    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);

        return keyGen.generateKey();
    }

    public static InputStream encryptStream(InputStream in, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        ByteArrayOutputStream ivOut = new ByteArrayOutputStream();

        ivOut.write(iv);

        CipherOutputStream out = new CipherOutputStream(ivOut, cipher);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        out.close();

        return new ByteArrayInputStream(ivOut.toByteArray());
    }

    public static void decryptStream(InputStream in, OutputStream out, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        if (in.read(iv) != GCM_IV_LENGTH) {
            throw new IOException("Failed to read IV");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = cis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();
        }
    }
}
