package edu.univ.erp.ui.components;

import edu.univ.erp.data.SettingsDao;

import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaintenanceRefresher {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceRefresher.class);

  private final SettingsDao settingsDao = new SettingsDao();
  private final Timer timer;

  public MaintenanceRefresher(Runnable refreshFn) {
    timer = new Timer(3000, e -> {
      try {
        boolean on = settingsDao.isMaintenanceMode();
        log.debug("Maintenance mode status refreshed: {}", on);
        refreshFn.run();  // <-- CRITICAL FIX
      } catch (Exception ex) {
        log.error("Failed to refresh maintenance mode", ex);
      }
    });

    log.info("MaintenanceRefresher timer started (3s interval)");
    timer.start();
  }

  public void stop() {
    timer.stop();
  }
}
