package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.UserDao;
import ke.co.tharaka.tums.model.StoredUserCredential;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class AuthenticationService {
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UserDao userDao) {
        this.userDao = userDao;
        this.passwordHasher = new PasswordHasher();
    }

    public UserAccount authenticate(String username, char[] password) throws SQLException {
        try {
            if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
                return null;
            }

            Optional<StoredUserCredential> credential = userDao.findActiveByUsername(
                    username.trim().toLowerCase(Locale.ROOT));
            if (!credential.isPresent()) {
                return null;
            }

            StoredUserCredential stored = credential.get();
            return passwordHasher.verify(password, stored.getPasswordHash(),
                    stored.getPasswordSalt(), stored.getPasswordIterations())
                    ? stored.getAccount()
                    : null;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
