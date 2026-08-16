package control.frontdesk;

import repo.BillingRepo;
import repo.GuestRepo;
import repo.HousekeepingTaskRepo;
import repo.ReservationRepo;
import repo.RoomRepo;
import repo.RoomStatusHistoryRepo;
import util.ConsoleUtil;
import view.frontdesk.FrontDeskView;

public class FrontDeskController {
  private final FrontDeskView frontDeskView = new FrontDeskView();
  private final GuestRepo guestRepo;
  private final BillingRepo billingRepo;
  private final ReservationRepo reservationRepo;
  private final RoomRepo roomRepo;
  private final RoomStatusHistoryRepo roomStatusHistoryRepo;
  private final HousekeepingTaskRepo housekeepingTaskRepo;

  public FrontDeskController(
      GuestRepo guestRepo,
      BillingRepo billingRepo,
      ReservationRepo reservationRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo,
      HousekeepingTaskRepo housekeepingTaskRepo,
      RoomRepo roomRepo) {
    this.guestRepo = guestRepo;
    this.billingRepo = billingRepo;
    this.reservationRepo = reservationRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
    this.housekeepingTaskRepo = housekeepingTaskRepo;
    this.roomRepo = roomRepo;
  }

  public void start() {
    while (true) {
      try {
        String choice = frontDeskView.displayMenu();

        if ("1".equals(choice)) {
          new ManageGuestController(reservationRepo, guestRepo, billingRepo).start();
        } else if ("2".equals(choice)) {
          new ManageGuestCheckOutController(
                  guestRepo, billingRepo, roomRepo, roomStatusHistoryRepo, housekeepingTaskRepo)
              .start();
        } else if ("3".equals(choice)) {
          new ManageRoomStatusController(
                  roomRepo, reservationRepo, guestRepo, roomStatusHistoryRepo)
              .start();
        } else if ("4".equals(choice)) {
          new ReportsController(billingRepo, guestRepo, roomRepo).start();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
