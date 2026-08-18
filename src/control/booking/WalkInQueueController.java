package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import adt.QueueInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.booking.WalkInQueueView;

public class WalkInQueueController {
  private static final String FIELD_NAME = "GUEST NAME";
  private static final String FIELD_GUEST_ID = "GUEST ID";
  private static final String FIELD_IC = "IC NUMBER";
  private static final String FIELD_PASSPORT = "PASSPORT NO";
  private static final String FIELD_PHONE = "PHONE NUMBER";
  private static final String FIELD_EMAIL = "EMAIL ADDRESS";
  private static final String FIELD_RES_ID = "RESERVATION ID";
  private static final String FIELD_CODE = "CONFIRMATION CODE";
  private static final String FIELD_ALL = "ALL FIELDS";

  private final WalkInQueueView walkInQueueView = new WalkInQueueView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  public WalkInQueueController(
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

  private BookingSettings settings() {
    return bookingSettingsRepo.getSettings();
  }

  public void startQueueManagement() {
    while (true) {
      try {
        Room.RoomType selectedLine = walkInQueueView.displayQueueSelectionMenu();
        if (selectedLine == null) {
          return;
        }

        manageLine(selectedLine);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageLine(Room.RoomType roomType) {
    int currentPage = 1;
    String searchField = FIELD_NAME;
    String searchTerm = null;
    boolean exactMatch = false;
    Integer minWaitMinutes = null;
    String sortCriteria = settings().getDefaultQueueSort();

    while (true) {
      try {
        // Read fresh each pass, so a page size or override rule changed under Settings takes
        // effect the moment the clerk comes back to this screen.
        BookingSettings config = settings();
        int pageSize = config.getPageSize();
        int graceMinutes = standardReservationRepo.getHoldGraceMinutes(roomType);

        int lapsed = standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);
        if (lapsed > 0) {
          walkInQueueView.displaySweepNotice(
              lapsed, graceMinutes, config.getMaxStrikes(), config.isRequeueOnLapse());
        }

        QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
        ListInterface<Reservation> lineRows =
            filterAndSortLine(
                standardReservationRepo.snapshotQueue(roomType),
                queue,
                searchField,
                searchTerm,
                exactMatch,
                minWaitMinutes,
                sortCriteria);
        ListInterface<Reservation> holds = standardReservationRepo.getHoldsByRoomType(roomType);

        ConsoleUtil.GetMenuInputResult result =
            walkInQueueView.renderQueueScreen(
                queue,
                lineRows,
                holds,
                guestRepo.getGuestList(),
                roomType,
                standardReservationRepo.getQueueCapacity(roomType),
                countVacantCleanRooms(roomType),
                arrivingToday(roomType),
                countVipWaiting(roomType),
                graceMinutes,
                searchField,
                searchTerm,
                exactMatch ? "EXACT" : "CONTAINS",
                minWaitMinutes,
                sortCriteria,
                currentPage,
                pageSize,
                config.isEnforceVipBypass());

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) {
          return;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleAddWalkIn(roomType);
          currentPage = 1;
        } else if ("G".equalsIgnoreCase(result.input)) {
          handleAllocateNext(roomType);
        } else if ("C".equalsIgnoreCase(result.input)) {
          handleCheckInHold(holds, roomType, pageSize);
        } else if ("X".equalsIgnoreCase(result.input)) {
          handleCloseQueue(roomType);
          currentPage = 1;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters =
              handleFilterMenu(
                  searchField, searchTerm, exactMatch ? "EXACT" : "CONTAINS", minWaitMinutes);
          searchField = filters[0];
          searchTerm = filters[1];
          exactMatch = "EXACT".equals(filters[2]);
          minWaitMinutes = (filters[3] == null) ? null : Integer.valueOf(filters[3]);
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = walkInQueueView.displaySortMenu();
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) lineRows.getNumberOfEntries() / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          }
        } else if (result.isNumber) {
          handleRowAction(lineRows, result.getAsInt(), currentPage, pageSize, roomType);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleAddWalkIn(Room.RoomType roomType) {
    new WalkInRegistrationController(
            standardReservationRepo,
            vipReservationRepo,
            guestRepo,
            memberRepo,
            roomRepo,
            bookingSettingsRepo)
        .registerWalkIn(roomType);
  }

  private void handleAllocateNext(Room.RoomType roomType) {
    Reservation front = standardReservationRepo.getQueueByRoomType(roomType).peek();
    if (front == null) {
      ConsoleUtil.printError("Nobody is standing in the " + roomType.name() + " line!");
      return;
    }

    allocateToGuest(front, roomType);
  }

  // One path for both [G] Allocate Next and the row submenu, so the front of the line and a
  // deliberate skip cannot end up with different rules.
  private void allocateToGuest(Reservation target, Room.RoomType roomType) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);

    int position = queue.getPosition(target);
    if (position == -1) {
      ConsoleUtil.printError("That booking is no longer standing in the line!");
      return;
    }

    BookingSettings config = settings();

    int vacant = countVacantCleanRooms(roomType);
    int arrivingTodayCount = arrivingToday(roomType);
    int freeToCounter = Math.max(0, vacant - arrivingTodayCount);
    int vipWaiting = countVipWaiting(roomType);

    if (freeToCounter <= 0) {
      walkInQueueView.displayNoVacantRoomScreen(roomType, arrivingTodayCount, vipWaiting);
      return;
    }

    boolean fifoSkip = position > 1;
    if (fifoSkip && !config.isAllowNonFrontAllocation()) {
      walkInQueueView.displayNonFrontBlockedScreen(position);
      return;
    }

    // High tier members bypass this line, so a free room only reaches the standard queue once
    // every waiting VIP for that room type could already have been given one.
    boolean vipBypass = config.isEnforceVipBypass() && freeToCounter <= vipWaiting;
    if (vipBypass && !config.isAllowBypassOverride()) {
      walkInQueueView.displayBypassBlockedScreen(roomType, freeToCounter, vipWaiting);
      return;
    }

    boolean overridden = fifoSkip || vipBypass;
    if (overridden
        && !walkInQueueView.displayAllocationOverrideScreen(
            roomType,
            target,
            guestRepo.findById(target.getGuestId()),
            position,
            queue.getNumberOfEntries(),
            guestsAheadOf(roomType, position),
            guestRepo.getGuestList(),
            freeToCounter,
            vipWaiting,
            fifoSkip,
            vipBypass)) {
      return;
    }

    Room room = roomRepo.findVacantCleanRoom(roomType);
    if (room == null) {
      ConsoleUtil.printError("No VACANT & CLEAN " + roomType.name() + " room is available!");
      return;
    }

    Guest guest = guestRepo.findById(target.getGuestId());

    if (!walkInQueueView.displayAllocateConfirmationScreen(
        target,
        guest,
        room,
        standardReservationRepo.getHoldGraceMinutes(roomType),
        vipWaiting,
        position)) {
      return;
    }

    Reservation allocated = standardReservationRepo.allocateQueued(target);
    if (allocated == null) {
      ConsoleUtil.printError("The guest left the line before the room could be assigned!");
      return;
    }

    allocated.setRoomNumber(room.getRoomNumber());
    standardReservationRepo.updateReservation(allocated);
    room.setStatus(Room.Status.OCCUPIED);
    roomRepo.updateRoom(room);

    walkInQueueView.displayAllocateSuccessScreen(
        allocated, guest, room, queue.getNumberOfEntries(), overridden);
  }

  private ListInterface<Reservation> guestsAheadOf(Room.RoomType roomType, int position) {
    ListInterface<Reservation> ahead = new ArrayList<>();
    ListInterface<Reservation> snapshot = standardReservationRepo.snapshotQueue(roomType);

    for (int i = 1; i < position && i <= snapshot.getNumberOfEntries(); i++) {
      Reservation r = snapshot.getEntry(i);
      if (r != null) ahead.add(r);
    }
    return ahead;
  }

  private void handleCheckInHold(
      ListInterface<Reservation> holds, Room.RoomType roomType, int pageSize) {
    if (holds == null || holds.isEmpty()) {
      ConsoleUtil.printError("No " + roomType.name() + " rooms are currently on hold!");
      return;
    }

    Integer selection =
        walkInQueueView.promptHoldSelection(
            holds,
            guestRepo.getGuestList(),
            standardReservationRepo.getHoldGraceMinutes(roomType),
            pageSize);
    if (selection == null) {
      return;
    }

    Reservation hold = holds.getEntry(selection);
    if (hold == null) {
      ConsoleUtil.printError("Invalid hold selection!");
      return;
    }

    Guest guest = guestRepo.findById(hold.getGuestId());
    Room room = standardReservationRepo.findHeldRoom(hold, roomRepo);

    Integer stayDays =
        walkInQueueView.promptStayDays(
            hold, guest, room, settings().getMaxStayNights(), maxNightsFrom(roomType));
    if (stayDays == null) {
      return;
    }

    standardReservationRepo.checkIn(hold, stayDays);
    walkInQueueView.displayCheckInSuccessScreen(hold, guest, room, stayDays);
  }

  // The first night is always available because this guest is already holding the room. Only the
  // nights after it have to be checked against what else the calendar has promised.
  private int maxNightsFrom(Room.RoomType roomType) {
    int cap = settings().getMaxStayNights();
    int extra =
        standardReservationRepo.findLongestBookableStay(
            roomRepo, roomType, LocalDate.now().plusDays(1), cap - 1);
    return Math.max(1, Math.min(cap, extra + 1));
  }

  private void handleCloseQueue(Room.RoomType roomType) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    if (queue.isEmpty()) {
      ConsoleUtil.printError("The " + roomType.name() + " line is already empty!");
      return;
    }

    if (!walkInQueueView.displayCloseQueueConfirmationScreen(
        roomType, queue.getNumberOfEntries())) {
      return;
    }

    int closed = standardReservationRepo.closeQueue(roomType);
    walkInQueueView.displayCloseQueueSuccessScreen(roomType, closed);
  }

  private void handleRowAction(
      ListInterface<Reservation> lineRows,
      int indexOnPage,
      int page,
      int pageSize,
      Room.RoomType roomType) {

    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > lineRows.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = lineRows.getEntry(actualIndex);
    if (selected == null) return;

    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);

    while (true) {
      try {
        Guest guest = guestRepo.findById(selected.getGuestId());
        int position = queue.getPosition(selected);

        int action =
            walkInQueueView.displayRowActionSubmenu(
                selected, guest, position, queue.getNumberOfEntries());

        if (action == 1) {
          walkInQueueView.displayReservationDetailScreen(
              selected, guest, position, queue.getNumberOfEntries());
        } else if (action == 2) {
          allocateToGuest(selected, roomType);
          return;
        } else if (action == 3) {
          if (walkInQueueView.displayCancelConfirmationScreen(selected, guest, position)) {
            standardReservationRepo.cancelReservation(selected);
            return;
          }
        } else {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns {field, term, matchMode, minWait}. minWait is carried as a string so the whole filter
  // set travels as one array rather than four out-parameters.
  private String[] handleFilterMenu(
      String currentField, String currentTerm, String currentMode, Integer currentMinWait) {

    String field = currentField;
    String term = currentTerm;
    String mode = currentMode;
    Integer minWait = currentMinWait;

    while (true) {
      try {
        int choice = walkInQueueView.displayFilterMainMenu(field, term, mode, minWait);

        if (choice == 1) {
          int picked = walkInQueueView.displaySearchFieldSubmenu(field);
          if (picked > 0) field = fieldNameFor(picked);
        } else if (choice == 2) {
          term = walkInQueueView.promptSearchTerm(field, term);
        } else if (choice == 3) {
          mode = "EXACT".equals(mode) ? "CONTAINS" : "EXACT";
        } else if (choice == 4) {
          minWait = walkInQueueView.promptMinimumWait(minWait);
        } else if (choice == 5) {
          field = FIELD_NAME;
          term = null;
          mode = "CONTAINS";
          minWait = null;
        } else if (choice == 6) {
          return new String[] {
            field, term, mode, (minWait == null) ? null : String.valueOf(minWait)
          };
        } else {
          return new String[] {
            currentField,
            currentTerm,
            currentMode,
            (currentMinWait == null) ? null : String.valueOf(currentMinWait)
          };
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String fieldNameFor(int choice) {
    if (choice == 1) return FIELD_NAME;
    if (choice == 2) return FIELD_GUEST_ID;
    if (choice == 3) return FIELD_IC;
    if (choice == 4) return FIELD_PASSPORT;
    if (choice == 5) return FIELD_PHONE;
    if (choice == 6) return FIELD_EMAIL;
    if (choice == 7) return FIELD_RES_ID;
    if (choice == 8) return FIELD_CODE;
    return FIELD_ALL;
  }

  private ListInterface<Reservation> filterAndSortLine(
      ListInterface<Reservation> source,
      QueueInterface<Reservation> queue,
      String field,
      String term,
      boolean exactMatch,
      Integer minWaitMinutes,
      String sort) {

    ListInterface<Reservation> filtered = new ArrayList<>();
    if (source == null || source.isEmpty()) {
      return filtered;
    }

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      if (!matchesSearch(r, g, field, term, exactMatch)) continue;
      if (minWaitMinutes != null && waitMinutesOf(r) < minWaitMinutes) continue;

      filtered.add(r);
    }

    if ("WAIT TIME (LONGEST -> SHORTEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareArrival(a, b));
    } else if ("WAIT TIME (SHORTEST -> LONGEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareArrival(b, a));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(a).compareToIgnoreCase(guestNameOf(b)));
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("STRIKES (HIGHEST -> LOWEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> Integer.compare(strikesOf(b), strikesOf(a)));
    } else {
      filtered.sort((a, b) -> Integer.compare(queue.getPosition(a), queue.getPosition(b)));
    }

    return filtered;
  }

  private boolean matchesSearch(
      Reservation r, Guest g, String field, String term, boolean exactMatch) {
    if (term == null || term.trim().isEmpty()) return true;

    String query = term.trim().toLowerCase();

    if (FIELD_GUEST_ID.equals(field))
      return hit(g == null ? null : g.getGuestId(), query, exactMatch);
    if (FIELD_NAME.equals(field)) return hit(g == null ? null : g.getName(), query, exactMatch);
    if (FIELD_IC.equals(field)) return hit(g == null ? null : g.getIcNumber(), query, exactMatch);
    if (FIELD_PASSPORT.equals(field)) {
      return hit(g == null ? null : g.getPassportNumber(), query, exactMatch);
    }
    if (FIELD_PHONE.equals(field)) {
      return hit(g == null ? null : g.getPhoneNumber(), query, exactMatch);
    }
    if (FIELD_EMAIL.equals(field)) return hit(g == null ? null : g.getEmail(), query, exactMatch);
    if (FIELD_RES_ID.equals(field)) return hit(r.getReservationId(), query, exactMatch);
    if (FIELD_CODE.equals(field)) return hit(r.getConfirmationNumber(), query, exactMatch);

    return hit(r.getReservationId(), query, exactMatch)
        || hit(r.getConfirmationNumber(), query, exactMatch)
        || (g != null
            && (hit(g.getGuestId(), query, exactMatch)
                || hit(g.getName(), query, exactMatch)
                || hit(g.getIcNumber(), query, exactMatch)
                || hit(g.getPassportNumber(), query, exactMatch)
                || hit(g.getPhoneNumber(), query, exactMatch)
                || hit(g.getEmail(), query, exactMatch)));
  }

  private boolean hit(String value, String query, boolean exactMatch) {
    if (value == null) return false;
    String candidate = value.toLowerCase();
    return exactMatch ? candidate.equals(query) : candidate.contains(query);
  }

  private long waitMinutesOf(Reservation r) {
    if (r.getQueueArrivalTime() == null) return 0;
    return Duration.between(r.getQueueArrivalTime(), LocalDateTime.now()).toMinutes();
  }

  private int compareArrival(Reservation a, Reservation b) {
    LocalDateTime first = a.getQueueArrivalTime();
    LocalDateTime second = b.getQueueArrivalTime();
    if (first == null && second == null) return 0;
    if (first == null) return -1;
    if (second == null) return 1;
    return first.compareTo(second);
  }

  private String guestNameOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null && g.getName() != null) ? g.getName() : "";
  }

  private int strikesOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null) ? g.getStrikeCount() : 0;
  }

  private int arrivingToday(Room.RoomType roomType) {
    return standardReservationRepo.countReservedArrivingOn(roomType, LocalDate.now());
  }

  private int countVacantCleanRooms(Room.RoomType roomType) {
    ListInterface<Room> rooms = roomRepo.getRoomList();
    int count = 0;
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room != null
          && room.getRoomType() == roomType
          && room.getStatus() == Room.Status.VACANT_CLEAN) {
        count++;
      }
    }
    return count;
  }

  private int countVipWaiting(Room.RoomType roomType) {
    ListInterface<Reservation> vipLine = vipReservationRepo.getListByRoomType(roomType);
    return (vipLine == null) ? 0 : vipLine.getNumberOfEntries();
  }
}
