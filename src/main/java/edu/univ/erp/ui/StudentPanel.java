package edu.univ.erp.ui;

import edu.univ.erp.domain.*;
import edu.univ.erp.util.AutoTableResize;
import edu.univ.erp.domain.grades.GradeComponent;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.ui.*;
import edu.univ.erp.ui.timetable.TimetableFrame;
import edu.univ.erp.ui.components.MaintenanceBanner;
import edu.univ.erp.ui.components.MaintenanceRefresher;
import edu.univ.erp.ui.components.DeadlineRefresher;
import edu.univ.erp.auth.AuthService;
import edu.univ.erp.auth.SessionManager;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import edu.univ.erp.ui.components.NotificationBanner;
import edu.univ.erp.service.NotificationListener;
import edu.univ.erp.service.NotificationService;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentPanel extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(StudentPanel.class);

  private final StudentService svc = new StudentService();
  private final MaintenanceBanner banner = new MaintenanceBanner();
  private final NotificationBanner toast = new NotificationBanner();

  private JTable tableCatalog;
  private JTable tableSections;
  private JTable tableEnrollments;

  private JButton viewCatalogButton;
  private JButton viewSectionsButton;
  private JButton registerButton;
  private JButton dropButton;
  private JButton viewGradesButton;
  private JButton viewTimetableButton;
  private JButton transcriptButton;
  private JButton changePwdButton;
  private JButton showNotifButton;

  public StudentPanel() {
    log.info("Initializing StudentPanel UI");

    setLayout(new MigLayout("fill, insets 15", "[grow][grow]", "[grow]"));

    tableCatalog = new JTable();
    tableCatalog.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    tableSections = new JTable();
    tableSections.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    tableEnrollments = new JTable();
    tableEnrollments.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    JPanel left = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow][][grow]"));

    viewCatalogButton = new JButton("View Catalog");
    left.add(viewCatalogButton, "wrap");
    left.add(new JScrollPane(tableCatalog), "grow, wrap");

    viewSectionsButton = new JButton("View Sections");
    left.add(viewSectionsButton, "wrap");
    left.add(new JScrollPane(tableSections), "grow");

    JPanel right = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow][][][][][]"));

    viewEnrollments();
    registerButton = new JButton("Register");
    dropButton = new JButton("Drop");
    viewGradesButton = new JButton("View Grades");
    viewTimetableButton = new JButton("View Timetable");
    transcriptButton = new JButton("Download Transcript");
    showNotifButton = new JButton("Show Notifications");
    changePwdButton = new JButton("Change Password");

    right.add(new JLabel("My Enrollments:"), "wrap");
    right.add(new JScrollPane(tableEnrollments), "grow, wrap");
    right.add(registerButton, "growx, split 6");
    right.add(dropButton, "growx");
    right.add(viewGradesButton, "growx");
    right.add(viewTimetableButton, "growx");
    right.add(transcriptButton, "growx");
    right.add(showNotifButton, "growx, wrap");
    right.add(changePwdButton, "growx");

    add(left, "grow");
    add(right, "grow");

    add(banner, "dock north");
    add(toast, "dock north");

    refreshLockState();
    new MaintenanceRefresher(() -> SwingUtilities.invokeLater(() -> {
      boolean on = new SettingsDao().isMaintenanceMode();
      banner.showBanner(on);    
      refreshLockState();        
      refreshDeadlineState();  
  }));
  

    // listeners
    viewCatalogButton.addActionListener(e -> loadCatalog());
    viewSectionsButton.addActionListener(e -> loadSections());
    registerButton.addActionListener(e -> registerInSection());
    dropButton.addActionListener(e -> dropSection());
    viewGradesButton.addActionListener(e -> viewGradesDialog());
    viewTimetableButton.addActionListener(e -> showTimetable());
    transcriptButton.addActionListener(e -> downloadTranscript());
    showNotifButton.addActionListener(e -> showNotifications());
    changePwdButton.addActionListener(e -> showChangePasswordDialog());

    viewEnrollments();

    NotificationListener.register(() ->
      toast.showMessage("New notification received!"));
  

  refreshDeadlineState();

