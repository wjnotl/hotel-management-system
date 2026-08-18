package view.booking;

import util.ConsoleUtil;

public class BookingView {

  private static final int SCREEN_WIDTH = 83;

  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN & BOOKING", SCREEN_WIDTH);
    System.out.println("1. Register Walk-In Guest");
    System.out.println("2. Walk-In Queue Management");
    System.out.println("3. Advance Reservations");
    System.out.println("4. Booking Reports & Analytics");
    System.out.println("5. Settings & Configuration");
    System.out.println("6. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).input;
  }
}
