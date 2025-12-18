package edu.univ.erp.service;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import edu.univ.erp.domain.grades.*;
import edu.univ.erp.exception.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentService {

  private static final Logger log = LoggerFactory.getLogger(StudentService.class);

  private final AccessControl ac = new AccessControl();
  private final CourseDao courseDao = new CourseDao();
  private final SectionDao sectionDao = new SectionDao();
  private final EnrollmentDao enrollmentDao = new EnrollmentDao();
  private final GradeDao gradeDao = new GradeDao();

  public List<Course> browseCatalog() {
    ac.requireRole(Role.STUDENT);
    log.debug("Student browsing course catalog");
    return courseDao.findAll();
  }


  public List<String[]> listSections(int courseId) {
    ac.requireRole(Role.STUDENT);
    log.info("Listing sections for courseId={}", courseId);

    if (courseId <= 0) {
      log.warn("Invalid courseId passed to listSections: {}", courseId);
      throw new ValidationException("Invalid course id.");
    }

    // ensure course exists (fail fast with clear message)
    try {
      Course c = courseDao.findById(courseId);
      if (c == null) {
        log.warn("Course not found for courseId={}", courseId);
        throw new NotFoundException("Course not found: " + courseId);
      }
    } catch (NotFoundException nfe) {
      throw nfe;
    } catch (Exception ex) {
      log.error("Unexpected error while checking course existence for id={}", courseId, ex);
      throw new DatabaseException("Failed to validate course existence", ex);
    }

    String sql = """
        SELECT 
            s.section_id,
            i.email AS instructor_name,
            s.day_time,
            s.room,
            s.capacity,
            s.semester,
            s.year
        FROM sections s
        LEFT JOIN instructors i ON i.instructor_id = s.instructor_id
        WHERE s.course_id = ?
    """;

    List<String[]> rows = new ArrayList<>();

    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, courseId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            rows.add(new String[] {
                rs.getString("section_id"),
                rs.getString("instructor_name"),
                rs.getString("day_time"),
                rs.getString("room"),
                rs.getString("capacity"),
                rs.getString("semester"),
                rs.getString("year")
            });
        }

        log.info("Found {} sections for courseId={}", rows.size(), courseId);

    } catch (SQLException e) {
        log.error("Failed to load sections for courseId={}", courseId, e);
        throw new DatabaseException("Failed to load sections", e);
    }

    return rows;
  }


  public void register(int sectionId) {
    ac.requireRole(Role.STUDENT);
    ac.requireMaintenanceOff();

    SettingsDao dao = new SettingsDao();
    LocalDate today = LocalDate.now();
    LocalDate addDeadline = dao.getAddDeadline();

    if (addDeadline != null && today.isAfter(addDeadline)) {
        throw new ValidationException("Registration closed on " + dao.formatDMY(addDeadline));
    }

    if (sectionId <= 0) {
        log.warn("Invalid sectionId passed to register: {}", sectionId);
        throw new ValidationException("Invalid section id.");
    }

    Student stu = (Student) SessionManager.getCurrentUser();
    if (stu == null) {
        log.warn("No current student in session while trying to register");
        throw new AccessDeniedException("No active student session.");
    }

    log.info("Student userId={} attempting to register for sectionId={}", stu.getUserId(), sectionId);

   
    int studentId = 0;
    String sqlGetStudentId = "SELECT student_id FROM students WHERE roll_no = ?";

    try (Connection con0 = DBPool.erp().getConnection();
         PreparedStatement ps0 = con0.prepareStatement(sqlGetStudentId)) {

        ps0.setString(1, stu.getRollNo());
        ResultSet rs0 = ps0.executeQuery();

        if (rs0.next()) {
            studentId = rs0.getInt("student_id");
        } else {
            throw new ValidationException("Student record not found for roll no: " + stu.getRollNo());
        }

    } catch (SQLException ex) {
        throw new DatabaseException("Failed to lookup student_id", ex);
    }

    
    Section sec;
    try {
        sec = sectionDao.findById(sectionId);
        if (sec == null) {
            throw new NotFoundException("Section not found: " + sectionId);
        }
    } catch (Exception ex) {
        throw new DatabaseException("Failed to lookup section", ex);
    }

    if (sec.getCapacity() < 0) {
        throw new ValidationException("Section capacity is invalid.");
    }

    
    try (Connection con = DBPool.erp().getConnection()) {
        con.setAutoCommit(false);

        
        String dupCheck = """
            SELECT COUNT(*) FROM enrollments
            WHERE student_id=? AND section_id=? AND status='ENROLLED'
        """;

        try (PreparedStatement ps = con.prepareStatement(dupCheck)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ResultSet rs = ps.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                con.rollback();
                throw new ValidationException("Already enrolled in this section.");
            }
        }

       
        Course course;
        try {
            course = courseDao.findById(sec.getCourseId());
            if (course == null) {
                con.rollback();
                throw new NotFoundException("Course for section not found.");
            }
        } catch (Exception ex) {
            con.rollback();
            throw new DatabaseException("Failed to lookup course for section", ex);
        }

       
        String multiCheck = """
            SELECT COUNT(*)
            FROM enrollments e
            JOIN sections s ON s.section_id = e.section_id
            WHERE e.student_id = ?
              AND s.course_id = ?
              AND e.status = 'ENROLLED'
        """;

        try (PreparedStatement ps = con.prepareStatement(multiCheck)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sec.getCourseId());

            ResultSet rs = ps.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                con.rollback();
                throw new ValidationException("You are already enrolled in another section of this course.");
            }
        }

        
        String countSql = "SELECT COUNT(*) FROM enrollments WHERE section_id=? AND status='ENROLLED'";
        int count;

        try (PreparedStatement ps = con.prepareStatement(countSql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            count = rs.getInt(1);
        }

        if (count >= sec.getCapacity()) {
            con.rollback();
            throw new ValidationException("Section is full.");
        }

       
        String insert = """
            INSERT INTO enrollments (student_id, section_id, status, registered_on)
            VALUES (?, ?, 'ENROLLED', NOW())
        """;

        try (PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ps.executeUpdate();
        }

        con.commit();
        log.info("Registration successful studentId={}, sectionId={}", studentId, sectionId);

    } catch (SQLException e) {
        throw new DatabaseException("Registration failed", e);
    }
}









