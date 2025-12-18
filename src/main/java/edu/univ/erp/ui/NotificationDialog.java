package edu.univ.erp.ui;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.*;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.NotificationService;
import edu.univ.erp.ui.UIError;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(NotificationDialog.class);

    private final NotificationService notifSvc = new NotificationService();
    private final AdminService adminSvc = new AdminService();

    public NotificationDialog(Window owner) {
        super(owner, "Send Notification", ModalityType.APPLICATION_MODAL);
        log.info("Opening NotificationDialog");
        init();
    }

    private void init() {

        User u = SessionManager.getCurrentUser();
        boolean isAdmin = (u instanceof Admin);

        log.debug("Initializing NotificationDialog for userId={}, role={}", u.getUserId(), u.getRole());

        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        String[] adminTargets = {"ALL","ALL_STUDENTS","ALL_INSTRUCTORS","COURSE","SECTION"};
        
        String[] instTargets = {"ALL_STUDENTS","COURSE","SECTION"};
        

        JComboBox<String> targetType =
                new JComboBox<>(isAdmin ? adminTargets : instTargets);

        JComboBox<Course> courseBox = new JComboBox<>();
        JComboBox<Section> sectionBox = new JComboBox<>();

        try {
            log.debug("Loading courses and sections for notification dialog");
            adminSvc.listCourses().forEach(courseBox::addItem);
            adminSvc.listSections().forEach(sectionBox::addItem);
        } catch (Exception ignored) {
            log.warn("Failed to load courses or sections for NotificationDialog");
        }

        JTextField title = new JTextField();
        JTextArea message = new JTextArea(5, 20);

        form.add(new JLabel("Target Type:"));
        form.add(targetType);

        form.add(new JLabel("Course:"));
        form.add(courseBox);

        form.add(new JLabel("Section:"));
        form.add(sectionBox);

        form.add(new JLabel("Title:"));
        form.add(title);

        form.add(new JLabel("Message:"));
        form.add(new JScrollPane(message));

        add(form, BorderLayout.CENTER);

        // visibility rules
        Runnable update = () -> {
            String t = (String) targetType.getSelectedItem();
            log.debug("Notification target changed to {}", t);
            courseBox.setEnabled("COURSE".equals(t));
            sectionBox.setEnabled("SECTION".equals(t));
        };
        update.run();
        targetType.addActionListener(e -> update.run());

        JPanel btns = new JPanel();
        JButton send = new JButton("Send");
        JButton cancel = new JButton("Cancel");

        btns.add(send);
        btns.add(cancel);
        add(btns, BorderLayout.SOUTH);

        send.addActionListener(e -> {
            log.info("Send button clicked in NotificationDialog");
            try {
                String t = (String) targetType.getSelectedItem();
                Integer tid = null;

                if ("COURSE".equals(t)) {
                    Course c = (Course) courseBox.getSelectedItem();
                    if (c == null) {
                        log.warn("Course target selected but no course chosen");
                        return;
                    }
                    tid = c.getCourseId();
                }

                if ("SECTION".equals(t)) {
                    Section s = (Section) sectionBox.getSelectedItem();
                    if (s == null) {
                        log.warn("Section target selected but no section chosen");
                        return;
                    }
                    tid = s.getSectionId();
                }

                String ttl = title.getText().trim();
                String msg = message.getText().trim();

                if (ttl.isBlank() || msg.isBlank()) {
                    log.warn("Notification send attempt failed due to empty fields");
                    UIError.info("Title and Message cannot be empty.");
                    return;
                }

                log.info("Sending notification: type={}, targetId={}, title={}", t, tid, ttl);
                notifSvc.sendNotification(t, tid, ttl, msg);
                log.info("Notification sent successfully");

                UIError.info("Sent.");
                dispose();

            } catch (Exception ex) {
                log.error("Failed to send notification", ex);
                UIError.show(ex);
            }
        });

        cancel.addActionListener(e -> {
            log.info("NotificationDialog cancelled");
            dispose();
        });

        pack();
        setLocationRelativeTo(getOwner());
        log.debug("NotificationDialog initialized and visible");
    }
}
