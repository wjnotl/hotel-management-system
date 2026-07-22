package control.vip;

import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private VipView vipView = new VipView();

  public VipController() {}

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if (choice == "1") {
          manageWaitlist();
        } else if (choice == "5") {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void manageWaitlist() {
    vipView.manageWaitlist(null, null);
  }
}
