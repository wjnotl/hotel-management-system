package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class AdvanceBookingView {

  // Column widths must sum to 92 - 3n to frame to the same width as the spanned heading above.
  private static final int[] BOOKING_WIDTHS = {4, 11, 18, 9, 11, 6, 12};
  private static final int[] AVAIL_WIDTHS = {4, 11, 8, 10, 12, 29};
  private static final int[] SPAN_WIDTH = {89};
  private static final int[] KV_WIDTHS = {22, 64};
  private static final int SCREEN_WIDTH = 83;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  public static class BookingRowDTO {
    private final String reservationId;
    private final String guestName;
    private final String roomType;
    private final String arrives;
    private final String nights;
    private final String bookedAt;

    public BookingRowDTO(
        String reservationId,
        String guestName,
        String roomType,
        String arrives,
        String nights,
        String bookedAt) {
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.roomType = roomType;
      this.arrives = arrives;
      this.nights = nights;
      this.bookedAt = bookedAt;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getRoomType() {
      return roomType;
    }

    public String getArrives() {
      return arrives;
    }

    public String getNights() {
      return nights;
    }

    public String getBookedAt() {
      return bookedAt;
    }
  }

  public GetMenuInputResult renderAdvanceScreen(
      ListInterface<BookingRowDTO> bookings,
      int luxuryCount,
      int suiteCount,
      int standardCount,
      int arrivingToday,
      String searchField,
      String searchTerm,
      String matchMode,
      String roomTypeFilter,
      LocalDate fromDate,
      LocalDate toDate,
      String sort,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADVANCE RESERVATIONS", SCREEN_WIDTH);

    System.out.println(
        "AWAITING ARRIVAL  : "
            + (luxuryCount + suiteCount + standardCount)
            + " booking(s) not yet standing in any line");
    System.out.println(
        "BY ROOM TYPE      : LUXURY "
            + luxuryCount
            + "   |   SUITE "
            + suiteCount
            + "   |   STANDARD "
            + standardCount);
    System.out.println("DUE IN TODAY      : " + arrivingToday + " booking(s)");
    System.out.println(
        "SEARCH            : "
            + (searchTerm == null
                ? "[ None ]"
                : "[ " + searchField + " " + matchMode + " \"" + searchTerm + "\" ]"));
    System.out.println(
        "ROOM TYPE FILTER  : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("ARRIVAL BETWEEN   : [ " + rangeLabel(fromDate, toDate) + " ]");
    System.out.println("SORT CRITERIA     : [ " + sort + " ]\n");

    printBookingTable(bookings, currentPage, pageSize);

    int totalMatches = (bookings == null) ? 0 : bookings.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);
    System.out.printf(
        "%nPage %d / %d (Matches: %d)%n%n",
        (totalPages == 0) ? 0 : currentPage, totalPages, totalMatches);

    System.out.println("[A] New Advance Booking   [M] Mark Arrival (joins the line)");
    System.out.println("[S] Search / Filter       [O] Change Sort Order   [R] Refresh");
    System.out.println("[P] Prev Page             [N] Next Page");
    System.out.println("[E] Exit to Walk-In & Booking Menu\n");
    System.out.println("Pick a row number to view, mark arrival or cancel that booking.\n");

    char[] commands = {'A', 'M', 'S', 'O', 'P', 'N', 'R', 'E'};

    int rowsOnPage = countRowsOnPage(totalMatches, currentPage, pageSize);
    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Enter a command or select a row (" + range + "): ", 1, rowsOnPage, commands);
  }

  private String rangeLabel(LocalDate from, LocalDate to) {
    if (from == null && to == null) return "Any date";
    if (from != null && to != null) {
      return from.format(DATE_FORMAT) + "  ..  " + to.format(DATE_FORMAT);
    }
    if (from != null) return "From " + from.format(DATE_FORMAT);
    return "Up to " + to.format(DATE_FORMAT);
  }

  private int countRowsOnPage(int totalMatches, int currentPage, int pageSize) {
    if (totalMatches == 0) return 0;
    int startIndex = (currentPage - 1) * pageSize + 1;
    if (startIndex > totalMatches) return 0;
    return Math.min(startIndex + pageSize - 1, totalMatches) - startIndex + 1;
  }

  private void printBookingTable(
      ListInterface<BookingRowDTO> bookings, int currentPage, int pageSize) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(BOOKING_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setTruncateAt(2, BOOKING_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(BOOKING_WIDTHS);
    TableUtil.TableSettings spanSettings = spanSettings();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"BOOKED AHEAD, NOT YET IN A LINE"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "TYPE", "ARRIVES", "NIGHTS", "BOOKED AT"},
        headerSettings);

    int totalMatches = (bookings == null) ? 0 : bookings.getNumberOfEntries();
    if (totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NO ADVANCE BOOKINGS MATCH THE CURRENT FILTERS ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      BookingRowDTO row = bookings.getEntry(i);
      if (row == null) continue;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            row.getReservationId(),
            row.getGuestName(),
            row.getRoomType(),
            row.getArrives(),
            row.getNights(),
            row.getBookedAt()
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  // Availability is shown before the type is picked, so a booking is never started against a type
  // that the calendar was always going to refuse.
  public Room.RoomType promptRoomType(
      Guest guest, LocalDate arrival, int[] totalByType, int[] freeByType) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - ROOM TYPE", SCREEN_WIDTH);
    System.out.println("Guest   : " + guest.getName() + " (" + guest.getGuestId() + ")");
    System.out.println("Arrives : " + arrival.format(DATE_FORMAT) + "\n");

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(AVAIL_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings = centeredHeader(AVAIL_WIDTHS);

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"AVAILABILITY ON " + arrival.format(DATE_FORMAT)}, spanSettings());
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM TYPE", "ROOMS", "FREE", "TAKEN", "STATUS"}, headerSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    Room.RoomType[] types = {Room.RoomType.LUXURY, Room.RoomType.SUITE, Room.RoomType.STANDARD};
    for (int i = 0; i < types.length; i++) {
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i + 1),
            types[i].name(),
            String.valueOf(totalByType[i]),
            String.valueOf(freeByType[i]),
            String.valueOf(totalByType[i] - freeByType[i]),
            (freeByType[i] > 0) ? "Bookable on this date" : "Fully booked on this date"
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println("4. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose a room type: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  // The raw text comes back for the controller to parse, so the rule about what a date may be
  // lives with the rest of the booking rules rather than in the screen.
  public String promptArrivalDate(
      Guest guest, LocalDate earliest, LocalDate latest, LocalDate current) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - ARRIVAL DATE", SCREEN_WIDTH);
    System.out.println("Guest: " + guest.getName() + " (" + guest.getGuestId() + ")\n");
    System.out.println("Format: YYYY-MM-DD");
    System.out.println(
        "Bookable window: "
            + earliest.format(DATE_FORMAT)
            + "  to  "
            + latest.format(DATE_FORMAT));
    if (current != null) {
      System.out.println("Current value: " + current.format(DATE_FORMAT));
    }
    System.out.println("\nE - Exit back to the guest search\n");

    return ConsoleUtil.getStringInput("Arrival date: ");
  }

  public String promptArrivalTime(LocalDate arrival, java.time.LocalTime current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - ARRIVAL TIME", SCREEN_WIDTH);
    System.out.println("Arrives on: " + arrival.format(DATE_FORMAT) + "\n");
    System.out.println("Format: HH:MM on a 24 hour clock, for example 14:30.");
    System.out.println("This is the time the desk expects the guest at the counter.");
    if (current != null) {
      System.out.println("Current value: " + current.format(TIME_FORMAT));
    }
    System.out.println("\nB - Back to the nights step\n");

    return ConsoleUtil.getStringInput("Arrival time: ");
  }

  public Integer promptNights(
      Room.RoomType roomType, LocalDate arrival, int maxBookable, int houseMax, Integer current) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - NIGHTS", SCREEN_WIDTH);
    System.out.println("Room type : " + roomType.name());
    System.out.println("Arrives   : " + arrival.format(DATE_FORMAT) + "\n");

    if (maxBookable < houseMax) {
      System.out.println(
          "The calendar allows "
              + maxBookable
              + " night(s) from this date. Night "
              + (maxBookable + 1)
              + " is already fully booked for this room type.");
    } else {
      System.out.println("The house limit is " + houseMax + " nights in one booking.");
    }

    if (current != null) {
      System.out.println("Current value: " + current + " night(s)");
    }

    System.out.println();
    System.out.println("Checkout day is a turnover day, so a stay ending on the 5th does not");
    System.out.println("block another guest arriving on the 5th.");
    System.out.println("\nPress Enter or 'C' to go back to the room type\n");

    return ConsoleUtil.getIntegerInput("Nights [1 - " + maxBookable + "]: ", 1, maxBookable);
  }

  public void displayFullyBookedScreen(
      Room.RoomType roomType, LocalDate arrival, LocalDate firstFullDate, int totalRooms) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NO ROOMS ON THAT DATE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] BOOKING REFUSED",
        "First Full Night",
        (firstFullDate != null) ? firstFullDate.format(DATE_FORMAT) : "N/A",
        "All "
            + totalRooms
            + " "
            + roomType.name()
            + " room(s) are already committed on that night, counting advance bookings, guests"
            + " still in their rooms and rooms currently on hold. Offer a different date, a"
            + " shorter stay or another room type.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayMemberBlockedScreen(Guest guest, Member member) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("HANDLED BY THE VIP MODULE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] " + member.getTier().name() + " MEMBER",
        "Guest",
        guest.getName() + " (" + guest.getGuestId() + ")",
        "This module books non-members only. A tier holder is ranked by priority score rather"
            + " than by arrival order, so their reservation has to be opened under Main Menu >"
            + " 2. VIP Priority Room Allocation. Nothing has been recorded here.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayNewBookingConfirmationScreen(
      Guest g,
      Room.RoomType roomType,
      LocalDateTime arrival,
      int nights,
      LocalDate departure,
      int freeAfterBooking) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ADVANCE BOOKING", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"BOOKING DETAILS"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", g.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", g.getName(), true);
    printKeyValue(kvSettings, "Phone Number", blankToNa(g.getPhoneNumber()), true);
    printKeyValue(kvSettings, "Email Address", blankToNa(g.getEmail()), true);
    printKeyValue(kvSettings, "Room Type Booked", roomType.name(), true);
    printKeyValue(
        kvSettings,
        "Expected Arrival",
        arrival.format(DATE_FORMAT) + " at " + arrival.format(TIME_FORMAT),
        true);
    printKeyValue(kvSettings, "Nights", String.valueOf(nights), true);
    printKeyValue(kvSettings, "Due To Check Out", departure.format(DATE_FORMAT), true);
    printKeyValue(
        kvSettings,
        "Rooms Left On Arrival",
        freeAfterBooking + " of this type after this booking is taken",
        true);
    printKeyValue(
        kvSettings,
        "Queue Placement",
        "None. The booking is held as RESERVED and only joins the line when the guest arrives"
            + " and the desk marks them in.",
        false);

    System.out.println();
    System.out.println("1. Create This Advance Booking");
    System.out.println("2. Do Not Create It\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayNewBookingSuccessScreen(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADVANCE BOOKING CREATED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: RESERVED",
        "Reservation ID",
        r.getReservationId() + "   (code " + r.getConfirmationNumber() + ")",
        ((g != null) ? g.getName() : "The guest")
            + " holds a "
            + r.getRoomType().name()
            + " booking arriving "
            + formatArrival(r.getExpectedArrivalTime())
            + " for "
            + ((r.getStayDays() != null) ? r.getStayDays() : 1)
            + " night(s). Use [M] Mark Arrival when they reach the desk. A room of this type is"
            + " now held back on those dates and will not be given to a walk-in.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayMarkArrivalConfirmationScreen(
      Reservation r, Guest g, int waiting, int capacity, String timingNote) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ARRIVAL", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST HAS ARRIVED"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Expected Arrival", formatArrival(r.getExpectedArrivalTime()), true);
    printKeyValue(kvSettings, "Timing", timingNote, true);
    printKeyValue(kvSettings, "Line Length Now", waiting + " / " + capacity + " slots used", true);
    printKeyValue(
        kvSettings,
        "Position On Joining",
        (waiting + 1)
            + ". A booking buys a room type on a date, not a place in the line, so arrival"
            + " time decides the order.",
        false);

    System.out.println();
    System.out.println("1. Move This Booking Into The " + r.getRoomType().name() + " Line");
    System.out.println("2. Leave It As An Advance Booking\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayMarkArrivalSuccessScreen(Reservation r, Guest g, int position, int waiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("JOINED THE LINE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: RESERVED -> WAITING",
        "Place In Line",
        position + " of " + waiting,
        ((g != null) ? g.getName() : "The guest")
            + " is now standing in the "
            + r.getRoomType().name()
            + " line under "
            + r.getReservationId()
            + ". Serve them from the Walk-In Queue screen.");
    ConsoleUtil.printContinueMessage();
  }

  public int displayRowActionSubmenu(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING ACTION: " + r.getReservationId(), SCREEN_WIDTH);
    System.out.println("Guest   : " + ((g != null) ? g.getName() : "N/A"));
    System.out.println("Type    : " + r.getRoomType().name());
    System.out.println("Arrives : " + formatArrival(r.getExpectedArrivalTime()) + "\n");

    System.out.println("1. View Booking Details");
    System.out.println("2. Mark Arrival (joins the line)");
    System.out.println("3. Cancel Booking");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public void displayDetailScreen(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING DETAILS", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Status", r.getStatus().name(), true);
    printKeyValue(kvSettings, "Expected Arrival", formatArrival(r.getExpectedArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Nights",
        (r.getStayDays() != null) ? String.valueOf(r.getStayDays()) : "Not stated",
        true);
    printKeyValue(
        kvSettings,
        "Due To Check Out",
        (r.getOccupancyEndDate() != null) ? r.getOccupancyEndDate().format(DATE_FORMAT) : "N/A",
        true);
    printKeyValue(kvSettings, "Booked At", formatTime(r.getReservationTime()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", (g != null) ? g.getGuestId() : r.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "IC / Passport No",
        (g == null)
            ? "N/A"
            : (g.getIcNumber() != null) ? g.getIcNumber() : blankToNa(g.getPassportNumber()),
        true);
    printKeyValue(
        kvSettings, "Phone Number", (g != null) ? blankToNa(g.getPhoneNumber()) : "N/A", true);
    printKeyValue(kvSettings, "Email Address", (g != null) ? blankToNa(g.getEmail()) : "N/A", true);
    printKeyValue(
        kvSettings, "Strike Count", String.valueOf((g != null) ? g.getStrikeCount() : 0), false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCancelConfirmationScreen(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM CANCELLATION", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] CANCEL ADVANCE BOOKING",
        "Target Booking",
        r.getReservationId() + "  -  " + ((g != null) ? g.getName() : "N/A"),
        "The booking is closed as CANCELLED and the room it was holding on "
            + formatArrival(r.getExpectedArrivalTime())
            + " goes back on sale. It never entered a line, so nobody else moves position.");

    System.out.println("1. Cancel This Advance Booking");
    System.out.println("2. Keep It\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayAlreadyInLineScreen(Guest guest, Reservation existing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALREADY IN LINE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] ARRIVAL REFUSED",
        "Target Guest",
        ((guest != null) ? guest.getName() : "Guest"),
        "This guest is already standing in the "
            + existing.getRoomType().name()
            + " line under "
            + existing.getReservationId()
            + ". Serve that entry before moving another booking in.");
    ConsoleUtil.printContinueMessage();
  }

  public int displayFilterMainMenu(
      String searchField,
      String searchTerm,
      String matchMode,
      String roomTypeFilter,
      LocalDate fromDate,
      LocalDate toDate) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & BOOKING FILTERS", SCREEN_WIDTH);
    System.out.println("Search Field   : [ " + searchField + " ]");
    System.out.println("Search Term    : [ " + (searchTerm == null ? "None" : searchTerm) + " ]");
    System.out.println("Match Mode     : [ " + matchMode + " ]");
    System.out.println(
        "Room Type      : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("Arrival Between: [ " + rangeLabel(fromDate, toDate) + " ]\n");

    System.out.println("1. Change Search Field");
    System.out.println("2. Change Search Term");
    System.out.println(
        "3. Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"));
    System.out.println("4. Room Type Filter");
    System.out.println("5. Arrival Date Range");
    System.out.println("6. Reset All Filters");
    System.out.println("7. Apply And Return");
    System.out.println("8. Back (discard these changes)\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 8).getAsInt();
  }

  public int displaySearchFieldSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH FIELD", SCREEN_WIDTH);
    System.out.println("Current: [ " + current + " ]\n");
    System.out.println(" 1. Guest Name");
    System.out.println(" 2. Guest ID");
    System.out.println(" 3. IC Number");
    System.out.println(" 4. Passport Number");
    System.out.println(" 5. Phone Number");
    System.out.println(" 6. Email Address");
    System.out.println(" 7. Reservation ID");
    System.out.println(" 8. Confirmation Code");
    System.out.println(" 9. All Of The Above");
    System.out.println("10. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 10).getAsInt();
    return (choice == 10) ? 0 : choice;
  }

  public String promptSearchTerm(String fieldLabel, String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH TERM", SCREEN_WIDTH);
    System.out.println("Field   : [ " + fieldLabel + " ]");
    System.out.println("Current : [ " + (current == null ? "None" : current) + " ]\n");
    System.out.println("Type '-' to clear the term.");
    System.out.println("E - Exit and keep the current term\n");

    return ConsoleUtil.getStringInput("Search term: ");
  }

  public int displayRoomTypeSubmenu(String currentRoomType) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM TYPE FILTER", SCREEN_WIDTH);
    System.out.println(
        "Current: [ " + (currentRoomType == null ? "ALL" : currentRoomType) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All");
    System.out.println("5. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
    return (choice == 5) ? 0 : choice;
  }

  // Returns the two raw strings the clerk typed, for the controller to parse.
  public String[] promptDateRange(LocalDate currentFrom, LocalDate currentTo) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ARRIVAL DATE RANGE", SCREEN_WIDTH);
    System.out.println("Current: [ " + rangeLabel(currentFrom, currentTo) + " ]\n");
    System.out.println("Format: YYYY-MM-DD. Leave either end blank to leave it open.");
    System.out.println("Type '-' at the first prompt to clear the whole range.");
    System.out.println("E - Exit and keep the current range\n");

    String from = ConsoleUtil.getStringInput("From date: ");
    if (from != null && ("E".equalsIgnoreCase(from.trim()) || "-".equals(from.trim()))) {
      return new String[] {from.trim(), null};
    }

    return new String[] {from, ConsoleUtil.getStringInput("To date: ")};
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER", SCREEN_WIDTH);
    System.out.println("1. Arrival Date (Soonest -> Latest)");
    System.out.println("2. Arrival Date (Latest -> Soonest)");
    System.out.println("3. Booked At (Newest -> Oldest)");
    System.out.println("4. Booked At (Oldest -> Newest)");
    System.out.println("5. Guest Name (A -> Z)");
    System.out.println("6. Guest Name (Z -> A)");
    System.out.println("7. Room Type (A -> Z)");
    System.out.println("8. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 8).getAsInt();
    if (choice == 1) return "ARRIVAL DATE (SOONEST -> LATEST)";
    if (choice == 2) return "ARRIVAL DATE (LATEST -> SOONEST)";
    if (choice == 3) return "BOOKED AT (NEWEST -> OLDEST)";
    if (choice == 4) return "BOOKED AT (OLDEST -> NEWEST)";
    if (choice == 5) return "GUEST NAME (A -> Z)";
    if (choice == 6) return "GUEST NAME (Z -> A)";
    if (choice == 7) return "ROOM TYPE (A -> Z)";
    return null;
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }

  private TableUtil.TableSettings kvSettings() {
    return new TableUtil.TableSettings(KV_WIDTHS);
  }

  private TableUtil.TableSettings spanSettings() {
    return new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);
  }

  private TableUtil.TableSettings centeredHeader(int[] widths) {
    TableUtil.TableSettings settings = new TableUtil.TableSettings(widths);
    for (int i = 0; i < widths.length; i++) {
      settings.setHAlign(i, TableUtil.Align.CENTER);
    }
    return settings;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil prints only width - 2 chars per cell, so long values are pre-wrapped here.
  private void printKeyValue(
      TableUtil.TableSettings settings, String key, String value, boolean moreRowsFollow) {
    ListInterface<String> lines = TextUtil.wrapText(value, KV_WIDTHS[1] - 2);

    for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
      TableUtil.printTableRow(new String[] {(i == 1) ? key : "", lines.getEntry(i)}, settings);
    }

    TableUtil.printTableBorder(
        settings,
        moreRowsFollow ? TableUtil.BorderPosition.MIDDLE : TableUtil.BorderPosition.BOTTOM);
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }

  private String formatArrival(LocalDateTime dateTime) {
    if (dateTime == null) return "Not set";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
  }
}
