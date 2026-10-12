package com.pixelpioneers.library.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Application Layer: password security.
 * Passwords are never stored as typed. Each one is salted with random bytes
 * and hashed with PBKDF2-WithHmacSHA256 (a standard algorithm built into the
 * JDK). The stored form is "saltHex:hashHex" (97 characters, which fits the
 * VARCHAR(100) password column in the Phase 3 ERD).
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordUtil() { }

    /** Hashes a plain password with a fresh random salt. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return toHex(salt) + ":" + toHex(pbkdf2(password, salt));
    }

    /** Checks a typed password against a stored "salt:hash" value. */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null || !stored.contains(":")) {
            return false;
        }
        String[] parts = stored.split(":", 2);
        byte[] salt;
        byte[] expected;
        try {
            salt = fromHex(parts[0]);
            expected = fromHex(parts[1]);
        } catch (IllegalArgumentException e) {
            return false; // stored value is not in the expected format
        }
        byte[] actual = pbkdf2(password, salt);
        // constant-time comparison so timing doesn't leak how much matched
        int diff = expected.length ^ actual.length;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            diff |= expected[i] ^ actual[i];
        }
        return diff == 0;
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available on this Java installation.", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
