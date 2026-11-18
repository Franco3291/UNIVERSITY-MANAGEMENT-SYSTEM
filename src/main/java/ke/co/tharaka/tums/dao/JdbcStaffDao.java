package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.config.ConnectionFactory;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.StaffMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcStaffDao implements StaffDao {
    private final ConnectionFactory connectionFactory;

    public JdbcStaffDao(AppConfig config) {
        this.connectionFactory = new ConnectionFactory(config);
    }

    @Override
    public List<StaffMember> searchActive(String query) throws SQLException {
        String sql = "SELECT s.id, s.staff_number, s.full_name, s.qualification, s.department_id, "
                + "d.name AS department_name, s.phone, s.email, s.responsibility "
                + "FROM university_staff s JOIN departments d ON d.id = s.department_id "
                + "WHERE s.is_active = TRUE AND d.is_active = TRUE "
                + "AND (s.staff_number LIKE ? OR s.full_name LIKE ? OR s.email LIKE ? "
                + "OR s.qualification LIKE ? OR d.name LIKE ? OR s.responsibility LIKE ?) "
                + "ORDER BY s.full_name, s.staff_number LIMIT 500";
        String pattern = "%" + query.trim() + "%";
        List<StaffMember> members = new ArrayList<StaffMember>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 6; index++) {
                statement.setString(index, pattern);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(readMember(resultSet));
                }
            }
        }
        return members;
    }

    @Override
    public List<AcademicCatalogItem> findActiveDepartments() throws SQLException {
        String sql = "SELECT id, code, name FROM departments WHERE is_active = TRUE ORDER BY name";
        List<AcademicCatalogItem> departments = new ArrayList<AcademicCatalogItem>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                departments.add(new AcademicCatalogItem(
                        resultSet.getLong("id"), 0, "", resultSet.getString("code"),
                        resultSet.getString("name"), "", 0));
            }
        }
        return departments;
    }

    @Override
    public StaffMember create(StaffMember member) throws SQLException {
        String sql = "INSERT INTO university_staff "
                + "(staff_number, full_name, qualification, department_id, phone, email, responsibility) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindMember(statement, member);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the new staff record ID.");
                }
                return withId(member, keys.getLong(1));
            }
        }
    }

    @Override
    public boolean update(StaffMember member) throws SQLException {
        String sql = "UPDATE university_staff SET staff_number = ?, full_name = ?, qualification = ?, "
                + "department_id = ?, phone = ?, email = ?, responsibility = ? "
                + "WHERE id = ? AND is_active = TRUE";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMember(statement, member);
            statement.setLong(8, member.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean archive(long id) throws SQLException {
        String sql = "UPDATE university_staff SET is_active = FALSE WHERE id = ? AND is_active = TRUE";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static void bindMember(PreparedStatement statement, StaffMember member) throws SQLException {
        statement.setString(1, member.getStaffNumber());
        statement.setString(2, member.getFullName());
        statement.setString(3, member.getQualification());
        statement.setLong(4, member.getDepartmentId());
        statement.setString(5, emptyToNull(member.getPhone()));
        statement.setString(6, emptyToNull(member.getEmail()));
        statement.setString(7, member.getResponsibility());
    }

    private static StaffMember readMember(ResultSet resultSet) throws SQLException {
        return new StaffMember(
                resultSet.getLong("id"),
                resultSet.getString("staff_number"),
                resultSet.getString("full_name"),
                resultSet.getString("qualification"),
                resultSet.getLong("department_id"),
                resultSet.getString("department_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("responsibility"));
    }

    private static StaffMember withId(StaffMember member, long id) {
        return new StaffMember(
                id,
                member.getStaffNumber(),
                member.getFullName(),
                member.getQualification(),
                member.getDepartmentId(),
                member.getDepartmentName(),
                member.getPhone(),
                member.getEmail(),
                member.getResponsibility());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
