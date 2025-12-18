package edu.univ.erp.ui;

import edu.univ.erp.domain.Role;
import edu.univ.erp.ui.admin.AdminPanel;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(DashboardFrame.class);

  public DashboardFrame(Role role) {
    log.debug("Initializing DashboardFrame for role={}", role);

    setTitle("University ERP");
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setUndecorated(false);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    switch (role) {
      case STUDENT -> {
        log.info("Loading StudentPanel for user");
        setContentPane(new StudentPanel());
      }
      case INSTRUCTOR -> {
        log.info("Loading InstructorPanel for user");
        setContentPane(new InstructorPanel());
      }
      case ADMIN -> {
        log.info("Loading AdminPanel for administrator");
        setContentPane(new AdminPanel());
      }
      default -> {
        log.error("Unknown role encountered: {}", role);
      }
    }

    log.debug("DashboardFrame initialized");
  }
}
