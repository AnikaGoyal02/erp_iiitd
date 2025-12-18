package edu.univ.erp.service;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;
import edu.univ.erp.exception.AccessDeniedException;
import edu.univ.erp.data.SettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessControl {

  private static final Logger log = LoggerFactory.getLogger(AccessControl.class);

  public void requireRole(Role r) {
    User u = SessionManager.getCurrentUser();

    if (u == null) {
      log.warn("Access denied: no user logged in. Required role={}", r);
      throw new AccessDeniedException("Not logged in");
    }

    if (u.getRole() != r) {
      log.warn("Access denied for user {} (id={}): required role={}, but found={}",
              u.getUsername(), u.getUserId(), r, u.getRole());
      throw new AccessDeniedException("Permission denied");
    }

    log.debug("Access granted for user {} (id={}) with role={}", 
              u.getUsername(), u.getUserId(), r);
  }

  public void requireMaintenanceOff() {
    boolean on = new SettingsDao().isMaintenanceMode();

    if (on) {
      log.warn("Blocked action due to maintenance mode.");
      throw new AccessDeniedException("Maintenance mode is ON");
    }

    log.debug("Maintenance mode is OFF. Action allowed.");
  }
}
