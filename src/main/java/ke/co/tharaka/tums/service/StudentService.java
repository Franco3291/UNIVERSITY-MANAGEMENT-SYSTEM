package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.StudentDao;
import ke.co.tharaka.tums.model.AppModule;
import ke.co.tharaka.tums.model.Student;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class StudentService {
    private static final Pattern ADMISSION_NUMBER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/-]*");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("[+0-9() .-]*");
    private static final String[] GENDERS = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"};

    private final StudentDao studentDao;

    public StudentService(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    public List<Student> search(UserAccount user, String query) throws SQLException {
        requireStudentAccess(user);
        return studentDao.searchActive(query == null ? "" : query.trim());
    }

    public Student create(UserAccount user, Student student) throws SQLException {
        requireAdministrator(user);
        return studentDao.create(validate(student, false));
    }

    public void update(UserAccount user, Student student) throws SQLException {
        requireAdministrator(user);
        Student validated = validate(student, true);
        if (!studentDao.update(validated)) {
            throw new IllegalStateException("The student record no longer exists or has already been archived.");
        }
    }

    public void archive(UserAccount user, long id) throws SQLException {
        requireAdministrator(user);
        if (id <= 0) {
            throw new IllegalArgumentException("Select a student record before archiving it.");
        }
        if (!studentDao.archive(id)) {
            throw new IllegalStateException("The student record no longer exists or has already been archived.");
        }
    }

    private static Student validate(Student student, boolean requireId) {
        if (student == null) {
            throw new IllegalArgumentException("Student details are required.");
        }
        if ((requireId && student.getId() <= 0) || (!requireId && student.getId() != 0)) {
            throw new IllegalArgumentException("The student record is not in a valid state.");
        }

        String admission = required(student.getAdmissionNumber(), "Admission number", 32)
                .toUpperCase(Locale.ROOT);
        if (!ADMISSION_NUMBER.matcher(admission).matches()) {
            throw new IllegalArgumentException(
                    "Admission number may contain letters, numbers, slashes, and hyphens.");
        }
        String fullName = required(student.getFullName(), "Full name", 150);
        String gender = required(student.getGender(), "Gender", 24).toUpperCase(Locale.ROOT);
        if (!contains(GENDERS, gender)) {
            throw new IllegalArgumentException("Select a valid gender.");
        }
        String phone = optional(student.getPhone(), "Phone number", 30);
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number may contain digits, spaces, '+', '-', '(', and ')'.");
        }
        String email = optional(student.getEmail(), "Email address", 254);
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        String programme = required(student.getProgramme(), "Programme", 120);
        String department = required(student.getDepartment(), "Department", 120);
        String academicLevel = required(student.getAcademicLevel(), "Academic level", 32);

        return new Student(student.getId(), admission, fullName, gender, phone, email,
                programme, department, academicLevel);
    }

    private static String required(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static String optional(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static void requireStudentAccess(UserAccount user) {
        if (user == null || !AppModule.STUDENTS.isAllowedFor(user.getRole())) {
            throw new SecurityException("Your account is not authorized to access student records.");
        }
    }

    private static void requireAdministrator(UserAccount user) {
        if (user == null || !AppModule.STUDENTS.isAllowedFor(user.getRole())
                || user.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Only administrators may create, edit, or archive student records.");
        }
    }

    private static boolean contains(String[] values, String value) {
        for (String candidate : values) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
