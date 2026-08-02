package view.frontdesk;

import util.ConsoleUtil;

public class FrontDeskView {
  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Front Desk Service");
    System.out.println("1. Guest Information");
    System.out.println("2. Manage Guest Check-Out");
    System.out.println("3. Manage Room Status");
    System.out.println("4. Reports");
    System.out.println("5. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).input;
  }
}
