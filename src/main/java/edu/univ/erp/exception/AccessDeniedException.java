package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessDeniedException extends ERPException {
  private static final Logger log = LoggerFactory.getLogger(AccessDeniedException.class);

  public AccessDeniedException(String message) {
    super(message);
    log.warn("AccessDeniedException: {}", message);
  }
}
