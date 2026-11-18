package ke.co.tharaka.tums.dao;

import ke.co.tharaka.tums.model.AcademicCatalogItem;

import java.sql.SQLException;
import java.util.List;

public interface AcademicCatalogDao {
    enum Kind {
        DEPARTMENT,
        PROGRAMME,
        COURSE
    }

    List<AcademicCatalogItem> findActive(Kind kind, String query) throws SQLException;

    AcademicCatalogItem create(Kind kind, AcademicCatalogItem item) throws SQLException;

    boolean update(Kind kind, AcademicCatalogItem item) throws SQLException;

    boolean archive(Kind kind, long id) throws SQLException;
}
