package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.config.ConnectionFactory;
import ke.co.tharaka.tums.model.StoredUserCredential;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public final class JdbcUserDao implements UserDao {
    private static final String FIND_ACTIVE_USER =
            "SELECT id, username, password_hash, password_salt, password_iterations, role "
                    + "FROM users WHERE username = ? AND is_active = TRUE";

    private final ConnectionFactory connectionFactory;

    public JdbcUserDao(AppConfig config) {
        this.connectionFactory = new ConnectionFactory(config);
    }

    @Override
    public Optional<StoredUserCredential> findActiveByUsername(String username) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_USER)) {
            statement.setString(1, normalizeUsername(username));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                UserAccount account = new UserAccount(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        UserRole.fromDatabase(resultSet.getString("role")));
                StoredUserCredential credential = new StoredUserCredential(
                        account,
                        resultSet.getBytes("password_hash"),
                        resultSet.getBytes("password_salt"),
                        resultSet.getInt("password_iterations"));
                return Optional.of(credential);
            }
        }
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
