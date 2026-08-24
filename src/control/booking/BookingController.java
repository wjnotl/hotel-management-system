package control.booking;

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
  private final BookingSettingsStore bookingSettingsStore;

  public BookingController(
      StandardReservationRepo standardReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      BookingSettingsStore bookingSettingsStore) {
    this.standardReservationRepo = standardReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.bookingSettingsStore = bookingSettingsStore;
  }

  public void start() {
    while (true) {
      try {
        String choice = bookingView.displayMenu();

        if ("6".equals(choice)) {
          return;
        } else if ("1".equals(choice)) {
          // A walk-in is done under time pressure, so it sits at the top of the module.
          newWalkInRegistrationController().registerWalkIn();
        } else if ("2".equals(choice)) {
          new WalkInQueueController(
                  standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsStore)
              .startQueueManagement();
        } else if ("3".equals(choice)) {
          new AdvanceBookingController(
                  standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsStore)
              .startAdvanceBookingManagement();
        } else if ("4".equals(choice)) {
          new BookingReportController(
                  standardReservationRepo, guestRepo, roomRepo, bookingSettingsStore)
              .startReportManagement();
        } else if ("5".equals(choice)) {
          new BookingSettingsController(bookingSettingsStore, standardReservationRepo)
              .startSettingsManagement();
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private WalkInRegistrationController newWalkInRegistrationController() {
    return new WalkInRegistrationController(
        standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsStore);
  }
}
