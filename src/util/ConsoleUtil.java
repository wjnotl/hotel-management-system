package util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConsoleUtil {
  private static final Scanner scanner = new Scanner(System.in);

  private static PrintStream originalOut;
  private static final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  public static boolean isRunningInIDE() {
    return System.console() == null;
  }

  public static void clearScreen() {
    if (isRunningInIDE()) {
      for (int i = 0; i < 50; i++) {
        System.out.println();
      }
    } else {
      System.out.print("\033\143");
      System.out.flush();
    }
  }

  public static void printTitleBox(String title) {
    printTitleBox(title, 40); // default min width = 40
  }

  public static void printTitleBox(String title, int minWidth) {
    if (title == null) title = "";

    int contentWidth = title.length() + 8; // 2 spaces + "::" on both sides
    int boxWidth = Math.max(minWidth, contentWidth);

    // Keep box layout symmetric
    if ((boxWidth - 8 - title.length()) % 2 != 0) {
      boxWidth++;
    }

    int totalPadding = boxWidth - title.length() - 8;
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;

    String border = ":".repeat(boxWidth);
    String side = "::";
    String middle = "  " + " ".repeat(leftPadding) + title + " ".repeat(rightPadding) + "  ";

    System.out.println(border);
    System.out.println(side + middle + side);
    System.out.println(border + "\n");
  }

  public static void printError(String message) {
    if (message == null) {
      message = "An unknown error occurred!";
    }

    clearScreen();
    System.out.println("Error: " + message);
    printContinueMessage();
  }

  public static void printContinueMessage() {
    printContinueMessage("Press Enter to continue...");
  }

  public static void printContinueMessage(String message) {
    System.out.print(message);
    scanner.nextLine();
  }

  public static boolean showConfirmMessage(String message) {
    while (true) {
      clearScreen();
      try {
        return getMenuInput(message + " (Y/N): ", new char[] {'Y', 'N'})
            .input
            .equalsIgnoreCase("Y");
      } catch (Exception e) {
        printError(e.getMessage());
      }
    }
  }

  public static class GetMenuInputArgs {
    public static class IntegerInputArgs {
      public final int min;
      public final int max;

      public IntegerInputArgs(int min, int max) {
        this.min = min;
        this.max = max;

        if (min > max) {
          throw new IllegalArgumentException("Minimum value cannot be greater than maximum value!");
        }
      }
    }

    public static class CharInputArgs {
      public final char[] validChars;

      public CharInputArgs(char[] validChars) {
        this.validChars = validChars;

        if (validChars == null || validChars.length == 0) {
          throw new IllegalArgumentException("Valid characters cannot be empty!");
        }
      }

      public String formatValidChars() {
        int len = validChars.length;

        // Single character case: 'A'
        if (len == 1) {
          return "'" + Character.toUpperCase(validChars[0]) + "'";
        }

        // Two characters case: 'A' and 'B'
        if (len == 2) {
          return "'"
              + Character.toUpperCase(validChars[0])
              + "' and '"
              + Character.toUpperCase(validChars[1])
              + "'";
        }

        // 3+ characters case: 'A', 'B' and 'C'
        String[] formatted = new String[len - 1];
        for (int i = 0; i < len - 1; i++) {
          formatted[i] = "'" + Character.toUpperCase(validChars[i]) + "'";
        }

        String lastItem = "'" + Character.toUpperCase(validChars[len - 1]) + "'";
        return String.join(", ", formatted) + " and " + lastItem;
      }
    }

    public final String prompt;
    public final IntegerInputArgs integerInputArgs;
    public final CharInputArgs charInputArgs;

    public GetMenuInputArgs(String prompt, IntegerInputArgs integerInputArgs) {
      this(prompt, integerInputArgs, null);
    }

    public GetMenuInputArgs(String prompt, CharInputArgs charInputArgs) {
      this(prompt, null, charInputArgs);
    }

    public GetMenuInputArgs(
        String prompt, IntegerInputArgs integerInputArgs, CharInputArgs charInputArgs) {
      this.prompt = prompt;
      this.integerInputArgs = integerInputArgs;
      this.charInputArgs = charInputArgs;

      if (integerInputArgs == null && charInputArgs == null) {
        throw new IllegalArgumentException("Input arguments cannot be null!");
      }
    }
  }

  public static class GetMenuInputResult {
    public final String input;
    public final boolean isNumber;

    public GetMenuInputResult(String input, boolean isNumber) {
      this.input = input;
      this.isNumber = isNumber;
    }

    public int getAsInt() {
      return Integer.parseInt(input);
    }
  }

  public static GetMenuInputResult getMenuInput(String prompt, int min, int max) {
    return getMenuInput(
        new GetMenuInputArgs(prompt, new GetMenuInputArgs.IntegerInputArgs(min, max)));
  }

  public static GetMenuInputResult getMenuInput(String prompt, char[] validChars) {
    return getMenuInput(
        new GetMenuInputArgs(prompt, null, new GetMenuInputArgs.CharInputArgs(validChars)));
  }

  public static GetMenuInputResult getMenuInput(
      String prompt, int min, int max, char[] validChars) {
    return getMenuInput(
        new GetMenuInputArgs(
            prompt,
            new GetMenuInputArgs.IntegerInputArgs(min, max),
            new GetMenuInputArgs.CharInputArgs(validChars)));
  }

  private static GetMenuInputResult getMenuInput(GetMenuInputArgs inputArgs) {
    System.out.print(inputArgs.prompt);
    String rawInput = scanner.nextLine().trim();

    if (rawInput.isEmpty()) {
      throw new IllegalArgumentException("Input cannot be empty!");
    }

    // Check if includes search for integer
    if (inputArgs.integerInputArgs != null) {
      try {
        int choice = Integer.parseInt(rawInput);

        if (rawInput.length() > 1 && rawInput.startsWith("0")) {
          throw new IllegalArgumentException(
              "Invalid input format! Number must not include leading zeros.");
        }

        // If the number is out of bounds, throw an error
        if (choice < inputArgs.integerInputArgs.min || choice > inputArgs.integerInputArgs.max) {
          if (inputArgs.integerInputArgs.min == inputArgs.integerInputArgs.max) {
            throw new IllegalArgumentException(
                "Invalid input! Number must be " + inputArgs.integerInputArgs.max + ".");
          }

          // if max - min = 1, then just throw must be x or y
          if (inputArgs.integerInputArgs.max - inputArgs.integerInputArgs.min == 1) {
            throw new IllegalArgumentException(
                "Invalid input! Number must be "
                    + inputArgs.integerInputArgs.min
                    + " or "
                    + inputArgs.integerInputArgs.max
                    + ".");
          }

          throw new IllegalArgumentException(
              "Invalid input! Number must be between "
                  + inputArgs.integerInputArgs.min
                  + " and "
                  + inputArgs.integerInputArgs.max
                  + ".");
        }

        return new GetMenuInputResult(rawInput, true);
      } catch (NumberFormatException e) {
        // Not a number exception (throw directly if didn't expect character)
        if (inputArgs.charInputArgs == null) {
          throw new IllegalArgumentException("Invalid input! Please provide a valid number.");
        }
      }
    }

    // Check character input if input is not a valid number
    if (rawInput.length() == 1) {
      char inputChar = Character.toUpperCase(rawInput.charAt(0));

      for (char valid : inputArgs.charInputArgs.validChars) {
        if (Character.toUpperCase(valid) == inputChar) {
          return new GetMenuInputResult(String.valueOf(inputChar), false);
        }
      }
    }

    throw new IllegalArgumentException(
        "Invalid input! You can only choose " + inputArgs.charInputArgs.formatValidChars() + ".");
  }

  public static String getStringInput(String prompt) {
    System.out.print(prompt);
    String input = scanner.nextLine().trim();
    if (input.matches("^[\\x20-\\x7E]*$")) {
      return input;
    }
    throw new IllegalArgumentException(
        "Unsupported characters detected! Please use standard English letters, numbers, and common"
            + " symbols only (e.g. A-Z, 0-9).");
  }

  public static Integer getIntegerInput(String prompt, int min, int max) {
    System.out.print(prompt);
    String rawInput = scanner.nextLine().trim();

    if (rawInput.isEmpty() || "C".equalsIgnoreCase(rawInput)) {
      return null; // Signals keep current value / cancel
    }

    try {
      int choice = Integer.parseInt(rawInput);

      if (rawInput.length() > 1 && rawInput.startsWith("0")) {
        throw new IllegalArgumentException(
            "Invalid input format! Number must not include leading zeros.");
      }

      if (choice < min || choice > max) {
        throw new IllegalArgumentException(
            "Invalid input! Value must be between " + min + " and " + max + ".");
      }

      return choice;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid input! Please provide a valid integer.");
    }
  }

  public static Double getDoubleInput(String prompt, double min, double max) {
    System.out.print(prompt);
    String rawInput = scanner.nextLine().trim();

    if (rawInput.isEmpty() || "C".equalsIgnoreCase(rawInput)) {
      return null;
    }

    try {
      double choice = Double.parseDouble(rawInput);

      if (choice < min || choice > max) {
        throw new IllegalArgumentException(
            "Invalid input! Value must be between " + min + " and " + max + ".");
      }

      return choice;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid input! Please provide a valid decimal number.");
    }
  }

  public static void startRecording() {
    if (originalOut == null) {
      originalOut = System.out;
      System.setOut(
          new PrintStream(new DualStream(originalOut, buffer), true, StandardCharsets.UTF_8));
    }
  }

  public static String getCapturedString() {
    return buffer.toString(StandardCharsets.UTF_8);
  }

  public static void clearBuffer() {
    buffer.reset();
  }

  public static void stopRecording() {
    if (originalOut != null) {
      System.setOut(originalOut);
      originalOut = null;
    }
  }

  private static class DualStream extends OutputStream {
    private final PrintStream terminal;
    private final ByteArrayOutputStream buffer;

    public DualStream(PrintStream terminal, ByteArrayOutputStream buffer) {
      this.terminal = terminal;
      this.buffer = buffer;
    }

    @Override
    public void write(int b) {
      terminal.write(b);
      buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      // Decode bytes to String so System.out uses Windows native terminal rendering
      String text = new String(b, off, len, StandardCharsets.UTF_8);
      terminal.print(text);

      // Save raw UTF-8 bytes to memory buffer for export
      buffer.write(b, off, len);
    }

    @Override
    public void flush() {
      terminal.flush();
    }
  }
}
