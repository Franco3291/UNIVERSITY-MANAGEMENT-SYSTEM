package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.config.ConnectionFactory;
import ke.co.tharaka.tums.model.UserRole;
import ke.co.tharaka.tums.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;

public final class AdminBootstrapService {
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final ConnectionFactory connectionFactory;
    private final PasswordHasher passwordHasher;

    public AdminBootstrapService(AppConfig config) {
        this.connectionFactory = new ConnectionFactory(config);
        this.passwordHasher = new PasswordHasher();
    }

    public void createFirstAdministrator(String username, char[] password) throws SQLException {
        try {
            String normalizedUsername = normalizeAndValidateUsername(username);
            validatePassword(password);
            PasswordHasher.Credential credential = passwordHasher.hash(password);

            try (Connection connection = connectionFactory.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    lockSetupRow(connection);
                    ensureNoExistingUsers(connection);
                    insertAdministrator(connection, normalizedUsername, credential);
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private static void lockSetupRow(Connection connection) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT id FROM app_setup_lock WHERE id = 1 FOR UPDATE");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("The app_setup_lock row is missing. Run database/schema.sql first.");
            }
        }
    }

    private static void ensureNoExistingUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            resultSet.next();
            if (resultSet.getLong(1) != 0) {
                throw new IllegalStateException("An administrator can only be bootstrapped before any users exist.");
            }
        }
    }

    private static void insertAdministrator(
            Connection connection,
            String username,
            PasswordHasher.Credential credential) throws SQLException {
        String sql = "INSERT INTO users "
                + "(username, password_hash, password_salt, password_iterations, role) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setBytes(2, credential.getHash());
            statement.setBytes(3, credential.getSalt());
            statement.setInt(4, credential.getIterations());
            statement.setString(5, UserRole.ADMINISTRATOR.name());
            statement.executeUpdate();
        }
    }

    private static String normalizeAndValidateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username must be between 3 and 64 characters.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3 || normalized.length() > 64
                || !normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Username must be 3-64 characters and contain only letters, numbers, '.', '_' or '-'.");
        }
        return normalized;
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Administrator password must contain at least 12 characters.");
        }
    }
}
