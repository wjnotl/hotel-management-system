package control;

import control.frontdesk.FrontDeskController;
import control.housekeeping.HouseKeepingController;
import control.vip.VipController;
import util.ConsoleUtil;
import util.DatabaseSeeder;
import view.MainMenuView;

public class HotelManagementSystem {
  private static MainMenuView mainMenuView = new MainMenuView();

  public static void main(String[] args) {
    // Database Seeder
    if (args.length > 0 && "--seed".equalsIgnoreCase(args[0])) {
      DatabaseSeeder.seedIfEmpty();
      return;
    }

    while (true) {
      try {
        String choice = mainMenuView.displayMainMenu();
        System.out.println(choice);

        if ("2".equals(choice)) {
          new VipController().start();
        } else if ("3".equals(choice)) {
          new HouseKeepingController().start();
        } else if ("4".equals(choice)) {
          new FrontDeskController().start();
        } else if ("5".equals(choice)) {
          if (ConsoleUtil.showConfirmMessage("Are you sure you want to exit?")) {
            mainMenuView.displayExitMessage();
            System.exit(0);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
