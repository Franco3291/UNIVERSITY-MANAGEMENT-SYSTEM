package ke.co.tharaka.tums.model;

public final class UserAccount {
    private final long id;
    private final String username;
    private final UserRole role;

    public UserAccount(long id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }
}
