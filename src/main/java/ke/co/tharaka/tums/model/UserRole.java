package ke.co.tharaka.tums.model;

public enum UserRole {
    ADMINISTRATOR("Administrator"),
    LECTURER("Lecturer"),
    FINANCE_OFFICER("Finance officer");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromDatabase(String value) {
        for (UserRole role : values()) {
            if (role.name().equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role in the database.");
    }
}
