package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class AdvanceBookingView {

  private static final int[] BOOKING_WIDTHS = {5, 12, 22, 14, 24};
  private static final int[] SPAN_WIDTH = {81};
  private static final int[] KV_WIDTHS = {22, 58};
  private static final int SCREEN_WIDTH = 83;

  public GetMenuInputResult renderAdvanceScreen(
      ListInterface<Reservation> bookings,
      ListInterface<Guest> guestList,
      int luxuryCount,
      int suiteCount,
      int standardCount,
      String search,
      String roomTypeFilter,
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
    System.out.println(
        "SEARCH QUERY      : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(
        "ROOM TYPE FILTER  : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA     : [ " + sort + " ]\n");

    printBookingTable(bookings, guestList, currentPage, pageSize);

    int totalMatches = (bookings == null) ? 0 : bookings.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);
    System.out.printf(
        "\nPage %d / %d (Matches: %d)\n\n",
        (totalPages == 0) ? 0 : currentPage, totalPages, totalMatches);

    System.out.println("[A] New Advance Booking   [M] Mark Arrival (joins the line)");
    System.out.println("[S] Search / Filter       [O] Change Sort Order");
    System.out.println("[P] Prev Page             [N] Next Page          [R] Refresh");
    System.out.println("[E] Exit to Booking Menu\n");

    char[] commands = {'A', 'M', 'S', 'O', 'P', 'N', 'R', 'E'};

    int rowsOnPage = countRowsOnPage(totalMatches, currentPage, pageSize);
    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Enter a command or select a row (" + range + "): ", 1, rowsOnPage, commands);
  }

  private int countRowsOnPage(int totalMatches, int currentPage, int pageSize) {
    if (totalMatches == 0) return 0;
    int startIndex = (currentPage - 1) * pageSize + 1;
    if (startIndex > totalMatches) return 0;
    return Math.min(startIndex + pageSize - 1, totalMatches) - startIndex + 1;
  }

  @SuppressWarnings("null")
  private void printBookingTable(
      ListInterface<Reservation> bookings,
      ListInterface<Guest> guestList,
      int currentPage,
      int pageSize) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(BOOKING_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setTruncateAt(2, BOOKING_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(BOOKING_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER);

    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"BOOKED AHEAD, NOT YET IN A LINE"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "ROOM TYPE", "BOOKED AT"}, headerSettings);

    int totalMatches = (bookings == null) ? 0 : bookings.getNumberOfEntries();
    if (totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NO ADVANCE BOOKINGS ARE AWAITING ARRIVAL ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = bookings.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            r.getReservationId(),
            (g != null) ? g.getName() : "N/A",
            r.getRoomType().name(),
            formatTime(r.getReservationTime())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  public String promptGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING", SCREEN_WIDTH);
    System.out.println("[Enter 'C' to Cancel]\n");
    return ConsoleUtil.getStringInput("Enter Guest ID or Name: ");
  }

  public Room.RoomType promptRoomType(Guest guest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NEW ADVANCE BOOKING - ROOM TYPE", SCREEN_WIDTH);
    System.out.println("Guest: " + guest.getName() + " (" + guest.getGuestId() + ")\n");
    System.out.println("1. Luxury");
    System.out.println("2. Suite");
    System.out.println("3. Standard");
    System.out.println("4. Cancel\n");

    int choice = ConsoleUtil.getMenuInput("Choose a room type: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  // A first-time caller has no guest file yet, so the desk is offered one instead of being
  // sent back to retype a search that can never match.
  public int displayGuestNotFoundScreen(String searchedTerm) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST NOT ON RECORD", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] NO MATCHING GUEST FILE",
        "Searched Term",
        searchedTerm,
        "No registered guest matches this guest ID or name. A guest booking for the first time"
            + " has no file yet, so one can be opened now before the booking is written.");

    System.out.println("1. Register This Person As A New Guest");
    System.out.println("2. Search Again");
    System.out.println("3. Cancel\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public boolean displayVipNoticeScreen(Guest guest, Member member) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PRIORITY GUEST DETECTED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] " + member.getTier().name() + " MEMBER",
        "Guest",
        guest.getName() + " (" + guest.getGuestId() + ")",
        "This guest holds a "
            + member.getTier().name()
            + " loyalty tier. The advance booking is recorded in the standard store as normal,"
            + " but when they arrive use [M] Mark Arrival rather than the walk-in line, so the"
            + " priority handling is applied instead of a FIFO place.");

    return promptConfirm("Continue creating this standard advance booking? (Y/N): ");
  }

  // Marking arrival is the moment a booked guest becomes a body at the counter, so the same
  // priority decision the walk-in line raises has to be raised here too.
  public int displayVipArrivalScreen(
      Guest guest,
      Member member,
      Room.RoomType roomType,
      int vacantRooms,
      int vipWaiting,
      int lineLength,
      boolean canServeNow) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PRIORITY GUEST ARRIVED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] " + member.getTier().name() + " MEMBER AT THE COUNTER"},
        spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(kvSettings, "Loyalty Tier", member.getTier().name(), true);
    printKeyValue(kvSettings, "Booked Type", roomType.name(), true);
    printKeyValue(kvSettings, "Vacant Clean Rooms", String.valueOf(vacantRooms), true);
    printKeyValue(kvSettings, "VIP Already Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(kvSettings, "Standard Line Length", String.valueOf(lineLength), false);

    System.out.println();

    if (canServeNow) {
      printNoticeBox(
          "RECOMMENDED ACTION",
          "Verdict",
          "SERVE NOW, DO NOT QUEUE",
          "A vacant clean "
              + roomType.name()
              + " room is free even after every waiting VIP is covered, so this booking can go"
              + " straight to a held room without the guest joining the line.");
    } else {
      printNoticeBox(
          "RECOMMENDED ACTION",
          "Verdict",
          (vacantRooms == 0) ? "NO ROOM TO GIVE" : "VIP BYPASS IN EFFECT",
          "Every vacant clean "
              + roomType.name()
              + " room is already spoken for by the "
              + vipWaiting
              + " VIP guest(s) waiting. Queue this guest, or handle them through the VIP module"
              + " so the priority score ranks them against those already waiting.");
    }

    if (canServeNow) {
      System.out.println("1. Assign A Room Now (Skip The Line)");
    } else {
      System.out.println("1. Assign A Room Now (UNAVAILABLE)");
    }
    System.out.println("2. Add To The Standard Walk-In Line Anyway");
    System.out.println("3. Cancel (Handle Through The VIP Module)\n");

    while (true) {
      try {
        return ConsoleUtil.getMenuInput("Choose an option: ", canServeNow ? 1 : 2, 3).getAsInt();
      } catch (IllegalArgumentException e) {
        System.out.println(
            "No room can be handed over right now. Choose 2 to queue them, or 3 to cancel.");
      }
    }
  }

  public boolean displayVipDirectAssignConfirmationScreen(
      Reservation booking, Guest guest, Member member, Room room, int graceMinutes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM PRIORITY ROOM ASSIGNMENT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ROOM HAND-OVER"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", booking.getReservationId(), true);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(kvSettings, "Loyalty Tier", member.getTier().name(), true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(
        kvSettings,
        "Hold Window",
        graceMinutes + " minutes before the room is released and a strike is recorded",
        false);

    System.out.println();
    return promptConfirm("Hand room " + room.getRoomNumber() + " to this guest now? (Y/N): ");
  }

  public void displayVipDirectAssignSuccessScreen(
      Reservation booking, Guest guest, Room room, int graceMinutes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PRIORITY ROOM ASSIGNED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ALLOCATED WITHOUT QUEUING"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", booking.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", booking.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", guest.getName(), true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Hold Expires In", graceMinutes + " minutes", false);

    System.out.println();
    System.out.println("The booking now appears under the walk-in line's [C] Check In Held Guest.");
    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayNewBookingConfirmationScreen(Guest g, Member m, Room.RoomType roomType) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ADVANCE BOOKING", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"BOOKING DETAILS"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", g.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", g.getName(), true);
    printKeyValue(
        kvSettings,
        "Phone Number",
        (g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A",
        true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
        true);
    printKeyValue(kvSettings, "Room Type Booked", roomType.name(), true);
    printKeyValue(
        kvSettings,
        "Queue Placement",
        "None. The booking is held as RESERVED and only joins the line when the guest arrives"
            + " and the desk marks them in.",
        false);

    System.out.println();
    return promptConfirm("Create this advance booking? (Y/N): ");
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
            + " booking. Use [M] Mark Arrival when they reach the desk to put them into the"
            + " line.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayMarkArrivalConfirmationScreen(
      Reservation r, Guest g, int waiting, int capacity) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ARRIVAL", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST HAS ARRIVED"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Booked At", formatTime(r.getReservationTime()), true);
    printKeyValue(kvSettings, "Line Length Now", waiting + " / " + capacity + " slots used", true);
    printKeyValue(
        kvSettings,
        "Position On Joining",
        (waiting + 1)
            + ". A booking buys a room type, not a place in the line, so arrival time decides"
            + " the order.",
        false);

    System.out.println();
    return promptConfirm("Move this booking into the " + r.getRoomType().name() + " line? (Y/N): ");
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

  public int displayRowActionSubmenu(Reservation r) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING ACTION: " + r.getReservationId(), SCREEN_WIDTH);
    System.out.println("1. View Booking Details");
    System.out.println("2. Mark Arrival (joins the line)");
    System.out.println("3. Cancel Booking");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public void displayDetailScreen(Reservation r, Guest g, Member m) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING DETAILS", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Status", r.getStatus().name(), true);
    printKeyValue(kvSettings, "Booked At", formatTime(r.getReservationTime()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", (g != null) ? g.getGuestId() : r.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "Phone Number",
        (g != null && g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A",
        true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
        true);
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
        "The booking is closed as CANCELLED. It never entered a line, so nobody else moves"
            + " position.");
    return promptConfirm("Cancel this advance booking? (Y/N): ");
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

  public int displayFilterMainMenu(String search, String roomTypeFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & BOOKING FILTERS", SCREEN_WIDTH);
    System.out.println("Active Search    : [ " + (search == null ? "None" : search) + " ]");
    System.out.println(
        "Active Room Type : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]\n");
    System.out.println("1. Text Search Submenu");
    System.out.println("2. Room Type Submenu");
    System.out.println("3. Reset All Filters");
    System.out.println("4. Apply and Return");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY", SCREEN_WIDTH);
    System.out.println(
        "Current Search: [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    System.out.println("Matches on guest name, reservation ID, confirmation code or phone.\n");
    System.out.println("1. Enter Search Term");
    System.out.println("2. Clear Search Term");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput() {
    return ConsoleUtil.getStringInput("\nEnter search term: ");
  }

  public int displayRoomTypeSubmenu(String currentRoomType) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM TYPE FILTER", SCREEN_WIDTH);
    System.out.println(
        "Current Selected Type: [ " + (currentRoomType == null ? "ALL" : currentRoomType) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER", SCREEN_WIDTH);
    System.out.println("1. Booked At (Newest -> Oldest)");
    System.out.println("2. Booked At (Oldest -> Newest)");
    System.out.println("3. Guest Name (A -> Z)");
    System.out.println("4. Guest Name (Z -> A)");
    System.out.println("5. Room Type (A -> Z)");
    System.out.println("6. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
    if (choice == 1) return "BOOKED AT (NEWEST -> OLDEST)";
    if (choice == 2) return "BOOKED AT (OLDEST -> NEWEST)";
    if (choice == 3) return "GUEST NAME (A -> Z)";
    if (choice == 4) return "GUEST NAME (Z -> A)";
    if (choice == 5) return "ROOM TYPE (A -> Z)";
    return null;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil wraps at the full column width but prints only width - 2 characters, so long
  // values are pre-wrapped here and emitted one row at a time to avoid losing characters.
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

  private boolean promptConfirm(String prompt) {
    while (true) {
      String choice = ConsoleUtil.getStringInput(prompt);
      if (choice != null) {
        if ("Y".equalsIgnoreCase(choice.trim())) return true;
        if ("N".equalsIgnoreCase(choice.trim())) return false;
      }
      System.out.println("Invalid input. Please enter 'Y' or 'N'.");
    }
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) return g;
    }
    return null;
  }
}
