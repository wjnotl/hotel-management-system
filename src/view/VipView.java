package view;

import adt.ListInterface;
import entity.Member;
import entity.Reservation;
import util.ConsoleUtil;
import util.TableUtil;

public class VipView {
  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println("1. Manage Waitlist");
    System.out.println("2. Manage Allocation");
    System.out.println("3. Settings & Configurations");
    System.out.println("4. Generate Analytics Report");
    System.out.println("5. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Select option: ", 1, 5).input;
  }

  public void displayWaitlistTable(
      ListInterface<Reservation> pageResult, int currentPage, int totalPages, int totalResults) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Manage Queue Waitlist");

    var tableSettings =
        new TableUtil.TableSettings(new int[] {5, 10, 20, 15, 10, 9, 9, 8})
            .setHAlign(0, TableUtil.Align.CENTER) // NO.
            .setHAlign(4, TableUtil.Align.CENTER) // TIER
            .setHAlign(5, TableUtil.Align.CENTER) // STRIKES
            .setHAlign(6, TableUtil.Align.CENTER) // BOILING
            .setHAlign(7, TableUtil.Align.RIGHT) // SCORE
            .setTruncate(2); // Truncate long names
  }

  public void manageWaitlist(String search, Member.LoyaltyTier tier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Manage Queue Waitlist");
    System.out.println("Nothing to do yet!");
    ConsoleUtil.printContinueMessage();
  }
}
