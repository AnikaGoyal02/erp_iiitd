package edu.univ.erp.data;

import edu.univ.erp.exception.DatabaseException;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDao {
  private static final Logger log = LoggerFactory.getLogger(BaseDao.class);

  protected void closeQuietly(AutoCloseable c) {
    try {
      if (c != null) {
        log.trace("Closing resource: {}", c.getClass().getSimpleName());
        c.close();
      }
    } catch (Exception ignored) {
      log.debug("Ignored exception while closing resource: {}", ignored.toString());
    }
  }

  protected void runUpdate(Connection con, String sql, Object... params) {
    log.debug("runUpdate SQL: {} | params={}", sql, params);
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++)
        ps.setObject(i + 1, params[i]);
      int updated = ps.executeUpdate();
      log.debug("runUpdate affected {} rows", updated);
    } catch (SQLException e) {
      log.error("DB update failed: {}", e.getMessage(), e);
      throw new DatabaseException("DB update failed: " + e.getMessage(), e);
    }
  }

  protected ResultSet runQuery(Connection con, String sql, Object... params) throws SQLException {
    log.debug("runQuery SQL: {} | params={}", sql, params);
    PreparedStatement ps = con.prepareStatement(sql);
    for (int i = 0; i < params.length; i++)
      ps.setObject(i + 1, params[i]);

    ResultSet rs = ps.executeQuery();
    log.trace("runQuery executed successfully");
    return rs;
  }
}
