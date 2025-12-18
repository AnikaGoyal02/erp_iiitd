package edu.univ.erp.ui;

import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIError {

  private static final Logger log = LoggerFactory.getLogger(UIError.class);

  public static void show(Throwable t) {
    log.error("UIError.show(): {}", t.getMessage(), t);
    t.printStackTrace();
    JOptionPane.showMessageDialog(null, t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
  }

  public static void info(String msg) {
    log.info("UIError.info(): {}", msg);
    JOptionPane.showMessageDialog(null, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
  }
}
