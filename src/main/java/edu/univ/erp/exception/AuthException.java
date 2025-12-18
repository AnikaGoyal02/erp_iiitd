package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthException extends ERPException {
  private static final Logger log = LoggerFactory.getLogger(AuthException.class);

  public AuthException(String message) {
    super(message);
    log.warn("AuthException: {}", message);
  }
}
