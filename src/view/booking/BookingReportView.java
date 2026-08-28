package view.booking;

import adt.ListInterface;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

// Report bodies go to a .txt, so this view only collects the scope and confirms it.
public class BookingReportView {

  private static final int[] SPAN_WIDTH = {90};
  private static final int[] KV_WIDTHS = {23, 64};
  private static final int SCREEN_WIDTH = 90;

  public int displayReportHubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING ANALYTICS HUB", SCREEN_WIDTH);
    System.out.println("1. Daily Arrival Register");
    System.out.println("   Who arrived, what they were given and how long they waited.\n");
    System.out.println("2. Queue Performance & No-Show Analysis");
    System.out.println("   Average and worst waits, served counts and no-show rates.\n");
    System.out.println("3. Forward Occupancy & Availability Forecast");
    System.out.println("   How full every night ahead is, per room type, and when it sells out.\n");
    System.out.println("4. Back to Walk-In & Booking Menu\n");

    int choice = ConsoleUtil.getMenuInput("Choose a report: ", 1, 4).getAsInt();
    return (choice == 4) ? 0 : choice;
  }

  /**
   * The scope screen for the two reservation reports.
   *
   * @return "1".."11" for a filter, or "X" to export, "R" to reset, "E" to leave
   */
  public String displayFilterControlPanel(
      String reportTitle,
      String searchLabel,
      String roomTypeFilter,
      String statusFilter,
      String sourceFilter,
      String periodLabel,
      Integer minWaitMinutes,
      String strikeLabel,
      String roomNumberFilter,
      String sortAttribute,
      String sortDirection,
      String groupBy,
      String columnsLabel,
      int recordLimit,
      int matchCount,
      boolean showStatusFilter) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(reportTitle, SCREEN_WIDTH);

    System.out.println(" 1. Text Search      : [ " + searchLabel + " ]");
    System.out.println(
        " 2. Room Type        : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    if (showStatusFilter) {
      System.out.println(
          " 3. Booking Status   : [ " + (statusFilter == null ? "ALL" : statusFilter) + " ]");
    } else {
      System.out.println(
          " 3. Minimum Wait     : [ "
              + (minWaitMinutes == null ? "None" : minWaitMinutes + " minutes")
              + " ]");
    }
    System.out.println(
        " 4. Booking Source   : [ " + (sourceFilter == null ? "ALL" : sourceFilter) + " ]");
    System.out.println(" 5. Reporting Period : [ " + periodLabel + " ]");
    System.out.println(" 6. Strike Count     : [ " + strikeLabel + " ]");
    System.out.println(
        " 7. Room Number      : [ " + (roomNumberFilter == null ? "Any" : roomNumberFilter) + " ]");
    System.out.println(" 8. Sort Order       : [ " + sortAttribute + " (" + sortDirection + ") ]");
    System.out.println(" 9. Group Rows By    : [ " + groupBy + " ]");
    System.out.println("10. Columns Exported : [ " + columnsLabel + " ]");
    System.out.println(
        "11. Record Limit     : [ "
            + (recordLimit == 0 ? "Show All" : "Top " + recordLimit)
            + " ]");
    System.out.println();
    System.out.println("Records matching the current scope: " + matchCount);
    System.out.println();
    System.out.println("[X] Export Report To TXT   [R] Reset All Filters");
    System.out.println("[E] Exit to Analytics Hub\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 11, new char[] {'X', 'R', 'E'})
        .input
        .toUpperCase();
  }

  /**
   * One page of the finished report, exactly as it was written to the file.
   *
   * <p>The report is rendered rather than only saved, because a receipt naming a file path is not
   * the report. The clerk reads it here and the .txt stays on disk for anyone who wants it.
   */
  public GetMenuInputResult displayReportPage(
      String title,
      ListInterface<String> lines,
      int page,
      int pageSize,
      int totalPages,
      String filePath) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

    int startIndex = (page - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, lines.getNumberOfEntries());

    for (int i = startIndex; i <= endIndex; i++) {
      System.out.println(lines.getEntry(i));
    }

    System.out.printf("%nPage %d / %d   (also saved to %s)%n%n", page, totalPages, filePath);
    System.out.println("[P] Prev Page   [N] Next Page   [E] Back to the receipt" + "\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'P', 'N', 'E'});
  }

  public GetMenuInputResult showExportReceipt(
      String reportTitle,
      String scope,
      String sortLabel,
      String groupLabel,
      int matchCount,
      int exportedCount,
      String filePath,
      String searchTargetLabel,
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
    printKeyValue(kvSettings, "Scope", scope, true);
    printKeyValue(kvSettings, "Sorted By", sortLabel, true);
    printKeyValue(kvSettings, "Grouped By", groupLabel, true);
    printKeyValue(
        kvSettings,
        "Records",
        matchCount + " matching, " + exportedCount + " written to the file",
        true);
    printKeyValue(kvSettings, "Saved To", filePath, false);

    System.out.println();
    if (offerBinarySearch) {
      System.out.println(
          "[F] Find "
              + searchTargetLabel
              + (binarySearchAvailable
                  ? " (binary search on the sorted rows)"
                  : " (re-sorts the report first)"));
    }
    System.out.println("[V] View The Report On Screen");
    System.out.println("[S] Change Filters   [R] Export Again   [E] Exit to Analytics Hub\n");

    char[] commands =
        offerBinarySearch ? new char[] {'F', 'V', 'S', 'R', 'E'} : new char[] {'V', 'S', 'R', 'E'};

    return ConsoleUtil.getMenuInput("Enter a command: ", commands);
  }

  // Binary search needs the sorted key, so the screen offers the re-sort, not a refusal.
  public boolean displayResortForSearchScreen(
      String currentSortLabel, String keyLabel, String requiredOrder) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("RE-SORT BEFORE SEARCHING", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] WRONG ORDER FOR A BINARY SEARCH"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Sorted By", currentSortLabel, true);
    printKeyValue(
        kvSettings,
        "System Notice",
        "This search needs the rows sorted by "
            + requiredOrder
            + ". Re-sorting changes nothing but the order of the rows.",
        false);

    System.out.println();
    System.out.println("1. Re-Sort By " + keyLabel + " And Search");
    System.out.println("2. Keep The Current Order And Go Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
  }

  public String promptReservationIdSearch() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FIND A RESERVATION ID", SCREEN_WIDTH);
    System.out.println("The register is sorted by reservation ID, so this halves the remaining");
    System.out.println("rows on every step instead of scanning them one by one.\n");
    System.out.println("E - Exit back to the receipt\n");
    return requireText("Enter the reservation ID: ", "Reservation ID cannot be empty!");
  }

  public void displayBinarySearchResult(
      String targetId,
      boolean found,
      String guestName,
      String roomType,
      String status,
      String waited,
      int rowIndex,
      int comparisons,
      int total) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BINARY SEARCH RESULT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {found ? "STATUS: FOUND" : "STATUS: [X] NOT FOUND"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Searched ID", targetId, true);
    printKeyValue(kvSettings, "Comparisons Used", comparisons + " of " + total + " rows", true);

    if (!found) {
      printKeyValue(
          kvSettings,
          "System Notice",
          "No booking with that ID is in the current report scope. Widen the filters or check"
              + " the ID.",
          false);
    } else {
      printKeyValue(kvSettings, "Row In Register", String.valueOf(rowIndex), true);
      printKeyValue(kvSettings, "Guest Name", guestName, true);
      printKeyValue(kvSettings, "Room Type", roomType, true);
      printKeyValue(kvSettings, "Status", status, true);
      printKeyValue(kvSettings, "Waited", waited, false);
    }

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public int displaySearchSubmenu(String field, String term, String matchMode) {
    return numberedMenu(
        "TEXT SEARCH",
        new String[] {
          "Field      : [ " + field + " ]",
          "Term       : [ " + (term == null ? "None" : term) + " ]",
          "Match Mode : [ " + matchMode + " ]"
        },
        new String[] {
          "Change Search Field",
          "Change Search Term",
          "Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"),
          "Clear The Search"
        });
  }

  public int displaySearchFieldSubmenu(String current) {
    return numberedMenu(
        "SEARCH FIELD",
        new String[] {"Current: [ " + current + " ]"},
        new String[] {
          "Guest Name",
          "Guest ID",
          "IC Number",
          "Passport Number",
          "Phone Number",
          "Email Address",
          "Reservation ID",
          "Confirmation Code",
          "All Of The Above"
        });
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

  public int displayRoomTypeSubmenu(String current) {
    return numberedMenu(
        "ROOM TYPE FILTER",
        new String[] {"Current: [ " + (current == null ? "ALL" : current) + " ]"},
        new String[] {"LUXURY", "SUITE", "STANDARD", "Show All"});
  }

  public int displayStatusSubmenu(String current) {
    return numberedMenu(
        "BOOKING STATUS FILTER",
        new String[] {"Current: [ " + (current == null ? "ALL" : current) + " ]"},
        new String[] {
          "RESERVED (booked, not arrived)",
          "WAITING (standing in a line)",
          "ALLOCATED (room on hold)",
          "CHECKED_IN",
          "CHECKED_OUT",
          "NO_SHOW",
          "CANCELLED",
          "Show All"
        });
  }

  public int displaySourceSubmenu(String current) {
    return numberedMenu(
        "BOOKING SOURCE FILTER",
        new String[] {
          "Current: [ " + (current == null ? "ALL" : current) + " ]",
          "",
          "A booking that carries an expected arrival time was made ahead;",
          "anything else was opened at the counter."
        },
        new String[] {"WALK-IN", "ADVANCE", "Show All"});
  }

  public int displayPeriodSubmenu(String current) {
    return numberedMenu(
        "REPORTING PERIOD",
        new String[] {"Current: [ " + current + " ]"},
        new String[] {
          "Today", "Yesterday", "Last 7 Days", "Last 30 Days", "All Time", "Custom Date Range"
        });
  }

  // Returns the two raw strings the clerk typed, for the controller to parse.
  public String[] promptDateRange(String currentLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CUSTOM DATE RANGE", SCREEN_WIDTH);
    System.out.println("Current: [ " + currentLabel + " ]\n");
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

  public Integer promptMinimumWait(Integer current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MINIMUM WAIT THRESHOLD", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "None" : current + " minutes") + " ]\n");
    System.out.println("Only bookings that waited at least this long stay in scope.");
    System.out.println("Enter 0 to clear the threshold.");
    System.out.println("Type 'C' to keep the current threshold\n");

    return requireInt("Minimum wait in minutes [0 - 1440]: ", 0, 1440);
  }

  // Returns {min, max}; -1 in either slot means that end is unbounded.
  public int[] promptStrikeRange(int currentMin, int currentMax) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STRIKE COUNT RANGE", SCREEN_WIDTH);
    System.out.println(
        "Current: [ "
            + ((currentMin < 0) ? "any" : String.valueOf(currentMin))
            + "  ..  "
            + ((currentMax < 0) ? "any" : String.valueOf(currentMax))
            + " ]\n");
    System.out.println("Type '-' at either prompt to leave that end unbounded.");
    System.out.println("E - Exit and keep the current range\n");

    ConsoleUtil.GetMenuInputResult min =
        ConsoleUtil.getMenuInput("Minimum strikes [0 - 50]: ", 0, 50, new char[] {'E', '-'});
    if (!min.isNumber && "E".equalsIgnoreCase(min.input)) {
      return new int[] {currentMin, currentMax};
    }

    ConsoleUtil.GetMenuInputResult max =
        ConsoleUtil.getMenuInput("Maximum strikes [0 - 50]: ", 0, 50, new char[] {'-'});

    int low = min.isNumber ? min.getAsInt() : -1;
    int high = max.isNumber ? max.getAsInt() : -1;
    if (low >= 0 && high >= 0 && high < low) {
      throw new IllegalArgumentException("The top of the range cannot fall below its bottom!");
    }
    return new int[] {low, high};
  }

  public String promptRoomNumber(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM NUMBER FILTER", SCREEN_WIDTH);
    System.out.println("Current: [ " + (current == null ? "Any" : current) + " ]\n");
    System.out.println("Only bookings attached to this room stay in scope.");
    System.out.println("Type '-' to clear the filter.");
    System.out.println("E - Exit and keep the current filter\n");

    return ConsoleUtil.getStringInput("Room number: ");
  }

  public int displaySortAttributeSubmenu(String current, boolean registerReport) {
    String[] options =
        registerReport
            ? new String[] {
              "Wait Duration",
              "Arrival Time",
              "Guest Name",
              "Room Type",
              "Booking Status",
              "Reservation ID (enables the binary search)"
            }
            : new String[] {
              "Wait Duration",
              "Arrival Time",
              "Guest Name",
              "Room Type",
              "Booking Status",
              "Strike Count"
            };

    return numberedMenu("SORT ATTRIBUTE", new String[] {"Current: [ " + current + " ]"}, options);
  }

  public int displaySortDirectionSubmenu(String current) {
    return numberedMenu(
        "SORT DIRECTION",
        new String[] {"Current: [ " + current + " ]"},
        new String[] {"Descending", "Ascending"});
  }

  public int displayGroupBySubmenu(String current) {
    return numberedMenu(
        "GROUP ROWS BY",
        new String[] {
          "Current: [ " + current + " ]",
          "",
          "Grouping splits the exported table into one section per value,",
          "each with its own row count."
        },
        new String[] {"No Grouping", "Room Type", "Booking Status", "Arrival Day"});
  }

  public int displayRecordLimitSubmenu(int current) {
    return numberedMenu(
        "RECORD LIMIT",
        new String[] {"Current: [ " + (current == 0 ? "Show All" : "Top " + current) + " ]"},
        new String[] {"Top 5", "Top 10", "Top 25", "Top 50", "Show All"});
  }

  /**
   * Toggles which columns reach the exported file.
   *
   * @return the 1-based column to flip, 0 to go back, -1 to select every column
   */
  public int displayColumnSelection(String[] columnNames, boolean[] selected) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("COLUMNS EXPORTED", SCREEN_WIDTH);
    System.out.println("Pick a number to switch that column on or off.\n");

    int chosen = 0;
    for (int i = 0; i < columnNames.length; i++) {
      if (selected[i]) chosen++;
      System.out.printf("%2d. [%s] %s%n", i + 1, selected[i] ? "x" : " ", columnNames[i]);
    }

    System.out.println();
    System.out.println(chosen + " of " + columnNames.length + " columns will be written.");
    System.out.println();
    System.out.println("[A] Select Every Column   [E] Exit\n");

    GetMenuInputResult result =
        ConsoleUtil.getMenuInput("Choose a column: ", 1, columnNames.length, new char[] {'A', 'E'});

    if ("E".equalsIgnoreCase(result.input)) return 0;
    if ("A".equalsIgnoreCase(result.input)) return -1;
    return result.getAsInt();
  }

  // Occupancy Forecast & Availability Report

  public String displayForecastControlPanel(
      String reportTitle,
      String windowLabel,
      String roomTypeFilter,
      String stateFilter,
      String occupancyLabel,
      boolean arrivalsDueOnly,
      String sortAttribute,
      String sortDirection,
      String groupBy,
      String columnsLabel,
      int recordLimit,
      int matchCount) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(reportTitle, SCREEN_WIDTH);

    System.out.println("1. Forecast Window    : [ " + windowLabel + " ]");
    System.out.println(
        "2. Room Type          : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("3. Availability State : [ " + stateFilter + " ]");
    System.out.println("4. Occupancy Band     : [ " + occupancyLabel + " ]");
    System.out.println("5. Arrivals Due Only  : [ " + (arrivalsDueOnly ? "YES" : "NO") + " ]");
    System.out.println("6. Sort Order         : [ " + sortAttribute + " (" + sortDirection + ") ]");
    System.out.println("7. Group Rows By      : [ " + groupBy + " ]");
    System.out.println("8. Columns Exported   : [ " + columnsLabel + " ]");
    System.out.println(
        "9. Record Limit       : [ "
            + (recordLimit == 0 ? "Show All" : "Top " + recordLimit)
            + " ]");
    System.out.println();
    System.out.println("Nights matching the current scope: " + matchCount);
    System.out.println();
    System.out.println("[X] Export Report To TXT   [R] Reset All Filters");
    System.out.println("[E] Exit to Analytics Hub\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 9, new char[] {'X', 'R', 'E'})
        .input
        .toUpperCase();
  }

  public int displayForecastStateSubmenu(String current) {
    return numberedMenu(
        "AVAILABILITY STATE",
        new String[] {
          "Current: [ " + current + " ]",
          "",
          "A night is FULL once every room of that type is committed, and",
          "OVERBOOKED when more are committed than the hotel owns."
        },
        new String[] {"All Nights", "Full Or Overbooked Only", "Open Only", "Overbooked Only"});
  }

  // Returns the two raw strings the clerk typed, for the controller to parse.
  public String[] promptForecastWindow(String currentLabel, int maxNights) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FORECAST WINDOW", SCREEN_WIDTH);
    System.out.println("Current: [ " + currentLabel + " ]\n");
    System.out.println("The first night to forecast, then how many nights to run for.");
    System.out.println("Format: YYYY-MM-DD. Type '-' for tonight.");
    System.out.println("E - Exit and keep the current window\n");

    String start = ConsoleUtil.getStringInput("First night: ");
    if (start.trim().isEmpty() || "E".equalsIgnoreCase(start.trim())) {
      return new String[] {start, null};
    }

    GetMenuInputResult nights =
        ConsoleUtil.getMenuInput(
            "Nights to forecast [1 - " + maxNights + "]: ", 1, maxNights, new char[] {'E'});

    return new String[] {start, nights.isNumber ? String.valueOf(nights.getAsInt()) : null};
  }

  // Returns {min, max} as percentages; -1 in either slot means that end is unbounded.
  public int[] promptOccupancyBand(int currentMin, int currentMax) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("OCCUPANCY BAND", SCREEN_WIDTH);
    System.out.println(
        "Current: [ "
            + ((currentMin < 0) ? "any" : currentMin + "%")
            + "  ..  "
            + ((currentMax < 0) ? "any" : currentMax + "%")
            + " ]\n");
    System.out.println("Only nights whose occupancy falls inside this band stay in scope.");
    System.out.println("Type '-' at either prompt to leave that end unbounded.");
    System.out.println("E - Exit and keep the current band\n");

    GetMenuInputResult min =
        ConsoleUtil.getMenuInput("Minimum occupancy [0 - 100]: ", 0, 100, new char[] {'E', '-'});
    if (!min.isNumber && "E".equalsIgnoreCase(min.input)) {
      return new int[] {currentMin, currentMax};
    }

    GetMenuInputResult max =
        ConsoleUtil.getMenuInput("Maximum occupancy [0 - 100]: ", 0, 100, new char[] {'-'});

    int low = min.isNumber ? min.getAsInt() : -1;
    int high = max.isNumber ? max.getAsInt() : -1;
    if (low >= 0 && high >= 0 && high < low) {
      throw new IllegalArgumentException("The top of the band cannot fall below its bottom!");
    }
    return new int[] {low, high};
  }

  public int displayForecastSortSubmenu(String current) {
    return numberedMenu(
        "SORT ATTRIBUTE",
        new String[] {"Current: [ " + current + " ]"},
        new String[] {
          "Occupancy", "Free Rooms", "Arrivals Due", "Room Type", "Date (enables the binary search)"
        });
  }

  public int displayForecastGroupSubmenu(String current) {
    return numberedMenu(
        "GROUP ROWS BY",
        new String[] {
          "Current: [ " + current + " ]",
          "",
          "Grouping splits the exported table into one section per value,",
          "each with its own row count."
        },
        new String[] {"No Grouping", "Room Type", "Night"});
  }

  public String promptForecastNightSearch() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FIND A NIGHT", SCREEN_WIDTH);
    System.out.println("The forecast is sorted by date, so this halves the remaining rows on");
    System.out.println("every step instead of scanning them one by one.\n");
    System.out.println("Format: YYYY-MM-DD.");
    System.out.println("E - Exit back to the receipt\n");
    return requireText("Enter the night: ", "The night cannot be empty!");
  }

  // The block is already laid out by the controller, which owns the table widths.
  public void displayForecastSearchResult(
      String night, boolean found, String renderedRows, int rowIndex, int comparisons, int total) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BINARY SEARCH RESULT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {found ? "STATUS: FOUND" : "STATUS: [X] NOT FOUND"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Searched Night", night, true);
    printKeyValue(kvSettings, "Comparisons Used", comparisons + " of " + total + " rows", true);

    if (found) {
      printKeyValue(kvSettings, "First Row In Report", String.valueOf(rowIndex), false);
      System.out.println();
      System.out.print(renderedRows);
    } else {
      printKeyValue(
          kvSettings,
          "System Notice",
          "That night is not in the current forecast window, or every row for it was filtered"
              + " out. Widen the window or clear the filters.",
          false);
    }

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  // Every submenu here is the same shape. Returning 0 means the clerk backed out.
  private int numberedMenu(String title, String[] contextLines, String[] options) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

    for (String line : contextLines) {
      System.out.println(line);
    }
    System.out.println();

    for (int i = 0; i < options.length; i++) {
      System.out.println((i + 1) + ". " + options[i]);
    }

    int backOption = options.length + 1;
    System.out.println(backOption + ". Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, backOption).getAsInt();
    return (choice == backOption) ? 0 : choice;
  }

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
