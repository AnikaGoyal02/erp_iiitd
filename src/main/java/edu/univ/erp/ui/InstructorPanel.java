package edu.univ.erp.ui;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.grades.*;
import edu.univ.erp.util.AutoTableResize;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.NotificationService;
import edu.univ.erp.auth.AuthService;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.ui.components.MaintenanceBanner;
import edu.univ.erp.ui.components.DeadlineRefresher;
import edu.univ.erp.ui.components.MaintenanceRefresher;
import edu.univ.erp.ui.components.NotificationBanner;
import edu.univ.erp.domain.Notification;
import edu.univ.erp.ui.NotificationDialog;
import edu.univ.erp.service.NotificationListener;
import edu.univ.erp.ui.UIError;

import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import java.io.File;
import java.util.*;

public class InstructorPanel extends JPanel {
    private final InstructorService svc = new InstructorService();
    private final MaintenanceBanner banner = new MaintenanceBanner();
    private final NotificationService notifSvc = new NotificationService();
    private final NotificationBanner notifBanner = new NotificationBanner();

    private JTable tableSections;
    private JTable tableStudents;

    private JButton loadSectionsButton;
    private JButton loadStudentsButton;
    private JButton addGradeButton;
    private JButton computeFinalButton;
    private JButton exportGradesButton;
    private JButton importGradesButton;
    private JButton statsButton;
    private JButton changePwdButton;
    private JButton notifyButton;
    private JButton showNotifButton;

    public InstructorPanel() {
        setLayout(new MigLayout("fill, insets 15", "[grow][grow]", "[grow]"));

        JPanel left = new JPanel(new MigLayout("fill", "[grow]", "[][grow]"));
        loadSectionsButton = new JButton("My Sections");
        tableSections = new JTable();
        left.add(loadSectionsButton, "wrap");
        left.add(new JScrollPane(tableSections), "grow");

        JPanel right = new JPanel(new MigLayout(
                "fillx, insets 0",
                "[grow][grow][grow][grow]", "[][grow][]"));

        loadStudentsButton = new JButton("Load Students");
        tableStudents = new JTable();
        addGradeButton = new JButton("Add Grade Component");
        computeFinalButton = new JButton("Compute Final Grade");
        exportGradesButton = new JButton("Export Grades CSV");
        importGradesButton = new JButton("Import Grades CSV");
        statsButton = new JButton("Class Stats");
        notifyButton = new JButton("Send Notification");
        showNotifButton = new JButton("Show Notifications");
        changePwdButton = new JButton("Change Password");

        right.add(loadStudentsButton, "span 4, growx, wrap");
        right.add(new JScrollPane(tableStudents), "span 4, grow, wrap");
        right.add(addGradeButton, "growx");
        right.add(computeFinalButton, "growx");
        right.add(exportGradesButton, "growx");
        right.add(importGradesButton, "growx, wrap");
        right.add(statsButton, "growx");
        right.add(notifyButton, "growx");
        right.add(showNotifButton, "growx");
        right.add(changePwdButton, "growx");

        add(left, "grow");
        add(right, "grow");
        add(banner, "dock north");
        add(notifBanner, "dock north");

        refreshLockState();
        new MaintenanceRefresher(() -> SwingUtilities.invokeLater(() -> {
            boolean on = new SettingsDao().isMaintenanceMode();
            banner.showBanner(on);
            refreshLockState();
        }));

        // actionlisteners
        loadSectionsButton.addActionListener(e -> loadSections());
        loadStudentsButton.addActionListener(e -> loadStudents());
        addGradeButton.addActionListener(e -> addGradeComponentDialog());
        computeFinalButton.addActionListener(e -> computeFinalGradeDialog());
        exportGradesButton.addActionListener(e -> exportGradesCSV());
        importGradesButton.addActionListener(e -> importGradesCSV());
        statsButton.addActionListener(e -> showStats());
        notifyButton.addActionListener(e ->
                new NotificationDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true));
        showNotifButton.addActionListener(e -> showNotifications());
        changePwdButton.addActionListener(e -> showChangePasswordDialog());

