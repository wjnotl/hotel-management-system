package control;

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
  private static BillingRepo billingRepo = new BillingRepo();
  private static GuestRepo guestRepo = new GuestRepo();
  private static HousekeepingStaffRepo housekeepingStaffRepo = new HousekeepingStaffRepo();
  private static HousekeepingTaskRepo houseKeepingTaskRepo = new HousekeepingTaskRepo();
  private static MemberRepo memberRepo = new MemberRepo();
  private static RoomRepo roomRepo = new RoomRepo();
  private static VipReservationRepo vipReservationRepo = new VipReservationRepo();

  public static void main(String[] args) {
    // Database Seeder
    if (args.length > 0 && "--seed".equalsIgnoreCase(args[0])) {
      DatabaseSeeder.seedIfEmpty();
      return;
    }

    allocationRepo.scheduleNextAutoExpirationTask(
        roomRepo, vipReservationRepo, guestRepo, memberRepo);

    while (true) {
      try {
        String choice = mainMenuView.displayMainMenu();
        System.out.println(choice);

        if ("2".equals(choice)) {
          new VipController(allocationRepo, guestRepo, memberRepo, roomRepo, vipReservationRepo)
              .start();
        } else if ("3".equals(choice)) {
          new HouseKeepingController().start();
        } else if ("4".equals(choice)) {
          new FrontDeskController().start();
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
