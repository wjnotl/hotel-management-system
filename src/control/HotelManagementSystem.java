package control;

import util.ConsoleUtil;

public class HotelManagementSystem {
  private static final boolean IS_DEBUG_MODE = false;

  public static void main(String[] args) {
    while (true) {
      try {
        runMainMenu();
      } catch (Exception e) {
        handleGlobalException(e);
      }
    }
  }

  private static void runMainMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MAIN MENU");
    System.out.println("1. Open sub module");
    System.out.println("2. Exit");

    int choice = ConsoleUtil.getIntInput("Select option: ", 1, 2);

    if (choice == 1) {
      runSubMenu();
    } else {
      System.exit(0);
    }
  }

  private static void runSubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SUB MODULE");
    System.out.println("1. Test 1");
    System.out.println("2. Back to main menu");

    int choice = ConsoleUtil.getIntInput("Select option: ", 1, 2);
    if (choice == 1) {
      System.out.println("Test 1");
      ConsoleUtil.getStringInput("Press Enter to continue...");
    } else {
      return;
    }
  }

  private static void handleGlobalException(Exception e) {
    if (IS_DEBUG_MODE) {
      ConsoleUtil.clearScreen();
      System.out.println("=== DEV DEBUG LOG ===");
      e.printStackTrace(System.out);
      System.out.println("=====================");
      ConsoleUtil.getStringInput("\nPress Enter to continue...");
    } else {
      ConsoleUtil.printError(e.getMessage());
    }
  }
}
