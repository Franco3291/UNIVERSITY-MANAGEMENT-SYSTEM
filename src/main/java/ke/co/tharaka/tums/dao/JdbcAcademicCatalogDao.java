package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.config.ConnectionFactory;
import ke.co.tharaka.tums.model.AcademicCatalogItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcAcademicCatalogDao implements AcademicCatalogDao {
    private final ConnectionFactory connectionFactory;

    public JdbcAcademicCatalogDao(AppConfig config) {
        this.connectionFactory = new ConnectionFactory(config);
    }

    @Override
    public List<AcademicCatalogItem> findActive(Kind kind, String query) throws SQLException {
        String sql = selectSql(kind);
        String pattern = "%" + query.trim() + "%";
        List<AcademicCatalogItem> items = new ArrayList<AcademicCatalogItem>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(readItem(resultSet));
                }
            }
        }
        return items;
    }

    @Override
    public AcademicCatalogItem create(Kind kind, AcademicCatalogItem item) throws SQLException {
        String sql = insertSql(kind);
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindValues(kind, statement, item);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the new academic record ID.");
                }
                return withId(item, keys.getLong(1));
            }
        }
    }

    @Override
    public boolean update(Kind kind, AcademicCatalogItem item) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql(kind))) {
            bindValues(kind, statement, item);
            statement.setLong(parameterCount(kind), item.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean archive(Kind kind, long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            if (hasActiveChildren(connection, kind, id)) {
                throw new IllegalStateException("Archive active programmes or courses before archiving this record.");
            }
            String sql = "UPDATE " + tableName(kind) + " SET is_active = FALSE WHERE id = ? AND is_active = TRUE";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                return statement.executeUpdate() == 1;
            }
        }
    }

    private static String selectSql(Kind kind) {
        switch (kind) {
            case DEPARTMENT:
                return "SELECT d.id, 0 AS parent_id, '' AS parent_name, d.code, d.name, d.description, "
                        + "0 AS credit_hours FROM departments d WHERE d.is_active = TRUE "
                        + "AND (d.code LIKE ? OR d.name LIKE ?) ORDER BY d.name LIMIT 500";
            case PROGRAMME:
                return "SELECT p.id, p.department_id AS parent_id, d.name AS parent_name, p.code, p.name, "
                        + "p.description, 0 AS credit_hours FROM programmes p "
                        + "JOIN departments d ON d.id = p.department_id "
                        + "WHERE p.is_active = TRUE AND d.is_active = TRUE "
                        + "AND (p.code LIKE ? OR p.name LIKE ?) ORDER BY p.name LIMIT 500";
            case COURSE:
                return "SELECT c.id, c.programme_id AS parent_id, p.name AS parent_name, c.code, c.name, "
                        + "c.description, c.credit_hours FROM courses c "
                        + "JOIN programmes p ON p.id = c.programme_id "
                        + "JOIN departments d ON d.id = p.department_id "
                        + "WHERE c.is_active = TRUE AND p.is_active = TRUE AND d.is_active = TRUE "
                        + "AND (c.code LIKE ? OR c.name LIKE ?) ORDER BY c.name LIMIT 500";
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
    }

    private static String insertSql(Kind kind) {
        switch (kind) {
            case DEPARTMENT:
                return "INSERT INTO departments (code, name, description) VALUES (?, ?, ?)";
            case PROGRAMME:
                return "INSERT INTO programmes (department_id, code, name, description) VALUES (?, ?, ?, ?)";
            case COURSE:
                return "INSERT INTO courses (programme_id, code, name, description, credit_hours) "
                        + "VALUES (?, ?, ?, ?, ?)";
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
    }

    private static String updateSql(Kind kind) {
        switch (kind) {
            case DEPARTMENT:
                return "UPDATE departments SET code = ?, name = ?, description = ? "
                        + "WHERE id = ? AND is_active = TRUE";
            case PROGRAMME:
                return "UPDATE programmes SET department_id = ?, code = ?, name = ?, description = ? "
                        + "WHERE id = ? AND is_active = TRUE";
            case COURSE:
                return "UPDATE courses SET programme_id = ?, code = ?, name = ?, description = ?, credit_hours = ? "
                        + "WHERE id = ? AND is_active = TRUE";
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
    }

    private static void bindValues(
            Kind kind,
            PreparedStatement statement,
            AcademicCatalogItem item) throws SQLException {
        int index = 1;
        if (kind != Kind.DEPARTMENT) {
            statement.setLong(index++, item.getParentId());
        }
        statement.setString(index++, item.getCode());
        statement.setString(index++, item.getName());
        statement.setString(index++, item.getDescription());
        if (kind == Kind.COURSE) {
            statement.setInt(index, item.getCreditHours());
        }
    }

    private static int parameterCount(Kind kind) {
        switch (kind) {
            case DEPARTMENT:
                return 4;
            case PROGRAMME:
                return 5;
            case COURSE:
                return 6;
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
    }

    private static boolean hasActiveChildren(Connection connection, Kind kind, long id) throws SQLException {
        String sql;
        switch (kind) {
            case DEPARTMENT:
                sql = "SELECT EXISTS(SELECT 1 FROM programmes WHERE department_id = ? AND is_active = TRUE)";
                break;
            case PROGRAMME:
                sql = "SELECT EXISTS(SELECT 1 FROM courses WHERE programme_id = ? AND is_active = TRUE)";
                break;
            case COURSE:
                return false;
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private static AcademicCatalogItem readItem(ResultSet resultSet) throws SQLException {
        return new AcademicCatalogItem(
                resultSet.getLong("id"),
                resultSet.getLong("parent_id"),
                resultSet.getString("parent_name"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("credit_hours"));
    }

    private static AcademicCatalogItem withId(AcademicCatalogItem item, long id) {
        return new AcademicCatalogItem(id, item.getParentId(), item.getParentName(),
                item.getCode(), item.getName(), item.getDescription(), item.getCreditHours());
    }

    private static String tableName(Kind kind) {
        switch (kind) {
            case DEPARTMENT:
                return "departments";
            case PROGRAMME:
                return "programmes";
            case COURSE:
                return "courses";
            default:
                throw new IllegalArgumentException("Unsupported academic catalog type.");
        }
    }
}
