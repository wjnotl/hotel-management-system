package control.vip;

import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private final VipView vipView = new VipView();
  private final VipManageWaitlistController waitlistController = new VipManageWaitlistController();

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          waitlistController.startWaitlistManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
