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

// The report bodies are no longer drawn here. Generating a report writes a .txt file and this
// view only confirms what was written, so the tables that used to live in this class moved into
// BookingReportController where the file content is built.
public class BookingReportView {

  // Every table below spans 90 printable columns: SPAN_WIDTH = {90}.
  // TableUtil adds 2 spaces padding (1 left + 1 right) per column automatically.
  private static final int[] SPAN_WIDTH = {90};
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
    System.out.println("9. Export Report To TXT");
    System.out.println("10. Back to Analytics Hub\n");

    System.out.println("Records matching the current scope: " + matchCount + "\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 10).getAsInt();
  }

  /**
   * Confirms a finished .txt export and collects the next command. The report itself is in the
   * file, so nothing but the receipt is printed here.
   *
   * @param offerBinarySearch true for the arrival register, which still exposes the reservation ID
   *     lookup over the sorted rows that were written
   * @param binarySearchAvailable true when the current sort order actually permits a binary search
   */
  public GetMenuInputResult showExportReceipt(
      String reportTitle,
      String scope,
      String sortLabel,
      int matchCount,
      int exportedCount,
      String filePath,
      boolean offerBinarySearch,
      boolean binarySearchAvailable) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EXPORT SUCCESSFUL", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {reportTitle}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Generated", formatTime(LocalDateTime.now()), true);
    printKeyValue(kvSettings, "Scope", scope, true);
    printKeyValue(kvSettings, "Sorted By", sortLabel, true);
    printKeyValue(
        kvSettings,
        "Records",
        matchCount + " matching, " + exportedCount + " written to the file",
        true);
    printKeyValue(kvSettings, "Saved To", filePath, false);

    System.out.println();
    if (offerBinarySearch) {
      if (binarySearchAvailable) {
        System.out.println("[F] Find A Reservation ID (binary search on the sorted register)");
      } else {
        System.out.println("[F] Find A Reservation ID (needs the RESERVATION ID sort order)");
      }
      System.out.println("[S] Change Filters     [R] Export Again     [E] Exit to Analytics Hub\n");

      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'F', 'S', 'R', 'E'});
    }

    System.out.println("[S] Change Filters     [R] Export Again     [E] Exit to Analytics Hub\n");

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

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }
}
