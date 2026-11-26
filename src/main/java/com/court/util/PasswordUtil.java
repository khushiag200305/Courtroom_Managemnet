package com.court.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing using only core Java (SHA-256 with a random salt).
 * <p>
 * Stored format: {@code base64(salt) + "$" + base64(sha256(salt + password))}.
 * The plain password is never saved anywhere.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return encode(salt) + "$" + encode(sha256(salt, password));
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null || !stored.contains("$")) {
            return false;
        }
        String[] parts = stored.split("\\$", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        // isEqual compares in constant time, which avoids timing attacks
        return MessageDigest.isEqual(expected, sha256(salt, password));
    }

    private static byte[] sha256(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // every Java installation must support SHA-256, so this never happens
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Run this class to generate a hash for seed.sql: java PasswordUtil mypassword */
    public static void main(String[] args) {
        for (String pw : args) {
            System.out.println(pw + " -> " + hash(pw));
        }
    }
}
