package edu.univ.erp.auth;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordUtil {

  private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);

  public static String hash(String plain) {
    log.debug("Hashing password (length={})", plain == null ? 0 : plain.length());
    return BCrypt.hashpw(plain, BCrypt.gensalt(10));
  }

  public static boolean verify(String plain, String hash) {
    log.debug("Verifying password (plain null? {}, hash null? {})", 
              plain == null, hash == null);

    if (plain == null || hash == null)
      return false;

    return BCrypt.checkpw(plain, hash);
  }
}
