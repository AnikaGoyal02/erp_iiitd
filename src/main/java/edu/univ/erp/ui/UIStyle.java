package edu.univ.erp.ui;

import javax.swing.*;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIStyle {

  private static final Logger log = LoggerFactory.getLogger(UIStyle.class);

  public static void applyDefaults() {
    log.info("Applying UI default styles");

    UIManager.put("Button.arc", 12);
    UIManager.put("Component.focusWidth", 1);
    UIManager.put("Table.showHorizontalLines", true);
    UIManager.put("Table.showVerticalLines", true);
    UIManager.put("Table.intercellSpacing", new Dimension(6, 6));
    UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 14));

    log.debug("UI defaults applied successfully");
  }
}
