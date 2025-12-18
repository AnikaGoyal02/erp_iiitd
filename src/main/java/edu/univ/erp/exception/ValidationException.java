package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationException extends ERPException {
  private static final Logger log = LoggerFactory.getLogger(ValidationException.class);

  public ValidationException(String message) {
    super(message);
    log.warn("ValidationException: {}", message);
  }
}
