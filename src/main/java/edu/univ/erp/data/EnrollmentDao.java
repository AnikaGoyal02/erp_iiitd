package edu.univ.erp.data;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.exception.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrollmentDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(EnrollmentDao.class);

  public List<Enrollment> findByStudent(int studentId) {
    log.debug("Fetching enrollments for studentId={}", studentId);

    String sql = "SELECT * FROM enrollments WHERE student_id=?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, studentId)) {

      List<Enrollment> list = new ArrayList<>();
      while (rs.next())
        list.add(map(rs));

      log.info("Found {} enrollments for studentId={}", list.size(), studentId);
      return list;

    } catch (SQLException e) {
      log.error("Enrollment list failed for studentId={}", studentId, e);
      throw new DatabaseException("Enrollment list failed", e);
    }
  }

  public Enrollment findById(int enrollmentId) {
    log.debug("Looking up enrollment by id={}", enrollmentId);

    String sql = "SELECT * FROM enrollments WHERE enrollment_id=?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, enrollmentId)) {

      if (!rs.next()) {
        log.warn("Enrollment not found for id={}", enrollmentId);
        throw new NotFoundException("Enrollment not found");
      }

      Enrollment e = map(rs);
      log.info("Loaded enrollment {}", enrollmentId);
      return e;

    } catch (SQLException ex) {
      log.error("Enrollment lookup failed for id={}", enrollmentId, ex);
      throw new DatabaseException("Enrollment lookup failed", ex);
    }
  }

  public Enrollment find(int studentId, int sectionId) {
    log.debug("Looking up enrollment studentId={} sectionId={}", studentId, sectionId);

    String sql = "SELECT * FROM enrollments WHERE student_id=? AND section_id=?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, studentId, sectionId)) {

      if (!rs.next()) {
        log.warn("Enrollment not found for studentId={} sectionId={}", studentId, sectionId);
        throw new NotFoundException("Enrollment not found");
      }

      Enrollment e = map(rs);
      log.info("Loaded enrollment for studentId={} sectionId={}", studentId, sectionId);
      return e;

    } catch (SQLException e) {
      log.error("Enrollment lookup error studentId={} sectionId={}", studentId, sectionId, e);
      throw new DatabaseException("Enrollment lookup error", e);
    }
  }

  private Enrollment map(ResultSet rs) throws SQLException {
    log.trace("Mapping ResultSet row to Enrollment object");

    return new Enrollment(
        rs.getInt("enrollment_id"),
        rs.getInt("student_id"),
        rs.getInt("section_id"),
        Enrollment.Status.valueOf(rs.getString("status")),
        rs.getString("registered_on"),
        rs.getString("dropped_on"),
        rs.getString("final_grade"));
  }
}
