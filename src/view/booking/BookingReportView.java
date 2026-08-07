package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class BookingReportView {

  // Every table below spans 88 printable columns: the widths sum plus one separator per gap.
  // TableUtil spends 2 of every column on padding, so a header of n characters needs n + 2.
  private static final int[] REGISTER_WIDTHS = {5, 11, 18, 11, 12, 9, 16};
  private static final int[] SUMMARY_WIDTHS = {12, 11, 11, 11, 10, 12, 15};
  private static final int[] WAIT_WIDTHS = {5, 11, 18, 11, 12, 9, 16};
  private static final int[] SUBTOTAL_WIDTHS = {14, 12, 12, 13, 15, 17};
  private static final int[] SPAN_WIDTH = {88};
  private static final int[] KV_WIDTHS = {23, 64};
  private static final int SCREEN_WIDTH = 90;

  public int displayReportHubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING ANALYTICS HUB", SCREEN_WIDTH);
    System.out.println("1. Daily Arrival Register");
    System.out.println("2. Queue Performance & No-Show Analysis");
    System.out.println("3. Back to Booking Menu\n");

    return ConsoleUtil.getMenuInput("Choose a report: ", 1, 3).getAsInt();
  }

  public int displayFilterControlPanel(
      String reportTitle,
      String search,
      String roomTypeFilter,
      String statusFilter,
      String tierFilter,
      String periodFilter,
      Integer minWaitMinutes,
      String sortAttribute,
      String sortDirection,
      int recordLimit,
      int matchCount,
      boolean showStatusFilter,
      boolean showMinWait) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(reportTitle, SCREEN_WIDTH);

    System.out.println("1. Text Search      : [ " + (search == null ? "None" : search) + " ]");
    System.out.println(
        "2. Room Type        : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    if (showStatusFilter) {
      System.out.println(
          "3. Booking Status   : [ " + (statusFilter == null ? "ALL" : statusFilter) + " ]");
    } else {
      System.out.println(
          "3. Minimum Wait     : [ "
              + (minWaitMinutes == null ? "None" : minWaitMinutes + " minutes")
              + " ]");
    }
    System.out.println(
        "4. Loyalty Tier     : [ " + (tierFilter == null ? "ALL" : tierFilter) + " ]");
    System.out.println("5. Reporting Period : [ " + periodFilter + " ]");
    System.out.println("6. Sort Order       : [ " + sortAttribute + " (" + sortDirection + ") ]");
    System.out.println(
        "7. Record Limit     : [ " + (recordLimit == 0 ? "Show All" : "Top " + recordLimit) + " ]");
    System.out.println("8. Reset All Filters");
    System.out.println("9. Generate Report Now");
    System.out.println("10. Back to Analytics Hub\n");

    System.out.println("Records matching the current scope: " + matchCount + "\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 10).getAsInt();
  }

  public GetMenuInputResult renderArrivalRegister(
      ListInterface<Reservation> rows,
      ListInterface<Guest> guestList,
      String scope,
      String sortLabel,
      int recordLimit,
      String[][] roomTypeTotals,
      boolean binarySearchAvailable) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("DAILY ARRIVAL REGISTER", SCREEN_WIDTH);

    printScopeHeader(scope, sortLabel, recordLimit, rows.getNumberOfEntries());

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(REGISTER_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setTruncateAt(2, REGISTER_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(REGISTER_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ARRIVALS BY ROOM TYPE"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "ROOM TYPE", "STATUS", "WAITED", "ARRIVED"},
        headerSettings);

    if (rows.isEmpty()) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NO BOOKINGS MATCH THE CURRENT SCOPE ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      int shown =
          (recordLimit == 0)
              ? rows.getNumberOfEntries()
              : Math.min(recordLimit, rows.getNumberOfEntries());

      for (int i = 1; i <= shown; i++) {
        Reservation r = rows.getEntry(i);
        if (r == null) continue;

        Guest g = findGuest(guestList, r.getGuestId());

        TableUtil.printTableRow(
            new String[] {
              String.valueOf(i),
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              r.getRoomType().name(),
              r.getStatus().name(),
              formatMinutes(waitMinutesOf(r)),
              formatClock(r.getQueueArrivalTime())
            },
            settings);
      }

      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

      if (recordLimit != 0 && rows.getNumberOfEntries() > recordLimit) {
        System.out.println(
            "Showing the top "
                + recordLimit
                + " of "
                + rows.getNumberOfEntries()
                + " matching records. Raise the record limit to see the rest.");
      }
    }

    System.out.println();
    printRoomTypeSubtotals(roomTypeTotals);

    System.out.println();
    if (binarySearchAvailable) {
      System.out.println("[F] Find A Reservation ID (binary search on the sorted register)");
    } else {
      System.out.println("[F] Find A Reservation ID (needs the RESERVATION ID sort order)");
    }
    System.out.println("[S] Change Filters     [R] Refresh     [E] Exit to Analytics Hub\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'F', 'S', 'R', 'E'});
  }

  private void printRoomTypeSubtotals(String[][] roomTypeTotals) {
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(SUBTOTAL_WIDTHS)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);
    TableUtil.TableSettings headerSettings = centeredHeader(SUBTOTAL_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"SUBTOTALS WITHIN SCOPE"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"ROOM TYPE", "IN SCOPE", "IN LINE", "ON HOLD", "CHECKED IN", "CLOSED"},
        headerSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    for (String[] row : roomTypeTotals) {
      TableUtil.printTableRow(row, settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  public GetMenuInputResult renderPerformanceReport(
      ListInterface<Reservation> rows,
      ListInterface<Guest> guestList,
      String scope,
      String sortLabel,
      int recordLimit,
      String[][] summaryRows,
      String[] overallRow) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE PERFORMANCE & NO-SHOW ANALYSIS", SCREEN_WIDTH);

    printScopeHeader(scope, sortLabel, recordLimit, rows.getNumberOfEntries());

    TableUtil.TableSettings summarySettings =
        new TableUtil.TableSettings(SUMMARY_WIDTHS)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.RIGHT)
            .setHAlign(3, TableUtil.Align.RIGHT)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER);
    TableUtil.TableSettings summaryHeader = centeredHeader(SUMMARY_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"OPERATIONAL SUMMARY"}, spanSettings);
    TableUtil.printTableBorder(summarySettings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {
          "ROOM TYPE", "ARRIVALS", "AVG WAIT", "MAX WAIT", "SERVED", "NO-SHOWS", "NO-SHOW RATE"
        },
        summaryHeader);
    TableUtil.printTableBorder(summarySettings, TableUtil.BorderPosition.MIDDLE);

    for (String[] row : summaryRows) {
      TableUtil.printTableRow(row, summarySettings);
    }

    TableUtil.printTableBorder(summarySettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(overallRow, summarySettings);
    TableUtil.printTableBorder(summarySettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    TableUtil.TableSettings waitSettings =
        new TableUtil.TableSettings(WAIT_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setTruncateAt(2, WAIT_WIDTHS[2] - 2);
    TableUtil.TableSettings waitHeader = centeredHeader(WAIT_WIDTHS);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"WORST WAITS IN SCOPE"}, spanSettings);
    TableUtil.printTableBorder(waitSettings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "ROOM TYPE", "OUTCOME", "WAITED", "STRIKES"},
        waitHeader);

    if (rows.isEmpty()) {
      TableUtil.printTableBorder(waitSettings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NO BOOKINGS MATCH THE CURRENT SCOPE ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(waitSettings, TableUtil.BorderPosition.MIDDLE);

      int shown =
          (recordLimit == 0)
              ? rows.getNumberOfEntries()
              : Math.min(recordLimit, rows.getNumberOfEntries());

      for (int i = 1; i <= shown; i++) {
        Reservation r = rows.getEntry(i);
        if (r == null) continue;

        Guest g = findGuest(guestList, r.getGuestId());

        TableUtil.printTableRow(
            new String[] {
              String.valueOf(i),
              r.getReservationId(),
              (g != null) ? g.getName() : "N/A",
              r.getRoomType().name(),
              r.getStatus().name(),
              formatMinutes(waitMinutesOf(r)),
              String.valueOf((g != null) ? g.getStrikeCount() : 0)
            },
            waitSettings);
      }

      TableUtil.printTableBorder(waitSettings, TableUtil.BorderPosition.BOTTOM);

      if (recordLimit != 0 && rows.getNumberOfEntries() > recordLimit) {
        System.out.println(
            "Showing the top "
                + recordLimit
                + " of "
                + rows.getNumberOfEntries()
                + " matching records. Raise the record limit to see the rest.");
      }
    }

    System.out.println("\n[S] Change Filters     [R] Refresh     [E] Exit to Analytics Hub\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'E'});
  }

  public String promptReservationIdSearch() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FIND A RESERVATION ID", SCREEN_WIDTH);
    System.out.println("The register is sorted by reservation ID, so this halves the remaining");
    System.out.println("rows on every step instead of scanning them one by one.\n");
    return ConsoleUtil.getStringInput("Enter the reservation ID (blank to cancel): ");
  }

  public void displayBinarySearchResult(
      String targetId, Reservation found, Guest guest, int rowIndex, int comparisons, int total) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BINARY SEARCH RESULT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {(found == null) ? "STATUS: [X] NOT FOUND" : "STATUS: FOUND"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Searched ID", targetId, true);
    printKeyValue(
        kvSettings,
        "Comparisons Used",
        comparisons + " of " + total + " rows (a linear scan would average " + (total / 2) + ")",
        true);

    if (found == null) {
      printKeyValue(
          kvSettings,
          "System Notice",
          "No booking with that reservation ID is inside the current report scope. Widen the"
              + " filters or check the ID.",
          false);
    } else {
      printKeyValue(kvSettings, "Row In Register", String.valueOf(rowIndex), true);
      printKeyValue(kvSettings, "Guest Name", (guest != null) ? guest.getName() : "N/A", true);
      printKeyValue(kvSettings, "Room Type", found.getRoomType().name(), true);
      printKeyValue(kvSettings, "Status", found.getStatus().name(), true);
      printKeyValue(kvSettings, "Waited", formatMinutes(waitMinutesOf(found)), false);
    }

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public int displayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM TYPE FILTER", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING STATUS FILTER", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. RESERVED (booked, not arrived)");
    System.out.println("2. WAITING (standing in a line)");
    System.out.println("3. ALLOCATED (room on hold)");
    System.out.println("4. CHECKED_IN");
    System.out.println("5. NO_SHOW");
    System.out.println("6. CANCELLED");
    System.out.println("7. Show All");
    System.out.println("8. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 8).getAsInt();
  }

  public int displayTierSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("LOYALTY TIER FILTER", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. DIAMOND");
    System.out.println("2. GOLD");
    System.out.println("3. SILVER");
    System.out.println("4. NON-MEMBER");
    System.out.println("5. Show All");
    System.out.println("6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displayPeriodSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORTING PERIOD", SCREEN_WIDTH);
    System.out.println("Current: [ " + current + " ]\n");
    System.out.println("1. Today");
    System.out.println("2. Last 7 Days");
    System.out.println("3. All Time");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public Integer promptMinimumWait(Integer current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MINIMUM WAIT THRESHOLD", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "None" : current + " minutes") + " ]\n");
    System.out.println("Only bookings that waited at least this long stay in scope.");
    System.out.println("Enter 0 to clear the threshold, or blank / 'C' to keep it.\n");

    return ConsoleUtil.getIntegerInput("Minimum wait in minutes [0 - 1440]: ", 0, 1440);
  }

  public int displaySortAttributeSubmenu(String current, boolean registerReport) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT ATTRIBUTE", SCREEN_WIDTH);
    System.out.println("Current: [ " + current + " ]\n");
    System.out.println("1. Wait Duration");
    System.out.println("2. Arrival Time");
    System.out.println("3. Guest Name");
    System.out.println("4. Room Type");
    if (registerReport) {
      System.out.println("5. Reservation ID (enables the binary search)");
      System.out.println("6. Back\n");
      return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
    }

    System.out.println("5. Strike Count");
    System.out.println("6. Back\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displaySortDirectionSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT DIRECTION", SCREEN_WIDTH);
    System.out.println("Current: [ " + current + " ]\n");
    System.out.println("1. Descending");
    System.out.println("2. Ascending");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayRecordLimitSubmenu(int current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("RECORD LIMIT", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == 0 ? "Show All" : "Top " + current) + " ]\n");
    System.out.println("1. Top 5");
    System.out.println("2. Top 10");
    System.out.println("3. Top 25");
    System.out.println("4. Show All");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public String promptSearchInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TEXT SEARCH", SCREEN_WIDTH);
    System.out.println("Matches on guest name, reservation ID, confirmation code or phone.");
    System.out.println("Leave blank to clear the search.\n");
    return ConsoleUtil.getStringInput("Enter search term: ");
  }

  private void printScopeHeader(String scope, String sortLabel, int recordLimit, int matches) {
    System.out.println("GENERATED : " + formatTime(LocalDateTime.now()));
    System.out.println("SCOPE     : " + scope);
    System.out.println("SORTED BY : " + sortLabel);
    System.out.println(
        "RECORDS   : "
            + matches
            + " matching, showing "
            + (recordLimit == 0 ? "all" : "top " + recordLimit)
            + "\n");
  }

  private TableUtil.TableSettings centeredHeader(int[] widths) {
    TableUtil.TableSettings settings = new TableUtil.TableSettings(widths);
    for (int i = 0; i < widths.length; i++) {
      settings.setHAlign(i, TableUtil.Align.CENTER);
    }
    return settings;
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

  // A booking that has been called waited until its room was held; one still in the line is
  // waiting right now.
  private long waitMinutesOf(Reservation r) {
    if (r.getQueueArrivalTime() == null) return -1;

    LocalDateTime end = (r.getAllocatedTime() != null) ? r.getAllocatedTime() : LocalDateTime.now();
    return java.time.Duration.between(r.getQueueArrivalTime(), end).toMinutes();
  }

  private String formatMinutes(long minutes) {
    if (minutes < 0) return "-";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  private String formatClock(LocalDateTime dateTime) {
    if (dateTime == null) return "-";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
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
