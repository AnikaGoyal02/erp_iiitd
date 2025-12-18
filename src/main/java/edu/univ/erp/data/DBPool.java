package edu.univ.erp.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBPool {

  private static HikariDataSource AUTH_POOL;
  private static HikariDataSource ERP_POOL;
  private static final Logger log = LoggerFactory.getLogger(DBPool.class);

  public static void init(Properties props) {
    log.info("Initializing database connection pools...");

    HikariConfig auth = new HikariConfig();
    auth.setJdbcUrl(props.getProperty("db.auth.url"));
    auth.setUsername(props.getProperty("db.user"));
    auth.setPassword(props.getProperty("db.pass"));
    auth.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "10")));

    log.debug("Auth DB URL: {}", props.getProperty("db.auth.url"));

    HikariConfig erp = new HikariConfig();
    erp.setJdbcUrl(props.getProperty("db.erp.url"));
    erp.setUsername(props.getProperty("db.user"));
    erp.setPassword(props.getProperty("db.pass"));
    erp.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "10")));

    log.debug("ERP DB URL: {}", props.getProperty("db.erp.url"));

    AUTH_POOL = new HikariDataSource(auth);
    ERP_POOL = new HikariDataSource(erp);

    log.info("DB Pools initialized successfully.");
  }

  public static DataSource auth() {
    log.trace("Fetching AUTH_POOL datasource");
    return AUTH_POOL;
  }

  public static DataSource erp() {
    log.trace("Fetching ERP_POOL datasource");
    return ERP_POOL;
  }

  public static void close() {
    log.info("Closing database pools...");

    if (AUTH_POOL != null) {
      log.debug("Closing AUTH_POOL");
      AUTH_POOL.close();
    }

    if (ERP_POOL != null) {
      log.debug("Closing ERP_POOL");
      ERP_POOL.close();
    }

    log.info("Database pools closed.");
  }
}