public void drop(int enrollmentId) {
  ac.requireRole(Role.STUDENT);
  ac.requireMaintenanceOff();

  SettingsDao dao = new SettingsDao();
  LocalDate today = LocalDate.now();
  LocalDate dropDeadline = dao.getDropDeadline();

  if (dropDeadline != null && today.isAfter(dropDeadline)) {
      throw new ValidationException("Drop period ended on " + dao.formatDMY(dropDeadline));
  }

  if (enrollmentId <= 0) {
      log.warn("Invalid enrollmentId passed to drop: {}", enrollmentId);
      throw new ValidationException("Invalid enrollment id.");
  }

  int studentId = getRealStudentId();
  log.info("Student studentId={} dropping enrollmentId={}", studentId, enrollmentId);



  Enrollment e = enrollmentDao.findById(enrollmentId);

  // Checking ownership
  if (e.getStudentId() != studentId) {
      log.warn("Student {} attempted to drop enrollment {} not belonging to them",
               studentId, enrollmentId);
      throw new AccessDeniedException("Not your enrollment.");
  }

  // Checking active status
  if (e.getStatus() != Enrollment.Status.ENROLLED) {
      throw new ValidationException("Cannot drop a non-active enrollment.");
  }


  String sql = "UPDATE enrollments SET status='DROPPED', dropped_on=NOW() WHERE enrollment_id=?";

  try (Connection con = DBPool.erp().getConnection();
       PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setInt(1, enrollmentId);
      ps.executeUpdate();

      log.info("Drop successful enrollmentId={} studentId={}", enrollmentId, studentId);

  } catch (SQLException ex) {
      log.error("Failed to drop enrollment {}", enrollmentId, ex);
      throw new DatabaseException("Failed to drop enrollment", ex);
  }
}





  public List<String[]> myEnrollments() {
    ac.requireRole(Role.STUDENT);

    Student stu = (Student) SessionManager.getCurrentUser();
    if (stu == null) {
      log.warn("No current student session in myEnrollments()");
      throw new AccessDeniedException("No active student session.");
    }

    log.debug("Fetching enrollments for student userId={}, rollNo={}", stu.getUserId(), stu.getRollNo());

    int studentId = 0;

    String sqlId = "SELECT student_id FROM students WHERE roll_no = ?";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sqlId)) {

        ps.setString(1, stu.getRollNo());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) studentId = rs.getInt(1);
        else {
          log.warn("Student record missing when fetching enrollments for rollNo={}", stu.getRollNo());
          throw new NotFoundException("Student record not found for roll " + stu.getRollNo());
        }

        log.debug("Resolved studentId={} for myEnrollments()", studentId);

    } catch (SQLException e) {
        log.error("Failed to resolve studentId for myEnrollments()", e);
        throw new DatabaseException("Failed to get student_id", e);
    }

    String sql = """
    SELECT e.enrollment_id,
           e.section_id,
           c.code,
           c.title,
           i.email AS instructor_name,
           c.credits,
           e.final_grade
    FROM enrollments e
    JOIN sections s ON s.section_id = e.section_id
    JOIN courses c ON c.course_id = s.course_id
    LEFT JOIN instructors i ON i.instructor_id = s.instructor_id
    WHERE e.student_id = ?
      AND e.status IN ('ENROLLED','COMPLETED')
    """;

    List<String[]> rows = new ArrayList<>();

    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            rows.add(new String[] {
              rs.getString("enrollment_id"),   
              rs.getString("section_id"),
              rs.getString("code"),
              rs.getString("title"),
              rs.getString("instructor_name"),
              rs.getString("credits"),
              rs.getString("final_grade")
          });
        }

        log.info("Fetched {} enrollments for studentId={}", rows.size(), studentId);

    } catch (SQLException e) {
        log.error("Failed to fetch enrollments for studentId={}", studentId, e);
        throw new DatabaseException("Failed to fetch enrollments", e);
    }

    return rows;
  }



  public List<edu.univ.erp.domain.grades.GradeComponent> viewGrades(int enrollmentId) {
    ac.requireRole(Role.STUDENT);
    Student s = (Student) SessionManager.getCurrentUser();
    if (s == null) {
      log.warn("No current student session in viewGrades()");
      throw new AccessDeniedException("No active student session.");
    }

    if (enrollmentId <= 0) {
      log.warn("Invalid enrollmentId passed to viewGrades: {}", enrollmentId);
      throw new ValidationException("Invalid enrollment id.");
    }

    log.info("Student userId={} viewing grades for enrollmentId={}", s.getUserId(), enrollmentId);

    Enrollment e = enrollmentDao.findById(enrollmentId);
    if (e == null) {
      log.warn("Enrollment not found enrollmentId={}", enrollmentId);
      throw new NotFoundException("Enrollment not found");
    }

    int realStudentId = getRealStudentId();
    if (e.getStudentId() != realStudentId) {
        log.warn("Unauthorized grade access: userId={}, enrollmentId={}", s.getUserId(), enrollmentId);
        throw new AccessDeniedException("Not your enrollment.");
    }

    return gradeDao.findByEnrollment(enrollmentId);
  }



  public List<Section> myRegisteredSections() {
    ac.requireRole(Role.STUDENT);
    Student stu = (Student) SessionManager.getCurrentUser();
    if (stu == null) {
      log.warn("No current student session in myRegisteredSections()");
      throw new AccessDeniedException("No active student session.");
    }

    log.debug("Fetching registered sections for studentId={}", stu.getUserId());

    String sql = """
        SELECT sec.section_id,
               sec.course_id,
               sec.instructor_id,
               sec.day_time,
               sec.room,
               sec.capacity,
               sec.semester,
               sec.year,
               c.code AS course_code,
               c.title AS course_title,
               c.credits AS credits,
               i.email AS instructor_email
        FROM enrollments e
        JOIN sections sec ON sec.section_id = e.section_id
        JOIN courses c ON c.course_id = sec.course_id
        LEFT JOIN instructors i ON i.instructor_id = sec.instructor_id
        WHERE e.student_id = ?
          AND e.status = 'ENROLLED'
    """;

    List<Section> list = new ArrayList<>();

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sql)) {

        int realStudentId = getRealStudentId();
        ps.setInt(1, realStudentId);

        var rs = ps.executeQuery();

        while (rs.next()) {
            Section s = new Section(
                rs.getInt("section_id"),
                rs.getInt("course_id"),
                rs.getObject("instructor_id", Integer.class),
                rs.getString("day_time"),
                rs.getString("room"),
                rs.getInt("capacity"),
                rs.getString("semester"),
                rs.getInt("year")
            );

            s.setCourseCode(rs.getString("course_code"));
            s.setCourseTitle(rs.getString("course_title"));
            s.setInstructorEmail(rs.getString("instructor_email"));
            s.setCredits(rs.getInt("credits"));

            list.add(s);
        }

        log.info("Loaded {} registered sections for studentId={}", list.size(), realStudentId);

    } catch (SQLException e) {
        log.error("Failed to load registered sections", e);
        throw new DatabaseException("Failed to load timetable sections", e);
    }

    return list;
  }



  public List<TranscriptRow> getTranscriptRows() {
    ac.requireRole(Role.STUDENT);
    Student stu = (Student) SessionManager.getCurrentUser();
    if (stu == null) {
      log.warn("No current student session in getTranscriptRows()");
      throw new AccessDeniedException("No active student session.");
    }

    log.debug("Fetching transcript rows for studentId={}", stu.getUserId());

    String sql = """
            SELECT c.code, c.title, c.credits,
                   s.semester, s.year,
                   e.final_grade
            FROM enrollments e
            JOIN sections s ON s.section_id = e.section_id
            JOIN courses c ON c.course_id = s.course_id
            WHERE e.student_id = ?
              AND e.status IN ('ENROLLED','COMPLETED')
            ORDER BY s.year DESC, s.semester
        """;
    List<TranscriptRow> list = new ArrayList<>();
    try (var con = DBPool.erp().getConnection();
        var ps = con.prepareStatement(sql)) {
      ps.setInt(1, getRealStudentId()); 
      var rs = ps.executeQuery();
      while (rs.next()) {
        list.add(new TranscriptRow(
            rs.getString("code"),
            rs.getString("title"),
            rs.getInt("credits"),
            rs.getString("semester"),
            rs.getInt("year"),
            rs.getString("final_grade")));
      }

      log.info("Fetched {} transcript rows for userId={}", list.size(), stu.getUserId());

    } catch (SQLException e) {
      log.error("Failed to fetch transcript for userId={}", stu.getUserId(), e);
      throw new DatabaseException("Failed to fetch transcript", e);
    }
    return list;
  }



  private int getRealStudentId() {
    Student stu = (Student) SessionManager.getCurrentUser();
    if (stu == null) {
      log.warn("No current student session in getRealStudentId()");
      throw new AccessDeniedException("No active student session.");
    }

    log.debug("Resolving real studentId for rollNo={}", stu.getRollNo());

    String sql = "SELECT student_id FROM students WHERE roll_no = ?";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, stu.getRollNo());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int id = rs.getInt(1);
            log.debug("Resolved realStudentId={} for rollNo={}", id, stu.getRollNo());
            return id;
        }

        log.warn("Student record missing for rollNo={}", stu.getRollNo());
        throw new NotFoundException("Student record missing for roll " + stu.getRollNo());

    } catch (SQLException e) {
        log.error("Failed student lookup for rollNo={}", stu.getRollNo(), e);
        throw new DatabaseException("Lookup failed", e);
    }
  }



}
