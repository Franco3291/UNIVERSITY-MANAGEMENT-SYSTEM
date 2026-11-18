package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.model.StoredUserCredential;

import java.sql.SQLException;
import java.util.Optional;

public interface UserDao {
    Optional<StoredUserCredential> findActiveByUsername(String username) throws SQLException;
}
