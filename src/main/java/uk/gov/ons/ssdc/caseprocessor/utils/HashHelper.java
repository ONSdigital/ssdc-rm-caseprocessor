package uk.gov.ons.ssdc.caseprocessor.utils;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
  private static final Logger log = LoggerFactory.getLogger(Class.class);

  public static byte[] hashString(byte[] input) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      log.error("Could not initialise hashing", e);
      throw new RuntimeException("Could not initialise hashing", e);
    }
    return digest.digest(input);
  }

  public static String bytesToHexString(byte[] hash) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
