package control.frontdesk;

import util.ConsoleUtil;
import view.frontdesk.FrontDeskView;

public class FrontDeskController {
  private final FrontDeskView frontDeskView = new FrontDeskView();
  private final GuestInformationController guestInfoController = new GuestInformationController();
  private final ManageGuestCheckOutController manageGuestCheckOutController =
      new ManageGuestCheckOutController();
  private final ManageRoomStatusController manageRoomStatusController =
      new ManageRoomStatusController();
  private final ReportsController reportsController = new ReportsController();

  public void start() {
    while (true) {
      try {
        String choice = frontDeskView.displayMenu();

        if ("1".equals(choice)) {
          guestInfoController.start();
        } else if ("2".equals(choice)) {
          manageGuestCheckOutController.start();
        } else if ("3".equals(choice)) {
          manageRoomStatusController.start();
        } else if ("4".equals(choice)) {
          reportsController.start();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