        NotificationListener.register(() -> notifBanner.showMessage("New notification received!"));
    }


 private void loadSections() {
    try {
        List<String[]> rows = svc.mySections();   

        if (rows.size() <= 1) {
            UIError.info("You have no assigned sections.");
            return;
        }

        String[] cols = rows.get(0);

        String[][] data = rows.subList(1, rows.size()).toArray(new String[0][]);

        tableSections.setModel(new javax.swing.table.DefaultTableModel(data, cols));

        AutoTableResize.autoResize(tableSections);

    } catch (Exception ex) {
        UIError.show(ex);
    }
}

    private String promptNonEmpty(String title, String message) {
        while (true) {
            String input = JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE);
            if (input == null) return null;
            input = input.trim();
            if (!input.isEmpty()) return input;
            UIError.info("Value cannot be empty.");
        }
    }

    private Integer promptInt(String title, String message, boolean positiveOnly) {
        while (true) {
            String input = JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE);
            if (input == null) return null;

            try {
                int val = Integer.parseInt(input.trim());
                if (positiveOnly && val <= 0) {
                    UIError.info("Value must be positive.");
                    continue;
                }
                return val;
            } catch (Exception e) {
                UIError.info("Invalid number.");
            }
        }
    }

    private String promptChoice(String title, String message, String[] options) {
        Object result = JOptionPane.showInputDialog(
                this, message, title,
                JOptionPane.PLAIN_MESSAGE, null, options,
                options[0]
        );
        return result == null ? null : result.toString();
    }

    private Integer getSelectedSectionId() {
        int row = tableSections.getSelectedRow();
        if (row == -1) {
            UIError.info("Select a section first.");
            return null;
        }
        try {
            return Integer.parseInt(tableSections.getValueAt(row, 0).toString());
        } catch (Exception e) {
            UIError.info("Invalid section selected.");
            return null;
        }
    }

    private Integer getSelectedEnrollmentId() {
        int row = tableStudents.getSelectedRow();
        if (row == -1) {
            UIError.info("Select a student first.");
            return null;
        }
        try {
            return Integer.parseInt(tableStudents.getValueAt(row, 0).toString());
        } catch (Exception e) {
            UIError.info("Invalid student selected.");
            return null;
        }
    }

    private void loadStudents() {
        try {
            Integer sectionId = getSelectedSectionId();
            if (sectionId == null) return;

            List<Enrollment> list = svc.sectionEnrollments(sectionId);
            var compsMap = svc.getComponentsForSection(sectionId);

            // unique component names
            Set<String> compNames = new TreeSet<>();
            for (var entry : compsMap.entrySet()) {
                for (GradeComponent gc : entry.getValue()) {
                    compNames.add(gc.getName());
                }
            }

            int baseCols = 5;
            String[] cols = new String[baseCols + compNames.size()];

            cols[0] = "Enrollment ID";
            cols[1] = "Roll No";
            cols[2] = "Student Mail";
            cols[3] = "Status";
            cols[4] = "Final Grade";

            int idx = 5;
            for (String name : compNames) cols[idx++] = name;

            String[][] data = new String[list.size()][cols.length];

            for (int i = 0; i < list.size(); i++) {
                Enrollment e = list.get(i);

                data[i][0] = String.valueOf(e.getEnrollmentId());
                data[i][1] = e.getStudentRollNo();
                data[i][2] = e.getStudentEmail();
                data[i][3] = e.getStatus().name();
                data[i][4] = e.getFinalGrade() == null ? "-" : e.getFinalGrade();

                var comps = compsMap.getOrDefault(e.getEnrollmentId(), List.of());
                Map<String, String> scoreMap = new HashMap<>();

                for (GradeComponent gc : comps) {
                    scoreMap.put(gc.getName(), gc.getScore() + "/" + gc.getMaxScore());
                }

                idx = 5;
                for (String name : compNames) {
                    data[i][idx++] = scoreMap.getOrDefault(name, "-");
                }
            }

            tableStudents.setModel(new javax.swing.table.DefaultTableModel(data, cols));
            AutoTableResize.autoResize(tableStudents);

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }
     private void addGradeComponentDialog() {
        if (isLocked()) {
            UIError.info("Maintenance Mode: cannot add grades.");
            return;
        }

        try {
            Integer enrollmentId = getSelectedEnrollmentId();
            if (enrollmentId == null) return;

            String[] types = {"QUIZ", "MIDSEM", "ENDSEM", "ASSIGNMENT", "PROJECT"};
            String type = promptChoice("Grade Input", "Select Component Type:", types);
            if (type == null) return;

            String name = promptNonEmpty("Grade Input", "Component Name:");
            if (name == null) return;

            Integer score = promptInt("Grade Input", "Score:", true);
            if (score == null) return;

            Integer maxScore = promptInt("Grade Input", "Max Score:", true);
            if (maxScore == null) return;
            if (maxScore < score) {
                UIError.info("Max Score cannot be smaller than Score.");
                return;
            }

            Integer weight = promptInt("Grade Input", "Weightage (%)", true);
            if (weight == null) return;
            if (weight > 100) {
                UIError.info("Weightage cannot exceed 100%.");
                return;
            }

            GradeComponent gc = switch (GradeType.valueOf(type)) {
                case QUIZ       -> new QuizComponent(name, score, maxScore, weight);
                case MIDSEM     -> new MidsemComponent(name, score, maxScore, weight);
                case ENDSEM     -> new EndsemComponent(name, score, maxScore, weight);
                case ASSIGNMENT -> new AssignmentComponent(name, score, maxScore, weight);
                case PROJECT    -> new ProjectComponent(name, score, maxScore, weight);
            };

            svc.addGradeComponent(enrollmentId, gc);
            UIError.info("Component added successfully.");
            loadStudents();

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }

    private void computeFinalGradeDialog() {
        if (isLocked()) {
            UIError.info("Maintenance Mode: cannot compute final grade.");
            return;
        }

        try {
            Integer enrollmentId = getSelectedEnrollmentId();
            if (enrollmentId == null) return;

            String grade = svc.computeFinalGrade(enrollmentId);
            UIError.info("Final Grade: " + grade);

            loadStudents();
        } catch (Exception ex) {
            UIError.show(ex);
        }
    }

    private void exportGradesCSV() {
        try {
            Integer sectionId = getSelectedSectionId();
            if (sectionId == null) return;

            List<String[]> rows = svc.exportSectionGrades(sectionId);

            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("grades.csv"));

            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return;

            File file = fc.getSelectedFile();
            if (file == null) {
                UIError.info("Invalid file selected.");
                return;
            }

            edu.univ.erp.util.export.CSVExporter.writeCSV(file.getAbsolutePath(), rows);
            UIError.info("CSV Exported.");

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }

    private void importGradesCSV() {
        if (isLocked()) {
            UIError.info("Maintenance Mode: cannot import grades.");
            return;
        }

        try {
            Integer sectionId = getSelectedSectionId();
            if (sectionId == null) return;

            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
                return;

            File file = fc.getSelectedFile();
            if (file == null || !file.exists() || !file.canRead()) {
                UIError.info("Invalid CSV file.");
                return;
            }

            svc.importGradesFromCSV(sectionId, file.toPath());
            UIError.info("CSV Imported.");
            loadStudents();

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }

 private void showStats() {
    try {
        Integer sectionId = getSelectedSectionId();
        if (sectionId == null) return;

        int sectionIdInt = sectionId.intValue();

        var stats = svc.classStats(sectionIdInt);

        String msg = String.format("""
                Class Statistics:

                Average: %.2f
                Minimum: %.2f
                Maximum: %.2f
                """,
                stats.get("average"),
                stats.get("min"),
                stats.get("max"));

        JOptionPane.showMessageDialog(this, msg, "Class Stats",
                JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception ex) {
        UIError.show(ex);
    }
}
    private void showChangePasswordDialog() {
        try {
            JPasswordField oldP = new JPasswordField();
            JPasswordField newP = new JPasswordField();
            JPasswordField confP = new JPasswordField();

            Object[] fields = {
                    "Old Password:", oldP,
                    "New Password:", newP,
                    "Confirm New:", confP
            };

            int ok = JOptionPane.showConfirmDialog(this, fields,
                    "Change Password", JOptionPane.OK_CANCEL_OPTION);

            if (ok != JOptionPane.OK_OPTION)
                return;

            String old = new String(oldP.getPassword());
            String nw = new String(newP.getPassword());
            String cf = new String(confP.getPassword());

            if (old.isBlank() || nw.isBlank() || cf.isBlank()) {
                UIError.info("Fields cannot be empty.");
                return;
            }

            if (!nw.equals(cf)) {
                UIError.info("Passwords do not match.");
                return;
            }

            new AuthService().changePassword(
                    SessionManager.getCurrentUser().getUserId(),
                    old, nw);

            UIError.info("Password changed successfully.");

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }

    private void showNotifications() {
        try {
            List<Notification> list = notifSvc.getMyNotifications();

            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No notifications.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Notification n : list) {
                sb.append("[").append(n.getCreatedAt()).append("] ")
                        .append(n.getTitle()).append("\n")
                        .append(n.getMessage()).append("\n\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(),
                    "Notifications", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            UIError.show(ex);
        }
    }
     private void refreshLockState() {
        boolean readonly = new SettingsDao().isMaintenanceMode();

        loadSectionsButton.setEnabled(true);
        loadStudentsButton.setEnabled(true);
        statsButton.setEnabled(true);
        showNotifButton.setEnabled(true);

        addGradeButton.setEnabled(!readonly);
        computeFinalButton.setEnabled(!readonly);
        importGradesButton.setEnabled(!readonly);
        exportGradesButton.setEnabled(!readonly);
        notifyButton.setEnabled(!readonly);
        changePwdButton.setEnabled(!readonly);
    }

    private boolean isLocked() {
        return new SettingsDao().isMaintenanceMode();
    }
}
