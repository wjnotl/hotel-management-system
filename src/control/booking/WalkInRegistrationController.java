package control.booking;

import adt.ListInterface;
import adt.QueueInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.booking.WalkInRegistrationView;

// Both the top-level Register Walk-In item and the [A] command inside a line come through here, so
// the assign-or-enqueue rule, the duplicate guard and the member block cannot drift apart.
public class WalkInRegistrationController {
  private static final int MODE_NEW = 2;
  private static final int MODE_BACK = 3;

  private final WalkInRegistrationView registrationView = new WalkInRegistrationView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  public WalkInRegistrationController(
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

  public void registerWalkIn() {
    registerWalkIn(null);
  }

  public void registerWalkIn(Room.RoomType preselectedType) {
    while (true) {
      try {
        int mode = registrationView.displayModeMenu();
        if (mode == MODE_BACK) return;

        Guest guest = (mode == MODE_NEW) ? registerNewGuest() : findExistingGuest();

        // Backing out of either mode returns to the mode menu, so picking the wrong one costs a
        // keystroke rather than the whole flow.
        if (guest == null) continue;

        if (isBlockedByLiveBooking(guest)) continue;

        // Checked before membership, because this module is the only place a standard booking can
        // be claimed and blocking a member first would strand the booking forever.
        Reservation reserved =
            standardReservationRepo.findReservedBookingForGuest(guest.getGuestId());
        if (reserved != null) {
          int choice = promptHasAdvanceBooking(guest, reserved);
          if (choice == 1) {
            arriveOnExistingBooking(reserved, guest);
            return;
          } else if (choice == 3) {
            continue;
          }
        }

        if (isMemberBlocked(guest)) continue;

        Room.RoomType roomType = preselectedType;
        if (roomType == null) {
          roomType = promptRoomType(guest);
          if (roomType == null) continue;
        }

        if (isBlockedBySameLine(guest, roomType)) continue;

        if (placeGuest(guest, roomType)) return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // The existing-guest search offers no registration of its own, because the mode menu already
  // asked that question and the clerk answered it.
  private Guest findExistingGuest() {
    return new GuestLookupController(guestRepo, standardReservationRepo)
        .findGuest("REGISTER WALK-IN - EXISTING GUEST");
  }

  private Guest registerNewGuest() {
    return new GuestRegistrationController(guestRepo).registerNewGuest(null);
  }

  private BookingSettings settings() {
    return bookingSettingsRepo.getSettings();
  }

  private boolean isMemberBlocked(Guest guest) {
    Member member = (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;
    if (member == null || member.getTier() == null) return false;

    registrationView.displayMemberBlockedScreen(guest, member);
    return true;
  }

  // The house rule, asked before a room type has been picked because it does not depend on one.
  private boolean isBlockedByLiveBooking(Guest guest) {
    if (!settings().isBlockDuplicateAcrossLines()) return false;
    return refuseSecondBooking(
        guest, standardReservationRepo.findLiveReservationForGuest(guest.getGuestId()), true);
  }

  // The rule that holds whatever the house setting says, asked once the line is known: nobody may
  // take two places in the same line. Standing in the LUXURY and SUITE lines at once is a
  // different thing and is allowed.
  private boolean isBlockedBySameLine(Guest guest, Room.RoomType roomType) {
    return refuseSecondBooking(
        guest,
        standardReservationRepo.findLiveReservationForGuest(guest.getGuestId(), roomType),
        false);
  }

  private boolean refuseSecondBooking(Guest guest, Reservation live, boolean acrossAllTypes) {
    if (live == null) return false;

    int position = 0;
    if (live.getStatus() == Reservation.Status.WAITING && live.getRoomType() != null) {
      position = standardReservationRepo.getQueueByRoomType(live.getRoomType()).getPosition(live);
    }

    registrationView.displayAlreadyActiveScreen(guest, live, position, acrossAllTypes);
    return true;
  }

  private Room.RoomType promptRoomType(Guest guest) {
    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    int[] vacant = new int[types.length];
    int[] arrivingToday = new int[types.length];
    int[] vipWaiting = new int[types.length];
    int[] lineLength = new int[types.length];

    LocalDate today = LocalDate.now();
    for (int i = 0; i < types.length; i++) {
      vacant[i] = countVacantCleanRooms(types[i]);
      arrivingToday[i] = standardReservationRepo.countReservedArrivingOn(types[i], today);
      vipWaiting[i] = countVipWaiting(types[i]);
      lineLength[i] = standardReservationRepo.getQueueByRoomType(types[i]).getNumberOfEntries();
    }

    while (true) {
      try {
        return registrationView.promptRoomType(
            guest,
            vacant,
            arrivingToday,
            vipWaiting,
            lineLength,
            settings().isEnforceVipBypass(),
            settings().isAutoAssignWhenRoomFree());
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptHasAdvanceBooking(Guest guest, Reservation booking) {
    while (true) {
      try {
        return registrationView.displayHasAdvanceBookingScreen(
            guest, booking, standardReservationRepo.isArrivalDueToday(booking));
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptEnqueueConfirmation(
      Guest guest,
      Room.RoomType roomType,
      int waiting,
      int queueCapacity,
      int vacant,
      int arrivingToday,
      int vipWaiting,
      String reasonForWaiting) {
    while (true) {
      try {
        return registrationView.displayEnqueueConfirmationScreen(
            guest,
            roomType,
            waiting + 1,
            waiting,
            queueCapacity,
            vacant,
            arrivingToday,
            vipWaiting,
            reasonForWaiting);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptAssignConfirmation(
      Guest guest, Room room, int graceMinutes, int vacant, int arrivingToday, int vipWaiting) {
    while (true) {
      try {
        return registrationView.displayAssignConfirmationScreen(
            guest, room, graceMinutes, vacant, arrivingToday, vipWaiting);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // True when this walk-in is finished with, either served, queued or refused. False sends the
  // clerk back to the guest search.
  private boolean placeGuest(Guest guest, Room.RoomType roomType) {
    BookingSettings config = settings();

    int vacant = countVacantCleanRooms(roomType);
    int arrivingToday = arrivingToday(roomType);
    int freeToCounter = Math.max(0, vacant - arrivingToday);
    int vipWaiting = countVipWaiting(roomType);
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    int lineLength = queue.getNumberOfEntries();

    boolean bypassClear = !config.isEnforceVipBypass() || freeToCounter > vipWaiting;

    // Serve at once only when a room of this type is genuinely free to give: not promised to an
    // advance booking arriving today, not owed to a waiting VIP, and with nobody already in line.
    if (config.isAutoAssignWhenRoomFree() && freeToCounter > 0 && bypassClear && lineLength == 0) {
      return assignRoomNow(guest, roomType, vacant, arrivingToday, vipWaiting);
    }

    return enqueueGuest(
        guest,
        roomType,
        vacant,
        arrivingToday,
        vipWaiting,
        reasonForWaiting(config, vacant, arrivingToday, vipWaiting, lineLength, bypassClear));
  }

  private String reasonForWaiting(
      BookingSettings config,
      int vacant,
      int arrivingToday,
      int vipWaiting,
      int lineLength,
      boolean bypassClear) {

    if (vacant == 0) {
      return "No vacant clean room of this type exists, so the guest waits for one to be"
          + " released by housekeeping or a check-out.";
    }
    if (vacant - arrivingToday <= 0) {
      return arrivingToday
          + " advance booking(s) are arriving today against the "
          + vacant
          + " free room(s) of this type, so every one of them is already promised.";
    }
    if (!bypassClear) {
      return vipWaiting
          + " high tier member(s) are entitled to the free room(s) under the VIP bypass rule,"
          + " so none is available to this line yet.";
    }
    if (lineLength > 0) {
      return lineLength
          + " guest(s) arrived earlier and are still waiting, so FIFO order places this guest"
          + " behind them even though a room is free.";
    }
    return "Automatic assignment is switched off under Settings & Configuration, so every"
        + " walk-in joins the line and is called by the [G] Allocate Next command.";
  }

  private boolean assignRoomNow(
      Guest guest, Room.RoomType roomType, int vacant, int arrivingToday, int vipWaiting) {

    Room room = roomRepo.findVacantCleanRoom(roomType);
    if (room == null) {
      ConsoleUtil.printError("No VACANT & CLEAN " + roomType.name() + " room is available!");
      return false;
    }

    int graceMinutes = standardReservationRepo.getHoldGraceMinutes(roomType);

    if (!promptAssignConfirmation(guest, room, graceMinutes, vacant, arrivingToday, vipWaiting)) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();

    // queueArrivalTime is stamped even though the guest never queues, so the wait-time report
    // still counts this arrival and records it as the zero-wait case it actually was.
    Reservation direct =
        new Reservation(
            standardReservationRepo.generateReservationId(),
            guest.getGuestId(),
            standardReservationRepo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.ALLOCATED,
            false,
            0,
            now,
            now,
            false);

    if (!standardReservationRepo.allocateDirect(direct)) {
      ConsoleUtil.printError("The reservation could not be recorded!");
      return true;
    }

    direct.setRoomNumber(room.getRoomNumber());
    standardReservationRepo.updateReservation(direct);
    room.setIsOccupied(true);
    roomRepo.updateRoom(room);

    registrationView.displayAssignSuccessScreen(direct, guest, room, graceMinutes);
    return true;
  }

  private boolean enqueueGuest(
      Guest guest,
      Room.RoomType roomType,
      int vacant,
      int arrivingToday,
      int vipWaiting,
      String reasonForWaiting) {

    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    int waiting = queue.getNumberOfEntries();

    if (standardReservationRepo.isLineClosed(roomType)) {
      registrationView.displayLineFullScreen(
          roomType, waiting, standardReservationRepo.getClosedLineLimit(roomType));
      return true;
    }

    if (!promptEnqueueConfirmation(
        guest,
        roomType,
        waiting,
        standardReservationRepo.getQueueCapacity(roomType),
        vacant,
        arrivingToday,
        vipWaiting,
        reasonForWaiting)) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    Reservation walkIn =
        new Reservation(
            standardReservationRepo.generateReservationId(),
            guest.getGuestId(),
            standardReservationRepo.generateConfirmationNumber(),
            roomType,
            Reservation.Status.WAITING,
            false,
            0,
            now,
            now,
            false);

    if (!standardReservationRepo.addReservation(walkIn)) {
      ConsoleUtil.printError(
          "The "
              + roomType.name()
              + " line is full and is not allowed to grow, so this walk-in could not be added!");
      return true;
    }

    registrationView.displayEnqueueSuccessScreen(
        walkIn,
        guest,
        queue.getPosition(walkIn),
        queue.getNumberOfEntries(),
        standardReservationRepo.getQueueCapacity(roomType));
    return true;
  }

  // The guest booked ahead and has now turned up, so their own booking is claimed rather than a
  // second one being opened alongside it.
  private void arriveOnExistingBooking(Reservation booking, Guest guest) {
    Room.RoomType roomType = booking.getRoomType();

    int vacant = countVacantCleanRooms(roomType);
    int arrivingToday = arrivingToday(roomType);
    int vipWaiting = countVipWaiting(roomType);
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    int lineLength = queue.getNumberOfEntries();

    BookingSettings config = settings();

    // The room this guest is claiming is one of the ones held back for today's arrivals, so it is
    // not subtracted from what they may be given.
    int freeToThisGuest = Math.max(0, vacant - Math.max(0, arrivingToday - 1));
    boolean bypassClear = !config.isEnforceVipBypass() || freeToThisGuest > vipWaiting;

    if (config.isAutoAssignWhenRoomFree()
        && freeToThisGuest > 0
        && bypassClear
        && lineLength == 0) {
      holdRoomForBooking(booking, guest, roomType, vacant, arrivingToday, vipWaiting);
      return;
    }

    if (standardReservationRepo.isLineClosed(roomType)) {
      registrationView.displayLineFullScreen(
          roomType, lineLength, standardReservationRepo.getClosedLineLimit(roomType));
      return;
    }

    if (!standardReservationRepo.joinQueue(booking)) {
      ConsoleUtil.printError("This booking could not be moved into the line!");
      return;
    }

    registrationView.displayMarkArrivalSuccessScreen(
        booking, guest, queue.getPosition(booking), queue.getNumberOfEntries());
  }

  private void holdRoomForBooking(
      Reservation booking,
      Guest guest,
      Room.RoomType roomType,
      int vacant,
      int arrivingToday,
      int vipWaiting) {

    Room room = roomRepo.findVacantCleanRoom(roomType);
    if (room == null) {
      ConsoleUtil.printError("No VACANT & CLEAN " + roomType.name() + " room is available!");
      return;
    }

    int graceMinutes = standardReservationRepo.getHoldGraceMinutes(roomType);

    if (!promptAssignConfirmation(guest, room, graceMinutes, vacant, arrivingToday, vipWaiting)) {
      return;
    }

    if (!standardReservationRepo.allocateDirect(booking)) {
      ConsoleUtil.printError("This booking could not be moved onto a room!");
      return;
    }

    booking.setRoomNumber(room.getRoomNumber());
    standardReservationRepo.updateReservation(booking);
    room.setIsOccupied(true);
    roomRepo.updateRoom(room);

    registrationView.displayAssignSuccessScreen(booking, guest, room, graceMinutes);
  }

  private int arrivingToday(Room.RoomType roomType) {
    return standardReservationRepo.countReservedArrivingOn(roomType, LocalDate.now());
  }

  private int countVacantCleanRooms(Room.RoomType roomType) {
    return standardReservationRepo.countFreeRooms(roomRepo, roomType);
  }

  private int countVipWaiting(Room.RoomType roomType) {
    ListInterface<Reservation> vipLine = vipReservationRepo.getListByRoomType(roomType);
    return (vipLine == null) ? 0 : vipLine.getNumberOfEntries();
  }
}
