package ke.co.tharaka.tums.model;

import java.util.EnumSet;

public enum AppModule {
    DASHBOARD("Dashboard", EnumSet.allOf(UserRole.class)),
    STUDENTS("Student records", EnumSet.of(UserRole.ADMINISTRATOR, UserRole.LECTURER)),
    STAFF_AND_COURSES("Academic catalogue", EnumSet.of(UserRole.ADMINISTRATOR, UserRole.LECTURER)),
    RESULTS("Exams and results", EnumSet.of(UserRole.ADMINISTRATOR, UserRole.LECTURER)),
    FINANCE("Fees and finance", EnumSet.of(UserRole.ADMINISTRATOR, UserRole.FINANCE_OFFICER)),
    SYSTEM_ADMINISTRATION("System administration", EnumSet.of(UserRole.ADMINISTRATOR));

    private final String displayName;
    private final EnumSet<UserRole> allowedRoles;

    AppModule(String displayName, EnumSet<UserRole> allowedRoles) {
        this.displayName = displayName;
        this.allowedRoles = allowedRoles.clone();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAllowedFor(UserRole role) {
        return allowedRoles.contains(role);
    }
}
