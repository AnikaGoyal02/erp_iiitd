package edu.univ.erp.auth;

import edu.univ.erp.data.AuthDao;
import edu.univ.erp.data.StudentDao;
import edu.univ.erp.data.InstructorDao;
import edu.univ.erp.domain.*;
import edu.univ.erp.exception.*;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AuthDao authDao = new AuthDao();
  private final StudentDao studentDao = new StudentDao();
  private final InstructorDao instructorDao = new InstructorDao();

  // login with temporary lock
  public User login(String username, String password) {

    log.info("Login attempt for username={}", username);

    var rec = authDao.findByUsername(username);

    Long lockedUntil = rec.lockedUntil();
    if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
      log.warn("User {} temporarily locked until {}", username, new Date(lockedUntil));
      throw new AuthException("Account temporarily locked until " + new Date(lockedUntil));
    }

    if ("LOCKED".equals(rec.status())) {
      log.error("User {} is permanently locked", username);
      throw new AuthException("Account locked due to multiple failed attempts.");
    }

    if (!PasswordUtil.verify(password, rec.passwordHash())) {
      int attempts = rec.failedAttempts() + 1;
      log.warn("Incorrect password for {} (attempt {} of 5)", username, attempts);

      authDao.updateFailedAttempts(rec.userId(), attempts);

      if (attempts >= 5) {
        long until = System.currentTimeMillis() + 30_000L;
        authDao.setLockedUntil(rec.userId(), until);
        log.error("User {} locked for 30 seconds", username);
        throw new AuthException("Too many failed attempts. Account locked for 30 seconds.");
      }

      throw new AuthException("Incorrect username or password.");
    }

    log.info("User {} authenticated successfully", username);

    authDao.updateFailedAttempts(rec.userId(), 0);
    authDao.clearLockedUntil(rec.userId());
    authDao.updateLastLogin(rec.userId(), System.currentTimeMillis());

    User user = switch (rec.role()) {
      case ADMIN -> {
        log.debug("Creating Admin object for {}", username);
        yield new Admin(rec.userId(), rec.username());
      }
      case STUDENT -> {
        log.debug("Loading Student profile for {}", username);
        var s = studentDao.findByUserId(rec.userId());
        yield new Student(rec.userId(), rec.username(), s.getRollNo(), s.getProgram(), s.getYear(), s.getEmail());
      }
      case INSTRUCTOR -> {
        log.debug("Loading Instructor profile for {}", username);
        var i = instructorDao.findByUserId(rec.userId());
        yield new Instructor(rec.userId(), rec.username(), i.getDepartment(), i.getEmail());
      }
      default -> {
        log.error("Unknown role for {}", username);
        throw new AuthException("Unknown role");
      }
    };

    SessionManager.setCurrentUser(user);
    log.info("User {} logged in successfully with role={}", username, rec.role());

    return user;
  }

  public void logout() {
    log.info("User {} logged out", 
        SessionManager.getCurrentUser() == null ? "UNKNOWN" : SessionManager.getCurrentUser().getUsername());
    SessionManager.logout();
  }

  public void changePassword(int userId, String oldPassword, String newPassword) {

    log.info("Password change request for userId={}", userId);

    var rec = authDao.findByUserId(userId);

    if (!PasswordUtil.verify(oldPassword, rec.passwordHash())) {
      log.warn("Password change failed for userId={} (old password incorrect)", userId);
      throw new AuthException("Old password incorrect.");
    }

    String newHash = PasswordUtil.hash(newPassword);
    authDao.updatePassword(userId, newHash);

    log.info("Password updated successfully for userId={}", userId);
  }
}
