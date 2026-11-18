package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.UserDao;
import ke.co.tharaka.tums.model.AppModule;
import ke.co.tharaka.tums.model.StoredUserCredential;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;
import ke.co.tharaka.tums.security.PasswordHasher;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AuthenticationServiceTest {
    @Test
    public void authenticatesMatchingCredentialsAndClearsPassword() throws SQLException {
        char[] originalPassword = "correct horse battery staple".toCharArray();
        PasswordHasher.Credential hashed = new PasswordHasher().hash(originalPassword);
        UserAccount expected = new UserAccount(7L, "admin", UserRole.ADMINISTRATOR);
        AuthenticationService service = new AuthenticationService(
                new InMemoryUserDao(expected, hashed, "admin"));
        char[] suppliedPassword = "correct horse battery staple".toCharArray();

        assertSame(expected, service.authenticate("ADMIN", suppliedPassword));
        assertArrayEquals(new char[suppliedPassword.length], suppliedPassword);
    }

    @Test
    public void rejectsIncorrectPassword() throws SQLException {
        PasswordHasher.Credential hashed = new PasswordHasher().hash("correct horse battery staple".toCharArray());
        UserAccount account = new UserAccount(7L, "admin", UserRole.ADMINISTRATOR);
        AuthenticationService service = new AuthenticationService(
                new InMemoryUserDao(account, hashed, "admin"));

        assertNull(service.authenticate("admin", "incorrect password".toCharArray()));
    }

    @Test
    public void filtersModulesByRole() {
        assertTrue(AppModule.FINANCE.isAllowedFor(UserRole.FINANCE_OFFICER));
        assertFalse(AppModule.FINANCE.isAllowedFor(UserRole.LECTURER));
        assertTrue(AppModule.SYSTEM_ADMINISTRATION.isAllowedFor(UserRole.ADMINISTRATOR));
        assertFalse(AppModule.SYSTEM_ADMINISTRATION.isAllowedFor(UserRole.FINANCE_OFFICER));
        assertEquals("Finance officer", UserRole.FINANCE_OFFICER.getDisplayName());
    }

    private static final class InMemoryUserDao implements UserDao {
        private final StoredUserCredential credential;
        private final String username;

        private InMemoryUserDao(UserAccount account, PasswordHasher.Credential hashed, String username) {
            this.credential = new StoredUserCredential(
                    account, hashed.getHash(), hashed.getSalt(), hashed.getIterations());
            this.username = username;
        }

        @Override
        public Optional<StoredUserCredential> findActiveByUsername(String requestedUsername) {
            return username.equals(requestedUsername) ? Optional.of(credential) : Optional.<StoredUserCredential>empty();
        }
    }
}
