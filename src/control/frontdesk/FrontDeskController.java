package control.frontdesk;

import util.ConsoleUtil;
import view.FrontDeskView;

public class FrontDeskController {
  private final FrontDeskView frontDeskView = new FrontDeskView();
  private final GuestInformationController GuestInfoController = new GuestInformationController();

  public void start() {
    while (true) {
      try {
        String choice = frontDeskView.displayMenu();

        if ("1".equals(choice)) {
          GuestInfoController.start();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