new DeadlineRefresher(() -> SwingUtilities.invokeLater(() -> {
    refreshDeadlineState();
}));
 
  }

  private void refreshLockState() {
    log.debug("Refreshing lock state (maintenance mode)");
    boolean readonly = new SettingsDao().isMaintenanceMode();
    registerButton.setEnabled(!readonly);
    dropButton.setEnabled(!readonly);
    changePwdButton.setEnabled(!readonly);
    viewGradesButton.setEnabled(true);
    viewTimetableButton.setEnabled(true);
    transcriptButton.setEnabled(true);
    showNotifButton.setEnabled(true);

    SettingsDao dao = new SettingsDao();
    LocalDate today = LocalDate.now();

    LocalDate addDeadline = dao.getAddDeadline();
    LocalDate dropDeadline = dao.getDropDeadline();

    if (addDeadline != null && today.isAfter(addDeadline)) {
        registerButton.setEnabled(false);
    }

    if (dropDeadline != null && today.isAfter(dropDeadline)) {
        dropButton.setEnabled(false);
    }

  }

  private boolean isLocked() {
    return new SettingsDao().isMaintenanceMode();
  }

  private void loadCatalog() {
    log.info("Loading course catalog");
    try {
      List<Course> list = svc.browseCatalog();
      String[] cols = { "ID", "Code", "Title", "Credits" };
      String[][] data = new String[list.size()][4];

      for (int i = 0; i < list.size(); i++) {
        Course c = list.get(i);
        data[i][0] = "" + c.getCourseId();
        data[i][1] = c.getCode() == null ? "" : c.getCode();
        data[i][2] = c.getTitle() == null ? "" : c.getTitle();
        data[i][3] = "" + (c.getCredits());
      }

      tableCatalog.setModel(new javax.swing.table.DefaultTableModel(data, cols));
      AutoTableResize.autoResize(tableCatalog);

    } catch (Exception ex) {
      log.error("Failed to load catalog", ex);
      UIError.show(ex);
    }
  }


  private void loadSections() {
    log.info("Loading sections for selected course");
    try {
        int row = tableCatalog.getSelectedRow();
        if (row == -1) {
            UIError.show(new Exception("Select a course."));
            return;
        }

        Optional<String> maybeId = safeGetTableValue(tableCatalog, row, 0);
        if (maybeId.isEmpty()) {
            UIError.show(new Exception("Invalid course selected."));
            return;
        }

        int cid = Integer.parseInt(maybeId.get().trim());
        List<String[]> rows = svc.listSections(cid);

        String[] cols = { "Section ID", "Instructor", "Time", "Room", "Capacity", "Semester", "Year" };

        String[][] data = new String[rows.size()][7];
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);

            for (int j = 0; j < 7; j++) {
                data[i][j] = (r != null && j < r.length && r[j] != null) ? r[j] : "";
            }
        }

        tableSections.setModel(new javax.swing.table.DefaultTableModel(data, cols));

        tableSections.getColumnModel().getColumn(0).setMinWidth(0);
        tableSections.getColumnModel().getColumn(0).setMaxWidth(0);
        tableSections.getColumnModel().getColumn(0).setWidth(0);

        AutoTableResize.autoResize(tableSections);

    } catch (Exception ex) {
        log.error("Failed to load sections", ex);
        UIError.show(ex);
    }
  }


  

  private void viewEnrollments() {
    log.info("Loading student enrollments");
    try {
      List<String[]> rows = svc.myEnrollments();

      String[] cols = { "Course Code", "Title", "Instructor", "Credits", "Final Grade" };

      String[][] data = new String[rows.size()][5];

      for (int i = 0; i < rows.size(); i++) {
        String[] r = rows.get(i);
        data[i][0] = (r.length > 2 && r[2] != null) ? r[2] : "";
        data[i][1] = (r.length > 3 && r[3] != null) ? r[3] : "";
        data[i][2] = (r.length > 4 && r[4] != null) ? r[4] : "";
        data[i][3] = (r.length > 5 && r[5] != null) ? r[5] : "";
        data[i][4] = (r.length > 6 && r[6] != null) ? r[6] : "-";
      }

      tableEnrollments.setModel(new javax.swing.table.DefaultTableModel(data, cols));
      AutoTableResize.autoResize(tableEnrollments);

    } catch (Exception ex) {
      log.error("Failed to load enrollments", ex);
      UIError.show(ex);
    }
  }


  private void registerInSection() {
    if (isLocked()) {
        UIError.info("Maintenance Mode: cannot register.");
        return;
    }

    try {
        int row = tableSections.getSelectedRow();
        if (row == -1) {
            UIError.show(new Exception("Select a section."));
            return;
        }

        Optional<String> maybeSid = safeGetTableValue(tableSections, row, 0);
        if (maybeSid.isEmpty() || maybeSid.get().trim().isEmpty()) {
            UIError.show(new Exception("Invalid section selected."));
            return;
        }

        int sid = Integer.parseInt(maybeSid.get().trim());
        log.info("Registering student in section {}", sid);

        svc.register(sid);
        UIError.info("Registered.");
        viewEnrollments();

    } catch (Exception ex) {
        log.error("Failed to register in section", ex);
        UIError.show(ex);
    }
}

  private void dropSection() {
    if (isLocked()) {
      log.warn("Drop blocked: maintenance mode active");
      UIError.info("Maintenance Mode: cannot drop.");
      return;
    }

    try {
      int row = tableEnrollments.getSelectedRow();
      if (row == -1) {
        log.warn("Attempted drop with no enrollment selected");
        UIError.show(new Exception("Select an enrollment."));
        return;
      }

      List<String[]> all = svc.myEnrollments();
      if (row < 0 || row >= all.size()) {
        log.error("Selected enrollment row index out of range: {}", row);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      String[] selectedRow = all.get(row);
      if (selectedRow == null || selectedRow.length == 0 || selectedRow[0] == null) {
        log.error("Invalid enrollment data at row {}", row);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      String enrollmentIdStr = selectedRow[0].trim();
      if (enrollmentIdStr.isEmpty()) {
        log.error("Empty enrollment id at row {}", row);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      int sid;
      try {
        sid = Integer.parseInt(enrollmentIdStr); 
      } catch (NumberFormatException ex) {
        log.error("Enrollment id not numeric: {}", enrollmentIdStr);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      log.info("Dropping enrollment for section {}", sid);
      svc.drop(sid);

      UIError.info("Dropped.");
      viewEnrollments();

    } catch (Exception ex) {
      log.error("Failed to drop section", ex);
      UIError.show(ex);
    }
  }

  private void viewGradesDialog() {
    log.info("Viewing grades for selected enrollment");
    try {
      int row = tableEnrollments.getSelectedRow();
      if (row == -1) {
        log.warn("Attempt to view grades with no enrollment selected");
        UIError.show(new Exception("Select an enrollment."));
        return;
      }

      List<String[]> all = svc.myEnrollments();
      if (row < 0 || row >= all.size()) {
        log.error("Selected enrollment row out of bounds: {}", row);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      String[] r = all.get(row);
      if (r == null || r.length == 0 || r[0] == null) {
        log.error("Enrollment data invalid at row {}", row);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      String enrollmentIdStr = r[0].trim();
      int eid;
      try {
        eid = Integer.parseInt(enrollmentIdStr);
      } catch (NumberFormatException ex) {
        log.error("Invalid enrollment id format: {}", enrollmentIdStr);
        UIError.show(new Exception("Invalid enrollment selected."));
        return;
      }

      List<GradeComponent> comps = svc.viewGrades(eid);
      StringBuilder sb = new StringBuilder();

      for (GradeComponent gc : comps) {
        sb.append(gc.toString()).append("\n");
      }

      JOptionPane.showMessageDialog(this, sb.toString(), "Grades",
          JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception ex) {
      log.error("Failed to load grades", ex);
      UIError.show(ex);
    }
  }

  private void showTimetable() {
    log.info("Opening timetable view");
    try {
      List<Section> list = svc.myRegisteredSections();
      new TimetableFrame(list).setVisible(true);
    } catch (Exception ex) {
      log.error("Failed to show timetable", ex);
      UIError.show(ex);
    }
  }

  private void downloadTranscript() {
    log.info("Downloading transcript as PDF");

    try {
      var rows = svc.getTranscriptRows();

      rows.sort((a, b) -> {
        int cmp = Integer.compare(b.getYear(), a.getYear());
        if (cmp != 0) return cmp;
        return a.getSemester().compareToIgnoreCase(b.getSemester());
      });

      JFileChooser fc = new JFileChooser();
      fc.setSelectedFile(new File("transcript.pdf"));
      if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        return;

      String path = fc.getSelectedFile().getAbsolutePath();
      if (path == null || path.trim().isEmpty()) {
        log.error("Invalid path selected for transcript PDF");
        UIError.info("Invalid file path.");
        return;
      }

      log.debug("Saving transcript PDF to {}", path);

      Document doc = new Document();
      PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));

      doc.open();

      Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
      Paragraph title = new Paragraph("Academic Transcript", titleFont);
      title.setAlignment(Element.ALIGN_CENTER);
      title.setSpacingAfter(20);
      doc.add(title);

      String[] headers = { "Course Code", "Title", "Credits", "Semester", "Year", "Final Grade", "Grade Point" };

      PdfPTable table = new PdfPTable(headers.length);
      table.setWidthPercentage(100);

      Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);

      for (String h : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
        cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        cell.setPadding(5);
        table.addCell(cell);
      }

      Font bodyFont = new Font(Font.HELVETICA, 11);

      double totalPoints = 0;
      double totalCredits = 0;

      for (var r : rows) {
        int gp = letterToGradePoint(r.getFinalGrade());
        int credits = r.getCredits();

        if (gp > 0) {
          totalPoints += gp * credits;
          totalCredits += credits;
        }

        table.addCell(new PdfPCell(new Phrase(nullSafe(r.getCourseCode()), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(nullSafe(r.getCourseTitle()), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(String.valueOf(credits), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(nullSafe(r.getSemester()), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(String.valueOf(r.getYear()), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(nullSafe(r.getFinalGrade()), bodyFont)));
        table.addCell(new PdfPCell(new Phrase(String.valueOf(gp), bodyFont)));
      }

      doc.add(table);

      doc.add(new Paragraph("\n"));

      double cgpa = (totalCredits == 0) ? 0 : (totalPoints / totalCredits);

      Font cgpaFont = new Font(Font.HELVETICA, 14, Font.BOLD);

      Paragraph cgpaP = new Paragraph("CGPA: " + String.format("%.2f", cgpa), cgpaFont);
      cgpaP.setAlignment(Element.ALIGN_RIGHT);
      doc.add(cgpaP);

      doc.close();

      UIError.info("Transcript PDF saved.");
      log.info("Transcript PDF successfully saved");

    } catch (Exception ex) {
      log.error("Failed to generate transcript PDF", ex);
      UIError.show(ex);
    }
  }

  private void showChangePasswordDialog() {
    log.info("Opening change password dialog");

    try {
      if (isLocked()) {
        log.warn("Blocked password change: maintenance mode");
        UIError.info("Maintenance Mode: cannot change password.");
        return;
      }

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

      if (old == null || old.isBlank() ||
          nw  == null || nw.isBlank()  ||
          cf  == null || cf.isBlank()) {

        JOptionPane.showMessageDialog(this,
            "Empty inputs are not allowed",
            "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (!nw.equals(cf)) {
        log.warn("Password mismatch in change password dialog");
        UIError.info("Passwords do not match.");
        return;
      }

      log.info("Changing password for user {}", SessionManager.getCurrentUser().getUserId());

      new AuthService().changePassword(
          SessionManager.getCurrentUser().getUserId(),
          old, nw);

      UIError.info("Password changed.");

    } catch (Exception ex) {
      log.error("Failed to change password", ex);
      UIError.show(ex);
    }
  }

  private int letterToGradePoint(String letter) {
    if (letter == null) return 0;

    switch (letter.trim().toUpperCase()) {
      case "A+":
      case "A":
        return 10;
      case "A-":
        return 9;
      case "B":
        return 8;
      case "B-":
        return 7;
      case "C":
        return 6;
      case "D":
        return 5;
      case "F":
        return 4;
      default:
        return 0;
    }
  }

  private void showNotifications() {
    log.info("Showing notifications for student");
    try {
      List<Notification> list = new NotificationService().getMyNotifications();

      if (list.isEmpty()) {
        log.debug("Student has no notifications");
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
      log.error("Failed to display notifications", ex);
      UIError.show(ex);
    }
  }

  private Optional<String> safeGetTableValue(JTable table, int row, int col) {
    try {
      if (table == null) return Optional.empty();
      if (row < 0 || row >= table.getRowCount()) return Optional.empty();
      if (col < 0 || col >= table.getColumnCount()) {
        // Column might be hidden (model has more columns than view) — try to read from model
        try {
          Object v = table.getModel().getValueAt(row, col);
          return v == null ? Optional.empty() : Optional.of(v.toString());
        } catch (Exception ex) {
          return Optional.empty();
        }
      }
      Object val = table.getValueAt(row, col);
      return val == null ? Optional.empty() : Optional.of(val.toString());
    } catch (Exception e) {
      log.error("safeGetTableValue error row={},col={}", row, col, e);
      return Optional.empty();
    }
  }

  private String nullSafe(String s) {
    return s == null ? "" : s;
  }


  private void refreshDeadlineState() {
    SettingsDao dao = new SettingsDao();
    LocalDate today = LocalDate.now();

    LocalDate addDeadline = dao.getAddDeadline();
    LocalDate dropDeadline = dao.getDropDeadline();

    boolean canAdd = (addDeadline == null || !today.isAfter(addDeadline));
    boolean canDrop = (dropDeadline == null || !today.isAfter(dropDeadline));

    registerButton.setEnabled(canAdd && !isLocked());
    dropButton.setEnabled(canDrop && !isLocked());
}


}
