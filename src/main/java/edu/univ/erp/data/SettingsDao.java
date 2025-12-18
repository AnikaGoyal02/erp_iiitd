package edu.univ.erp.data;

import edu.univ.erp.exception.DatabaseException;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(SettingsDao.class);

  public boolean isMaintenanceMode() {
    log.debug("Checking maintenance mode status");

    String sql = "SELECT value FROM settings WHERE `key`='maintenance_mode'";
    try (Connection con = DBPool.erp().getConnection();
        ResultSet rs = runQuery(con, sql)) {

      if (rs.next()) {
        boolean mode = Boolean.parseBoolean(rs.getString("value"));
        log.info("Maintenance mode status loaded: {}", mode);
        return mode;
      }

      log.warn("Maintenance mode setting not found, defaulting to false");
      return false;

    } catch (SQLException e) {
      log.error("Error checking maintenance mode", e);
      throw new DatabaseException("Error checking maintenance mode", e);
    }
  }

  public void setMaintenanceMode(boolean mode, int updatedBy) {
    log.debug("Updating maintenance mode to {} by userId={}", mode, updatedBy);

    String sql = """
        INSERT INTO settings(`key`,`value`,updated_by)
        VALUES('maintenance_mode',?,?)
        ON DUPLICATE KEY UPDATE `value`=?, updated_by=?
        """;

    try (Connection con = DBPool.erp().getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, String.valueOf(mode));
      ps.setInt(2, updatedBy);
      ps.setString(3, String.valueOf(mode));
      ps.setInt(4, updatedBy);
      ps.executeUpdate();

      log.info("Maintenance mode updated to {} by userId={}", mode, updatedBy);

    } catch (SQLException e) {
      log.error("Error updating maintenance mode to {} by userId={}", mode, updatedBy, e);
      throw new DatabaseException("Error updating maintenance mode", e);
    }
  }

  public LocalDate parseDMY(String s) {
    try {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd:MM:yyyy");
        return LocalDate.parse(s.trim(), f);
    } catch (Exception ex) {
        return null;
    }
  }

  public String formatDMY(LocalDate d) {
      if (d == null) return "";
      DateTimeFormatter f = DateTimeFormatter.ofPattern("dd:MM:yyyy");
      return d.format(f);
  }



  public LocalDate getAddDeadline() {
    String raw = getValue("ADD_DEADLINE");
    return (raw == null || raw.isBlank()) ? null : parseDMY(raw);
  }

  public LocalDate getDropDeadline() {
      String raw = getValue("DROP_DEADLINE");
      return (raw == null || raw.isBlank()) ? null : parseDMY(raw);
  }

  public void setAddDeadline(LocalDate d) {
      setValue("ADD_DEADLINE", formatDMY(d));
  }

  public void setDropDeadline(LocalDate d) {
      setValue("DROP_DEADLINE", formatDMY(d));
  }

  private String getValue(String key) {
    String sql = "SELECT value FROM settings WHERE `key`=?";

    try (Connection con = DBPool.erp().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, key);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) return rs.getString("value");
        return null;

    } catch (SQLException e) {
        log.error("Failed to read settings key={}", key, e);
        throw new DatabaseException("Failed to read setting: " + key, e);
    }
}

  private void setValue(String key, String value) {
      String sql = """
          INSERT INTO settings(`key`, `value`)
          VALUES (?, ?)
          ON DUPLICATE KEY UPDATE value = ?
      """;

      try (Connection con = DBPool.erp().getConnection();
          PreparedStatement ps = con.prepareStatement(sql)) {

          ps.setString(1, key);
          ps.setString(2, value);
          ps.setString(3, value);
          ps.executeUpdate();

      } catch (SQLException e) {
          log.error("Failed to update settings key={} value={}", key, value, e);
          throw new DatabaseException("Failed to update setting: " + key, e);
      }
  }  
}