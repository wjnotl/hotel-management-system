package view;

import util.ConsoleUtil;

public class MainMenuView {
  private void displayPrettyLogo() {
    if (ConsoleUtil.isRunningInIDE()) {
      System.out.print(
          "_________ _______  _______           _______ _________\r\n"
              + //
              "\\__   __/(  ___  )(  ____ )|\\     /|(       )\\__   __/\r\n"
              + //
              "   ) (   | (   ) || (    )|| )   ( || () () |   ) (   \r\n"
              + //
              "   | |   | (___) || (____)|| |   | || || || |   | |   \r\n"
              + //
              "   | |   |  ___  ||     __)| |   | || |(_)| |   | |   \r\n"
              + //
              "   | |   | (   ) || (\\ (   | |   | || |   | |   | |   \r\n"
              + //
              "   | |   | )   ( || ) \\ \\__| (___) || )   ( |   | |   \r\n"
              + //
              "   )_(   |/     \\||/   \\__/(_______)|/     \\|   )_(   \r\n"
              + //
              "                                                      \n");
    } else {
      System.out.print(
          """
          ████████╗ █████╗ ██████╗ ██╗   ██╗███╗   ███╗████████╗
          ╚══██╔══╝██╔══██╗██╔══██╗██║   ██║████╗ ████║╚══██╔══╝
             ██║   ███████║██████╔╝██║   ██║██╔████╔██║   ██║
             ██║   ██╔══██║██╔══██╗██║   ██║██║╚██╔╝██║   ██║
             ██║   ██║  ██║██║  ██║╚██████╔╝██║ ╚═╝ ██║   ██║
             ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝   ╚═╝
          """);
    }
  }

  public String displayMainMenu() {
    ConsoleUtil.clearScreen();

    displayPrettyLogo();
    ConsoleUtil.printTitleBox("RESORT MANAGEMENT SYSTEM", 54);

    System.out.println("1. Standard Booking & Walk-In");
    System.out.println("2. VIP Priority Room Allocation");
    System.out.println("3. Housekeeping & Task Logging");
    System.out.println("4. Front-Desk Search & Services");
    System.out.println("5. Exit\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).input;
  }

  public void displayExitMessage() {
    ConsoleUtil.clearScreen();

    displayPrettyLogo();
    ConsoleUtil.printTitleBox("RESORT MANAGEMENT SYSTEM", 54);

    System.out.println(" ".repeat(22) + "Thank you!" + " ".repeat(22));
  }
}
