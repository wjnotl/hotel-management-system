package view;

import util.ConsoleUtil;

public class MainMenuView {
  public int displayMainMenu() {
    ConsoleUtil.clearScreen();

    System.out.print(
        """
        ████████╗ █████╗ ██████╗ ██╗   ██╗███╗   ███╗████████╗
        ╚══██╔══╝██╔══██╗██╔══██╗██║   ██║████╗ ████║╚══██╔══╝
           ██║   ███████║██████╔╝██║   ██║██╔████╔██║   ██║
           ██║   ██╔══██║██╔══██╗██║   ██║██║╚██╔╝██║   ██║
           ██║   ██║  ██║██║  ██║╚██████╔╝██║ ╚═╝ ██║   ██║
           ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝   ╚═╝
        """);
    ConsoleUtil.printTitleBox("RESORT MANAGEMENT SYSTEM", 54);

    System.out.println("1. Standard Booking & Walk-In");
    System.out.println("2. VIP Priority Room Allocation");
    System.out.println("3. Housekeeping & Task Logging");
    System.out.println("4. Front-Desk Search & Services");
    System.out.println("5. Exit\n");

    return ConsoleUtil.getIntInput("Select option: ", 1, 5);
  }
}
