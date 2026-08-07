package view.booking;

import util.ConsoleUtil;

public class BookingView {

  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Standard Booking & Walk-In");
    System.out.println("1. Walk-In Queue Management");
    System.out.println("2. Advance Reservations");
    System.out.println("3. Generate Booking Report");
    System.out.println("4. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).input;
  }
}
