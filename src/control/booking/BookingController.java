package control.booking;

import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.booking.BookingView;

public class BookingController {
  private final BookingView bookingView = new BookingView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  public BookingController(
      StandardReservationRepo standardReservationRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      BookingSettingsRepo bookingSettingsRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.bookingSettingsRepo = bookingSettingsRepo;
  }

  public void start() {
    while (true) {
      try {
        int choice = bookingView.displayMenu();

        if (choice == 0) {
          return;
        } else if (choice == 1) {
          // A walk-in is the one thing the desk does under time pressure, so it sits at the top
          // of the module instead of three screens inside queue management.
          newWalkInRegistrationController().registerWalkIn();
        } else if (choice == 2) {
          new WalkInQueueController(
                  standardReservationRepo,
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  bookingSettingsRepo)
              .startQueueManagement();
        } else if (choice == 3) {
          new AdvanceBookingController(
                  standardReservationRepo,
                  vipReservationRepo,
                  guestRepo,
                  memberRepo,
                  roomRepo,
                  bookingSettingsRepo)
              .startAdvanceBookingManagement();
        } else if (choice == 4) {
          new BookingReportController(
                  standardReservationRepo, guestRepo, roomRepo, bookingSettingsRepo)
              .startReportManagement();
        } else if (choice == 5) {
          new BookingSettingsController(bookingSettingsRepo, standardReservationRepo)
              .startSettingsManagement();
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private WalkInRegistrationController newWalkInRegistrationController() {
    return new WalkInRegistrationController(
        standardReservationRepo,
        vipReservationRepo,
        guestRepo,
        memberRepo,
        roomRepo,
        bookingSettingsRepo);
  }
}
