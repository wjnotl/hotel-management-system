package control.vip;

import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private VipView vipView = new VipView();

  public void start() {
    while (true) {
      try {
        int choice = vipView.displayMenu();

        if (choice == 1) {
          handleVipOption1();
        } else if (choice == 2) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void handleVipOption1() {
    // TODO: Implement VIP Option 1
    vipView.displayVipOption1();
  }
}
