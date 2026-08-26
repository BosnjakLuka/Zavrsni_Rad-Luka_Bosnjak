package hr.lukabosnjak.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** PBKDF2 password storage using a versioned, self-contained encoded value. */
public final class PasswordHasher {
    static final String ALGORITHM_ID = "pbkdf2-sha256";
    static final int ITERATIONS = 600_000;
    static final int SALT_BYTES = 16;
    static final int KEY_BITS = 256;

    private static final String JDK_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int MAX_STORED_ITERATIONS = 2_000_000;

    private final SecureRandom secureRandom;

    public PasswordHasher() {
        this(new SecureRandom());
    }

    PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String hash(char[] password) {
        requirePassword(password);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS, KEY_BITS);
        try {
            return ALGORITHM_ID + "$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(derived);
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    public boolean verify(char[] password, String encodedHash) {
        if (password == null || password.length == 0 || encodedHash == null) {
            return false;
        }
        try {
            String[] parts = encodedHash.split("\\$", -1);
            if (parts.length != 4 || !ALGORITHM_ID.equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0 || iterations > MAX_STORED_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length < SALT_BYTES || expected.length == 0) {
                return false;
            }
            byte[] actual = derive(password, salt, iterations, expected.length * Byte.SIZE);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
                Arrays.fill(expected, (byte) 0);
                Arrays.fill(salt, (byte) 0);
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int keyBits) {
        char[] passwordCopy = Arrays.copyOf(password, password.length);
        PBEKeySpec keySpec = new PBEKeySpec(passwordCopy, salt, iterations, keyBits);
        Arrays.fill(passwordCopy, '\0');
        try {
            return SecretKeyFactory.getInstance(JDK_ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    private void requirePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
