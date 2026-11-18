package ke.co.tharaka.tums.model;

public final class StoredUserCredential {
    private final UserAccount account;
    private final byte[] passwordHash;
    private final byte[] passwordSalt;
    private final int passwordIterations;

    public StoredUserCredential(
            UserAccount account,
            byte[] passwordHash,
            byte[] passwordSalt,
            int passwordIterations) {
        this.account = account;
        this.passwordHash = passwordHash.clone();
        this.passwordSalt = passwordSalt.clone();
        this.passwordIterations = passwordIterations;
    }

    public UserAccount getAccount() {
        return account;
    }

    public byte[] getPasswordHash() {
        return passwordHash.clone();
    }

    public byte[] getPasswordSalt() {
        return passwordSalt.clone();
    }

    public int getPasswordIterations() {
        return passwordIterations;
    }
}
