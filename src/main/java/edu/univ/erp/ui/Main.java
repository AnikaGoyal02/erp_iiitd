package edu.univ.erp.ui;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import edu.univ.erp.data.DBPool;
import javax.swing.*;
import java.io.FileInputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    log.info("ERP Application starting...");

    try {
      log.debug("Applying FlatOneDarkIJTheme...");
      FlatOneDarkIJTheme.setup();

      log.debug("Applying UI defaults...");
      UIStyle.applyDefaults();

      log.debug("Loading application.properties...");
      Properties props = new Properties();
      props.load(new FileInputStream("src/main/resources/application.properties"));

      log.debug("Initializing DBPool...");
      DBPool.init(props);

      log.info("Launching LoginFrame...");
      SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));

    } catch (Exception e) {
      log.error("Fatal error during application startup", e);
      UIError.show(e);
    }
  }
}
