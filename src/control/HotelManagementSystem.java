package control;

import control.booking.BookingController;
import control.frontdesk.FrontDeskController;
import control.housekeeping.HouseKeepingController;
import control.vip.VipController;
import repo.*;
import util.ConsoleUtil;
import util.DatabaseSeeder;
import view.MainMenuView;

public class HotelManagementSystem {
  private static MainMenuView mainMenuView = new MainMenuView();

  private static AllocationRepo allocationRepo = new AllocationRepo();
  private static GuestRepo guestRepo = new GuestRepo();
  private static HousekeepingStaffRepo housekeepingStaffRepo = new HousekeepingStaffRepo();
  private static HousekeepingTaskRepo houseKeepingTaskRepo = new HousekeepingTaskRepo();
  private static HousekeepingSettingsRepo housekeepingSettingsRepo = new HousekeepingSettingsRepo();
  private static MemberRepo memberRepo = new MemberRepo();
  private static RoomRepo roomRepo = new RoomRepo();
  private static RoomStatusHistoryRepo roomStatusHistoryRepo = new RoomStatusHistoryRepo();
  private static ReservationRepo reservationRepo = new ReservationRepo();
  private static BookingSettingsRepo bookingSettingsRepo = new BookingSettingsRepo();
  private static StandardReservationRepo standardReservationRepo =
      new StandardReservationRepo(reservationRepo, bookingSettingsRepo);
  private static VipReservationRepo vipReservationRepo = new VipReservationRepo(reservationRepo);
  private static VipSystemConfigRepo vipSystemConfigRepo = new VipSystemConfigRepo();
  private static BillingRepo billingRepo = new BillingRepo();

  public static void main(String[] args) {
    // Database Seeder
    if (args.length > 0 && "--seed".equalsIgnoreCase(args[0])) {
      DatabaseSeeder.seedIfEmpty();
      return;
    }

    VipController.scheduleNextAutoExpirationTask(
        allocationRepo, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
    VipController.scheduleNextBoilingTask(
        vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
    VipController.startMidnightStrikeResetScheduler(guestRepo, vipSystemConfigRepo);

    while (true) {
      try {
        String choice = mainMenuView.displayMainMenu();

        if ("1".equals(choice)) {
          new BookingController(
                  standardReservationRepo,
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  bookingSettingsRepo)
              .start();
        } else if ("2".equals(choice)) {
          new VipController(
                  allocationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  vipReservationRepo,
                  vipSystemConfigRepo)
              .start();
        } else if ("3".equals(choice)) {
          new HouseKeepingController(
                  houseKeepingTaskRepo,
                  housekeepingStaffRepo,
                  roomRepo,
                  roomStatusHistoryRepo,
                  housekeepingSettingsRepo)
              .start();
        } else if ("4".equals(choice)) {
          new FrontDeskController(
                  guestRepo,
                  billingRepo,
                  reservationRepo,
                  roomStatusHistoryRepo,
                  houseKeepingTaskRepo,
                  roomRepo)
              .start();
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
