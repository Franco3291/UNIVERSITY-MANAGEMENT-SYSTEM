package ke.co.tharaka.tums.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {
    @Test
    public void verifiesTheOriginalPasswordAndRejectsAnother() {
        PasswordHasher hasher = new PasswordHasher();
        PasswordHasher.Credential credential = hasher.hash("correct horse battery staple".toCharArray());

        assertTrue(hasher.verify("correct horse battery staple".toCharArray(),
                credential.getHash(), credential.getSalt(), credential.getIterations()));
        assertFalse(hasher.verify("incorrect password".toCharArray(),
                credential.getHash(), credential.getSalt(), credential.getIterations()));
    }

    @Test
    public void rejectsMalformedStoredCredentials() {
        assertFalse(new PasswordHasher().verify(
                "password".toCharArray(), new byte[8], new byte[16], 310000));
    }
}
