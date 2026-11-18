package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.StudentDao;
import ke.co.tharaka.tums.model.Student;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StudentServiceTest {
    @Test
    public void normalizesAndCreatesStudentForAdministrator() throws SQLException {
        InMemoryStudentDao dao = new InMemoryStudentDao();
        StudentService service = new StudentService(dao);

        Student created = service.create(administrator(), new Student(
                0, " thu/001 ", "  Amina Njeri  ", "female", " ", " ",
                "  Bachelor of Science  ", "  Computing  ", " Year 1 "));

        assertEquals(1L, created.getId());
        assertEquals("THU/001", created.getAdmissionNumber());
        assertEquals("Amina Njeri", created.getFullName());
        assertEquals("FEMALE", created.getGender());
        assertEquals("", created.getPhone());
        assertEquals("Bachelor of Science", created.getProgramme());
        assertEquals("Computing", created.getDepartment());
        assertEquals("Year 1", created.getAcademicLevel());
    }

    @Test
    public void lecturerCanSearchButCannotCreateOrArchive() throws SQLException {
        StudentService service = new StudentService(new InMemoryStudentDao());
        UserAccount lecturer = user(2L, "lecturer", UserRole.LECTURER);

        assertTrue(service.search(lecturer, "").isEmpty());
        expectSecurityException(() -> service.create(lecturer, validStudent()));
        expectSecurityException(() -> service.archive(lecturer, 1L));
    }

    @Test
    public void rejectsInvalidEmailBeforeWriting() throws SQLException {
        InMemoryStudentDao dao = new InMemoryStudentDao();
        StudentService service = new StudentService(dao);
        Student invalid = new Student(
                0, "THU/001", "Amina Njeri", "FEMALE", "", "not-an-email",
                "Bachelor of Science", "Computing", "Year 1");

        try {
            service.create(administrator(), invalid);
            fail("Invalid email should be rejected.");
        } catch (IllegalArgumentException expected) {
            assertEquals("Enter a valid email address.", expected.getMessage());
        }
        assertTrue(dao.saved.isEmpty());
    }

    private static Student validStudent() {
        return new Student(0, "THU/001", "Amina Njeri", "FEMALE", "", "",
                "Bachelor of Science", "Computing", "Year 1");
    }

    private static UserAccount administrator() {
        return user(1L, "admin", UserRole.ADMINISTRATOR);
    }

    private static UserAccount user(long id, String username, UserRole role) {
        return new UserAccount(id, username, role);
    }

    private static void expectSecurityException(CheckedAction action) throws SQLException {
        try {
            action.run();
            fail("The action should be forbidden for this role.");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("Only administrators"));
        }
    }

    private interface CheckedAction {
        void run() throws SQLException;
    }

    private static final class InMemoryStudentDao implements StudentDao {
        private final List<Student> saved = new ArrayList<Student>();
        private long nextId = 1;

        @Override
        public List<Student> searchActive(String query) {
            return Collections.unmodifiableList(saved);
        }

        @Override
        public Student create(Student student) {
            Student created = new Student(nextId++, student.getAdmissionNumber(), student.getFullName(),
                    student.getGender(), student.getPhone(), student.getEmail(), student.getProgramme(),
                    student.getDepartment(), student.getAcademicLevel());
            saved.add(created);
            return created;
        }

        @Override
        public boolean update(Student student) {
            return false;
        }

        @Override
        public boolean archive(long id) {
            return false;
        }
    }
}
