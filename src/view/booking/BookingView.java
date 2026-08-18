package view.booking;

import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;

public class BookingView {

  private static final int SCREEN_WIDTH = 83;

  // Returns 1..5, or 0 to leave the module.
  public int displayMenu() {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("WALK-IN & BOOKING", SCREEN_WIDTH);
      System.out.println("1. Register Walk-In Guest");
      System.out.println("2. Walk-In Queue Management");
      System.out.println("3. Advance Reservations");
      System.out.println("4. Booking Reports & Analytics");
      System.out.println("5. Settings & Configuration");
      System.out.println();
      System.out.println("B - Back to Main Menu");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput("Choose an option: ", 1, 5, new char[] {'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return 0;
        return result.getAsInt();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }
}
