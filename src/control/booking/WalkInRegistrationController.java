package control.booking;

import adt.QueueInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.WalkInRegistrationView;

// The menu item and the [A] command both come through here, so the rules cannot drift.
public class WalkInRegistrationController {
  private static final int MODE_NEW = 2;
  private static final int MODE_BACK = 3;

  private final WalkInRegistrationView registrationView = new WalkInRegistrationView();
  private final StandardReservationRepo standardReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsStore bookingSettingsStore;

  public WalkInRegistrationController(
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

  public void registerWalkIn() {
    registerWalkIn(null);
  }

  public void registerWalkIn(Room.RoomType preselectedType) {
    while (true) {
      try {
        int mode = registrationView.displayModeMenu();
        if (mode == MODE_BACK) return;

        Guest guest = (mode == MODE_NEW) ? registerNewGuest() : findExistingGuest();

        // Backing out returns to the mode menu, not out of the walk-in.
        if (guest == null) continue;

        if (isBlockedByLiveBooking(guest)) continue;

        // Checked before membership, or a member with a booking would be stranded.
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

  // No registration here; the mode menu already asked that.
  private Guest findExistingGuest() {
    return new GuestLookupController(guestRepo, standardReservationRepo)
        .findGuest("REGISTER WALK-IN - EXISTING GUEST");
  }

  private Guest registerNewGuest() {
    return new GuestRegistrationController(guestRepo).registerNewGuest(null);
  }

  private BookingSettings settings() {
    return bookingSettingsStore.getSettings();
  }

  private boolean isMemberBlocked(Guest guest) {
    Member member = (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;
    if (member == null || member.getTier() == null) return false;

    registrationView.displayMemberBlockedScreen(guest, member);
    return true;
  }

  // The house rule, asked before a room type is picked because it does not need one.
  private boolean isBlockedByLiveBooking(Guest guest) {
    if (!settings().isBlockDuplicateAcrossLines()) return false;
    return refuseSecondBooking(
        guest, standardReservationRepo.findLiveReservationForGuest(guest.getGuestId()), true);
  }

  // The rule that always holds: no two places in one line. Two different lines are fine.
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

  // True when the walk-in is finished with. False returns to the guest search.
  private boolean placeGuest(Guest guest, Room.RoomType roomType) {
    BookingSettings config = settings();

    int vacant = countVacantCleanRooms(roomType);
    int arrivingToday = arrivingToday(roomType);
    int freeToCounter = Math.max(0, vacant - arrivingToday);
    int vipWaiting = countVipWaiting(roomType);
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    int lineLength = queue.getNumberOfEntries();

    boolean bypassClear = !config.isEnforceVipBypass() || freeToCounter > vipWaiting;

    // Serve now only when a room is truly free: none promised today, none owed to a VIP, no line.
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
      return "No vacant clean room of this type exists.";
    }
    if (vacant - arrivingToday <= 0) {
      return arrivingToday
          + " advance booking(s) arrive today against "
          + vacant
          + " free room(s), so all are promised.";
    }
    if (!bypassClear) {
      return vipWaiting + " VIP guest(s) are entitled to the free room(s) under the bypass rule.";
    }
    if (lineLength > 0) {
      return lineLength + " guest(s) arrived earlier, so FIFO places this guest behind them.";
    }
    return "Automatic assignment is off in Settings, so every walk-in joins the line.";
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

    // Stamped though nobody queued, so the wait report counts this as a zero wait.
    Reservation direct =
        new Reservation(
            standardReservationRepo.generateReservationId(),
            guest.getGuestId(),
            // The confirmation code is minted at check-in, so the record carries none yet.
            null,
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
            // The confirmation code is minted at check-in, so the record carries none yet.
            null,
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

  // The guest booked ahead and turned up, so that booking is claimed, not a second one.
  private void arriveOnExistingBooking(Reservation booking, Guest guest) {
    Room.RoomType roomType = booking.getRoomType();

    int vacant = countVacantCleanRooms(roomType);
    int arrivingToday = arrivingToday(roomType);
    int vipWaiting = countVipWaiting(roomType);
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    int lineLength = queue.getNumberOfEntries();

    BookingSettings config = settings();

    // This guest is claiming one of today's held-back rooms, so it is not subtracted twice.
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
    return standardReservationRepo.countVipWaiting(roomType);
  }
}
