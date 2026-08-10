package control.booking;

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

  public BookingController(
      StandardReservationRepo standardReservationRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
  }

  public void start() {
    while (true) {
      try {
        String choice = bookingView.displayMenu();

        if ("1".equals(choice)) {
          new WalkInQueueController(
                  standardReservationRepo, vipReservationRepo, guestRepo, memberRepo, roomRepo)
              .startQueueManagement();
        } else if ("2".equals(choice)) {
          new AdvanceBookingController(
                  standardReservationRepo, vipReservationRepo, guestRepo, memberRepo, roomRepo)
              .startAdvanceBookingManagement();
        } else if ("3".equals(choice)) {
          new BookingReportController(standardReservationRepo, guestRepo, memberRepo, roomRepo)
              .startReportManagement();
        } else if ("4".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }
}
