package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseException extends ERPException {
  private static final Logger log = LoggerFactory.getLogger(DatabaseException.class);

  public DatabaseException(String message) {
    super(message);
    log.error("DatabaseException: {}", message);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
    log.error("DatabaseException: {}", message, cause);
  }
}
