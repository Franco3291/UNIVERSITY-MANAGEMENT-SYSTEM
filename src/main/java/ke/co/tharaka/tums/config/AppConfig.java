package ke.co.tharaka.tums.config;

import java.util.Map;

public final class AppConfig {
    private static final String DEFAULT_DATABASE_URL =
            "jdbc:mysql://localhost:3306/tums?serverTimezone=UTC&useSSL=false";

    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;

    private AppConfig(String databaseUrl, String databaseUser, String databasePassword) {
        this.databaseUrl = databaseUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
    }

    public static AppConfig fromEnvironment(Map<String, String> environment) {
        String databaseUrl = valueOrDefault(environment.get("TUMS_DB_URL"), DEFAULT_DATABASE_URL);
        String databaseUser = environment.get("TUMS_DB_USER");
        String databasePassword = environment.get("TUMS_DB_PASSWORD");

        if (!databaseUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("TUMS_DB_URL must be a MySQL JDBC URL.");
        }
        if (databaseUser == null || databaseUser.trim().isEmpty()) {
            throw new IllegalArgumentException("Set TUMS_DB_USER to the MySQL account used by TUMS.");
        }
        if (databasePassword == null || databasePassword.isEmpty()) {
            throw new IllegalArgumentException("Set TUMS_DB_PASSWORD to the MySQL account password.");
        }

        return new AppConfig(databaseUrl, databaseUser.trim(), databasePassword);
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
