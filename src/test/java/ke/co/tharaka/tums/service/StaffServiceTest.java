package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.StaffDao;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.StaffMember;
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

public class StaffServiceTest {
    @Test
    public void normalizesStaffMemberForAdministrator() throws SQLException {
        InMemoryStaffDao dao = new InMemoryStaffDao();
        StaffService service = new StaffService(dao);

        StaffMember created = service.create(administrator(), new StaffMember(
                0, " thu/001 ", "  Amina Njeri ", "  MSc Computer Science ",
                3, " Computing ", " ", " ", "  Lecturer "));

        assertEquals(1L, created.getId());
        assertEquals("THU/001", created.getStaffNumber());
        assertEquals("Amina Njeri", created.getFullName());
        assertEquals("MSc Computer Science", created.getQualification());
        assertEquals(3L, created.getDepartmentId());
        assertEquals("Lecturer", created.getResponsibility());
    }

    @Test
    public void rejectsInvalidEmailWithoutWriting() throws SQLException {
        InMemoryStaffDao dao = new InMemoryStaffDao();
        StaffService service = new StaffService(dao);

        try {
            service.create(administrator(), new StaffMember(
                    0, "THU/001", "Amina Njeri", "MSc Computer Science",
                    3, "Computing", "", "invalid-email", "Lecturer"));
            fail("Invalid email should be rejected.");
        } catch (IllegalArgumentException expected) {
            assertEquals("Enter a valid email address.", expected.getMessage());
        }
        assertTrue(dao.saved.isEmpty());
    }

    @Test
    public void rejectsDepartmentThatIsNotActive() throws SQLException {
        InMemoryStaffDao dao = new InMemoryStaffDao();
        StaffService service = new StaffService(dao);
        StaffMember member = new StaffMember(
                0, "THU/001", "Amina Njeri", "MSc Computer Science",
                99, "Archived department", "", "", "Lecturer");

        try {
            service.create(administrator(), member);
            fail("An inactive department should be rejected.");
        } catch (IllegalArgumentException expected) {
            assertEquals("Select an active department for this staff member.", expected.getMessage());
        }
        assertTrue(dao.saved.isEmpty());
    }

    @Test
    public void lecturerCanSearchButCannotCreateOrArchive() throws SQLException {
        InMemoryStaffDao dao = new InMemoryStaffDao();
        StaffService service = new StaffService(dao);
        UserAccount lecturer = new UserAccount(2L, "lecturer", UserRole.LECTURER);

        assertTrue(service.search(lecturer, "").isEmpty());
        try {
            service.create(lecturer, new StaffMember(
                    0, "THU/001", "Amina Njeri", "MSc Computer Science",
                    3, "Computing", "", "", "Lecturer"));
            fail("Lecturer must not be able to create staff records.");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("Only administrators"));
        }
        try {
            service.archive(lecturer, 1L);
            fail("Lecturer must not be able to archive staff records.");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("Only administrators"));
        }
    }

    private static UserAccount administrator() {
        return new UserAccount(1L, "admin", UserRole.ADMINISTRATOR);
    }

    private static final class InMemoryStaffDao implements StaffDao {
        private final List<StaffMember> saved = new ArrayList<StaffMember>();

        @Override
        public List<StaffMember> searchActive(String query) {
            return Collections.unmodifiableList(saved);
        }

        @Override
        public List<AcademicCatalogItem> findActiveDepartments() {
            return Collections.singletonList(
                    new AcademicCatalogItem(3, 0, "", "CSC", "Computing", "", 0));
        }

        @Override
        public StaffMember create(StaffMember member) {
            StaffMember created = new StaffMember(
                    saved.size() + 1, member.getStaffNumber(), member.getFullName(),
                    member.getQualification(), member.getDepartmentId(), member.getDepartmentName(),
                    member.getPhone(), member.getEmail(), member.getResponsibility());
            saved.add(created);
            return created;
        }

        @Override
        public boolean update(StaffMember member) {
            return false;
        }

        @Override
        public boolean archive(long id) {
            return false;
        }
    }
}
