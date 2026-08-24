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
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.WalkInQueueView;

public class WalkInQueueController {
  private static final String BLANK_INPUT = "Input cannot be empty!";

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
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsStore bookingSettingsStore;

  public WalkInQueueController(
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

  private BookingSettings settings() {
    return bookingSettingsStore.getSettings();
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
        // Read fresh each pass, so a setting changed under Settings takes effect at once.
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
                buildLineRowDTO(lineRows, queue),
                buildHoldRowDTO(holds, graceMinutes),
                roomType,
                describeNextUp(queue.peek()),
                queue.getNumberOfEntries(),
                standardReservationRepo.getQueueCapacity(roomType),
                queue.isFull(),
                standardReservationRepo.canQueueExpand(roomType),
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

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleAddWalkIn(roomType);
          currentPage = 1;
        } else if ("G".equalsIgnoreCase(result.input)) {
          handleAllocateNext(roomType);
        } else if ("C".equalsIgnoreCase(result.input)) {
          handleHoldAction(holds, roomType, pageSize, graceMinutes);
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
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if (result.isNumber) {
          handleRowAction(lineRows, result.getAsInt(), currentPage, pageSize, roomType);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<WalkInQueueView.LineRowDTO> buildLineRowDTO(
      ListInterface<Reservation> lineRows, QueueInterface<Reservation> queue) {

    ListInterface<WalkInQueueView.LineRowDTO> rows = new ArrayList<>();

    for (int i = 1; i <= lineRows.getNumberOfEntries(); i++) {
      Reservation r = lineRows.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      // Read off the queue, so the true place in line shows under any table sort.
      int position = queue.getPosition(r);

      rows.add(
          new WalkInQueueView.LineRowDTO(
              (position == -1) ? "-" : String.valueOf(position),
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              (g != null) ? blankToNa(g.getPhoneNumber()) : "N/A",
              formatWait(r.getQueueArrivalTime()),
              (g != null) ? g.getStrikeCount() : 0));
    }
    return rows;
  }

  private ListInterface<WalkInQueueView.HoldRowDTO> buildHoldRowDTO(
      ListInterface<Reservation> holds, int graceMinutes) {

    ListInterface<WalkInQueueView.HoldRowDTO> rows = new ArrayList<>();
    if (holds == null) return rows;

    for (int i = 1; i <= holds.getNumberOfEntries(); i++) {
      Reservation r = holds.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      rows.add(
          new WalkInQueueView.HoldRowDTO(
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              (r.getRoomNumber() != null) ? r.getRoomNumber() : "UNLINKED",
              formatWait(r.getAllocatedTime()),
              formatRemaining(r.getAllocatedTime(), graceMinutes)));
    }
    return rows;
  }

  private String describeNextUp(Reservation next) {
    if (next == null) return "Line is empty";

    Guest g = guestRepo.findById(next.getGuestId());
    return ((g != null) ? g.getName() : "Unknown") + "  (" + next.getReservationId() + ")";
  }

  private void handleAddWalkIn(Room.RoomType roomType) {
    new WalkInRegistrationController(
            standardReservationRepo, guestRepo, memberRepo, roomRepo, bookingSettingsStore)
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

  // One path for [G] and the row submenu, so front and skip share the same rules.
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

    // A free room reaches this line only once every waiting VIP could have had one.
    boolean vipBypass = config.isEnforceVipBypass() && freeToCounter <= vipWaiting;
    if (vipBypass && !config.isAllowBypassOverride()) {
      walkInQueueView.displayBypassBlockedScreen(roomType, freeToCounter, vipWaiting);
      return;
    }

    Guest guest = guestRepo.findById(target.getGuestId());
    boolean overridden = fifoSkip || vipBypass;

    if (overridden
        && !promptAllocationOverride(
            roomType,
            target,
            guest,
            position,
            queue.getNumberOfEntries(),
            position - 1,
            namesAheadOf(roomType, position),
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

    if (!promptAllocateConfirmation(
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
    room.setIsOccupied(true);
    roomRepo.updateRoom(room);

    walkInQueueView.displayAllocateSuccessScreen(
        allocated, guest, room, queue.getNumberOfEntries(), overridden);

    // The guest is standing there, so the hold usually becomes a check-in at once.
    if (promptCheckInNow(guest, room)) {
      checkInHold(allocated, guest, room, roomType);
    }
  }

  private boolean promptCheckInNow(Guest guest, Room room) {
    while (true) {
      try {
        return walkInQueueView.displayCheckInNowScreen(guest, room);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // One check-in path for the offer above and the [C] hold list, so both stamp alike.
  private void checkInHold(Reservation hold, Guest guest, Room room, Room.RoomType roomType) {
    Integer stayDays =
        promptStayDays(hold, guest, room, settings().getMaxStayNights(), maxNightsFrom(roomType));
    if (stayDays == null) return;

    standardReservationRepo.checkIn(hold, stayDays);
    walkInQueueView.displayCheckInSuccessScreen(
        hold, guest, room, stayDays, hold.getOccupancyEndDate());
  }

  private String namesAheadOf(Room.RoomType roomType, int position) {
    ListInterface<Reservation> snapshot = standardReservationRepo.snapshotQueue(roomType);
    int ahead = Math.min(position - 1, snapshot.getNumberOfEntries());
    if (ahead <= 0) return "nobody";

    StringBuilder names = new StringBuilder();
    int shown = Math.min(ahead, 3);

    for (int i = 1; i <= shown; i++) {
      Reservation r = snapshot.getEntry(i);
      Guest g = (r == null) ? null : guestRepo.findById(r.getGuestId());
      if (names.length() > 0) names.append(", ");
      names.append((g != null) ? g.getName() : (r != null ? r.getReservationId() : "N/A"));
    }

    if (ahead > shown) {
      names.append(" and ").append(ahead - shown).append(" other(s)");
    }
    return names.toString();
  }

  private int promptSearchField(String current) {
    while (true) {
      try {
        return walkInQueueView.displaySearchFieldSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Blank redraws, so a stray Enter never clears the filter.
  private String promptSearchTerm(String fieldLabel, String current) {
    while (true) {
      try {
        String typed = walkInQueueView.promptSearchTerm(fieldLabel, current);
        if (typed.trim().isEmpty()) continue;
        return typed;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Integer promptMinimumWait(Integer current) {
    while (true) {
      try {
        return walkInQueueView.promptMinimumWait(current);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptAllocationOverride(
      Room.RoomType roomType,
      Reservation target,
      Guest guest,
      int position,
      int waiting,
      int aheadCount,
      String namesAhead,
      int freeToCounter,
      int vipWaiting,
      boolean fifoSkip,
      boolean vipBypass) {
    while (true) {
      try {
        return walkInQueueView.displayAllocationOverrideScreen(
            roomType,
            target,
            guest,
            position,
            waiting,
            aheadCount,
            namesAhead,
            freeToCounter,
            vipWaiting,
            fifoSkip,
            vipBypass);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptAllocateConfirmation(
      Reservation target, Guest guest, Room room, int graceMinutes, int vipWaiting, int position) {
    while (true) {
      try {
        return walkInQueueView.displayAllocateConfirmationScreen(
            target, guest, room, graceMinutes, vipWaiting, position);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptCloseQueueConfirmation(Room.RoomType roomType, int waiting) {
    while (true) {
      try {
        return walkInQueueView.displayCloseQueueConfirmationScreen(roomType, waiting);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Integer promptStayDays(
      Reservation hold, Guest guest, Room room, int maxStayNights, int availableNights) {
    while (true) {
      try {
        return walkInQueueView.promptStayDays(hold, guest, room, maxStayNights, availableNights);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns the 1-based index into the whole hold list, or null when the clerk backed out.
  private Integer promptHoldSelection(
      ListInterface<WalkInQueueView.HoldRowDTO> rows, int graceMinutes, int pageSize) {
    int total = (rows == null) ? 0 : rows.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);
    int page = 1;

    while (true) {
      try {
        page = Math.min(Math.max(page, 1), Math.max(totalPages, 1));
        ConsoleUtil.GetMenuInputResult result =
            walkInQueueView.renderHoldPicker(rows, graceMinutes, page, pageSize);

        if ("E".equalsIgnoreCase(result.input)) return null;

        if ("N".equalsIgnoreCase(result.input)) {
          if (page < totalPages) {
            page++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
          continue;
        }
        if ("P".equalsIgnoreCase(result.input)) {
          if (page > 1) {
            page--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
          continue;
        }

        return (page - 1) * pageSize + result.getAsInt();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleHoldAction(
      ListInterface<Reservation> holds, Room.RoomType roomType, int pageSize, int graceMinutes) {
    if (holds == null || holds.isEmpty()) {
      ConsoleUtil.printError("No " + roomType.name() + " rooms are currently on hold!");
      return;
    }

    Integer selection =
        promptHoldSelection(buildHoldRowDTO(holds, graceMinutes), graceMinutes, pageSize);
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

    int action = promptHoldAction(hold, guest, room, graceMinutes);
    if (action == 1) {
      checkInHold(hold, guest, room, roomType);
    } else if (action == 2) {
      markHoldNoShow(hold, guest, room);
    }
  }

  private int promptHoldAction(Reservation hold, Guest guest, Room room, int graceMinutes) {
    while (true) {
      try {
        return walkInQueueView.displayHoldActionScreen(hold, guest, room, graceMinutes);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // The strike is the shared daily counter, so this only ever adds one.
  private void markHoldNoShow(Reservation hold, Guest guest, Room room) {
    int strikes = (guest != null) ? guest.getStrikeCount() : 0;

    if (!promptHoldNoShowConfirmation(hold, guest, room, strikes)) {
      return;
    }

    if (!standardReservationRepo.markHoldNoShow(hold, roomRepo)) {
      ConsoleUtil.printError("That hold is no longer open!");
      return;
    }

    int after = strikes;
    if (guest != null) {
      after = strikes + 1;
      guest.setStrikeCount(after);
      guestRepo.updateGuest(guest);
    }

    walkInQueueView.displayHoldNoShowSuccessScreen(hold, guest, room, after);
  }

  private boolean promptHoldNoShowConfirmation(
      Reservation hold, Guest guest, Room room, int strikesNow) {
    while (true) {
      try {
        return walkInQueueView.displayHoldNoShowConfirmationScreen(
            hold, guest, room, strikesNow, standardReservationRepo.getMaxStrikes());
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // The first night is already held, so only the later nights are checked.
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

    if (!promptCloseQueueConfirmation(roomType, queue.getNumberOfEntries())) {
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

        int vacant = countVacantCleanRooms(roomType);
        int freeToCounter = Math.max(0, vacant - arrivingToday(roomType));
        int vipWaiting = countVipWaiting(roomType);
        Room onOffer = roomRepo.findVacantCleanRoom(roomType);

        // Shown before the choice, so the clerk knows which assign option will go through.
        boolean fifoSkip = position > 1;
        boolean vipBypass = settings().isEnforceVipBypass() && freeToCounter <= vipWaiting;

        int action =
            walkInQueueView.displayAllocationDetailScreen(
                selected,
                guest,
                position,
                queue.getNumberOfEntries(),
                (onOffer == null) ? null : onOffer.getRoomNumber(),
                freeToCounter,
                vipWaiting,
                fifoSkip || vipBypass);

        if (action == 1 || action == 2) {
          allocateToGuest(selected, roomType);
          return;
        } else if (action == 3) {
          if (promptCancelConfirmation(selected, guest, position)) {
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

  private boolean promptCancelConfirmation(Reservation target, Guest guest, int position) {
    while (true) {
      try {
        return walkInQueueView.displayCancelConfirmationScreen(target, guest, position);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns {field, term, matchMode, minWait}, so the filter set moves as one array.
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
          int picked = promptSearchField(field);
          if (picked > 0) field = fieldNameFor(picked);
        } else if (choice == 2) {
          String typed = promptSearchTerm(field, term);
          if (!"E".equalsIgnoreCase(typed.trim())) {
            term = "-".equals(typed.trim()) ? null : typed.trim();
          }
        } else if (choice == 3) {
          mode = "EXACT".equals(mode) ? "CONTAINS" : "EXACT";
        } else if (choice == 4) {
          Integer entered = promptMinimumWait(minWait);
          if (entered != null) {
            minWait = (entered == 0) ? null : entered;
          }
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

    if (FIELD_GUEST_ID.equals(field)) {
      return hit(g == null ? null : g.getGuestId(), query, exactMatch);
    }
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

  private String formatWait(LocalDateTime from) {
    if (from == null) return "N/A";

    long minutes = Duration.between(from, LocalDateTime.now()).toMinutes();
    if (minutes < 0) return "0m";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  private String formatRemaining(LocalDateTime allocatedTime, int graceMinutes) {
    if (allocatedTime == null) return "N/A";

    long secondsLeft =
        (graceMinutes * 60L) - Duration.between(allocatedTime, LocalDateTime.now()).toSeconds();
    if (secondsLeft <= 0) return "LAPSED";
    return (secondsLeft / 60) + "m " + String.format("%02ds", secondsLeft % 60);
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

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
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
