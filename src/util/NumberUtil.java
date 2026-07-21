package util;

import java.security.SecureRandom;

public class NumberUtil {

  // Cryptographically secure random generator (Thread-safe)
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // Generates a numeric PIN string of a specified length
  public static String generateDigitPin(int length) {
    if (length <= 0 || length > 9) {
      throw new IllegalArgumentException("PIN length must be between 1 and 9 digits.");
    }

    // Calculate maximum bound: length=6 -> 10^6 = 1,000,000 (0 to 999,999)
    int bound = (int) Math.pow(10, length);
    int randomNumber = SECURE_RANDOM.nextInt(bound);

    // Format dynamically with leading zeros (e.g., "%06d" or "%08d")
    return String.format("%0" + length + "d", randomNumber);
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

  // Generates a formatted ID String with custom prefix and zero-padded length
  public static String generateFormattedId(String prefix, int min, int max, int digits) {
    int randomNum = getRandomInt(min, max);
    return prefix + String.format("%0" + digits + "d", randomNum);
  }
}
