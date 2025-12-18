package edu.univ.erp.service;

import edu.univ.erp.auth.PasswordUtil;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import edu.univ.erp.exception.*;
import java.io.*;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {

  private static final Logger log = LoggerFactory.getLogger(AdminService.class);

  private final AccessControl ac = new AccessControl();
  private final SettingsDao settingsDao = new SettingsDao();
  private final AuthDao authDao = new AuthDao();
  private final StudentDao studentDao = new StudentDao();
  private final InstructorDao instructorDao = new InstructorDao();
  private final CourseDao courseDao = new CourseDao();
  private final SectionDao sectionDao = new SectionDao();

  private static final Pattern COURSE_CODE_RE = Pattern.compile("^[A-Z0-9]+$");
  private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  public void createUserWithProfile(
      String username,
      String password,
      Role role,
      String roll,
      String program,
      String yearStr,
      String stuEmail,
      String department,
      String instEmail
  ) {
    ac.requireRole(Role.ADMIN);

    log.info("Admin creating full user profile: username={}, role={}", username, role);


    validateUser(username, password, role);

    if (role == Role.STUDENT) {
      validateStudent(roll, program, yearStr, stuEmail);
    } else if (role == Role.INSTRUCTOR) {
      validateInstructor(department, instEmail);
    }


    int userId = authDao.insertUser(username, role, PasswordUtil.hash(password));
    log.info("User inserted into authdb userId={}", userId);

    try {
      if (role == Role.STUDENT) {
        int year = Integer.parseInt(yearStr.trim());
        studentDao.insert(userId, roll.trim(), program.trim(), year, stuEmail.trim());
      } else if (role == Role.INSTRUCTOR) {
        instructorDao.insert(userId, department.trim(), instEmail.trim());
      }

      log.info("Full user profile created successfully for userId={}", userId);

    } catch (Exception e) {
      log.error("Profile creation failed. Rolling back auth userId={}", userId, e);
      authDao.deleteUser(userId); 
      throw e;
    }
  }


  private void validateUser(String username, String password, Role role) {
    if (username == null || username.isBlank())
      throw new ValidationException("Username cannot be empty.");

    if (authDao.usernameExists(username))
      throw new ValidationException("Username already exists.");

    if (password == null || password.isBlank())
      throw new ValidationException("Password cannot be empty.");

    if (role == null)
      throw new ValidationException("Role must be selected.");
  }

  private void validateStudent(String roll, String program, String yearStr, String email) {
    if (roll == null || roll.isBlank())
      throw new ValidationException("Roll number cannot be empty.");

    try {
      long rv = Long.parseLong(roll.trim());
      if (rv <= 0)
        throw new ValidationException("Roll number must be a positive number.");
    } catch (NumberFormatException e) {
      throw new ValidationException("Roll number must be numeric.");
    }

    if (studentDao.rollExists(roll.trim()))
      throw new ValidationException("A student with this roll number already exists.");

    if (program == null || program.isBlank())
      throw new ValidationException("Program cannot be empty.");

    int year;
    try {
      year = Integer.parseInt(yearStr.trim());
    } catch (Exception e) {
      throw new ValidationException("Year must be a number.");
    }

    if (year < 1 || year > 4) {
      throw new ValidationException("Year must be between 1 and 4.");
    }

    if (email == null || email.isBlank() || !EMAIL_RE.matcher(email).matches())
      throw new ValidationException("Invalid email address.");

    if (studentDao.emailExists(email.trim()))
      throw new ValidationException("A student with this email already exists.");

    if (instructorDao.emailExists(email.trim()))
      throw new ValidationException("This email is already used by an instructor.");
}


  private void validateInstructor(String department, String email) {
    if (department == null || department.isBlank())
      throw new ValidationException("Department cannot be empty.");

    if (email == null || email.isBlank() || !EMAIL_RE.matcher(email).matches())
      throw new ValidationException("Invalid email address.");

    if (studentDao.emailExists(email.trim()))
      throw new ValidationException("This email is already assigned to a student.");

    if (instructorDao.emailExists(email.trim()))
      throw new ValidationException("An instructor with this email already exists.");
  }


  public void createCourse(String code, String title, String description, int credits) {
    ac.requireRole(Role.ADMIN);
    log.info("Admin creating course: code={}, title={}", code, title);

    if (code == null || code.isBlank())
        throw new ValidationException("Course code cannot be empty.");

    String codeTrim = code.trim().toUpperCase();

    if (!COURSE_CODE_RE.matcher(codeTrim).matches())
        throw new ValidationException("Course code must be uppercase alphanumeric with no spaces.");

    if (title == null || title.isBlank())
        throw new ValidationException("Course title cannot be empty.");

    if (description == null || description.isBlank())
        throw new ValidationException("Course description cannot be empty.");

    if (credits <= 0)
        throw new ValidationException("Credits must be positive.");

    try {
        courseDao.findByCode(codeTrim);
        throw new ValidationException("A course with this code already exists.");
    } catch (NotFoundException ignore) {}

    courseDao.insert(codeTrim, title.trim(), description.trim(), credits);
    log.info("Course created: {}", codeTrim);
}


public void updateCourse(int courseId, String code, String title, String description, int credits) {
  ac.requireRole(Role.ADMIN);

  if (credits <= 0)
      throw new ValidationException("Credits must be positive.");

  if (code == null || code.isBlank())
      throw new ValidationException("Course code cannot be empty.");

  String codeTrim = code.trim().toUpperCase();

  if (!COURSE_CODE_RE.matcher(codeTrim).matches())
      throw new ValidationException("Course code must be uppercase alphanumeric with no spaces.");

  if (title == null || title.isBlank())
      throw new ValidationException("Course title cannot be empty.");

  if (description == null || description.isBlank())
      throw new ValidationException("Course description cannot be empty.");

  courseDao.update(courseId, codeTrim, title.trim(), description.trim(), credits);
  log.info("Course updated: {}", codeTrim);
}


  public int countEnrollmentsForSection(int sectionId) {
    return sectionDao.countEnrollments(sectionId);
  }

  public void deleteSection(int sectionId) {
    ac.requireRole(Role.ADMIN);
    int count = sectionDao.countEnrollments(sectionId);
    if (count > 0)
      throw new ValidationException("Cannot delete section with enrolled students.");
    sectionDao.delete(sectionId);
  }

  public void createSection(int courseId, Integer instructorId, String dayTime, String room,
                            int capacity, String semester, int year) {
    ac.requireRole(Role.ADMIN);

    if (capacity <= 0)
      throw new ValidationException("Capacity must be positive.");
    if (dayTime == null || dayTime.isBlank())
      throw new ValidationException("Day/Time cannot be empty.");
    if (room == null || room.isBlank())
      throw new ValidationException("Room cannot be empty.");
    if (year <= 0)
      throw new ValidationException("Year must be a positive integer.");
    if (semester == null || semester.isBlank())
      throw new ValidationException("Semester must be provided.");

    String sem = semester.trim();
    if (!sem.equals("Summer") && !sem.equals("Winter") && !sem.equals("Monsoon"))
      throw new ValidationException("Semester must be one of: Summer, Winter, Monsoon.");

    sectionDao.insert(courseId, instructorId, dayTime.trim(), room.trim(), capacity, sem, year);
  }

  public void updateSection(int sectionId, Integer instructorId, String dayTime, String room,
                            int capacity, String semester, int year) {
    ac.requireRole(Role.ADMIN);

    if (capacity < 0)
      throw new ValidationException("Capacity cannot be negative.");
    if (dayTime == null || dayTime.isBlank())
      throw new ValidationException("Day/Time cannot be empty.");
    if (room == null || room.isBlank())
      throw new ValidationException("Room cannot be empty.");
    if (year <= 0)
      throw new ValidationException("Year must be a positive integer.");
    if (semester == null || semester.isBlank())
      throw new ValidationException("Semester must be provided.");

    String sem = semester.trim();
    if (!sem.equals("Summer") && !sem.equals("Winter") && !sem.equals("Monsoon"))
      throw new ValidationException("Semester must be one of: Summer, Winter, Monsoon.");

    sectionDao.update(sectionId, instructorId, dayTime.trim(), room.trim(), capacity, sem, year);
  }

  public void assignInstructor(int sectionId, int instructorUserId) {
    ac.requireRole(Role.ADMIN);

    Integer instrId = instructorDao.findInstructorIdByUserId(instructorUserId);
    if (instrId == null)
      throw new ValidationException("No instructor profile for user: " + instructorUserId);

    sectionDao.assignInstructor(sectionId, instrId);
  }

  public void assignInstructorByEmail(int sectionId, String email) {
    ac.requireRole(Role.ADMIN);

    // Finding instructor
    Integer instrId = instructorDao.findInstructorIdByEmail(email);
    if (instrId == null)
        throw new ValidationException("No instructor found with that email.");

    // Loading section
    Section sec = sectionDao.findById(sectionId);
    if (sec == null)
        throw new NotFoundException("Section not found.");

    if (sec.getInstructorId() != null &&
        sec.getInstructorId().equals(instrId)) {

        throw new ValidationException("This instructor is already assigned to this section.");
    }

   
    sectionDao.assignInstructor(sectionId, instrId);
}


  public boolean toggleMaintenanceMode() {
    ac.requireRole(Role.ADMIN);

    boolean now = !settingsDao.isMaintenanceMode();
    settingsDao.setMaintenanceMode(now, SessionManager.getCurrentUser().getUserId());
    return now;
  }

  public List<Course> listCourses() { return courseDao.findAll(); }
  public List<Instructor> listInstructors() { return instructorDao.findAll(); }
  public List<Section> listSections() { return sectionDao.findAll(); }

  public String backupERPDatabase(String outputPath, String dbUser, String dbPass) {
    ac.requireRole(Role.ADMIN);
    try {
      ProcessBuilder pb = new ProcessBuilder(
          "docker", "exec", "-i", "erp-mariadb",
          "mysqldump",
          "-u" + dbUser,
          "-p" + dbPass,
          "--databases", "erpdb", "authdb"
      );

      pb.redirectErrorStream(true);
      Process process = pb.start();

      try (InputStream in = process.getInputStream();
           FileOutputStream out = new FileOutputStream(outputPath)) {

        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
          out.write(buffer, 0, len);
        }
      }

      int rc = process.waitFor();
      if (rc != 0)
        throw new DatabaseException("Backup failed (exit " + rc + ")");

      return outputPath;

    } catch (Exception ex) {
      throw new DatabaseException("Backup operation failed", ex);
    }
  }

  public void restoreERPDatabase(String sqlPath, String dbUser, String dbPass) {
    ac.requireRole(Role.ADMIN);

    try {
      ProcessBuilder pb = new ProcessBuilder(
          "docker", "exec", "-i", "erp-mariadb",
          "mysql",
          "-u" + dbUser,
          "-p" + dbPass
      );

      pb.redirectErrorStream(true);
      Process process = pb.start();

      try (FileInputStream fis = new FileInputStream(sqlPath);
           OutputStream os = process.getOutputStream()) {
        fis.transferTo(os);
      }

      String output = new String(process.getInputStream().readAllBytes());
      int rc = process.waitFor();

      if (rc != 0)
        throw new DatabaseException("Restore failed (exit " + rc + "). Output:\n" + output);

    } catch (Exception ex) {
      throw new DatabaseException("Restore operation failed", ex);
    }
  }

  public void deleteUser(int userId) {
    ac.requireRole(Role.ADMIN);

    int current = SessionManager.getCurrentUser().getUserId();
    if (current == userId)
      throw new AccessDeniedException("You cannot delete yourself.");

    studentDao.deleteByUserId(userId);
    instructorDao.deleteByUserId(userId);
    authDao.deleteUser(userId);
  }

  public void deleteUserByUsername(String username) {
    ac.requireRole(Role.ADMIN);

    Integer userId = authDao.findUserIdByUsername(username);
    if (userId == null)
      throw new NotFoundException("No such username: " + username);

    deleteUser(userId);
  }

  public boolean userExists(String username) {
    return authDao.usernameExists(username);
  }
}
