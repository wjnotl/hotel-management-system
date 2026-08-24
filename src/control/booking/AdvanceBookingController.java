package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.AdvanceBookingView;

public class AdvanceBookingController {
  private static final int MODE_NEW = 2;
  private static final int MODE_BACK = 3;

  private static final String FIELD_NAME = "GUEST NAME";
  private static final String FIELD_GUEST_ID = "GUEST ID";
  private static final String FIELD_IC = "IC NUMBER";
  private static final String FIELD_PASSPORT = "PASSPORT NO";
  private static final String FIELD_PHONE = "PHONE NUMBER";
  private static final String FIELD_EMAIL = "EMAIL ADDRESS";
  private static final String FIELD_RES_ID = "RESERVATION ID";
  private static final String FIELD_CODE = "CONFIRMATION CODE";
  private static final String FIELD_ALL = "ALL FIELDS";

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

  private final AdvanceBookingView advanceBookingView = new AdvanceBookingView();
  private final StandardReservationRepo standardReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsStore bookingSettingsStore;

  public AdvanceBookingController(
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

  public void startAdvanceBookingManagement() {
    int currentPage = 1;
    String searchField = FIELD_NAME;
    String searchTerm = null;
    boolean exactMatch = false;
    String roomTypeFilter = null;
    LocalDate fromDate = null;
    LocalDate toDate = null;
    String sortCriteria = settings().getDefaultAdvanceSort();

    while (true) {
      try {
        int pageSize = settings().getPageSize();

        standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);

        ListInterface<Reservation> reserved = collectReserved();
        ListInterface<Reservation> rows =
            filterAndSort(
                reserved,
                searchField,
                searchTerm,
                exactMatch,
                roomTypeFilter,
                fromDate,
                toDate,
                sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            advanceBookingView.renderAdvanceScreen(
                buildBookingRowDTO(rows),
                countByRoomType(reserved, Room.RoomType.LUXURY),
                countByRoomType(reserved, Room.RoomType.SUITE),
                countByRoomType(reserved, Room.RoomType.STANDARD),
                countArrivingToday(reserved),
                searchField,
                searchTerm,
                exactMatch ? "EXACT" : "CONTAINS",
                roomTypeFilter,
                fromDate,
                toDate,
                sortCriteria,
                currentPage,
                pageSize);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleNewBooking();
          currentPage = 1;
        } else if ("S".equalsIgnoreCase(result.input)) {
          Object[] filters =
              handleFilterMenu(
                  searchField,
                  searchTerm,
                  exactMatch ? "EXACT" : "CONTAINS",
                  roomTypeFilter,
                  fromDate,
                  toDate);
          searchField = (String) filters[0];
          searchTerm = (String) filters[1];
          exactMatch = "EXACT".equals(filters[2]);
          roomTypeFilter = (String) filters[3];
          fromDate = (LocalDate) filters[4];
          toDate = (LocalDate) filters[5];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = advanceBookingView.displaySortMenu();
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) rows.getNumberOfEntries() / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          }
        } else if (result.isNumber) {
          handleRowAction(rows, result.getAsInt(), currentPage, pageSize);
          currentPage = 1;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleNewBooking() {
    while (true) {
      try {
        int mode = advanceBookingView.displayModeMenu();
        if (mode == MODE_BACK) return;

        Guest guest = (mode == MODE_NEW) ? registerNewGuest() : findExistingGuest();

        // Backing out returns to the mode menu, not out of the booking.
        if (guest == null) continue;

        // A tier buys no priority here, because the booking already holds the night.
        if (collectBooking(guest)) return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // No registration here; the mode menu already asked that.
  private Guest findExistingGuest() {
    return new GuestLookupController(guestRepo, standardReservationRepo)
        .findGuest("NEW ADVANCE BOOKING - EXISTING GUEST");
  }

  private Guest registerNewGuest() {
    return new GuestRegistrationController(guestRepo).registerNewGuest(null);
  }

  private boolean collectBooking(Guest guest) {
    BookingSettings config = settings();

    LocalDate arrivalDate = null;
    Integer nights = null;

    while (true) {
      arrivalDate = promptArrivalDate(guest, config, arrivalDate);
      if (arrivalDate == null) return false;

      while (true) {
        Room.RoomType roomType = promptRoomType(guest, arrivalDate);
        if (roomType == null) break;

        while (true) {
          int maxBookable =
              standardReservationRepo.findLongestBookableStay(
                  roomRepo, roomType, arrivalDate, config.getMaxStayNights());

          if (config.isBlockOverbooking() && maxBookable <= 0) {
            advanceBookingView.displayFullyBookedScreen(
                roomType,
                arrivalDate,
                arrivalDate,
                standardReservationRepo.getTotalRoomsOfType(roomRepo, roomType));
            break;
          }

          int allowed = config.isBlockOverbooking() ? maxBookable : config.getMaxStayNights();

          Integer picked =
              promptNights(roomType, arrivalDate, allowed, config.getMaxStayNights(), nights);
          if (picked == null) break;

          nights = picked;

          // Re-checked late, because another booking may have taken the last room meanwhile.
          LocalDate firstFull =
              standardReservationRepo.findFirstFullDate(roomRepo, roomType, arrivalDate, nights);
          if (config.isBlockOverbooking() && firstFull != null) {
            advanceBookingView.displayFullyBookedScreen(
                roomType,
                arrivalDate,
                firstFull,
                standardReservationRepo.getTotalRoomsOfType(roomRepo, roomType));
            continue;
          }

          // A booking reserves a night, not a moment, so the date is stored at start of day.
          LocalDateTime arrival = arrivalDate.atStartOfDay();
          int freeAcross =
              standardReservationRepo.countAvailableAcross(roomRepo, roomType, arrivalDate, nights);

          if (!promptNewBookingConfirmation(
              guest, roomType, arrival, nights, arrivalDate.plusDays(nights), freeAcross - 1)) {
            continue;
          }

          Reservation booking =
              new Reservation(
                  standardReservationRepo.generateReservationId(),
                  guest.getGuestId(),
                  standardReservationRepo.generateConfirmationNumber(),
                  roomType,
                  Reservation.Status.RESERVED,
                  false,
                  0,
                  LocalDateTime.now(),
                  null,
                  // isVip marks the owning module, not the tier, and the repo filters on it.
                  false);

          booking.setExpectedArrivalTime(arrival);
          booking.setStayDays(nights);

          standardReservationRepo.addReservation(booking);
          advanceBookingView.displayNewBookingSuccessScreen(booking, guest);
          return true;
        }
      }
    }
  }

  private LocalDate earliestArrival(BookingSettings config) {
    LocalDate today = LocalDate.now();
    return config.isAllowSameDayAdvanceBooking() ? today : today.plusDays(1);
  }

  private LocalDate latestArrival(BookingSettings config) {
    int lead = config.getAdvanceBookingLeadDays();
    if (lead <= BookingSettings.UNLIMITED_LEAD_DAYS) {
      return LocalDate.now().plusYears(5);
    }
    return LocalDate.now().plusDays(lead);
  }

  // Null means the clerk left. Retrying here keeps a bad date on the date screen.
  private LocalDate promptArrivalDate(Guest guest, BookingSettings config, LocalDate current) {
    while (true) {
      try {
        LocalDate earliest = earliestArrival(config);
        LocalDate latest = latestArrival(config);

        String typed = advanceBookingView.promptArrivalDate(guest, earliest, latest, current);
        if (typed == null || "E".equalsIgnoreCase(typed.trim())) return null;

        if (typed.trim().isEmpty()) {
          throw new IllegalArgumentException("Arrival date cannot be empty!");
        }

        LocalDate picked = parseDate(typed.trim());
        if (picked.isBefore(earliest)) {
          throw new IllegalArgumentException(
              "That date is before the bookable window opens on " + format(earliest) + ".");
        }
        if (picked.isAfter(latest)) {
          throw new IllegalArgumentException(
              "Bookings are only taken up to "
                  + format(latest)
                  + " under the current lead time setting.");
        }

        return picked;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Room.RoomType promptRoomType(Guest guest, LocalDate arrival) {
    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    int[] total = new int[types.length];
    int[] free = new int[types.length];

    for (int i = 0; i < types.length; i++) {
      total[i] = standardReservationRepo.getTotalRoomsOfType(roomRepo, types[i]);
      free[i] = standardReservationRepo.countAvailableOn(roomRepo, types[i], arrival);
    }

    while (true) {
      try {
        return advanceBookingView.promptRoomType(guest, arrival, total, free);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Integer promptNights(
      Room.RoomType roomType, LocalDate arrival, int maxBookable, int houseMax, Integer current) {
    while (true) {
      try {
        return advanceBookingView.promptNights(roomType, arrival, maxBookable, houseMax, current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptNewBookingConfirmation(
      Guest guest,
      Room.RoomType roomType,
      LocalDateTime arrival,
      int nights,
      LocalDate checkOutDate,
      int othersFree) {
    while (true) {
      try {
        return advanceBookingView.displayNewBookingConfirmationScreen(
            guest, roomType, arrival, nights, checkOutDate, othersFree);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptCheckInConfirmation(
      Reservation booking,
      Guest guest,
      Room room,
      int nights,
      Member.LoyaltyTier tier,
      LocalDate checkOutDate) {
    while (true) {
      try {
        return advanceBookingView.displayCheckInConfirmationScreen(
            booking, guest, room, nights, tier, checkOutDate);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptCancelConfirmation(Reservation booking, Guest guest) {
    while (true) {
      try {
        return advanceBookingView.displayCancelConfirmationScreen(booking, guest);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void markArrival(Reservation booking) {
    if (booking == null) return;

    Guest guest = guestRepo.findById(booking.getGuestId());

    if (!standardReservationRepo.isArrivalDueToday(booking)) {
      LocalDate bookedFor = standardReservationRepo.bookedArrivalDate(booking);
      advanceBookingView.displayWrongArrivalDayScreen(
          booking, guest, bookedFor, ChronoUnit.DAYS.between(LocalDate.now(), bookedFor));
      return;
    }

    Reservation alreadyQueued =
        findQueuedReservationForGuest(booking.getRoomType(), booking.getGuestId());
    if (alreadyQueued != null) {
      advanceBookingView.displayAlreadyInLineScreen(guest, alreadyQueued);
      return;
    }

    // The slot was promised, but the room must still be clean. Never cancel over this.
    Room room = roomRepo.findVacantCleanRoom(booking.getRoomType());
    if (room == null) {
      advanceBookingView.displayNoRoomReadyScreen(
          booking, guest, countVacantCleanRooms(booking.getRoomType()));
      return;
    }

    int nights = (booking.getStayDays() != null) ? booking.getStayDays() : 1;

    if (!promptCheckInConfirmation(
        booking, guest, room, nights, tierOf(guest), LocalDate.now().plusDays(nights))) {
      return;
    }

    booking.setRoomNumber(room.getRoomNumber());
    standardReservationRepo.updateReservation(booking);

    if (!standardReservationRepo.checkIn(booking, nights)) {
      ConsoleUtil.printError("This booking could not be checked in!");
      return;
    }

    room.setIsOccupied(true);
    roomRepo.updateRoom(room);

    advanceBookingView.displayCheckInSuccessScreen(
        booking, guest, room, nights, booking.getOccupancyEndDate());
  }

  // A no-show costs the night, so it carries the same strike a lapsed hold does.
  private boolean markNoShow(Reservation booking, Guest guest) {
    int strikes = strikesOf(guest);

    if (!promptNoShowConfirmation(
        booking, guest, strikes, standardReservationRepo.getMaxStrikes())) {
      return false;
    }

    booking.setStatus(Reservation.Status.NO_SHOW);
    standardReservationRepo.updateReservation(booking);

    int after = strikes;
    if (guest != null) {
      after = strikes + 1;
      guest.setStrikeCount(after);
      guestRepo.updateGuest(guest);
    }

    advanceBookingView.displayClosureSuccessScreen(booking, guest, true, after);
    return true;
  }

  private int strikesOf(Guest guest) {
    return (guest != null) ? guest.getStrikeCount() : 0;
  }

  private boolean promptNoShowConfirmation(
      Reservation booking, Guest guest, int strikesNow, int maxStrikes) {
    while (true) {
      try {
        return advanceBookingView.displayNoShowConfirmationScreen(
            booking, guest, strikesNow, maxStrikes);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int countVacantCleanRooms(Room.RoomType roomType) {
    return standardReservationRepo.countFreeRooms(roomRepo, roomType);
  }

  private Member.LoyaltyTier tierOf(Guest guest) {
    if (guest == null || guest.getMemberId() == null) return null;
    Member member = memberRepo.findById(guest.getMemberId());
    return (member == null) ? null : member.getTier();
  }

  private void handleRowAction(
      ListInterface<Reservation> rows, int indexOnPage, int page, int pageSize) {
    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > rows.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = rows.getEntry(actualIndex);
    if (selected == null) return;

    while (true) {
      try {
        Guest guest = guestRepo.findById(selected.getGuestId());
        int action = advanceBookingView.displayRowActionSubmenu(selected, guest);

        if (action == 1) {
          advanceBookingView.displayDetailScreen(selected, guest, tierOf(guest));
        } else if (action == 2) {
          markArrival(selected);
          return;
        } else if (action == 3) {
          if (promptCancelConfirmation(selected, guest)) {
            standardReservationRepo.cancelReservation(selected);
            advanceBookingView.displayClosureSuccessScreen(
                selected, guest, false, strikesOf(guest));
            return;
          }
        } else if (action == 4) {
          if (markNoShow(selected, guest)) return;
        } else {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns {field, term, mode, roomType, from, to}.
  private Object[] handleFilterMenu(
      String currentField,
      String currentTerm,
      String currentMode,
      String currentRoomType,
      LocalDate currentFrom,
      LocalDate currentTo) {

    String field = currentField;
    String term = currentTerm;
    String mode = currentMode;
    String roomType = currentRoomType;
    LocalDate from = currentFrom;
    LocalDate to = currentTo;

    while (true) {
      try {
        int choice =
            advanceBookingView.displayFilterMainMenu(field, term, mode, roomType, from, to);

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
          int picked = promptRoomTypeFilter(roomType);
          if (picked == 1) roomType = "LUXURY";
          else if (picked == 2) roomType = "SUITE";
          else if (picked == 3) roomType = "STANDARD";
          else if (picked == 4) roomType = null;
        } else if (choice == 5) {
          LocalDate[] range = askDateRange(from, to);
          from = range[0];
          to = range[1];
        } else if (choice == 6) {
          field = FIELD_NAME;
          term = null;
          mode = "CONTAINS";
          roomType = null;
          from = null;
          to = null;
        } else if (choice == 7) {
          return new Object[] {field, term, mode, roomType, from, to};
        } else {
          return new Object[] {
            currentField, currentTerm, currentMode, currentRoomType, currentFrom, currentTo
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

  private ListInterface<AdvanceBookingView.BookingRowDTO> buildBookingRowDTO(
      ListInterface<Reservation> bookings) {

    ListInterface<AdvanceBookingView.BookingRowDTO> rows = new ArrayList<>();

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Reservation r = bookings.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      Member.LoyaltyTier tier = tierOf(g);
      LocalDate start = r.getOccupancyStartDate();
      Integer nights = r.getStayDays();

      rows.add(
          new AdvanceBookingView.BookingRowDTO(
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              r.getRoomType().name(),
              (tier == null) ? "NONE" : tier.name(),
              (start != null) ? start.format(SHORT_DATE_FORMAT) : "-",
              (start != null && nights != null)
                  ? start.plusDays(nights).format(SHORT_DATE_FORMAT)
                  : "-",
              (nights != null) ? String.valueOf(nights) : "-"));
    }
    return rows;
  }

  private LocalDate parseDate(String raw) {
    try {
      return LocalDate.parse(raw, DATE_FORMAT);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date! Type it as YYYY-MM-DD, for example 2026-08-21.");
    }
  }

  private int promptSearchField(String current) {
    while (true) {
      try {
        return advanceBookingView.displaySearchFieldSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptRoomTypeFilter(String current) {
    while (true) {
      try {
        return advanceBookingView.displayRoomTypeSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Blank redraws, so a stray Enter never clears the filter.
  private String promptSearchTerm(String fieldLabel, String current) {
    while (true) {
      try {
        String typed = advanceBookingView.promptSearchTerm(fieldLabel, current);
        if (typed.trim().isEmpty()) continue;
        return typed;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private LocalDate[] askDateRange(LocalDate currentFrom, LocalDate currentTo) {
    while (true) {
      try {
        String[] typed = advanceBookingView.promptDateRange(currentFrom, currentTo);
        String from = (typed[0] == null) ? "" : typed[0].trim();
        if (from.isEmpty()) continue;
        if ("E".equalsIgnoreCase(from)) {
          return new LocalDate[] {currentFrom, currentTo};
        }
        if (typed[1] == null || typed[1].trim().isEmpty()) continue;

        LocalDate parsedFrom = parseOrNull(typed[0]);
        LocalDate parsedTo = parseOrNull(typed[1]);
        if (parsedFrom != null && parsedTo != null && parsedTo.isBefore(parsedFrom)) {
          throw new IllegalArgumentException("The end of the range cannot fall before its start!");
        }
        return new LocalDate[] {parsedFrom, parsedTo};
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private LocalDate parseOrNull(String raw) {
    if (raw == null || raw.trim().isEmpty() || "-".equals(raw.trim())) return null;
    return parseDate(raw.trim());
  }

  private String format(LocalDate date) {
    return (date == null) ? "N/A" : date.format(DATE_FORMAT);
  }

  private ListInterface<Reservation> collectReserved() {
    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    ListInterface<Reservation> reserved = new ArrayList<>();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && r.getStatus() == Reservation.Status.RESERVED) {
        reserved.add(r);
      }
    }
    return reserved;
  }

  private ListInterface<Reservation> filterAndSort(
      ListInterface<Reservation> source,
      String field,
      String term,
      boolean exactMatch,
      String roomTypeFilter,
      LocalDate fromDate,
      LocalDate toDate,
      String sort) {

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      if (!matchesSearch(r, g, field, term, exactMatch)) continue;
      if (roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase(r.getRoomType().name())) {
        continue;
      }
      if (!matchesDateRange(r, fromDate, toDate)) continue;

      filtered.add(r);
    }

    if ("ARRIVAL DATE (LATEST -> SOONEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareArrival(b, a));
    } else if ("BOOKED AT (NEWEST -> OLDEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareBookedAt(b, a));
    } else if ("BOOKED AT (OLDEST -> NEWEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareBookedAt(a, b));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(a).compareToIgnoreCase(guestNameOf(b)));
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("ROOM TYPE (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> a.getRoomType().name().compareTo(b.getRoomType().name()));
    } else {
      filtered.sort((a, b) -> compareArrival(a, b));
    }

    return filtered;
  }

  private boolean matchesDateRange(Reservation r, LocalDate from, LocalDate to) {
    if (from == null && to == null) return true;

    LocalDateTime expected = r.getExpectedArrivalTime();
    if (expected == null) return false;

    LocalDate arrival = expected.toLocalDate();
    if (from != null && arrival.isBefore(from)) return false;
    return to == null || !arrival.isAfter(to);
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

  private int compareArrival(Reservation a, Reservation b) {
    LocalDateTime first = a.getExpectedArrivalTime();
    LocalDateTime second = b.getExpectedArrivalTime();
    if (first == null && second == null) return 0;
    if (first == null) return 1;
    if (second == null) return -1;
    return first.compareTo(second);
  }

  private int compareBookedAt(Reservation a, Reservation b) {
    LocalDateTime first = a.getReservationTime();
    LocalDateTime second = b.getReservationTime();
    if (first == null && second == null) return 0;
    if (first == null) return -1;
    if (second == null) return 1;
    return first.compareTo(second);
  }

  private int countByRoomType(ListInterface<Reservation> source, Room.RoomType roomType) {
    int count = 0;
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r != null && r.getRoomType() == roomType) {
        count++;
      }
    }
    return count;
  }

  private int countArrivingToday(ListInterface<Reservation> source) {
    LocalDate today = LocalDate.now();
    int count = 0;
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r != null
          && r.getExpectedArrivalTime() != null
          && r.getExpectedArrivalTime().toLocalDate().isEqual(today)) {
        count++;
      }
    }
    return count;
  }

  private String guestNameOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null && g.getName() != null) ? g.getName() : "";
  }

  private Reservation findQueuedReservationForGuest(Room.RoomType roomType, String guestId) {
    ListInterface<Reservation> snapshot = standardReservationRepo.snapshotQueue(roomType);
    for (int i = 1; i <= snapshot.getNumberOfEntries(); i++) {
      Reservation r = snapshot.getEntry(i);
      if (r != null && guestId.equalsIgnoreCase(r.getGuestId())) {
        return r;
      }
    }
    return null;
  }
}
