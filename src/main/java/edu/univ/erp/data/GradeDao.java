package edu.univ.erp.data;

import edu.univ.erp.domain.grades.*;
import edu.univ.erp.exception.*;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradeDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(GradeDao.class);

  public List<GradeComponent> findByEnrollment(int enrollmentId) {
    log.debug("Fetching grade components for enrollmentId={}", enrollmentId);

    String sql = "SELECT * FROM grades WHERE enrollment_id=?";

    try (Connection con = DBPool.erp().getConnection();
         ResultSet rs = runQuery(con, sql, enrollmentId)) {

      List<GradeComponent> list = new ArrayList<>();

      while (rs.next()) {
        GradeType type = GradeType.valueOf(rs.getString("component_type"));
        String name = rs.getString("component_name");
        double score = rs.getDouble("score");
        double max = rs.getDouble("max_score");
        double weight = rs.getDouble("weightage");

        log.trace("Mapping grade row: type={} name={} score={} max={} weight={}",
            type, name, score, max, weight);

        list.add(switch (type) {
          case QUIZ -> new QuizComponent(name, score, max, weight);
          case MIDSEM -> new MidsemComponent(name, score, max, weight);
          case ENDSEM -> new EndsemComponent(name, score, max, weight);
          case ASSIGNMENT -> new AssignmentComponent(name, score, max, weight);
          case PROJECT -> new ProjectComponent(name, score, max, weight);
          default -> throw new DatabaseException("Unknown grade type");
        });
      }

      log.info("Loaded {} grade components for enrollmentId={}", list.size(), enrollmentId);
      return list;

    } catch (SQLException e) {
      log.error("Grade lookup failed for enrollmentId={}", enrollmentId, e);
      throw new DatabaseException("Grade lookup failed", e);
    }
  }

}
