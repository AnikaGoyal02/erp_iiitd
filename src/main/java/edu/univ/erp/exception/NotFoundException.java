package edu.univ.erp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotFoundException extends ERPException {
  private static final Logger log = LoggerFactory.getLogger(NotFoundException.class);

  public NotFoundException(String message) {
    super(message);
    log.warn("NotFoundException: {}", message);
  }
}
