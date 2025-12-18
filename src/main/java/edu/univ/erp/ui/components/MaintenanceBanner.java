package edu.univ.erp.ui.components;

import javax.swing.*;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaintenanceBanner extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceBanner.class);

  private final JLabel label;

  public MaintenanceBanner() {
    log.debug("Initializing MaintenanceBanner");

    setLayout(new BorderLayout());
    label = new JLabel("MAINTENANCE MODE IS ACTIVE", SwingConstants.CENTER);
    label.setForeground(Color.WHITE);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
    setBackground(new Color(180, 0, 0));
    add(label, BorderLayout.CENTER);
    setVisible(false);

    log.debug("MaintenanceBanner initialized (hidden by default)");
  }

  public void showBanner(boolean visible) {
    log.info("Setting maintenance banner visibility to {}", visible);
    setVisible(visible);
  }
}
