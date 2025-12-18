package edu.univ.erp.data;

import edu.univ.erp.domain.Course;
import edu.univ.erp.exception.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourseDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(CourseDao.class);

    public List<Course> findAll() {
        String sql = "SELECT course_id, code, title, description, credits FROM courses ORDER BY code";
        try (Connection con = DBPool.erp().getConnection();
             ResultSet rs = runQuery(con, sql)) {

            List<Course> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Course(
                    rs.getInt("course_id"),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("credits")
                ));
            }
            return list;

        } catch (SQLException e) {
            throw new DatabaseException("Failed loading courses", e);
        }
    }

    public Course findByCode(String code) {
        String sql = "SELECT course_id, code, title, description, credits FROM courses WHERE code = ?";
        try (Connection con = DBPool.erp().getConnection();
             ResultSet rs = runQuery(con, sql, code)) {

            if (!rs.next()) throw new NotFoundException("Course not found: " + code);

            return new Course(
                rs.getInt("course_id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("credits")
            );

        } catch (SQLException e) {
            throw new DatabaseException("Course lookup by code failed", e);
        }
    }

    public Course findById(int id) {
        String sql = "SELECT course_id, code, title, description, credits FROM courses WHERE course_id = ?";
        try (Connection con = DBPool.erp().getConnection();
             ResultSet rs = runQuery(con, sql, id)) {

            if (!rs.next()) throw new NotFoundException("Course not found");

            return new Course(
                rs.getInt("course_id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("credits")
            );

        } catch (SQLException e) {
            throw new DatabaseException("Course lookup failed", e);
        }
    }

    public void insert(String code, String title, String description, int credits) {
        String sql = "INSERT INTO courses (code, title, description, credits) VALUES (?, ?, ?, ?)";

        try (Connection con = DBPool.erp().getConnection()) {
            runUpdate(con, sql, code, title, description, credits);
        } catch (SQLException e) {
            throw new DatabaseException("Course insert failed", e);
        }
    }

    public void update(int courseId, String code, String title, String description, int credits) {
        String sql = "UPDATE courses SET code=?, title=?, description=?, credits=? WHERE course_id=?";

        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setInt(4, credits);
            ps.setInt(5, courseId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Course update failed", e);
        }
    }
}
