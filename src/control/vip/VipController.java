package control.vip;

import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private final VipView vipView = new VipView();

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          new VipManageWaitlistController().startWaitlistManagement();
        } else if ("2".equals(choice)) {
          new VipManageAllocationController().startAllocationManagement();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
