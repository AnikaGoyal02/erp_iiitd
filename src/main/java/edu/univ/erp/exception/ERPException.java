package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ERPException extends RuntimeException {
  private static final Logger log = LoggerFactory.getLogger(ERPException.class);

  public ERPException(String message) {
    super(message);
    log.error("ERPException: {}", message);
  }

  public ERPException(String message, Throwable cause) {
    super(message, cause);
    log.error("ERPException: {}", message, cause);
  }
}
