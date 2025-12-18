package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManager {
  private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
  private static final ThreadLocal<User> current = new ThreadLocal<>();

  public static User getCurrentUser() {
    User u = current.get();
    if (u != null) {
      log.debug("Current user fetched: {} (id={})", u.getUsername(), u.getUserId());
    } else {
      log.debug("No current user in session.");
    }
    return u;
  }

  public static void setCurrentUser(User u) {
    if (u != null) {
      log.info("User logged in and stored in session: {} (id={})", u.getUsername(), u.getUserId());
    } else {
      log.warn("Attempted to set null user in session.");
    }
    current.set(u);
  }

  public static void logout() {
    User u = current.get();
    if (u != null) {
      log.info("User logged out: {} (id={})", u.getUsername(), u.getUserId());
    } else {
      log.debug("Logout called with no active user.");
    }
    current.remove();
  }
}
