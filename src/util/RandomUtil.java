package util;

import java.security.SecureRandom;

public class RandomUtil {

  // Cryptographically secure random generator (Thread-safe)
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // Generates a cryptographically secure 8-digit numeric PIN code string
  public static String generate8DigitPin() {
    int randomNumber = SECURE_RANDOM.nextInt(100_000_000);
    return String.format("%08d", randomNumber);
  }

  // Generates a random integer between min and max (inclusive)
  public static int getRandomInt(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("Min value cannot be greater than Max value");
    }
    return SECURE_RANDOM.nextInt((max - min) + 1) + min;
  }

  // Generates a random integer from 0 up to max (inclusive)
  public static int getRandomInt(int max) {
    return getRandomInt(0, max);
  }

  // Generates a formatted ID String with zero padding
  public static String generateFormattedId(String prefix, int min, int max, int digits) {
    int randomNum = getRandomInt(min, max);
    return prefix + String.format("%0" + digits + "d", randomNum);
  }
}
