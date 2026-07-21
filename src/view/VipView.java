package view;

import entity.Member;
import util.ConsoleUtil;

public class VipView {
  public int displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println("1. Manage Waitlist");
    System.out.println("2. Manage Allocation");
    System.out.println("3. Settings & Configurations");
    System.out.println("4. Generate Analytics Report");
    System.out.println("5. Back to Main Menu\n");

    return ConsoleUtil.getIntInput("Select option: ", 1, 5);
  }

  public void manageWaitlist(String search, Member.LoyaltyTier tier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Manage Queue Waitlist");
    System.out.println("Nothing to do yet!");
    ConsoleUtil.printContinueMessage();
  }
}
