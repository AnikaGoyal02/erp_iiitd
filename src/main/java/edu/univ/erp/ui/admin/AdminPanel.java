package edu.univ.erp.ui.admin;

import edu.univ.erp.domain.*;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.*;
import edu.univ.erp.data.*;
import edu.univ.erp.auth.AuthService;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.ui.components.MaintenanceBanner;
import edu.univ.erp.ui.components.MaintenanceRefresher;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import java.io.File;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminPanel extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(AdminPanel.class);

  private final AdminService svc = new AdminService();
  private final MaintenanceBanner banner = new MaintenanceBanner();

  private static final String[] SEMESTERS = { "Summer", "Winter", "Monsoon" };

  public AdminPanel() {
    log.debug("Initializing AdminPanel");

    setLayout(new MigLayout("fillx, insets 12", "[grow]", "[][][][][][]"));

    add(banner, "dock north");
    new MaintenanceRefresher(() -> SwingUtilities.invokeLater(() -> {
      boolean on = new SettingsDao().isMaintenanceMode();
      banner.showBanner(on);     // update banner
  }));
  
    log.debug("Maintenance banner attached");

    JButton createUserButton = new JButton("Create User");
    JButton deleteUserButton = new JButton("Delete User");
    JButton createCourseButton = new JButton("Create Course");
    JButton deleteSectionButton = new JButton("Delete Section");
    JButton editCourseButton = new JButton("Edit Course");
    JButton createSectionButton = new JButton("Create Section");
    JButton editSectionButton = new JButton("Edit Section");
    JButton assignInstructorButton = new JButton("Assign Instructor");
    JButton maintenanceToggleButton = new JButton("Toggle Maintenance Mode");
    JButton backupButton = new JButton("Backup DB");
    JButton restoreButton = new JButton("Restore DB");
    JButton setAddDeadlineButton = new JButton("Set Add Deadline");
    JButton setDropDeadlineButton = new JButton("Set Drop Deadline");
    JButton notifyButton = new JButton("Send Notification");
    JButton changePwdButton = new JButton("Change Password");

    add(createUserButton, "growx, wrap");
    add(deleteUserButton, "growx, wrap");
    add(createCourseButton, "growx, wrap");
    add(deleteSectionButton, "growx, wrap");
    add(editCourseButton, "growx, wrap");
    add(createSectionButton, "growx, wrap");
    add(editSectionButton, "growx, wrap");
    add(assignInstructorButton, "growx, wrap");
    add(maintenanceToggleButton, "growx, wrap");
    add(backupButton, "growx, wrap");
    add(restoreButton, "growx, wrap");
    add(setAddDeadlineButton, "growx, wrap");
    add(setDropDeadlineButton, "growx, wrap");
    add(changePwdButton, "growx, wrap");
    add(notifyButton, "growx, wrap");

    createUserButton.addActionListener(e -> createUserDialog());
    deleteUserButton.addActionListener(e -> deleteUserDialog());
    deleteSectionButton.addActionListener(e -> deleteSectionDialog());
    createCourseButton.addActionListener(e -> createCourseDialog());
    editCourseButton.addActionListener(e -> editCourseDialog());
    createSectionButton.addActionListener(e -> createSectionDialog());
    editSectionButton.addActionListener(e -> editSectionDialog());
    assignInstructorButton.addActionListener(e -> assignInstructorDialog());
    maintenanceToggleButton.addActionListener(e -> toggleMaintenance());
    backupButton.addActionListener(e -> backupDialog());
    restoreButton.addActionListener(e -> restoreDialog());
    setAddDeadlineButton.addActionListener(e -> setAddDeadline());
    setDropDeadlineButton.addActionListener(e -> setDropDeadline());

    notifyButton.addActionListener(e ->
        new NotificationDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true));
    changePwdButton.addActionListener(e -> showChangePasswordDialog());

    log.debug("AdminPanel initialized successfully");
  }


  private Integer askingPositiveInt(String message) {
    String input = JOptionPane.showInputDialog(message);
    if (input == null) return null;
    input = input.trim();
    if (input.isEmpty()) {
      UIError.info("Value cannot be empty.");
      return null;
    }
    try {
      int val = Integer.parseInt(input);
      if (val <= 0) {
        UIError.info("Value must be a positive number.");
        return null;
      }
      return val;
    } catch (Exception e) {
      UIError.info("Invalid number.");
      return null;
    }
  }

  private String askNonEmpty(String message) {
    String input = JOptionPane.showInputDialog(message);
    if (input == null) return null;
    input = input.trim();
    if (input.isEmpty()) {
      UIError.info("Value cannot be empty.");
      return null;
    }
    return input;
  }

  private String askCourseCode(String message) {
    String input = JOptionPane.showInputDialog(message);
    if (input == null) return null;

    input = input.trim().toUpperCase();
    if (!input.matches("[A-Z0-9]+")) {
      UIError.info("Course code must be CAPITAL letters and digits only, no spaces.");
      return null;
    }
    return input;
  }

  

  private void createUserDialog() {
    try {
      String username = askNonEmpty("Username:");
      if (username == null) return;

      if (svc.userExists(username)) {
        UIError.info("Username already exists.");
        return;
      }

      String password = askNonEmpty("Password:");
      if (password == null) return;

      String[] roles = { "ADMIN", "INSTRUCTOR", "STUDENT" };
      String roleStr = (String) JOptionPane.showInputDialog(
          this, "Role:", "Select Role",
          JOptionPane.PLAIN_MESSAGE, null, roles, roles[0]);
      if (roleStr == null) return;

      Role role = Role.valueOf(roleStr);

      
      String roll = null, program = null, email = null, dept = null;
      String yearStr = null;

      if (role == Role.STUDENT) {
        Integer rollVal = askingPositiveInt("Roll Number:");
        if (rollVal == null) return;
        roll = rollVal.toString();

        program = askNonEmpty("Program:");
        if (program == null) return;

        Integer y = askingPositiveInt("Year:");
        if (y == null) return;
        yearStr = y.toString();

        email = askNonEmpty("Email:");
        if (email == null) return;
      }

      if (role == Role.INSTRUCTOR) {
        dept = askNonEmpty("Department:");
        if (dept == null) return;

        email = askNonEmpty("Email:");
        if (email == null) return;
      }

      svc.createUserWithProfile(
          username,
          password,
          role,
          roll,
          program,
          yearStr,
          email,
          dept,
          email // instructor email
      );

      UIError.info("User created successfully.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void deleteUserDialog() {
    try {
      String username = JOptionPane.showInputDialog(
          this, "Enter username to delete:", "Delete User", JOptionPane.PLAIN_MESSAGE);

      if (username == null || username.isBlank()) return;

      int confirm = JOptionPane.showConfirmDialog(
          this,
          "Delete user \"" + username + "\" and ALL related records?",
          "Confirm Delete",
          JOptionPane.YES_NO_OPTION
      );

      if (confirm != JOptionPane.YES_OPTION) return;

      svc.deleteUserByUsername(username.trim());

      UIError.info("User deleted successfully.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void createCourseDialog() {
    try {
        String code = askCourseCode("Course Code (CAPS & digits only):");
        if (code == null) return;

        String title = askNonEmpty("Title:");
        if (title == null) return;

        String description = askNonEmpty("Description:");
        if (description == null) return;

        Integer creditsObj = askingPositiveInt("Credits:");
        if (creditsObj == null) return;

        svc.createCourse(code, title, description, creditsObj);

        UIError.info("Course created.");

    } catch (Exception ex) {
        UIError.show(ex);
    }
}



  private void editCourseDialog() {
    try {
        String cc = askCourseCode("Enter Course Code to edit:");
        if (cc == null) return;

        Course c = new CourseDao().findByCode(cc);
        if (c == null) {
            UIError.info("Course not found.");
            return;
        }

        String code = askCourseCode("New Code (" + c.getCode() + "):");
        if (code == null) return;

        String title = askNonEmpty("New Title (" + c.getTitle() + "):");
        if (title == null) return;

        String description = askNonEmpty("New Description (currently: "
                + (c.getDescription() == null ? "none" : c.getDescription()) + "):");
        if (description == null) return;

        Integer creditsObj = askingPositiveInt("New Credits (" + c.getCredits() + "):");
        if (creditsObj == null) return;

        svc.updateCourse(
            c.getCourseId(),
            code,
            title,
            description,
            creditsObj
        );

        UIError.info("Course updated.");

    } catch (Exception ex) {
        UIError.show(ex);
    }
}


  private String askSemesterDropdown(String current) {
    String[] choices = { "Summer", "Winter", "Monsoon" };
    return (String) JOptionPane.showInputDialog(
        this,
        current == null ? "Select Semester:" : "Semester (" + current + "):",
        "Semester",
        JOptionPane.PLAIN_MESSAGE,
        null,
        choices,
        current == null ? choices[0] : current
    );
  }

  private void createSectionDialog() {
    try {
      String courseCode = askCourseCode("Course Code (e.g., CSE121):");
      if (courseCode == null) return;

      Course course = new CourseDao().findByCode(courseCode);
      if (course == null) {
        UIError.info("Course not found.");
        return;
      }

      int courseId = course.getCourseId();

      String email = JOptionPane.showInputDialog("Instructor Email (blank = none):");
      Integer instructorId = null;

      if (email != null && !email.isBlank()) {
        instructorId = new InstructorDao().findInstructorIdByEmail(email.trim());
        if (instructorId == null) {
          UIError.info("No instructor found with this email.");
          return;
        }
      }

      String dayTime = askNonEmpty("Day/Time (e.g., Tue/Thu 15:00-16:30):");
      if (dayTime == null) return;
      
      if (!isValidDayTime(dayTime)) {
          UIError.info("Invalid format.\nExample: Tue/Thu 15:00-16:30");
          return;
      }
      

      String room = askNonEmpty("Room:");
      if (room == null) return;

      Integer capObj = askingPositiveInt("Capacity:");
      if (capObj == null) return;

      String semester = askSemesterDropdown(null);
      if (semester == null) return;

      Integer yearObj = askingPositiveInt("Year:");
      if (yearObj == null) return;

      svc.createSection(courseId, instructorId, dayTime, room, capObj, semester, yearObj);

      UIError.info("Section created.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void deleteSectionDialog() {
    try {
      String courseCode = askCourseCode("Course Code:");
      if (courseCode == null) return;

      Course course = new CourseDao().findByCode(courseCode);
      if (course == null) {
        UIError.info("Course not found.");
        return;
      }

      java.util.List<Section> sections = new SectionDao().findByCourse(course.getCourseId());
      if (sections.isEmpty()) {
        UIError.info("No sections exist for this course.");
        return;
      }

      Section selected;

      if (sections.size() == 1) {
        selected = sections.get(0);
      } else {
        String[] opts = sections.stream()
            .map(sec -> "ID " + sec.getSectionId() + " (" + sec.getSemester() + " " + sec.getYear() + ")")
            .toArray(String[]::new);

        String choice = (String)
            JOptionPane.showInputDialog(this, "Select Section:", "Delete Section",
            JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

        if (choice == null) return;

        int idx = java.util.Arrays.asList(opts).indexOf(choice);
        selected = sections.get(idx);
      }

      int sid = selected.getSectionId();
      int enrolled = svc.countEnrollmentsForSection(sid);

      if (enrolled > 0) {
        UIError.info("Cannot delete — " + enrolled + " students are enrolled.");
        return;
      }

      int confirm = JOptionPane.showConfirmDialog(
          this, "Delete Section ID " + sid + "?", "Confirm",
          JOptionPane.YES_NO_OPTION);

      if (confirm != JOptionPane.YES_OPTION) return;

      svc.deleteSection(sid);
      UIError.info("Section deleted.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void editSectionDialog() {
    try {
      String courseCode = askCourseCode("Course Code:");
      if (courseCode == null) return;

      Course course = new CourseDao().findByCode(courseCode);
      if (course == null) {
        UIError.info("Course not found.");
        return;
      }

      java.util.List<Section> sections = new SectionDao().findByCourse(course.getCourseId());
      if (sections.isEmpty()) {
        UIError.info("No sections exist.");
        return;
      }

      Section s;

      if (sections.size() == 1) {
        s = sections.get(0);
      } else {
        String[] opts = sections.stream()
            .map(sec -> "Section ID: " + sec.getSectionId())
            .toArray(String[]::new);

        String choice = (String)
            JOptionPane.showInputDialog(this, "Select Section:", "Edit Section",
            JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

        if (choice == null) return;

        int idx = java.util.Arrays.asList(opts).indexOf(choice);
        s = sections.get(idx);
      }

      int sid = s.getSectionId();

      String email = JOptionPane.showInputDialog("Instructor Email (" + s.getInstructorId() + "):");
      Integer instrId = null;

      if (email != null && !email.isBlank()) {
        instrId = new InstructorDao().findInstructorIdByEmail(email.trim());
        if (instrId == null) {
          UIError.info("No instructor found with this email.");
          return;
        }
      }

      String dayTime = askNonEmpty("Day/Time (" + s.getDayTime() + "):");
      if (dayTime == null) return;

      String room = askNonEmpty("Room (" + s.getRoom() + "):");
      if (room == null) return;

      Integer capObj = askingPositiveInt("Capacity (" + s.getCapacity() + "):");
      if (capObj == null) return;

      String semester = askSemesterDropdown(s.getSemester());
      if (semester == null) return;

      Integer yearObj = askingPositiveInt("Year (" + s.getYear() + "):");
      if (yearObj == null) return;

      svc.updateSection(sid, instrId, dayTime, room, capObj, semester, yearObj);

      UIError.info("Section updated.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private boolean isValidDayTime(String s) {
    if (s == null) return false;

    String regex =
        "^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(/(Mon|Tue|Wed|Thu|Fri|Sat|Sun))?\\s" +
        "[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]$";

    return s.matches(regex);
}


  private void assignInstructorDialog() {
    try {
      String courseCode = askCourseCode("Course Code:");
      if (courseCode == null) return;

      Course course = new CourseDao().findByCode(courseCode);
      if (course == null) {
        UIError.info("Course not found.");
        return;
      }

      java.util.List<Section> sections = new SectionDao().findByCourse(course.getCourseId());
      if (sections.isEmpty()) {
        UIError.info("No sections exist.");
        return;
      }

      Section selected;

      if (sections.size() == 1) {
        selected = sections.get(0);
      } else {
        String[] opts = sections.stream()
            .map(sec -> "Section ID: " + sec.getSectionId())
            .toArray(String[]::new);

        String choice = (String)
            JOptionPane.showInputDialog(this, "Select Section:", "Choose Section",
            JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

        if (choice == null) return;

        int idx = java.util.Arrays.asList(opts).indexOf(choice);
        selected = sections.get(idx);
      }

      int sid = selected.getSectionId();

      String email = askNonEmpty("Instructor Email:");
      if (email == null) return;
      email = email.trim();
      String currentEmail = selected.getInstructorEmail();
      if (currentEmail != null && currentEmail.equalsIgnoreCase(email)) {
        UIError.info("This instructor is already assigned to this section.");
        return;
      }

      Integer instrId = new InstructorDao().findInstructorIdByEmail(email);
      if (instrId == null) {
        UIError.info("No instructor found with this email.");
        return;
      }

      svc.assignInstructorByEmail(sid, email);
      UIError.info("Instructor assigned.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
}


  private void toggleMaintenance() {
    try {
      boolean state = svc.toggleMaintenanceMode();
      UIError.info("Maintenance mode is now: " + (state ? "ON" : "OFF"));
    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void backupDialog() {
    try {
      JFileChooser fc = new JFileChooser();
      fc.setSelectedFile(new File("erp_backup.sql"));

      if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        return;

      String path = fc.getSelectedFile().getAbsolutePath();

      String dbUser = askNonEmpty("DB user:");
      if (dbUser == null) return;

      String dbPass = askNonEmpty("DB password:");
      if (dbPass == null) return;

      String out = svc.backupERPDatabase(
          path,
          dbUser.isBlank() ? System.getProperty("erp.user", "root") : dbUser,
          dbPass.isBlank() ? System.getProperty("erp.pass", "root") : dbPass
      );

      UIError.info("Backup written to:\n" + out);

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void restoreDialog() {
    try {
      JFileChooser fc = new JFileChooser();
      if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        return;

      String path = fc.getSelectedFile().getAbsolutePath();

      int ok = JOptionPane.showConfirmDialog(
          this,
          "This will overwrite ALL DB data.\nContinue?",
          "Confirm Restore",
          JOptionPane.YES_NO_OPTION
      );

      if (ok != JOptionPane.YES_OPTION) return;

      String dbUser = askNonEmpty("DB user:");
      if (dbUser == null) return;

      String dbPass = askNonEmpty("DB password:");
      if (dbPass == null) return;

      svc.restoreERPDatabase(
          path,
          dbUser.isBlank() ? System.getProperty("erp.user", "root") : dbUser,
          dbPass.isBlank() ? System.getProperty("erp.pass", "root") : dbPass
      );

      UIError.info("Database restored successfully.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void showChangePasswordDialog() {
    try {
      JPasswordField oldP = new JPasswordField();
      JPasswordField newP = new JPasswordField();
      JPasswordField confP = new JPasswordField();

      Object[] form = {
          "Old Password:", oldP,
          "New Password:", newP,
          "Confirm New Password:", confP
      };

      int ok = JOptionPane.showConfirmDialog(
          this,
          form,
          "Change Password",
          JOptionPane.OK_CANCEL_OPTION
      );

      if (ok != JOptionPane.OK_OPTION) return;

      String old = new String(oldP.getPassword());
      String nw = new String(newP.getPassword());
      String cf = new String(confP.getPassword());

      if (old.isBlank() || nw.isBlank() || cf.isBlank()) {
        UIError.info("No field can be empty.");
        return;
      }

      if (!nw.equals(cf)) {
        UIError.info("New passwords do not match.");
        return;
      }

      new AuthService().changePassword(
          SessionManager.getCurrentUser().getUserId(),
          old,
          nw
      );

      UIError.info("Password changed successfully.");

    } catch (Exception ex) {
      UIError.show(ex);
    }
  }

  private void setAddDeadline() {
    String input = JOptionPane.showInputDialog("Enter Add Deadline (DD:MM:YYYY):");
    if (input == null) return;

    SettingsDao dao = new SettingsDao();
    LocalDate d = dao.parseDMY(input);

    if (d == null) {
      UIError.info("Invalid date. Use DD:MM:YYYY.");
      return;
    }

    dao.setAddDeadline(d);
    UIError.info("Add deadline saved: " + dao.formatDMY(d));
  }

  private void setDropDeadline() {
    String input = JOptionPane.showInputDialog("Enter Drop Deadline (DD:MM:YYYY):");
    if (input == null) return;

    SettingsDao dao = new SettingsDao();
    LocalDate d = dao.parseDMY(input);

    if (d == null) {
      UIError.info("Invalid date. Use DD:MM:YYYY.");
      return;
    }

    dao.setDropDeadline(d);
    UIError.info("Drop deadline saved: " + dao.formatDMY(d));
  }
}
