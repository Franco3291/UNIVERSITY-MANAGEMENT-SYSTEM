package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.StaffMember;

import java.sql.SQLException;
import java.util.List;

public interface StaffDao {
    List<StaffMember> searchActive(String query) throws SQLException;

    List<AcademicCatalogItem> findActiveDepartments() throws SQLException;

    StaffMember create(StaffMember member) throws SQLException;

    boolean update(StaffMember member) throws SQLException;

    boolean archive(long id) throws SQLException;
}
