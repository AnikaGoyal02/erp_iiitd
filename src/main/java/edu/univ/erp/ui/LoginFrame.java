package edu.univ.erp.ui;

import edu.univ.erp.auth.AuthService;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(LoginFrame.class);

    private final AuthService auth = new AuthService();

    public LoginFrame() {
        log.info("Initializing LoginFrame UI");

        setTitle("ERP Login");
        setSize(460, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new MigLayout("fill, insets 25", "[grow]", "[grow]"));

        JPanel panel = new JPanel(new MigLayout(
                "wrap 2, insets 20, gapy 18",
                "[right][230!]",
                "[]25[]25[]"
        ));

        Font labelFont = new Font("SansSerif", Font.PLAIN, 16);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 16);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 16);

        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(labelFont);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(labelFont);

        JTextField userField = new JTextField();
        userField.setPreferredSize(new Dimension(230, 32));
        userField.setFont(fieldFont);

        JPasswordField passField = new JPasswordField();
        passField.setPreferredSize(new Dimension(230, 32));
        passField.setFont(fieldFont);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(buttonFont);
        loginBtn.setPreferredSize(new Dimension(120, 40));
        loginBtn.setFocusable(false);

        panel.add(userLbl);
        panel.add(userField, "growx");

        panel.add(passLbl);
        panel.add(passField, "growx");

        panel.add(loginBtn, "span 2, align center, gaptop 15");

        add(panel, "align center");

        loginBtn.addActionListener(e -> {
            String username = userField.getText();
            log.info("User attempting login: {}", username);

            try {
                var user = auth.login(
                        username,
                        new String(passField.getPassword()));

                log.info("Login successful for username={}, role={}", username, user.getRole());
                dispose();
                new DashboardFrame(user.getRole()).setVisible(true);

            } catch (Exception ex) {
                log.warn("Login failed for username={}", username, ex);
                UIError.show(ex);
            }
        });

        log.debug("LoginFrame UI initialized");
    }
}
