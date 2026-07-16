package control;

import control.vip.VipController;
import util.ConsoleUtil;
import view.MainMenuView;

public class HotelManagementSystem {
  private static MainMenuView mainMenuView = new MainMenuView();

  public static void main(String[] args) {
    while (true) {
      try {
        int choice = mainMenuView.displayMainMenu();

        if (choice == 2) {
          new VipController().start();
        } else if (choice == 5) {
          System.exit(0);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
