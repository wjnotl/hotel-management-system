package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

// The report bodies are not drawn here. Generating a report writes a .txt file, so this view only
// collects the scope and confirms what was written.
public class BookingReportView {

  private static final int[] SPAN_WIDTH = {90};
  private static final int[] KV_WIDTHS = {23, 64};
  private static final int SCREEN_WIDTH = 90;

  public int displayReportHubMenu() {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("BOOKING ANALYTICS HUB", SCREEN_WIDTH);
      System.out.println("1. Daily Arrival Register");
      System.out.println("   Who arrived, what they were given and how long they waited.\n");
      System.out.println("2. Queue Performance & No-Show Analysis");
      System.out.println("   Average and worst waits, served counts and no-show rates.\n");
      System.out.println("3. Room Utilisation & Forecast");
      System.out.println("   Rooms committed per night ahead, from the booking calendar.\n");
      System.out.println("B - Back to Walk-In & Booking Menu");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput("Choose a report: ", 1, 3, new char[] {'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return 0;
        return result.getAsInt();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  /**
   * The scope screen for the two reservation reports.
   *
   * @return "1".."12" for a filter, or "X" to export, "R" to reset, "B" to go back
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

    String error = null;

    while (true) {
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
          " 7. Room Number      : [ "
              + (roomNumberFilter == null ? "Any" : roomNumberFilter)
              + " ]");
      System.out.println(
          " 8. Sort Order       : [ " + sortAttribute + " (" + sortDirection + ") ]");
      System.out.println(" 9. Group Rows By    : [ " + groupBy + " ]");
      System.out.println("10. Columns Exported : [ " + columnsLabel + " ]");
      System.out.println(
          "11. Record Limit     : [ "
              + (recordLimit == 0 ? "Show All" : "Top " + recordLimit)
              + " ]");
      System.out.println();
      System.out.println("Records matching the current scope: " + matchCount);
      System.out.println();
      System.out.println("X - Export Report To TXT");
      System.out.println("R - Reset All Filters");
      System.out.println("B - Back to Analytics Hub");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput("Choose an option: ", 1, 11, new char[] {'X', 'R', 'B'});

        if (result.isBlank) return "B";
        return result.input.toUpperCase();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public GetMenuInputResult showExportReceipt(
      String reportTitle,
      String scope,
      String sortLabel,
      String groupLabel,
      int matchCount,
      int exportedCount,
      String filePath,
      boolean offerBinarySearch,
      boolean binarySearchAvailable) {

    String error = null;

    while (true) {
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
      printKeyValue(kvSettings, "Grouped By", groupLabel, true);
      printKeyValue(
          kvSettings,
          "Records",
          matchCount + " matching, " + exportedCount + " written to the file",
          true);
      printKeyValue(kvSettings, "Saved To", filePath, false);

      System.out.println();
      if (offerBinarySearch) {
        if (binarySearchAvailable) {
          System.out.println("F - Find A Reservation ID (binary search on the sorted register)");
        } else {
          System.out.println("F - Find A Reservation ID (needs the RESERVATION ID sort order)");
        }
      }
      System.out.println("S - Change Filters");
      System.out.println("R - Export Again");
      System.out.println("B - Back to Analytics Hub");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        char[] commands =
            offerBinarySearch ? new char[] {'F', 'S', 'R', 'B'} : new char[] {'S', 'R', 'B'};

        GetMenuInputResult result = ConsoleUtil.getNavInput("Enter a command: ", commands);
        if (result.isBlank) return new GetMenuInputResult("B", false);
        return result;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public String promptReservationIdSearch() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FIND A RESERVATION ID", SCREEN_WIDTH);
    System.out.println("The register is sorted by reservation ID, so this halves the remaining");
    System.out.println("rows on every step instead of scanning them one by one.\n");
    System.out.println("B - Back");
    System.out.println();
    return ConsoleUtil.getStringInput("Enter the reservation ID: ");
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
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("SEARCH TERM", SCREEN_WIDTH);
      System.out.println("Field   : [ " + fieldLabel + " ]");
      System.out.println("Current : [ " + (current == null ? "None" : current) + " ]\n");
      System.out.println("Type '-' to clear the term.");
      System.out.println();
      System.out.println("B - Back (keep the current term)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        String input = ConsoleUtil.getStringInput("Search term: ");
        if (input == null || input.trim().isEmpty() || "B".equalsIgnoreCase(input.trim())) {
          return current;
        }
        if ("-".equals(input.trim())) return null;
        return input.trim();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
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

  // Returns {from, to}; a null entry leaves that end of the range open.
  public LocalDate[] promptDateRange(LocalDate currentFrom, LocalDate currentTo) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("CUSTOM DATE RANGE", SCREEN_WIDTH);
      System.out.println(
          "Current: [ "
              + ((currentFrom == null) ? "open" : currentFrom.format(ConsoleUtil.DATE_FORMAT))
              + "  ..  "
              + ((currentTo == null) ? "open" : currentTo.format(ConsoleUtil.DATE_FORMAT))
              + " ]\n");
      System.out.println("Format: YYYY-MM-DD. Leave either end blank to leave it open.");
      System.out.println();
      System.out.println("B - Back (keep the current range)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        String rawFrom = ConsoleUtil.getStringInput("From date: ");
        if (rawFrom != null && "B".equalsIgnoreCase(rawFrom.trim())) {
          return new LocalDate[] {currentFrom, currentTo};
        }

        LocalDate from = parseOrNull(rawFrom);
        LocalDate to = parseOrNull(ConsoleUtil.getStringInput("To date: "));

        if (from != null && to != null && to.isBefore(from)) {
          error = "The end of the range cannot fall before its start.";
          continue;
        }
        return new LocalDate[] {from, to};
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  private LocalDate parseOrNull(String raw) {
    if (raw == null || raw.trim().isEmpty()) return null;
    try {
      return LocalDate.parse(raw.trim(), ConsoleUtil.DATE_FORMAT);
    } catch (java.time.format.DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date! Type it as YYYY-MM-DD, for example 2026-08-21.");
    }
  }

  public Integer promptMinimumWait(Integer current) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("MINIMUM WAIT THRESHOLD", SCREEN_WIDTH);
      System.out.println(
          "Current: [ " + (current == null ? "None" : current + " minutes") + " ]\n");
      System.out.println("Only bookings that waited at least this long stay in scope.");
      System.out.println("Enter 0 to clear the threshold.");
      System.out.println();
      System.out.println("B - Back (keep the current threshold)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        Integer entered =
            ConsoleUtil.getIntegerInput("Minimum wait in minutes [0 - 1440]: ", 0, 1440);
        if (entered == null) return current;
        return (entered == 0) ? null : entered;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  // Returns {min, max}; -1 in either slot means that end is unbounded.
  public int[] promptStrikeRange(int currentMin, int currentMax) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("STRIKE COUNT RANGE", SCREEN_WIDTH);
      System.out.println(
          "Current: [ "
              + ((currentMin < 0) ? "any" : String.valueOf(currentMin))
              + "  ..  "
              + ((currentMax < 0) ? "any" : String.valueOf(currentMax))
              + " ]\n");
      System.out.println("Leave either prompt blank to leave that end unbounded.");
      System.out.println();
      System.out.println("B - Back (keep the current range)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        Integer min = ConsoleUtil.getIntegerInput("Minimum strikes [0 - 50]: ", 0, 50);
        Integer max = ConsoleUtil.getIntegerInput("Maximum strikes [0 - 50]: ", 0, 50);

        int low = (min == null) ? -1 : min;
        int high = (max == null) ? -1 : max;

        if (low >= 0 && high >= 0 && high < low) {
          error = "The maximum cannot be lower than the minimum.";
          continue;
        }
        return new int[] {low, high};
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public String promptRoomNumber(String current) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("ROOM NUMBER FILTER", SCREEN_WIDTH);
      System.out.println("Current: [ " + (current == null ? "Any" : current) + " ]\n");
      System.out.println("Only bookings attached to this room stay in scope.");
      System.out.println("Type '-' to clear the filter.");
      System.out.println();
      System.out.println("B - Back (keep the current filter)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        String input = ConsoleUtil.getStringInput("Room number: ");
        if (input == null || input.trim().isEmpty() || "B".equalsIgnoreCase(input.trim())) {
          return current;
        }
        if ("-".equals(input.trim())) return null;
        return input.trim();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
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
          "each with its own subtotal line."
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
    String error = null;

    while (true) {
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
      System.out.println("A - Select Every Column");
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput(
                "Choose a column: ", 1, columnNames.length, new char[] {'A', 'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return 0;
        if ("A".equalsIgnoreCase(result.input)) return -1;

        int index = result.getAsInt();
        if (selected[index - 1] && chosen == 1) {
          error = "At least one column has to be written to the file.";
          continue;
        }
        return index;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public String displayUtilisationPanel(
      int horizonDays, String roomTypeFilter, LocalDate startDate, int matchCount) {

    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("ROOM UTILISATION & FORECAST - SCOPE", SCREEN_WIDTH);

      System.out.println(
          "1. Start Date       : [ " + startDate.format(ConsoleUtil.DATE_FORMAT) + " ]");
      System.out.println("2. Horizon          : [ " + horizonDays + " nights ]");
      System.out.println(
          "3. Room Type        : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
      System.out.println();
      System.out.println("Live bookings feeding the forecast: " + matchCount);
      System.out.println();
      System.out.println("X - Export Report To TXT");
      System.out.println("R - Reset Scope");
      System.out.println("B - Back to Analytics Hub");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput("Choose an option: ", 1, 3, new char[] {'X', 'R', 'B'});

        if (result.isBlank) return "B";
        return result.input.toUpperCase();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public LocalDate promptStartDate(LocalDate current) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("FORECAST START DATE", SCREEN_WIDTH);
      System.out.println("Current: [ " + current.format(ConsoleUtil.DATE_FORMAT) + " ]\n");
      System.out.println("Format: YYYY-MM-DD");
      System.out.println();
      System.out.println("B - Back (keep the current date)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        LocalDate entered = ConsoleUtil.getDateInput("Start date: ");
        return (entered == null) ? current : entered;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  public Integer promptHorizon(int current) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox("FORECAST HORIZON", SCREEN_WIDTH);
      System.out.println("Current: [ " + current + " nights ]\n");
      System.out.println("How many nights forward the forecast table should cover.");
      System.out.println();
      System.out.println("B - Back (keep the current horizon)");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        Integer entered = ConsoleUtil.getIntegerInput("Nights [1 - 90]: ", 1, 90);
        return (entered == null) ? Integer.valueOf(current) : entered;
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
  }

  // Every submenu in this view is the same shape: a heading, some context lines, a numbered list
  // and a back option. Returning 0 means the clerk backed out.
  private int numberedMenu(String title, String[] contextLines, String[] options) {
    String error = null;

    while (true) {
      ConsoleUtil.clearScreen();
      ConsoleUtil.printTitleBox(title, SCREEN_WIDTH);

      for (String line : contextLines) {
        System.out.println(line);
      }
      System.out.println();

      for (int i = 0; i < options.length; i++) {
        System.out.println((i + 1) + ". " + options[i]);
      }
      System.out.println();
      System.out.println("B - Back");
      System.out.println();

      ConsoleUtil.printFieldError(error);

      try {
        GetMenuInputResult result =
            ConsoleUtil.getNavInput("Choose an option: ", 1, options.length, new char[] {'B'});

        if (result.isBlank || "B".equalsIgnoreCase(result.input)) return 0;
        return result.getAsInt();
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
    }
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
