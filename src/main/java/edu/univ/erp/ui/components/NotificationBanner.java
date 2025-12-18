package edu.univ.erp.ui.components;

import javax.swing.*;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationBanner extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(NotificationBanner.class);

    private final JLabel label = new JLabel("");

    public NotificationBanner() {
        log.debug("Initializing NotificationBanner");

        setLayout(new BorderLayout());
        setBackground(new Color(255, 235, 148));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
        setVisible(false);

        log.debug("NotificationBanner initialized (hidden by default)");
    }

    public void showMessage(String msg) {
        log.info("Showing notification message: {}", msg);

        label.setText(msg);
        setVisible(true);

        Timer t = new Timer(3000, e -> {
            log.debug("Hiding notification banner after timeout");
            setVisible(false);
        });

        t.setRepeats(false);
        t.start();
    }
}
