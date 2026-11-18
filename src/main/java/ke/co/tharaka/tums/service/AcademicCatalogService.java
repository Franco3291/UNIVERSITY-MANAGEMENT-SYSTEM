package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.AcademicCatalogDao;
import ke.co.tharaka.tums.dao.AcademicCatalogDao.Kind;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.AppModule;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AcademicCatalogService {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]*");

    private final AcademicCatalogDao dao;

    public AcademicCatalogService(AcademicCatalogDao dao) {
        this.dao = dao;
    }

    public List<AcademicCatalogItem> search(UserAccount user, Kind kind, String query) throws SQLException {
        requireCatalogAccess(user);
        return dao.findActive(requireKind(kind), query == null ? "" : query.trim());
    }

    public AcademicCatalogItem create(
            UserAccount user, Kind kind, AcademicCatalogItem item) throws SQLException {
        requireAdministrator(user);
        return dao.create(requireKind(kind), validate(kind, item, false));
    }

    public void update(
            UserAccount user, Kind kind, AcademicCatalogItem item) throws SQLException {
        requireAdministrator(user);
        Kind safeKind = requireKind(kind);
        if (!dao.update(safeKind, validate(safeKind, item, true))) {
            throw new IllegalStateException("The academic record no longer exists or has been archived.");
        }
    }

    public void archive(UserAccount user, Kind kind, long id) throws SQLException {
        requireAdministrator(user);
        if (id <= 0) {
            throw new IllegalArgumentException("Select an academic record before archiving it.");
        }
        if (!dao.archive(requireKind(kind), id)) {
            throw new IllegalStateException("The academic record no longer exists or has already been archived.");
        }
    }

    private static AcademicCatalogItem validate(Kind kind, AcademicCatalogItem item, boolean updating) {
        Kind safeKind = requireKind(kind);
        if (item == null) {
            throw new IllegalArgumentException("Academic record details are required.");
        }
        if ((updating && item.getId() <= 0) || (!updating && item.getId() != 0)) {
            throw new IllegalArgumentException("The academic record is not in a valid state.");
        }
        String code = required(item.getCode(), "Code", 24);
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Code may contain letters, numbers, periods, underscores, slashes, and hyphens.");
        }
        String name = required(item.getName(), "Name", safeKind == Kind.DEPARTMENT ? 120 : 150);
        String description = optional(item.getDescription(), "Description", 500);
        long parentId = item.getParentId();
        if (safeKind == Kind.DEPARTMENT && parentId != 0) {
            throw new IllegalArgumentException("Departments cannot have a parent record.");
        }
        if (safeKind != Kind.DEPARTMENT && parentId <= 0) {
            throw new IllegalArgumentException(safeKind == Kind.PROGRAMME
                    ? "Select a department for this programme."
                    : "Select a programme for this course.");
        }
        int creditHours = item.getCreditHours();
        if (safeKind == Kind.COURSE && (creditHours < 1 || creditHours > 12)) {
            throw new IllegalArgumentException("Course credit hours must be between 1 and 12.");
        }
        if (safeKind != Kind.COURSE && creditHours != 0) {
            throw new IllegalArgumentException("Only a course can have credit hours.");
        }
        return new AcademicCatalogItem(item.getId(), parentId, item.getParentName(),
                code.toUpperCase(Locale.ROOT), name, description, creditHours);
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

    private static Kind requireKind(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Academic catalog type is required.");
        }
        return kind;
    }

    private static void requireCatalogAccess(UserAccount user) {
        if (user == null || !AppModule.STAFF_AND_COURSES.isAllowedFor(user.getRole())) {
            throw new SecurityException("Your account is not authorized to view academic catalog records.");
        }
    }

    private static void requireAdministrator(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Only administrators may create, update, or archive academic records.");
        }
    }
}
