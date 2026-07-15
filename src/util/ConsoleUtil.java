package util;

import java.util.Scanner;

public class ConsoleUtil {
  private static final Scanner scanner = new Scanner(System.in);
  private static final int MIN_WIDTH_TITLE_BOX = 40;

  public static void clearScreen() {
    System.out.print("\033\143");
    System.out.flush();
  }

  public static void printTitleBox(String title) {
    if (title == null) title = "";

    int contentWidth = title.length() + 8; // 2 spaces + "::" on both sides
    int boxWidth = Math.max(MIN_WIDTH_TITLE_BOX, contentWidth);

    // Keep box layout symmetric
    if ((boxWidth - 8 - title.length()) % 2 != 0) {
      boxWidth++;
    }

    int totalPadding = boxWidth - title.length() - 8;
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;

    String border = ":".repeat(boxWidth);
    String side = "::";
    String middle = " ".repeat(leftPadding) + title + " ".repeat(rightPadding);

    System.out.println(border);
    System.out.println(side + "  " + middle + "  " + side);
    System.out.println(border + "\n");
  }

  public static void printError(String message) {
    clearScreen();
    System.out.println("Error: " + message);
    System.out.println("Press Enter to continue...");
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

      printError("Invalid choice!");
    }
  }

  public static int getIntInput(String prompt, int min, int max) {
    System.out.print(prompt);

    try {
      int choice = Integer.parseInt(scanner.nextLine().trim());

      // If the number is out of bounds, throw an error
      if (choice < min || choice > max) {
        throw new IllegalArgumentException(
            "Selection out of bounds! Must be between " + min + " and " + max + ".");
      }

      return choice;

    } catch (NumberFormatException e) {
      // If they typed letters, catch the format bug and throw a clean message up
      throw new IllegalArgumentException("Invalid input format! Please type a valid number.");
    }
  }

  public static String getStringInput(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine().trim();
  }
}
