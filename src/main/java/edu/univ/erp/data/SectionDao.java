package edu.univ.erp.data;

import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.exception.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(SectionDao.class);

  public List<Section> findByCourse(int courseId) {
    log.debug("Fetching sections for courseId={}", courseId);

    String sql = "SELECT * FROM sections WHERE course_id=?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, courseId)) {

      List<Section> list = new ArrayList<>();
      while (rs.next())
        list.add(map(rs));

      log.info("Loaded {} sections for courseId={}", list.size(), courseId);
      return list;

    } catch (SQLException e) {
      log.error("Section lookup failed for courseId={}", courseId, e);
      throw new DatabaseException("Section lookup failed", e);
    }
  }

  public List<Section> findAll() {
    log.debug("Fetching all sections");

    String sql = "SELECT * FROM sections ORDER BY section_id";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql)) {

      List<Section> list = new ArrayList<>();
      while (rs.next()) {
        list.add(map(rs));
      }

      log.info("Loaded {} total sections", list.size());
      return list;

    } catch (SQLException e) {
      log.error("Failed loading all sections", e);
      throw new DatabaseException("Failed loading sections", e);
    }
  }

  public Section findById(int id) {
    log.debug("Looking up section by id={}", id);

    String sql = "SELECT * FROM sections WHERE section_id=?";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql, id)) {

      if (!rs.next()) {
        log.warn("Section not found for id={}", id);
        throw new NotFoundException("Section not found");
      }

      Section s = map(rs);
      log.info("Section found: id={}", id);
      return s;

    } catch (SQLException e) {
      log.error("Error loading section id={}", id, e);
      throw new DatabaseException("Section error", e);
    }
  }

  private Section map(ResultSet rs) throws SQLException {
    return new Section(
        rs.getInt("section_id"),
        rs.getInt("course_id"),
        rs.getObject("instructor_id", Integer.class),
        rs.getString("day_time"),
        rs.getString("room"),
        rs.getInt("capacity"),
        rs.getString("semester"),
        rs.getInt("year"));
  }

  public void insert(int courseId, Integer instructorId, String dayTime, String room, int capacity, String semester,
      int year) {

    log.debug("Inserting section for courseId={} instructorId={} room={}", courseId, instructorId, room);

    String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection con = DBPool.erp().getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setInt(1, courseId);
      if (instructorId == null)
        ps.setNull(2, Types.INTEGER);
      else
        ps.setInt(2, instructorId);
      ps.setString(3, dayTime);
      ps.setString(4, room);
      ps.setInt(5, capacity);
      ps.setString(6, semester);
      ps.setInt(7, year);
      ps.executeUpdate();

      log.info("Section inserted successfully for courseId={}", courseId);

    } catch (SQLException e) {
      log.error("Section insert failed for courseId={}", courseId, e);
      throw new DatabaseException("Section insert failed", e);
    }
  }

  public void update(int sectionId, Integer instructorId, String dayTime, String room, int capacity, String semester,
      int year) {

    log.debug("Updating sectionId={} instructorId={} room={}", sectionId, instructorId, room);

    String sql = "UPDATE sections SET instructor_id=?, day_time=?, room=?, capacity=?, semester=?, year=? WHERE section_id=?";
    try (Connection con = DBPool.erp().getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {

      if (instructorId == null)
        ps.setNull(1, Types.INTEGER);
      else
        ps.setInt(1, instructorId);
      ps.setString(2, dayTime);
      ps.setString(3, room);
      ps.setInt(4, capacity);
      ps.setString(5, semester);
      ps.setInt(6, year);
      ps.setInt(7, sectionId);
      ps.executeUpdate();

      log.info("Section updated successfully sectionId={}", sectionId);

    } catch (SQLException e) {
      log.error("Section update failed sectionId={}", sectionId, e);
      throw new DatabaseException("Section update failed", e);
    }
  }

  public void assignInstructor(int sectionId, int instructorId) {
    log.debug("Assigning instructor {} to section {}", instructorId, sectionId);

    String sql = "UPDATE sections SET instructor_id = ? WHERE section_id = ?";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, instructorId);
        ps.setInt(2, sectionId);
        ps.executeUpdate();

        log.info("Instructor {} assigned to section {}", instructorId, sectionId);

    } catch (SQLException e) {
        log.error("Assign instructor failed for sectionId={} instructorId={}", sectionId, instructorId, e);
        throw new DatabaseException("Assign instructor failed", e);
    }
  }


  public Student findByStudentId(int studentId) {
    log.debug("Looking up student details for studentId={}", studentId);

    String sql = """
        SELECT s.student_id, s.user_id, s.roll_no, s.program, s.year, s.email, u.username
        FROM students s
        JOIN authdb.users_auth u ON u.user_id = s.user_id
        WHERE s.student_id = ?
    """;

    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            log.warn("Student not found studentId={}", studentId);
            throw new NotFoundException("Student not found for student_id=" + studentId);
        }

        Student s = new Student(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("roll_no"),
                rs.getString("program"),
                rs.getInt("year"),
                rs.getString("email")
        );

        log.info("Student loaded userId={} for studentId={}", s.getUserId(), studentId);
        return s;

    } catch (SQLException e) {
        log.error("Student lookup failed studentId={}", studentId, e);
        throw new DatabaseException("Student lookup failed", e);
    }
  }


  public int countEnrollments(int sectionId) {
    String sql = "SELECT COUNT(*) FROM enrollments WHERE section_id = ? AND status = 'ENROLLED'";
    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, sectionId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);

    } catch (SQLException e) {
        throw new DatabaseException("Failed counting enrollments", e);
    }
}

public void delete(int sectionId) {
    String sql = "DELETE FROM sections WHERE section_id = ?";
    try (Connection con = DBPool.erp().getConnection()) {
        runUpdate(con, sql, sectionId);
    } catch (SQLException e) {
        throw new DatabaseException("Failed to delete section", e);
    }
}


}
