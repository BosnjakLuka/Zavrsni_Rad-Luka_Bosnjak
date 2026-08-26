package hr.lukabosnjak.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void createsDifferentSaltedHashesAndVerifiesOnlyTheCorrectPassword() {
        char[] password = "SoftwareTest1".toCharArray();

        String first = hasher.hash(password);
        String second = hasher.hash(password);

        assertNotEquals(first, second);
        assertTrue(hasher.verify(password, first));
        assertTrue(hasher.verify(password, second));
        assertFalse(hasher.verify("WrongPassword1".toCharArray(), first));
    }

    @Test
    void rejectsMalformedOrUnsupportedHashValues() {
        assertFalse(hasher.verify("SoftwareTest1".toCharArray(), null));
        assertFalse(hasher.verify("SoftwareTest1".toCharArray(), "not-a-password-hash"));
        assertFalse(hasher.verify("SoftwareTest1".toCharArray(),
                "other$600000$YWJjZGVmZ2hpamtsbW5vcA==$YWJjZA=="));
        assertFalse(hasher.verify("SoftwareTest1".toCharArray(),
                "pbkdf2-sha256$999999999$YWJjZGVmZ2hpamtsbW5vcA==$YWJjZA=="));
    }
}
