package util;

import java.util.Scanner;

public class ConsoleUtil {
  private static final Scanner scanner = new Scanner(System.in);

  public static void clearScreen() {
    System.out.print("\033\143");
    System.out.flush();
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
    System.out.println(message);
    scanner.nextLine();
  }

  public static boolean showConfirmMessage(String message) {
    while (true) {
      clearScreen();
      System.out.print(message + " (Y/N): ");
      String choice = scanner.nextLine().trim();

      if (choice.equalsIgnoreCase("Y")) {
        return true;
      }
      if (choice.equalsIgnoreCase("N")) {
        return false;
      }

      printError("Invalid choice! You can only choose Y or N.");
    }
  }

  public static int getIntInput(String prompt, int min, int max) {
    System.out.print(prompt);

    String rawInput = scanner.nextLine().trim();

    if (rawInput.length() > 1 && rawInput.startsWith("0")) {
      throw new IllegalArgumentException("Invalid input format! Do not include leading zeros.");
    }

    try {
      int choice = Integer.parseInt(rawInput);

      // If the number is out of bounds, throw an error
      if (choice < min || choice > max) {
        // if max - min = 1, then just throw must be x or y
        if (max - min == 1) {
          throw new IllegalArgumentException("Invalid input! Must be " + min + " or " + max + ".");
        }

        throw new IllegalArgumentException(
            "Invalid input! Must be between " + min + " and " + max + ".");
      }

      return choice;

    } catch (NumberFormatException e) {
      // If they typed letters, catch the format bug and throw a clean message up
      throw new IllegalArgumentException("Invalid input! Please type a valid number.");
    }
  }

  public static String getStringInput(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine().trim();
  }
}
