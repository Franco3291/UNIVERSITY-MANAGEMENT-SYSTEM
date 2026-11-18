package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.model.Student;

import java.sql.SQLException;
import java.util.List;

public interface StudentDao {
    List<Student> searchActive(String query) throws SQLException;

    Student create(Student student) throws SQLException;

    boolean update(Student student) throws SQLException;

    boolean archive(long id) throws SQLException;
}
