package view;

import util.ConsoleUtil;

public class VipView {
  public int displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println("1. VIP Option 1");
    System.out.println("2. Back to main menu");

    return ConsoleUtil.getIntInput("Select option: ", 1, 2);
  }

  public void displayVipOption1() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Option 1");
    System.out.println("Nothing to do yet!");
    ConsoleUtil.printContinueMessage();
  }
}
