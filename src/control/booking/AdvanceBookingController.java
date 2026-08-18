package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import adt.QueueInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.booking.AdvanceBookingView;

public class AdvanceBookingController {
  private static final int STEP_DATE = 1;
  private static final int STEP_TYPE = 2;
  private static final int STEP_NIGHTS = 3;
  private static final int STEP_TIME = 4;
  private static final int STEP_CONFIRM = 5;

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
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private final AdvanceBookingView advanceBookingView = new AdvanceBookingView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsRepo bookingSettingsRepo;

  public AdvanceBookingController(
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
        } else if ("M".equalsIgnoreCase(result.input)) {
          handleMarkArrivalByPrompt(rows, currentPage, pageSize);
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
        Guest guest =
            new GuestLookupController(guestRepo, standardReservationRepo)
                .findGuest("NEW ADVANCE BOOKING - FIND THE GUEST", true);
        if (guest == null) return;

        Member member =
            (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;
        if (member != null && member.getTier() != null) {
          advanceBookingView.displayMemberBlockedScreen(guest, member);
          continue;
        }

        if (collectBooking(guest)) return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Walks the arrival date, room type, nights and time as separate steps so backing out of any of
  // them lands on the one before it rather than throwing the whole booking away.
  private boolean collectBooking(Guest guest) {
    BookingSettings config = settings();

    LocalDate arrivalDate = null;
    Room.RoomType roomType = null;
    Integer nights = null;
    LocalTime arrivalTime = LocalTime.of(14, 0);

    int step = STEP_DATE;

    while (true) {
      try {
        if (step == STEP_DATE) {
          LocalDate earliest = earliestArrival(config);
          LocalDate latest = latestArrival(config);

          String typed = advanceBookingView.promptArrivalDate(guest, earliest, latest, arrivalDate);
          if (typed == null || "E".equalsIgnoreCase(typed.trim())) return false;

          // A blank line keeps whatever was already captured, so walking back into this step and
          // pressing Enter does not wipe the date the clerk already agreed with the guest.
          if (typed.trim().isEmpty()) {
            if (arrivalDate == null) {
              throw new IllegalArgumentException("Arrival date cannot be empty!");
            }
            step = STEP_TYPE;
            continue;
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

          arrivalDate = picked;
          step = STEP_TYPE;

        } else if (step == STEP_TYPE) {
          Room.RoomType picked = promptRoomType(guest, arrivalDate);
          if (picked == null) {
            step = STEP_DATE;
            continue;
          }

          roomType = picked;
          step = STEP_NIGHTS;

        } else if (step == STEP_NIGHTS) {
          int maxBookable =
              standardReservationRepo.findLongestBookableStay(
                  roomRepo, roomType, arrivalDate, config.getMaxStayNights());

          if (config.isBlockOverbooking() && maxBookable <= 0) {
            advanceBookingView.displayFullyBookedScreen(
                roomType,
                arrivalDate,
                arrivalDate,
                standardReservationRepo.getTotalRoomsOfType(roomRepo, roomType));
            step = STEP_TYPE;
            continue;
          }

          int allowed = config.isBlockOverbooking() ? maxBookable : config.getMaxStayNights();

          Integer picked =
              advanceBookingView.promptNights(
                  roomType, arrivalDate, allowed, config.getMaxStayNights(), nights);
          if (picked == null) {
            step = STEP_TYPE;
            continue;
          }

          nights = picked;
          step = STEP_TIME;

        } else if (step == STEP_TIME) {
          String typed = advanceBookingView.promptArrivalTime(arrivalDate, arrivalTime);

          if (typed == null || typed.trim().isEmpty() || "B".equalsIgnoreCase(typed.trim())) {
            step = STEP_NIGHTS;
            continue;
          }

          arrivalTime = parseTime(typed.trim());
          step = STEP_CONFIRM;

        } else {
          // Re-checked at the last moment, because another booking may have taken the last room of
          // this type while this one was being typed in.
          LocalDate firstFull =
              standardReservationRepo.findFirstFullDate(roomRepo, roomType, arrivalDate, nights);
          if (config.isBlockOverbooking() && firstFull != null) {
            advanceBookingView.displayFullyBookedScreen(
                roomType,
                arrivalDate,
                firstFull,
                standardReservationRepo.getTotalRoomsOfType(roomRepo, roomType));
            step = STEP_NIGHTS;
            continue;
          }

          LocalDateTime arrival = LocalDateTime.of(arrivalDate, arrivalTime);
          int freeAcross =
              standardReservationRepo.countAvailableAcross(roomRepo, roomType, arrivalDate, nights);

          if (!advanceBookingView.displayNewBookingConfirmationScreen(
              guest, roomType, arrival, nights, arrivalDate.plusDays(nights), freeAcross - 1)) {
            step = STEP_TIME;
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
                  false);

          booking.setExpectedArrivalTime(arrival);
          booking.setStayDays(nights);

          standardReservationRepo.addReservation(booking);
          advanceBookingView.displayNewBookingSuccessScreen(booking, guest);
          return true;
        }
      } catch (Exception e) {
        // Reported against the step that raised it, so a mistyped date is retyped on the date
        // screen instead of throwing away the guest and the whole booking.
        ConsoleUtil.printError(e.getMessage());
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

  private Room.RoomType promptRoomType(Guest guest, LocalDate arrival) {
    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    int[] total = new int[types.length];
    int[] free = new int[types.length];

    for (int i = 0; i < types.length; i++) {
      total[i] = standardReservationRepo.getTotalRoomsOfType(roomRepo, types[i]);
      free[i] = standardReservationRepo.countAvailableOn(roomRepo, types[i], arrival);
    }

    return advanceBookingView.promptRoomType(guest, arrival, total, free);
  }

  private void handleMarkArrivalByPrompt(
      ListInterface<Reservation> rows, int currentPage, int pageSize) {
    if (rows == null || rows.isEmpty()) {
      ConsoleUtil.printError("There are no advance bookings awaiting arrival!");
      return;
    }

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, rows.getNumberOfEntries());
    int rowsOnPage = endIndex - startIndex + 1;

    if (rowsOnPage <= 0) {
      ConsoleUtil.printError("There are no advance bookings on this page!");
      return;
    }

    // The table renumbers every page from 1, so the row read off the screen is an offset into the
    // page and the page origin has to be added back before indexing the list.
    Integer selection =
        ConsoleUtil.getIntegerInput(
            "\nEnter the row number of the guest who arrived [1 - "
                + rowsOnPage
                + "] (blank to cancel): ",
            1,
            rowsOnPage);
    if (selection == null) {
      return;
    }

    markArrival(rows.getEntry(startIndex + selection - 1));
  }

  private void markArrival(Reservation booking) {
    if (booking == null) {
      ConsoleUtil.printError("Invalid booking selection!");
      return;
    }

    Guest guest = guestRepo.findById(booking.getGuestId());

    Member member =
        (guest != null && guest.getMemberId() != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;
    if (member != null && member.getTier() != null) {
      advanceBookingView.displayMemberBlockedScreen(guest, member);
      return;
    }

    Reservation alreadyQueued =
        findQueuedReservationForGuest(booking.getRoomType(), booking.getGuestId());
    if (alreadyQueued != null) {
      advanceBookingView.displayAlreadyInLineScreen(guest, alreadyQueued);
      return;
    }

    if (standardReservationRepo.isLineAtPolicyLimit(booking.getRoomType())) {
      ConsoleUtil.printError(
          "The "
              + booking.getRoomType().name()
              + " line has reached the maximum length set under Settings & Configuration!");
      return;
    }

    QueueInterface<Reservation> queue =
        standardReservationRepo.getQueueByRoomType(booking.getRoomType());

    if (!advanceBookingView.displayMarkArrivalConfirmationScreen(
        booking,
        guest,
        queue.getNumberOfEntries(),
        standardReservationRepo.getQueueCapacity(booking.getRoomType()),
        timingNoteFor(booking))) {
      return;
    }

    if (!standardReservationRepo.joinQueue(booking)) {
      ConsoleUtil.printError("This booking could not be moved into the line!");
      return;
    }

    advanceBookingView.displayMarkArrivalSuccessScreen(
        booking, guest, queue.getPosition(booking), queue.getNumberOfEntries());
  }

  private String timingNoteFor(Reservation booking) {
    LocalDateTime expected = booking.getExpectedArrivalTime();
    if (expected == null) return "No arrival time was recorded on this booking.";

    long minutes = Duration.between(expected, LocalDateTime.now()).toMinutes();
    if (Math.abs(minutes) < 60) return "On time, within the hour.";

    long hours = Math.abs(minutes) / 60;
    if (hours < 24) {
      return (minutes < 0) ? hours + " hour(s) early." : hours + " hour(s) late.";
    }

    long days = hours / 24;
    return (minutes < 0)
        ? days + " day(s) early. The room was only held from the booked date."
        : days + " day(s) late. The room may already have gone to somebody else.";
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
          advanceBookingView.displayDetailScreen(selected, guest);
        } else if (action == 2) {
          markArrival(selected);
          return;
        } else if (action == 3) {
          if (advanceBookingView.displayCancelConfirmationScreen(selected, guest)) {
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
          int picked = advanceBookingView.displaySearchFieldSubmenu(field);
          if (picked > 0) field = fieldNameFor(picked);
        } else if (choice == 2) {
          String typed = advanceBookingView.promptSearchTerm(field, term);
          if (typed != null && !typed.trim().isEmpty() && !"E".equalsIgnoreCase(typed.trim())) {
            term = "-".equals(typed.trim()) ? null : typed.trim();
          }
        } else if (choice == 3) {
          mode = "EXACT".equals(mode) ? "CONTAINS" : "EXACT";
        } else if (choice == 4) {
          int picked = advanceBookingView.displayRoomTypeSubmenu(roomType);
          if (picked == 1) roomType = "LUXURY";
          else if (picked == 2) roomType = "SUITE";
          else if (picked == 3) roomType = "STANDARD";
          else if (picked == 4) roomType = null;
        } else if (choice == 5) {
          String[] typed = advanceBookingView.promptDateRange(from, to);
          String rawFrom = (typed[0] == null) ? "" : typed[0].trim();

          if (!"E".equalsIgnoreCase(rawFrom)) {
            if ("-".equals(rawFrom)) {
              from = null;
              to = null;
            } else {
              LocalDate parsedFrom = parseOrNull(rawFrom);
              LocalDate parsedTo = parseOrNull(typed[1]);

              if (parsedFrom != null && parsedTo != null && parsedTo.isBefore(parsedFrom)) {
                throw new IllegalArgumentException(
                    "The end of the range cannot fall before its start!");
              }
              from = parsedFrom;
              to = parsedTo;
            }
          }
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

  // The view is handed finished strings, so it never has to look a guest up to draw a row.
  private ListInterface<AdvanceBookingView.BookingRowDTO> buildBookingRowDTO(
      ListInterface<Reservation> bookings) {

    ListInterface<AdvanceBookingView.BookingRowDTO> rows = new ArrayList<>();

    for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
      Reservation r = bookings.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      rows.add(
          new AdvanceBookingView.BookingRowDTO(
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              r.getRoomType().name(),
              formatArrival(r.getExpectedArrivalTime()),
              (r.getStayDays() != null) ? String.valueOf(r.getStayDays()) : "-",
              formatShortDate(r.getReservationTime())));
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

  private LocalDate parseOrNull(String raw) {
    if (raw == null || raw.trim().isEmpty()) return null;
    return parseDate(raw.trim());
  }

  private LocalTime parseTime(String raw) {
    try {
      return LocalTime.parse(raw, TIME_FORMAT);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid time! Type it as HH:MM on a 24 hour clock, for example 14:30.");
    }
  }

  private String format(LocalDate date) {
    return (date == null) ? "N/A" : date.format(DATE_FORMAT);
  }

  private String formatArrival(LocalDateTime dateTime) {
    if (dateTime == null) return "Not set";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
  }

  private String formatShortDate(LocalDateTime dateTime) {
    if (dateTime == null) return "-";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yy"));
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
