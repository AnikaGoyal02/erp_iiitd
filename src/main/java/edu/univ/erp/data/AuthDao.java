package edu.univ.erp.data;

import edu.univ.erp.exception.*;
import edu.univ.erp.domain.Role;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthDao extends BaseDao {

  private static final Logger log = LoggerFactory.getLogger(AuthDao.class);

  public record AuthUserRecord(int userId, String username, Role role, String passwordHash, String status,
      int failedAttempts, Long lockedUntil) {
  }

  public AuthUserRecord findByUsername(String username) {
    log.info("findByUsername called for username={}", username);
    String sql = "SELECT user_id, username, role, password_hash, status, failed_attempts, locked_until "
        + "FROM users_auth WHERE username = ?";

    try (Connection con = DBPool.auth().getConnection();
        ResultSet rs = runQuery(con, sql, username)) {

      if (rs.next()) {
        log.debug("User {} found with user_id={}", username, rs.getInt("user_id"));
        return new AuthUserRecord(
            rs.getInt("user_id"),
            rs.getString("username"),
            Role.valueOf(rs.getString("role")),
            rs.getString("password_hash"),
            rs.getString("status"),
            rs.getInt("failed_attempts"),
            rs.getObject("locked_until", Long.class));
      }

      log.warn("User {} not found", username);
      throw new NotFoundException("User not found");

    } catch (SQLException e) {
      log.error("Database error in findByUsername for username={}", username, e);
      throw new DatabaseException("AuthDB error: " + e.getMessage(), e);
    }
  }

  public AuthUserRecord findByUserId(int userId) {
    log.info("findByUserId called for userId={}", userId);

    String sql = "SELECT user_id, username, role, password_hash, status, failed_attempts, locked_until "
        + "FROM users_auth WHERE user_id = ?";

    try (Connection con = DBPool.auth().getConnection();
        ResultSet rs = runQuery(con, sql, userId)) {

      if (rs.next()) {
        log.debug("User with user_id={} found", userId);
        return new AuthUserRecord(
            rs.getInt("user_id"),
            rs.getString("username"),
            Role.valueOf(rs.getString("role")),
            rs.getString("password_hash"),
            rs.getString("status"),
            rs.getInt("failed_attempts"),
            rs.getObject("locked_until", Long.class));
      }

      log.warn("User with user_id={} not found", userId);
      throw new NotFoundException("User not found");

    } catch (SQLException e) {
      log.error("Database error in findByUserId for userId={}", userId, e);
      throw new DatabaseException("AuthDB error: " + e.getMessage(), e);
    }
  }

  public int insertUser(String username, Role role, String passwordHash) {
    log.info("insertUser called for username={} role={}", username, role);

    String sql = "INSERT INTO users_auth (username, role, password_hash) VALUES (?, ?, ?)";

    try (Connection con = DBPool.auth().getConnection();
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setString(1, username);
      ps.setString(2, role.name());
      ps.setString(3, passwordHash);
      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          int newId = rs.getInt(1);
          log.info("Successfully inserted user {} with user_id={}", username, newId);
          return newId;
        }
        throw new DatabaseException("Failed generating user_id");
      }

    } catch (SQLException e) {
      log.error("insertUser failed for username={}", username, e);
      throw new DatabaseException("User insert failed", e);
    }
  }

  public void updateFailedAttempts(int userId, int attempts) {
    log.info("updateFailedAttempts called for userId={}, attempts={}", userId, attempts);

    String sql = "UPDATE users_auth SET failed_attempts=? WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, attempts, userId);
    } catch (SQLException e) {
      log.error("updateFailedAttempts failed for userId={}", userId, e);
      throw new DatabaseException("Failed updating attempts", e);
    }
  }

  public void updateStatus(int userId, String status) {
    log.info("updateStatus called for userId={}, status={}", userId, status);

    String sql = "UPDATE users_auth SET status=? WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, status, userId);
    } catch (SQLException e) {
      log.error("updateStatus failed for userId={}", userId, e);
      throw new DatabaseException("Status update failed", e);
    }
  }

  public void setLockedUntil(int userId, long untilMillis) {
    log.warn("setLockedUntil called for userId={}, until={}", userId, untilMillis);

    String sql = "UPDATE users_auth SET locked_until=? WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, untilMillis, userId);
    } catch (SQLException e) {
      log.error("setLockedUntil failed for userId={}", userId, e);
      throw new DatabaseException("Failed setting lock", e);
    }
  }

  public void clearLockedUntil(int userId) {
    log.info("clearLockedUntil called for userId={}", userId);

    String sql = "UPDATE users_auth SET locked_until=NULL WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, userId);
    } catch (SQLException e) {
      log.error("clearLockedUntil failed for userId={}", userId, e);
      throw new DatabaseException("Failed clearing lock", e);
    }
  }

  public void updatePassword(int userId, String newHash) {
    log.info("updatePassword called for userId={}", userId);

    String sql = "UPDATE users_auth SET password_hash=?, failed_attempts=0, status='ACTIVE' WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, newHash, userId);
    } catch (SQLException e) {
      log.error("updatePassword failed for userId={}", userId, e);
      throw new DatabaseException("Failed updating password", e);
    }
  }

  public void updateLastLogin(int userId, long whenMillis) {
    log.info("updateLastLogin called for userId={}", userId);

    String sql = "UPDATE users_auth SET last_login=FROM_UNIXTIME(?) WHERE user_id=?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, whenMillis / 1000L, userId);
    } catch (SQLException e) {
      log.error("updateLastLogin failed for userId={}", userId, e);
      throw new DatabaseException("Failed updating last_login", e);
    }
  }

  public void deleteUser(int userId) {
    log.warn("deleteUser called for userId={}", userId);

    String sql = "DELETE FROM users_auth WHERE user_id = ?";
    try (Connection con = DBPool.auth().getConnection()) {
      runUpdate(con, sql, userId);
    } catch (SQLException e) {
      log.error("deleteUser failed for userId={}", userId, e);
      throw new DatabaseException("Failed to delete user from authdb", e);
    }
  }

  public Integer findUserIdByUsername(String username) {
    log.info("findUserIdByUsername called for username={}", username);

    String sql = "SELECT user_id FROM users_auth WHERE username = ?";
    try (Connection con = DBPool.auth().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();

      if (rs.next()) {
        int userId = rs.getInt("user_id");
        log.debug("Username {} -> user_id={}", username, userId);
        return userId;
      }

      log.warn("No userId found for username={}", username);
      throw new NotFoundException("No such username: " + username);

    } catch (SQLException e) {
      log.error("findUserIdByUsername DB error for username={}", username, e);
      throw new DatabaseException("Failed to lookup user", e);
    }
  }

  public boolean usernameExists(String username) {
    String sql = "SELECT 1 FROM users_auth WHERE username = ?";
    try (Connection con = DBPool.auth().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
  
      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();
      return rs.next();
  
    } catch (SQLException e) {
      log.error("usernameExists DB error for username={}", username, e);
      throw new DatabaseException("Failed checking username", e);
    }
  }
  
}
