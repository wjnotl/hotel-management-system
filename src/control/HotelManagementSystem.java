package control;

import control.vip.VipController;
import util.ConsoleUtil;
import view.MainMenuView;

public class HotelManagementSystem {
  private static MainMenuView mainMenuView = new MainMenuView();

  public static void main(String[] args) {
    while (true) {
      try {
        String choice = mainMenuView.displayMainMenu();
        System.out.println(choice);

        if ("2".equals(choice)) {
          new VipController().start();
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
