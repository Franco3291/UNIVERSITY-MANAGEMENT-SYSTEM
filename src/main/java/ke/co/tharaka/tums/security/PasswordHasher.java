package ke.co.tharaka.tums.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int ITERATIONS = 310000;
    private static final int MIN_ACCEPTED_ITERATIONS = 100000;
    private static final int MAX_ACCEPTED_ITERATIONS = 2000000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public Credential hash(char[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return new Credential(derive(password, salt, ITERATIONS), salt, ITERATIONS);
    }

    public boolean verify(char[] password, byte[] expectedHash, byte[] salt, int iterations) {
        if (password == null || expectedHash == null || salt == null
                || expectedHash.length != HASH_LENGTH_BITS / 8
                || salt.length != SALT_LENGTH
                || iterations < MIN_ACCEPTED_ITERATIONS
                || iterations > MAX_ACCEPTED_ITERATIONS) {
            return false;
        }
        byte[] actualHash = derive(password, salt, iterations);
        try {
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            Arrays.fill(actualHash, (byte) 0);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required.");
        }
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("The configured password hashing algorithm is unavailable.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public static final class Credential {
        private final byte[] hash;
        private final byte[] salt;
        private final int iterations;

        private Credential(byte[] hash, byte[] salt, int iterations) {
            this.hash = hash.clone();
            this.salt = salt.clone();
            this.iterations = iterations;
            Arrays.fill(hash, (byte) 0);
        }

        public byte[] getHash() {
            return hash.clone();
        }

        public byte[] getSalt() {
            return salt.clone();
        }

        public int getIterations() {
            return iterations;
        }
    }
}
