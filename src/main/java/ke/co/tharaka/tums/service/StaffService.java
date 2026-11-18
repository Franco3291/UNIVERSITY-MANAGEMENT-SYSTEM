package ke.co.tharaka.tums.service;

import ke.co.tharaka.tums.dao.StaffDao;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.AppModule;
import ke.co.tharaka.tums.model.StaffMember;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class StaffService {
    private static final Pattern STAFF_NUMBER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/-]*");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("[+0-9() .-]*");

    private final StaffDao staffDao;

    public StaffService(StaffDao staffDao) {
        this.staffDao = staffDao;
    }

    public List<StaffMember> search(UserAccount user, String query) throws SQLException {
        requireStaffAccess(user);
        return staffDao.searchActive(query == null ? "" : query.trim());
    }

    public List<AcademicCatalogItem> findDepartments(UserAccount user) throws SQLException {
        requireStaffAccess(user);
        return staffDao.findActiveDepartments();
    }

    public StaffMember create(UserAccount user, StaffMember member) throws SQLException {
        requireAdministrator(user);
        StaffMember validated = validate(member, false);
        requireActiveDepartment(validated.getDepartmentId());
        return staffDao.create(validated);
    }

    public void update(UserAccount user, StaffMember member) throws SQLException {
        requireAdministrator(user);
        StaffMember validated = validate(member, true);
        requireActiveDepartment(validated.getDepartmentId());
        if (!staffDao.update(validated)) {
            throw new IllegalStateException("The staff record no longer exists or has been archived.");
        }
    }

    public void archive(UserAccount user, long id) throws SQLException {
        requireAdministrator(user);
        if (id <= 0) {
            throw new IllegalArgumentException("Select a staff member before archiving the record.");
        }
        if (!staffDao.archive(id)) {
            throw new IllegalStateException("The staff record no longer exists or has already been archived.");
        }
    }

    private static StaffMember validate(StaffMember member, boolean updating) {
        if (member == null) {
            throw new IllegalArgumentException("Staff details are required.");
        }
        if ((updating && member.getId() <= 0) || (!updating && member.getId() != 0)) {
            throw new IllegalArgumentException("The staff record is not in a valid state.");
        }
        String number = required(member.getStaffNumber(), "Staff number", 32).toUpperCase(Locale.ROOT);
        if (!STAFF_NUMBER.matcher(number).matches()) {
            throw new IllegalArgumentException("Staff number may contain letters, numbers, slashes, and hyphens.");
        }
        String name = required(member.getFullName(), "Full name", 150);
        String qualification = required(member.getQualification(), "Qualification", 150);
        if (member.getDepartmentId() <= 0) {
            throw new IllegalArgumentException("Select an active department for this staff member.");
        }
        String phone = optional(member.getPhone(), "Phone number", 30);
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number may contain digits, spaces, '+', '-', '(', and ')'.");
        }
        String email = optional(member.getEmail(), "Email address", 254);
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        String responsibility = required(member.getResponsibility(), "Responsibility", 150);
        return new StaffMember(member.getId(), number, name, qualification,
                member.getDepartmentId(), member.getDepartmentName(), phone, email, responsibility);
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

    private static void requireStaffAccess(UserAccount user) {
        if (user == null || !AppModule.STAFF.isAllowedFor(user.getRole())) {
            throw new SecurityException("Your account is not authorized to view staff records.");
        }
    }

    private static void requireAdministrator(UserAccount user) {
        if (user == null || user.getRole() != UserRole.ADMINISTRATOR) {
            throw new SecurityException("Only administrators may create, update, or archive staff records.");
        }
    }

    private void requireActiveDepartment(long departmentId) throws SQLException {
        for (AcademicCatalogItem department : staffDao.findActiveDepartments()) {
            if (department.getId() == departmentId) {
                return;
            }
        }
        throw new IllegalArgumentException("Select an active department for this staff member.");
    }
}
