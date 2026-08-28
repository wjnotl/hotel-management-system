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
  private static final int[] BOOKING_WIDTHS = {4, 11, 15, 10, 9, 7, 7, 5};
  private static final int[] AVAIL_WIDTHS = {4, 11, 8, 10, 12, 29};
  private static final int[] SPAN_WIDTH = {89};
  private static final int[] KV_WIDTHS = {22, 64};
  private static final int SCREEN_WIDTH = 83;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static class BookingRowDTO {
    private final String reservationId;
    private final String guestName;
    private final String roomType;
    private final String tier;
    private final String arrives;
    private final String departs;
    private final String nights;

    public BookingRowDTO(
        String reservationId,
        String guestName,
        String roomType,
        String tier,
        String arrives,
        String departs,
        String nights) {
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.roomType = roomType;
      this.tier = tier;
      this.arrives = arrives;
      this.departs = departs;
      this.nights = nights;
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

    public String getTier() {
      return tier;
    }

    public String getArrives() {
      return arrives;
    }

    public String getDeparts() {
      return departs;
    }

    public String getNights() {
      return nights;
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

    System.out.println("[A] New Advance Booking   [S] Search / Filter");
    System.out.println("[O] Change Sort Order     [R] Refresh");
    System.out.println("[P] Prev Page             [N] Next Page");
    System.out.println("[E] Exit to Walk-In & Booking Menu\n");
    System.out.println("Pick a row number to check in, cancel or close that booking.\n");

    char[] commands = {'A', 'S', 'O', 'P', 'N', 'R', 'E'};

    int rowsOnPage = countRowsOnPage(totalMatches, currentPage, pageSize);
    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Enter a command or select a row (" + range + "): ", 1, rowsOnPage, commands);
  }

  private String rangeLabel(LocalDate from, LocalDate to) {
    if (from == null) {
      return (to == null) ? "Any date" : "Up to " + to.format(DATE_FORMAT);
    }
    if (to == null) return "From " + from.format(DATE_FORMAT);
    return from.format(DATE_FORMAT) + "  ..  " + to.format(DATE_FORMAT);
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
            .setHAlign(7, TableUtil.Align.CENTER)
            .setTruncateAt(2, BOOKING_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(BOOKING_WIDTHS);
    TableUtil.TableSettings spanSettings = spanSettings();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"BOOKED AHEAD"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "TYPE", "TIER", "ARRIVE", "DEPART", "NTS"},
        headerSettings);

    if (bookings == null || bookings.getNumberOfEntries() == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NO ADVANCE BOOKINGS MATCH THE CURRENT FILTERS ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    int totalMatches = bookings.getNumberOfEntries();
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
            row.getTier(),
            row.getArrives(),
            row.getDeparts(),
            row.getNights()
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  // Asked first as in the walk-in flow: a first-time caller has no record to search for.
  public int displayModeMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING", SCREEN_WIDTH);

    System.out.println("Has this guest stayed here before?\n");
    System.out.println("1. Existing Guest");
    System.out.println("   Search the register by ID, name, IC, passport, phone or email.\n");
    System.out.println("2. New Guest");
    System.out.println("   Open a guest file now, then take the booking against it.\n");
    System.out.println("3. Back to Advance Reservations\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  // Availability is shown first, so a booking never starts against a refused type.
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

  // Raw text comes back, so the rule about what a date may be lives with the controller.
  public String promptArrivalDate(
      Guest guest, LocalDate earliest, LocalDate latest, LocalDate current) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - ARRIVAL DATE", SCREEN_WIDTH);
    System.out.println("Guest: " + guest.getName() + " (" + guest.getGuestId() + ")\n");
    System.out.println("Format: YYYY-MM-DD");
    System.out.println(
        "Bookable window: " + earliest.format(DATE_FORMAT) + "  to  " + latest.format(DATE_FORMAT));
    if (current != null) {
      System.out.println("Current value: " + current.format(DATE_FORMAT));
    }
    System.out.println("\nE - Exit back to the guest search\n");

    return requireText("Arrival date: ", "Arrival date cannot be empty! Type 'E' to go back.");
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
    System.out.println("\nType 'C' to go back to the room type\n");

    return requireInt("Nights [1 - " + maxBookable + "]: ", 1, maxBookable);
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
            + " room(s) are committed on that night. Offer a different date, a shorter stay or"
            + " another room type.");
    ConsoleUtil.printContinueMessage();
  }

  String tierLabel(Member.LoyaltyTier tier) {
    return (tier == null) ? "NONE" : tier.name();
  }

  // The nights count alone never says which nights, so both are printed together.
  String stayRangeLabel(Reservation r, int nights) {
    LocalDate start = r.getOccupancyStartDate();
    if (start == null) {
      return nights + " night(s)";
    }

    return start.format(DATE_FORMAT)
        + "  ..  "
        + start.plusDays(nights).format(DATE_FORMAT)
        + "   ("
        + nights
        + " night(s))";
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
    printKeyValue(kvSettings, "Expected Arrival", arrival.format(DATE_FORMAT), true);
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
        "None. The night is committed to this booking now, so the guest is checked straight"
            + " into a room on arrival instead of standing in the walk-in line.",
        false);

    System.out.println();
    System.out.println("1. Create This Advance Booking");
    System.out.println("2. Do Not Create It\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayNewBookingSuccessScreen(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADVANCE BOOKING CREATED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: RESERVED"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", confirmationOf(r), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Arriving", formatArrival(r.getExpectedArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Nights Booked",
        String.valueOf((r.getStayDays() != null) ? r.getStayDays() : 1),
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCheckInConfirmationScreen(
      Reservation r,
      Guest g,
      Room room,
      int nights,
      Member.LoyaltyTier tier,
      LocalDate checkOutDate) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM CHECK-IN", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST HAS ARRIVED"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Member Tier", tierLabel(tier), true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Room To Give", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Expected Arrival", formatArrival(r.getExpectedArrivalTime()), true);
    printKeyValue(kvSettings, "Booked Days", stayRangeLabel(r, nights), true);
    // Check-in re-anchors the stay, so an early or late arrival gets its real checkout.
    printKeyValue(
        kvSettings,
        "Checking Out On",
        (checkOutDate != null) ? checkOutDate.format(DATE_FORMAT) : "N/A",
        false);

    System.out.println();
    System.out.println("1. Check In Now And Give Room " + room.getRoomNumber());
    System.out.println("2. Leave It As An Advance Booking\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayCheckInSuccessScreen(
      Reservation r, Guest g, Room room, int nights, LocalDate checkOutDate) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-IN COMPLETE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: RESERVED -> CHECKED_IN"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", confirmationOf(r), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "Room Given",
        room.getRoomNumber() + "  (" + r.getRoomType().name() + ")",
        true);
    printKeyValue(kvSettings, "Nights Booked", String.valueOf(nights), true);
    printKeyValue(
        kvSettings,
        "Due To Check Out",
        (checkOutDate != null) ? checkOutDate.format(DATE_FORMAT) : "N/A",
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayWrongArrivalDayScreen(
      Reservation r, Guest g, LocalDate bookedFor, long daysOff) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NOT THE BOOKED DATE", SCREEN_WIDTH);

    String timing =
        (daysOff > 0)
            ? "arrives in " + daysOff + " day(s)"
            : "was due " + Math.abs(daysOff) + " day(s) ago";

    printNoticeBox(
        "STATUS: [X] CHECK-IN REFUSED",
        "Booked For",
        ((bookedFor != null) ? bookedFor.format(DATE_FORMAT) : "N/A")
            + "   (today is "
            + LocalDate.now().format(DATE_FORMAT)
            + ")",
        ((g != null) ? g.getName() : "This guest")
            + " holds "
            + r.getReservationId()
            + " for a night that "
            + timing
            + ", and a booking can only be used on the night it reserves. Take this as a walk-in,"
            + " or close the booking as a no-show.");

    ConsoleUtil.printContinueMessage();
  }

  public void displayNoRoomReadyScreen(Reservation r, int vacantCount) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NO ROOM READY YET", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] CHECK-IN HELD UP",
        "Rooms Clean And Free",
        vacantCount + " " + r.getRoomType().name() + " room(s)",
        "No "
            + r.getRoomType().name()
            + " room is VACANT & CLEAN right now. The booking stays RESERVED, so try again once"
            + " housekeeping releases one.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayNoShowConfirmationScreen(
      Reservation r, Guest g, int strikesNow, int maxStrikes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM NO-SHOW", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] MARK AS NO-SHOW",
        "Target Booking",
        r.getReservationId() + "  -  " + ((g != null) ? g.getName() : "N/A"),
        "The booking closes as NO_SHOW and the night goes back on sale. The guest takes strike "
            + (strikesNow + 1)
            + " of "
            + maxStrikes
            + " for today. Cancel instead if the guest called ahead.");

    System.out.println("1. Mark This Booking As A No-Show");
    System.out.println("2. Leave It Alone\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public void displayClosureSuccessScreen(Reservation r, Guest g, boolean noShow, int strikesNow) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(noShow ? "MARKED AS NO-SHOW" : "BOOKING CANCELLED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {noShow ? "STATUS: -> NO_SHOW" : "STATUS: -> CANCELLED"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(
        kvSettings,
        "Closed Booking",
        r.getReservationId() + "  -  " + ((g != null) ? g.getName() : "N/A"),
        true);
    printKeyValue(kvSettings, "Night", "Back on sale", noShow);
    if (noShow) {
      printKeyValue(kvSettings, "Strikes Held", String.valueOf(strikesNow), false);
    }

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public int displayRowActionSubmenu(Reservation r, Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING ACTION: " + r.getReservationId(), SCREEN_WIDTH);
    System.out.println("Guest   : " + ((g != null) ? g.getName() : "N/A"));
    System.out.println("Type    : " + r.getRoomType().name());
    System.out.println("Arrives : " + formatArrival(r.getExpectedArrivalTime()) + "\n");

    System.out.println("1. View Booking Details");
    System.out.println("2. Check In (guest has arrived)");
    System.out.println("3. Cancel Booking");
    System.out.println("4. Mark As No-Show");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public void displayDetailScreen(Reservation r, Guest g, Member.LoyaltyTier tier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING DETAILS", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", confirmationOf(r), true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Status", r.getStatus().name(), true);
    printKeyValue(kvSettings, "Expected Arrival", formatArrival(r.getExpectedArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Booked Days",
        stayRangeLabel(r, (r.getStayDays() != null) ? r.getStayDays() : 0),
        true);
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
    printKeyValue(kvSettings, "Room Given", blankToNa(r.getRoomNumber()), true);
    printKeyValue(kvSettings, "Booked At", formatTime(r.getReservationTime()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", (g != null) ? g.getGuestId() : r.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Member Tier", tierLabel(tier), true);
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
        "The booking closes as CANCELLED and the room it holds on "
            + formatArrival(r.getExpectedArrivalTime())
            + " goes back on sale.");

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
        "Already in the "
            + existing.getRoomType().name()
            + " line under "
            + existing.getReservationId()
            + ". Serve that entry first.");
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
    System.out.println("1. Guest Name");
    System.out.println("2. Guest ID");
    System.out.println("3. IC Number");
    System.out.println("4. Passport Number");
    System.out.println("5. Phone Number");
    System.out.println("6. Email Address");
    System.out.println("7. Reservation ID");
    System.out.println("8. All Of The Above");
    System.out.println("9. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 9).getAsInt();
    return (choice == 9) ? 0 : choice;
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
    System.out.println("Format: YYYY-MM-DD.");
    System.out.println("Type '-' at either prompt to leave that end open.");
    System.out.println("E - Exit and keep the current range\n");

    String from = ConsoleUtil.getStringInput("From date: ");
    if (from.trim().isEmpty() || "E".equalsIgnoreCase(from.trim())) {
      return new String[] {from, null};
    }

    String to = ConsoleUtil.getStringInput("To date: ");
    return new String[] {from, to};
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

  // A code names a stay in progress, so every screen before check-in says where it comes from.
  private String confirmationOf(Reservation reservation) {
    String code = reservation.getConfirmationNumber();
    return (code == null || code.trim().isEmpty()) ? "Issued at check-in" : code;
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

  // A booking reserves a night, not a moment, so the hour is never shown.
  private String formatArrival(LocalDateTime dateTime) {
    if (dateTime == null) return "Not set";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
  }

  // Blank is an error, so Enter never discards a part-finished entry. 'C' is the way out.
  private Integer requireInt(String prompt, int min, int max) {
    ConsoleUtil.GetMenuInputResult result =
        ConsoleUtil.getMenuInput(prompt, min, max, new char[] {'C'});
    return result.isNumber ? Integer.valueOf(result.getAsInt()) : null;
  }

  // A blank line is rejected here so the error redraws this screen instead of the menu above it.
  private String requireText(String prompt, String emptyMessage) {
    String typed = ConsoleUtil.getStringInput(prompt);
    if (typed == null || typed.trim().isEmpty()) {
      throw new IllegalArgumentException(emptyMessage);
    }
    return typed;
  }
}
