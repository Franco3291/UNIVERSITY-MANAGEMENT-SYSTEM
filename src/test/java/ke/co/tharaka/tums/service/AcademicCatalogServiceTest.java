package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.AcademicCatalogDao;
import ke.co.tharaka.tums.dao.AcademicCatalogDao.Kind;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
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

public class AcademicCatalogServiceTest {
    @Test
    public void normalizesDepartmentAndAllowsAdministratorToCreateIt() throws SQLException {
        InMemoryAcademicCatalogDao dao = new InMemoryAcademicCatalogDao();
        AcademicCatalogService service = new AcademicCatalogService(dao);

        AcademicCatalogItem created = service.create(administrator(), Kind.DEPARTMENT,
                new AcademicCatalogItem(0, 0, "", " csc ", " Computing ", "  School of computing  ", 0));

        assertEquals(1L, created.getId());
        assertEquals("CSC", created.getCode());
        assertEquals("Computing", created.getName());
        assertEquals("School of computing", created.getDescription());
    }

    @Test
    public void rejectsInvalidCourseCreditsWithoutWriting() throws SQLException {
        InMemoryAcademicCatalogDao dao = new InMemoryAcademicCatalogDao();
        AcademicCatalogService service = new AcademicCatalogService(dao);
        AcademicCatalogItem course = new AcademicCatalogItem(
                0, 1, "Computing", "CSC101", "Programming", "", 0);

        try {
            service.create(administrator(), Kind.COURSE, course);
            fail("Invalid credit hours should be rejected.");
        } catch (IllegalArgumentException expected) {
            assertEquals("Course credit hours must be between 1 and 12.", expected.getMessage());
        }
        assertTrue(dao.saved.isEmpty());
    }

    @Test
    public void lecturerCanViewButCannotChangeAcademicCatalogue() throws SQLException {
        InMemoryAcademicCatalogDao dao = new InMemoryAcademicCatalogDao();
        AcademicCatalogService service = new AcademicCatalogService(dao);
        UserAccount lecturer = new UserAccount(2L, "lecturer", UserRole.LECTURER);

        assertTrue(service.search(lecturer, Kind.DEPARTMENT, "").isEmpty());
        try {
            service.create(lecturer, Kind.DEPARTMENT,
                    new AcademicCatalogItem(0, 0, "", "CSC", "Computing", "", 0));
            fail("Lecturers must not be able to create academic catalogue records.");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("Only administrators"));
        }
    }

    private static UserAccount administrator() {
        return new UserAccount(1L, "admin", UserRole.ADMINISTRATOR);
    }

    private static final class InMemoryAcademicCatalogDao implements AcademicCatalogDao {
        private final List<AcademicCatalogItem> saved = new ArrayList<AcademicCatalogItem>();

        @Override
        public List<AcademicCatalogItem> findActive(Kind kind, String query) {
            return Collections.unmodifiableList(saved);
        }

        @Override
        public AcademicCatalogItem create(Kind kind, AcademicCatalogItem item) {
            AcademicCatalogItem created = new AcademicCatalogItem(
                    saved.size() + 1, item.getParentId(), item.getParentName(),
                    item.getCode(), item.getName(), item.getDescription(), item.getCreditHours());
            saved.add(created);
            return created;
        }

        @Override
        public boolean update(Kind kind, AcademicCatalogItem item) {
            return false;
        }

        @Override
        public boolean archive(Kind kind, long id) {
            return false;
        }
    }
}
