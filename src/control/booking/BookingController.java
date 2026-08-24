package control.booking;

import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.BookingView;

public class BookingController {
  private final BookingView bookingView = new BookingView();
  private final StandardReservationRepo standardReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  public BookingController(
      StandardReservationRepo standardReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      BookingSettingsRepo bookingSettingsRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.bookingSettingsRepo = bookingSettingsRepo;
  }

  public void start() {
    while (true) {
      try {
        String choice = bookingView.displayMenu();

        if ("6".equals(choice)) {
          return;
        } else if ("1".equals(choice)) {
          // A walk-in is the one thing the desk does under time pressure, so it sits at the top
          // of the module instead of three screens inside queue management.
          newWalkInRegistrationController().registerWalkIn();
        } else if ("2".equals(choice)) {
          new WalkInQueueController(
                  standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsRepo)
              .startQueueManagement();
        } else if ("3".equals(choice)) {
          new AdvanceBookingController(
                  standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsRepo)
              .startAdvanceBookingManagement();
        } else if ("4".equals(choice)) {
          new BookingReportController(
                  standardReservationRepo, guestRepo, roomRepo, bookingSettingsRepo)
              .startReportManagement();
        } else if ("5".equals(choice)) {
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
        standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsRepo);
  }
}
