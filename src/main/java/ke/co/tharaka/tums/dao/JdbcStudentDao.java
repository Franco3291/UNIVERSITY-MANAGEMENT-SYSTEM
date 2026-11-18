package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.config.ConnectionFactory;
import ke.co.tharaka.tums.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcStudentDao implements StudentDao {
    private static final String STUDENT_COLUMNS =
            "id, admission_number, full_name, gender, phone, email, programme, department, academic_level";
    private static final String INSERT_STUDENT =
            "INSERT INTO students (admission_number, full_name, gender, phone, email, programme, department, "
                    + "academic_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_STUDENT =
            "UPDATE students SET admission_number = ?, full_name = ?, gender = ?, phone = ?, email = ?, "
                    + "programme = ?, department = ?, academic_level = ? WHERE id = ? AND is_active = TRUE";

    private final ConnectionFactory connectionFactory;

    public JdbcStudentDao(AppConfig config) {
        this.connectionFactory = new ConnectionFactory(config);
    }

    @Override
    public List<Student> searchActive(String query) throws SQLException {
        String sql = "SELECT " + STUDENT_COLUMNS
                + " FROM students WHERE is_active = TRUE AND "
                + "(admission_number LIKE ? OR full_name LIKE ? OR email LIKE ? OR programme LIKE ? "
                + "OR department LIKE ?) ORDER BY full_name, admission_number LIMIT 500";
        String pattern = "%" + query.trim() + "%";
        List<Student> students = new ArrayList<Student>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 5; index++) {
                statement.setString(index, pattern);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(readStudent(resultSet));
                }
            }
        }
        return students;
    }

    @Override
    public Student create(Student student) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_STUDENT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindStudent(statement, student);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the new student record ID.");
                }
                return withId(student, keys.getLong(1));
            }
        }
    }

    @Override
    public boolean update(Student student) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STUDENT)) {
            bindStudent(statement, student);
            statement.setLong(9, student.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean archive(long id) throws SQLException {
        String sql = "UPDATE students SET is_active = FALSE WHERE id = ? AND is_active = TRUE";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static Student readStudent(ResultSet resultSet) throws SQLException {
        return new Student(
                resultSet.getLong("id"),
                resultSet.getString("admission_number"),
                resultSet.getString("full_name"),
                resultSet.getString("gender"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("programme"),
                resultSet.getString("department"),
                resultSet.getString("academic_level"));
    }

    private static void bindStudent(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getAdmissionNumber());
        statement.setString(2, student.getFullName());
        statement.setString(3, student.getGender());
        statement.setString(4, emptyToNull(student.getPhone()));
        statement.setString(5, emptyToNull(student.getEmail()));
        statement.setString(6, student.getProgramme());
        statement.setString(7, student.getDepartment());
        statement.setString(8, student.getAcademicLevel());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static Student withId(Student student, long id) {
        return new Student(
                id,
                student.getAdmissionNumber(),
                student.getFullName(),
                student.getGender(),
                student.getPhone(),
                student.getEmail(),
                student.getProgramme(),
                student.getDepartment(),
                student.getAcademicLevel());
    }
}
