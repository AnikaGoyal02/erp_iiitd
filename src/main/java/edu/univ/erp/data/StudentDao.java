package edu.univ.erp.data;

import edu.univ.erp.domain.Student;
import edu.univ.erp.exception.*;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(StudentDao.class);
    
    public Student findByUserId(int userId) {
        log.debug("Looking up student by userId={}", userId);

        String sql = """
            SELECT s.user_id, s.roll_no, s.program, s.year, s.email,
                   u.username
            FROM students s
            JOIN authdb.users_auth u ON s.user_id = u.user_id
            WHERE s.user_id = ?
        """;
        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                log.warn("Student profile missing for user_id={}", userId);
                throw new NotFoundException("Student profile missing (user_id=" + userId + ")");
            }

            Student s = new Student(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("roll_no"),
                    rs.getString("program"),
                    rs.getInt("year"),
                    rs.getString("email")
            );

            log.info("Student loaded successfully for userId={}", userId);
            return s;

        } catch (SQLException e) {
            log.error("Student lookup failed for userId={}", userId, e);
            throw new DatabaseException("Student lookup failed", e);
        }
    }

    
    public Student findByStudentId(int studentId) {
        log.debug("Looking up student by studentId={}", studentId);

        String sql = """
            SELECT s.student_id, s.user_id, s.roll_no, s.program, s.year, s.email,
                   u.username
            FROM students s
            JOIN authdb.users_auth u ON s.user_id = u.user_id
            WHERE s.student_id = ?
        """;

        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                log.warn("Student not found for student_id={}", studentId);
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

            log.info("Student loaded successfully for studentId={}", studentId);
            return s;

        } catch (SQLException e) {
            log.error("Student lookup by student_id failed for studentId={}", studentId, e);
            throw new DatabaseException("Student lookup by student_id failed", e);
        }
    }

    
    public void insert(int userId, String roll, String program, int year, String email) {
        log.debug("Inserting student userId={}, roll={}, program={}, year={}, email={}",
                  userId, roll, program, year, email);

        String sql = "INSERT INTO students (user_id, roll_no, program, year, email) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBPool.erp().getConnection()) {
            runUpdate(con, sql, userId, roll, program, year, email);
            log.info("Student inserted userId={}", userId);
        } catch (SQLException e) {
            log.error("Student insert failed for userId={}", userId, e);
            throw new DatabaseException("Student insert failed", e);
        }
    }

    
    public void deleteByUserId(int userId) {
        log.debug("Deleting student profile for userId={}", userId);

        String sql = "DELETE FROM students WHERE user_id = ?";
        try (Connection con = DBPool.erp().getConnection()) {
            runUpdate(con, sql, userId);
            log.info("Student profile deleted for userId={}", userId);
        } catch (SQLException e) {
            log.error("Failed to delete student profile for userId={}", userId, e);
            throw new DatabaseException("Failed to delete student profile", e);
        }
    }


    public boolean rollExists(String roll) {
        String sql = "SELECT 1 FROM students WHERE roll_no = ?";
        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
    
            ps.setString(1, roll);
            ResultSet rs = ps.executeQuery();
            return rs.next();
    
        } catch (SQLException e) {
            log.error("Error checking roll existence for roll={}", roll, e);
            throw new DatabaseException("Failed checking roll existence", e);
        }
    }
    


    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM students WHERE email = ?";
        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
    
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
    
        } catch (SQLException e) {
            throw new DatabaseException("Failed checking email existence", e);
        }
    }
    
}
