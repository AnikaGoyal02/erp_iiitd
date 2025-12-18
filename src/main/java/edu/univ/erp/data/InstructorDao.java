package edu.univ.erp.data;

import edu.univ.erp.domain.Instructor;
import edu.univ.erp.exception.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstructorDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(InstructorDao.class);

  public List<Instructor> findAll() {
    log.debug("Fetching all instructors");
    String sql = """
        SELECT u.user_id, u.username, i.department, i.email
        FROM instructors i
        JOIN users_auth u ON u.user_id = i.user_id
        ORDER BY u.username
        """;
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql)) {

      List<Instructor> list = new ArrayList<>();
      while (rs.next()) {
        log.trace("Mapping instructor: userId={} username={} dept={} email={}",
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("department"),
            rs.getString("email"));

        list.add(new Instructor(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("department"),
            rs.getString("email")));
      }

      log.info("Loaded {} instructors", list.size());
      return list;

    } catch (SQLException e) {
      log.error("Failed loading instructors", e);
      throw new DatabaseException("Failed loading instructors", e);
    }
  }

  public Instructor findByUserId(int userId) {
    log.debug("Looking up instructor by userId={}", userId);

    String sql = "SELECT user_id, department, email FROM instructors WHERE user_id = ?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, userId)) {

      if (!rs.next()) {
        log.warn("Instructor not found for userId={}", userId);
        throw new NotFoundException("Instructor not found");
      }

      log.trace("Instructor found: userId={} dept={} email={}",
          userId, rs.getString("department"), rs.getString("email"));

      return new Instructor(userId, "unknown", rs.getString("department"), rs.getString("email"));

    } catch (SQLException e) {
      log.error("Instructor lookup failed for userId={}", userId, e);
      throw new DatabaseException("Instructor lookup failed", e);
    }
  }

  public void insert(int userId, String department, String email) {
    log.debug("Inserting instructor: userId={} dept={} email={}", userId, department, email);

    String sql = "INSERT INTO instructors (user_id, department, email) VALUES (?, ?, ?)";
    try (Connection con = DBPool.erp().getConnection()) {

      runUpdate(con, sql, userId, department, email);
      log.info("Instructor inserted: userId={}", userId);

    } catch (SQLException e) {
      log.error("Instructor insert failed for userId={}", userId, e);
      throw new DatabaseException("Instructor insert failed", e);
    }
  }

  public String findUsernameByInstructorId(int instructorId) {
    log.debug("Fetching username for instructorId={}", instructorId);

    String sql = """
        SELECT u.username
        FROM instructors i
        JOIN users_auth u ON u.user_id = i.user_id
        WHERE i.instructor_id = ?
        """;
    try (var con = DBPool.erp().getConnection();
        var ps = con.prepareStatement(sql)) {

      ps.setInt(1, instructorId);
      var rs = ps.executeQuery();

      if (rs.next()) {
        log.trace("Username found for instructorId={}: {}", instructorId, rs.getString("username"));
        return rs.getString("username");
      }

      log.warn("No username found for instructorId={}", instructorId);
      return null;

    } catch (SQLException e) {
      log.error("Failed loading instructor username for instructorId={}", instructorId, e);
      throw new DatabaseException("Failed loading instructor username", e);
    }
  }

  public Integer findInstructorIdByEmail(String email) {
    log.debug("Looking up instructorId by email={}", email);

    String sql = "SELECT instructor_id FROM instructors WHERE email = ?";
    try (Connection con = DBPool.erp().getConnection();
         ResultSet rs = runQuery(con, sql, email)) {

        if (!rs.next()) {
            log.warn("No instructor found with email={}", email);
            return null;
        }

        int id = rs.getInt("instructor_id");
        log.trace("InstructorId={} found for email={}", id, email);
        return id;

    } catch (SQLException e) {
        log.error("Instructor lookup failed for email={}", email, e);
        throw new DatabaseException("Instructor lookup failed", e);
    }
  }

  public void deleteByUserId(int userId) {
    log.debug("Deleting instructor by userId={}", userId);

    try (Connection con = DBPool.erp().getConnection()) {

        String clear = """
            UPDATE sections
            SET instructor_id = NULL
            WHERE instructor_id = (
                SELECT instructor_id FROM instructors WHERE user_id = ?
            )
        """;

        runUpdate(con, clear, userId);
        log.trace("Cleared sections for userId={}", userId);

        String sql = "DELETE FROM instructors WHERE user_id = ?";
        runUpdate(con, sql, userId);

        log.info("Instructor deleted for userId={}", userId);

    } catch (SQLException e) {
        log.error("Failed to delete instructor for userId={}", userId, e);
        throw new DatabaseException("Failed to delete instructor", e);
    }
  }

  public Integer findInstructorIdByUserId(int userId) {
    log.debug("Fetching instructorId by userId={}", userId);

    String sql = "SELECT instructor_id FROM instructors WHERE user_id=?";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int id = rs.getInt("instructor_id");
            log.trace("Found instructorId={} for userId={}", id, userId);
            return id;
        }

        log.warn("No instructor found for userId={}", userId);
        return null;

    } catch (SQLException e) {
        log.error("Failed to lookup instructor for userId={}", userId, e);
        throw new DatabaseException("Failed to lookup instructor", e);
    }
  }


  public boolean emailExists(String email) {
    String sql = "SELECT 1 FROM instructors WHERE email = ?";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next();

    } catch (SQLException e) {
        throw new DatabaseException("Failed checking email existence", e);
    }
  }


}
