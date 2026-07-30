package view.vip;

import util.ConsoleUtil;

public class VipView {

  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println("1. Manage Waitlist");
    System.out.println("2. Manage Allocation");
    System.out.println("3. Settings & Configurations");
    System.out.println("4. Generate Analytics Report");
    System.out.println("5. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).input;
  }
}
