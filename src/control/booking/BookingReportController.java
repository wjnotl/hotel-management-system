package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.BookingSettings;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import repo.GuestRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import util.TxtExportUtil;
import view.booking.BookingReportView;

public class BookingReportController {
  private static final int ARRIVAL_REGISTER = 1;
  private static final int QUEUE_PERFORMANCE = 2;

  private static final String REGISTER_TITLE = "DAILY ARRIVAL REGISTER";
  private static final String PERFORMANCE_TITLE = "QUEUE PERFORMANCE & NO-SHOW ANALYSIS";
  private static final String NEW_LINE = System.lineSeparator();
  private static final String BLANK_INPUT = "Input cannot be empty!";

  // Leaves room for the title box and the command line on an 80 by 25 console.
  private static final int REPORT_PAGE_LINES = 18;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private static final String FIELD_NAME = "GUEST NAME";
  private static final String FIELD_GUEST_ID = "GUEST ID";
  private static final String FIELD_IC = "IC NUMBER";
  private static final String FIELD_PASSPORT = "PASSPORT NO";
  private static final String FIELD_PHONE = "PHONE NUMBER";
  private static final String FIELD_EMAIL = "EMAIL ADDRESS";
  private static final String FIELD_RES_ID = "RESERVATION ID";
  private static final String FIELD_CODE = "CONFIRMATION CODE";
  private static final String FIELD_ALL = "ALL FIELDS";

  private static final String[] COLUMN_NAMES = {
    "Reservation ID",
    "Confirmation Code",
    "Guest ID",
    "Guest Name",
    "Phone Number",
    "Room Type",
    "Room No",
    "Status",
    "Source",
    "Waited",
    "Arrived",
    "Nights",
    "Strikes"
  };

  private final BookingReportView reportView = new BookingReportView();
  private final StandardReservationRepo standardReservationRepo;
  private final GuestRepo guestRepo;
  private final RoomRepo roomRepo;
  private final BookingSettingsStore bookingSettingsStore;

  public BookingReportController(
      StandardReservationRepo standardReservationRepo,
      GuestRepo guestRepo,
      RoomRepo roomRepo,
      BookingSettingsStore bookingSettingsStore) {
    this.standardReservationRepo = standardReservationRepo;
    this.guestRepo = guestRepo;
    this.roomRepo = roomRepo;
    this.bookingSettingsStore = bookingSettingsStore;
  }

  private BookingSettings settings() {
    return bookingSettingsStore.getSettings();
  }

  public void startReportManagement() {
    while (true) {
      try {
        int choice = reportView.displayReportHubMenu();
        if (choice == 0) {
          return;
        }

        if (choice == ARRIVAL_REGISTER || choice == QUEUE_PERFORMANCE) {
          runReportPipeline(choice);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // One object, so a reset is one assignment and the export gets the whole scope.
  private static class ReportScope {
    private String searchField = FIELD_NAME;
    private String searchTerm;
    private boolean exactMatch;
    private String roomTypeFilter;
    private String statusFilter;
    private String sourceFilter;
    private String periodFilter;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer minWaitMinutes;
    private int minStrikes = -1;
    private int maxStrikes = -1;
    private String roomNumberFilter;
    private String sortAttribute;
    private String sortDirection = "DESCENDING";
    private String groupBy = "NO GROUPING";
    private int recordLimit;
    private boolean[] columns;
  }

  private ReportScope defaultScope(boolean isRegister) {
    ReportScope scope = new ReportScope();
    BookingSettings config = settings();

    scope.periodFilter = isRegister ? config.getDefaultReportPeriod() : "ALL TIME";
    scope.sortAttribute = isRegister ? "ARRIVAL TIME" : "WAIT DURATION";
    scope.recordLimit = config.getDefaultRecordLimit();
    scope.columns = new boolean[COLUMN_NAMES.length];

    int[] defaults = isRegister ? new int[] {0, 3, 5, 7, 9, 10} : new int[] {0, 3, 5, 7, 9, 12};
    for (int index : defaults) {
      scope.columns[index] = true;
    }
    return scope;
  }

  private void runReportPipeline(int reportType) {
    boolean isRegister = (reportType == ARRIVAL_REGISTER);
    ReportScope scope = defaultScope(isRegister);

    String title = isRegister ? "DAILY ARRIVAL REGISTER - SCOPE" : "QUEUE PERFORMANCE - SCOPE";

    while (true) {
      try {
        standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);

        ListInterface<Reservation> matched = filterReservations(scope);

        String command =
            reportView.displayFilterControlPanel(
                title,
                searchLabel(scope),
                scope.roomTypeFilter,
                scope.statusFilter,
                scope.sourceFilter,
                periodLabel(scope),
                scope.minWaitMinutes,
                strikeLabel(scope),
                scope.roomNumberFilter,
                scope.sortAttribute,
                scope.sortDirection,
                scope.groupBy,
                columnsLabel(scope),
                scope.recordLimit,
                matched.getNumberOfEntries(),
                isRegister);

        if ("E".equals(command)) {
          return;
        } else if ("R".equals(command)) {
          scope = defaultScope(isRegister);
        } else if ("X".equals(command)) {
          if (exportReport(reportType, matched, scope)) {
            return;
          }
        } else if ("1".equals(command)) {
          handleSearchSubmenu(scope);
        } else if ("2".equals(command)) {
          int picked = promptRoomTypeFilter(scope.roomTypeFilter);
          if (picked > 0) scope.roomTypeFilter = roomTypeNameFor(picked);
        } else if ("3".equals(command)) {
          if (isRegister) {
            int picked = promptStatusFilter(scope.statusFilter);
            if (picked > 0) scope.statusFilter = statusNameFor(picked);
          } else {
            Integer minWait = promptMinimumWait(scope.minWaitMinutes);
            if (minWait != null) scope.minWaitMinutes = minWait;
          }
        } else if ("4".equals(command)) {
          int picked = promptSourceFilter(scope.sourceFilter);
          if (picked == 1) scope.sourceFilter = "WALK-IN";
          else if (picked == 2) scope.sourceFilter = "ADVANCE";
          else if (picked == 3) scope.sourceFilter = null;
        } else if ("5".equals(command)) {
          handlePeriodSubmenu(scope);
        } else if ("6".equals(command)) {
          int[] range = promptStrikeRange(scope.minStrikes, scope.maxStrikes);
          scope.minStrikes = range[0];
          scope.maxStrikes = range[1];
        } else if ("7".equals(command)) {
          String typedRoom = promptRoomNumber(scope.roomNumberFilter);
          if (!"E".equalsIgnoreCase(typedRoom.trim())) {
            scope.roomNumberFilter = "-".equals(typedRoom.trim()) ? null : typedRoom.trim();
          }
        } else if ("8".equals(command)) {
          int attribute = promptSortAttribute(scope.sortAttribute, isRegister);
          if (attribute > 0) {
            int direction = promptSortDirection(scope.sortDirection);
            if (direction == 1 || direction == 2) {
              scope.sortAttribute = sortAttributeFor(attribute, isRegister);
              scope.sortDirection = (direction == 1) ? "DESCENDING" : "ASCENDING";
            }
          }
        } else if ("9".equals(command)) {
          int picked = promptGroupBy(scope.groupBy);
          if (picked == 1) scope.groupBy = "NO GROUPING";
          else if (picked == 2) scope.groupBy = "ROOM TYPE";
          else if (picked == 3) scope.groupBy = "BOOKING STATUS";
          else if (picked == 4) scope.groupBy = "ARRIVAL DAY";
        } else if ("10".equals(command)) {
          handleColumnSelection(scope);
        } else if ("11".equals(command)) {
          int picked = promptRecordLimit(scope.recordLimit);
          if (picked == 1) scope.recordLimit = 5;
          else if (picked == 2) scope.recordLimit = 10;
          else if (picked == 3) scope.recordLimit = 25;
          else if (picked == 4) scope.recordLimit = 50;
          else if (picked == 5) scope.recordLimit = 0;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleSearchSubmenu(ReportScope scope) {
    while (true) {
      try {
        int choice =
            reportView.displaySearchSubmenu(
                scope.searchField, scope.searchTerm, scope.exactMatch ? "EXACT" : "CONTAINS");

        if (choice == 0) return;

        if (choice == 1) {
          int picked = promptSearchField(scope.searchField);
          if (picked > 0) scope.searchField = fieldNameFor(picked);
        } else if (choice == 2) {
          String typed = promptSearchTerm(scope.searchField, scope.searchTerm);
          if (!"E".equalsIgnoreCase(typed.trim())) {
            scope.searchTerm = "-".equals(typed.trim()) ? null : typed.trim();
          }
        } else if (choice == 3) {
          scope.exactMatch = !scope.exactMatch;
        } else if (choice == 4) {
          scope.searchTerm = null;
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptRoomTypeFilter(String current) {
    while (true) {
      try {
        return reportView.displayRoomTypeSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptStatusFilter(String current) {
    while (true) {
      try {
        return reportView.displayStatusSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptSourceFilter(String current) {
    while (true) {
      try {
        return reportView.displaySourceSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptSortAttribute(String current, boolean registerReport) {
    while (true) {
      try {
        return reportView.displaySortAttributeSubmenu(current, registerReport);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptSortDirection(String current) {
    while (true) {
      try {
        return reportView.displaySortDirectionSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptGroupBy(String current) {
    while (true) {
      try {
        return reportView.displayGroupBySubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptRecordLimit(int current) {
    while (true) {
      try {
        return reportView.displayRecordLimitSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Integer promptMinimumWait(Integer current) {
    while (true) {
      try {
        return reportView.promptMinimumWait(current);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int[] promptStrikeRange(int currentMin, int currentMax) {
    while (true) {
      try {
        return reportView.promptStrikeRange(currentMin, currentMax);
      } catch (Exception e) {
        if (!BLANK_INPUT.equals(e.getMessage())) ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String promptRoomNumber(String current) {
    while (true) {
      try {
        String typed = reportView.promptRoomNumber(current);
        if (typed.trim().isEmpty()) continue;
        return typed;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptSearchField(String current) {
    while (true) {
      try {
        return reportView.displaySearchFieldSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Blank redraws, so a stray Enter never clears the filter.
  private String promptSearchTerm(String fieldLabel, String current) {
    while (true) {
      try {
        String typed = reportView.promptSearchTerm(fieldLabel, current);
        if (typed.trim().isEmpty()) continue;
        return typed;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handlePeriodSubmenu(ReportScope scope) {
    int picked;
    while (true) {
      try {
        picked = reportView.displayPeriodSubmenu(periodLabel(scope));
        break;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
    if (picked == 0) return;

    if (picked == 6) {
      while (true) {
        try {
          String[] typed = reportView.promptDateRange(periodLabel(scope));
          String from = (typed[0] == null) ? "" : typed[0].trim();
          // Blank redraws, so a stray Enter never clears the range.
          if (from.isEmpty()) continue;
          if ("E".equalsIgnoreCase(from)) return;
          if (typed[1] == null || typed[1].trim().isEmpty()) continue;

          LocalDate parsedFrom = parseOrNull(typed[0]);
          LocalDate parsedTo = parseOrNull(typed[1]);

          if (parsedFrom != null && parsedTo != null && parsedTo.isBefore(parsedFrom)) {
            throw new IllegalArgumentException(
                "The end of the range cannot fall before its start!");
          }

          scope.fromDate = parsedFrom;
          scope.toDate = parsedTo;
          scope.periodFilter = (parsedFrom == null && parsedTo == null) ? "ALL TIME" : "CUSTOM";
          return;
        } catch (IllegalArgumentException e) {
          ConsoleUtil.printError(e.getMessage());
        }
      }
    }

    scope.fromDate = null;
    scope.toDate = null;
    if (picked == 1) scope.periodFilter = "TODAY";
    else if (picked == 2) scope.periodFilter = "YESTERDAY";
    else if (picked == 3) scope.periodFilter = "LAST 7 DAYS";
    else if (picked == 4) scope.periodFilter = "LAST 30 DAYS";
    else scope.periodFilter = "ALL TIME";
  }

  private void handleColumnSelection(ReportScope scope) {
    while (true) {
      try {
        int picked = reportView.displayColumnSelection(COLUMN_NAMES, scope.columns);
        if (picked == 0) return;

        if (picked == -1) {
          for (int i = 0; i < scope.columns.length; i++) {
            scope.columns[i] = true;
          }
          continue;
        }

        scope.columns[picked - 1] = !scope.columns[picked - 1];
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Sorted once, so the .txt and the binary search share one ordered list.
  private boolean exportReport(
      int reportType, ListInterface<Reservation> matched, ReportScope scope) {

    boolean isRegister = (reportType == ARRIVAL_REGISTER);
    String title = isRegister ? REGISTER_TITLE : PERFORMANCE_TITLE;
    String sortLabel = sortLabelOf(scope);
    String scopeLabel = buildScopeLabel(scope);

    ListInterface<Reservation> sorted = sortReservations(matched, scope);
    String content = buildReportText(isRegister, sorted, title, scopeLabel, sortLabel, scope);
    String path = TxtExportUtil.export(reportFileName(isRegister), content);

    // Generating a report means producing it, so it is shown before the .txt is written.
    showReportOnScreen(title, content, path);

    while (true) {
      try {
        ConsoleUtil.GetMenuInputResult result =
            reportView.showExportReceipt(
                title,
                scopeLabel,
                sortLabel,
                scope.groupBy,
                sorted.getNumberOfEntries(),
                exportedRowCount(sorted.getNumberOfEntries(), scope.recordLimit),
                path,
                isRegister,
                isBinarySearchable(scope));

        if ("E".equalsIgnoreCase(result.input)) {
          return true;
        } else if ("S".equalsIgnoreCase(result.input)) {
          return false;
        } else if ("R".equalsIgnoreCase(result.input)) {
          content = buildReportText(isRegister, sorted, title, scopeLabel, sortLabel, scope);
          path = TxtExportUtil.export(reportFileName(isRegister), content);
        } else if ("V".equalsIgnoreCase(result.input)) {
          showReportOnScreen(title, content, path);
        } else if ("F".equalsIgnoreCase(result.input)) {
          // Refusing would teach nothing, so the screen offers the re-sort that makes it legal.
          if (!isBinarySearchable(scope) && promptResortForSearch(sortLabel)) {
            scope.sortAttribute = "RESERVATION ID";
            scope.sortDirection = "ASCENDING";
            sortLabel = sortLabelOf(scope);
            sorted = sortReservations(matched, scope);
            content = buildReportText(isRegister, sorted, title, scopeLabel, sortLabel, scope);
            path = TxtExportUtil.export(reportFileName(isRegister), content);
          }

          if (isBinarySearchable(scope)) {
            handleBinarySearch(sorted, scope);
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String buildReportText(
      boolean isRegister,
      ListInterface<Reservation> sorted,
      String title,
      String scopeLabel,
      String sortLabel,
      ReportScope scope) {

    return isRegister
        ? buildArrivalRegisterTxt(sorted, title, scopeLabel, sortLabel, scope)
        : buildPerformanceReportTxt(sorted, title, scopeLabel, sortLabel, scope);
  }

  private String reportFileName(boolean isRegister) {
    return isRegister ? "booking/daily_arrival_register" : "booking/queue_performance_report";
  }

  // Split into lines once and paged here, because a view must not own the position.
  private void showReportOnScreen(String title, String content, String path) {
    ListInterface<String> lines = new ArrayList<>();
    for (String line : content.split("\\R", -1)) {
      lines.add(line);
    }

    int totalPages =
        Math.max(1, (int) Math.ceil((double) lines.getNumberOfEntries() / REPORT_PAGE_LINES));
    int page = 1;

    while (true) {
      try {
        ConsoleUtil.GetMenuInputResult result =
            reportView.displayReportPage(title, lines, page, REPORT_PAGE_LINES, totalPages, path);

        if ("E".equalsIgnoreCase(result.input)) return;

        if ("N".equalsIgnoreCase(result.input)) {
          if (page < totalPages) {
            page++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (page > 1) {
            page--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String sortLabelOf(ReportScope scope) {
    return scope.sortAttribute + " (" + scope.sortDirection + ")";
  }

  private boolean isBinarySearchable(ReportScope scope) {
    return "RESERVATION ID".equalsIgnoreCase(scope.sortAttribute)
        && "ASCENDING".equalsIgnoreCase(scope.sortDirection);
  }

  private boolean promptResortForSearch(String currentSortLabel) {
    while (true) {
      try {
        return reportView.displayResortForSearchScreen(currentSortLabel);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleBinarySearch(ListInterface<Reservation> sorted, ReportScope scope) {
    String target;
    while (true) {
      try {
        target = reportView.promptReservationIdSearch();
        break;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
    if (target == null || "B".equalsIgnoreCase(target.trim())) {
      return;
    }
    if (target.trim().isEmpty()) {
      throw new IllegalArgumentException("Reservation ID cannot be empty!");
    }

    String needle = target.trim();
    int low = 1;
    int high = sorted.getNumberOfEntries();
    int comparisons = 0;
    int foundAt = -1;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      comparisons++;

      Reservation candidate = sorted.getEntry(mid);
      // Every id is RES- plus five digits, so string order and numeric order agree.
      int comparison = candidate.getReservationId().compareToIgnoreCase(needle);

      if (comparison == 0) {
        foundAt = mid;
        break;
      } else if (comparison < 0) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    Reservation found = (foundAt == -1) ? null : sorted.getEntry(foundAt);
    Guest guest = (found != null) ? guestRepo.findById(found.getGuestId()) : null;

    reportView.displayBinarySearchResult(
        needle,
        found != null,
        (guest != null) ? guest.getName() : "N/A",
        (found != null) ? found.getRoomType().name() : "-",
        (found != null) ? found.getStatus().name() : "-",
        (found != null) ? formatMinutes(waitMinutesOf(found)) : "-",
        foundAt,
        comparisons,
        sorted.getNumberOfEntries());
  }

  private ListInterface<Reservation> filterReservations(ReportScope scope) {
    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    ListInterface<Reservation> matched = new ArrayList<>();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || r.getRoomType() == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      if (!matchesSearch(r, g, scope)) continue;
      if (scope.roomTypeFilter != null
          && !scope.roomTypeFilter.equalsIgnoreCase(r.getRoomType().name())) {
        continue;
      }
      if (scope.statusFilter != null
          && !scope.statusFilter.equalsIgnoreCase(r.getStatus().name())) {
        continue;
      }
      if (scope.sourceFilter != null && !scope.sourceFilter.equalsIgnoreCase(sourceOf(r))) continue;
      if (!matchesPeriod(r, scope)) continue;
      if (scope.minWaitMinutes != null && waitMinutesOf(r) < scope.minWaitMinutes) continue;
      if (!matchesStrikeRange(g, scope)) continue;
      if (scope.roomNumberFilter != null
          && !scope.roomNumberFilter.equalsIgnoreCase(r.getRoomNumber())) {
        continue;
      }

      matched.add(r);
    }

    return matched;
  }

  private boolean matchesStrikeRange(Guest g, ReportScope scope) {
    if (scope.minStrikes < 0 && scope.maxStrikes < 0) return true;

    int strikes = (g != null) ? g.getStrikeCount() : 0;
    if (scope.minStrikes >= 0 && strikes < scope.minStrikes) return false;
    return scope.maxStrikes < 0 || strikes <= scope.maxStrikes;
  }

  private String sourceOf(Reservation r) {
    return (r.getExpectedArrivalTime() != null) ? "ADVANCE" : "WALK-IN";
  }

  private ListInterface<Reservation> sortReservations(
      ListInterface<Reservation> source, ReportScope scope) {

    ListInterface<Reservation> sorted = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      sorted.add(source.getEntry(i));
    }

    boolean ascending = "ASCENDING".equalsIgnoreCase(scope.sortDirection);
    String attribute = scope.sortAttribute;

    if ("RESERVATION ID".equalsIgnoreCase(attribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? a.getReservationId().compareToIgnoreCase(b.getReservationId())
                  : b.getReservationId().compareToIgnoreCase(a.getReservationId()));
    } else if ("ARRIVAL TIME".equalsIgnoreCase(attribute)) {
      sorted.sort((a, b) -> ascending ? compareArrival(a, b) : compareArrival(b, a));
    } else if ("GUEST NAME".equalsIgnoreCase(attribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? guestNameOf(a).compareToIgnoreCase(guestNameOf(b))
                  : guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("ROOM TYPE".equalsIgnoreCase(attribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? a.getRoomType().name().compareTo(b.getRoomType().name())
                  : b.getRoomType().name().compareTo(a.getRoomType().name()));
    } else if ("BOOKING STATUS".equalsIgnoreCase(attribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? a.getStatus().name().compareTo(b.getStatus().name())
                  : b.getStatus().name().compareTo(a.getStatus().name()));
    } else if ("STRIKE COUNT".equalsIgnoreCase(attribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? Integer.compare(strikesOf(a), strikesOf(b))
                  : Integer.compare(strikesOf(b), strikesOf(a)));
    } else {
      sorted.sort(
          (a, b) ->
              ascending
                  ? Long.compare(waitMinutesOf(a), waitMinutesOf(b))
                  : Long.compare(waitMinutesOf(b), waitMinutesOf(a)));
    }

    return sorted;
  }

  // ================= TXT REPORT CONTENT =================

  private String buildArrivalRegisterTxt(
      ListInterface<Reservation> sorted,
      String title,
      String scopeLabel,
      String sortLabel,
      ReportScope scope) {

    int matched = sorted.getNumberOfEntries();
    int shown = exportedRowCount(matched, scope.recordLimit);

    StringBuilder sb = new StringBuilder();
    appendReportHeader(sb, title, scopeLabel, sortLabel, scope.groupBy, matched, shown);

    if (shown == 0) {
      appendSectionHeading(sb, "ARRIVALS");
      sb.append("*** NO BOOKINGS MATCH THE CURRENT SCOPE ***").append(NEW_LINE);
    } else {
      appendGroupedRows(sb, sorted, shown, scope);
      appendTruncationNote(sb, matched, scope.recordLimit);
    }

    sb.append(NEW_LINE);
    appendSectionHeading(sb, "SUBTOTALS WITHIN SCOPE");
    sb.append(
        buildTxtTable(
            new String[] {"Room Type", "In Scope", "In Line", "On Hold", "Checked In", "Closed"},
            buildRoomTypeSubtotals(sorted)));

    return sb.toString();
  }

  private String buildPerformanceReportTxt(
      ListInterface<Reservation> sorted,
      String title,
      String scopeLabel,
      String sortLabel,
      ReportScope scope) {

    int matched = sorted.getNumberOfEntries();
    int shown = exportedRowCount(matched, scope.recordLimit);

    StringBuilder sb = new StringBuilder();
    appendReportHeader(sb, title, scopeLabel, sortLabel, scope.groupBy, matched, shown);

    String[][] perType = buildPerformanceSummary(sorted);
    String[][] summaryRows = new String[perType.length + 1][];
    for (int i = 0; i < perType.length; i++) {
      summaryRows[i] = perType[i];
    }
    summaryRows[perType.length] = buildOverallSummary(sorted);

    appendSectionHeading(sb, "OPERATIONAL SUMMARY");
    sb.append(
        buildTxtTable(
            new String[] {
              "Room Type", "Arrivals", "Avg Wait", "Max Wait", "Served", "No-Shows", "No-Show Rate"
            },
            summaryRows));

    sb.append(NEW_LINE);

    if (shown == 0) {
      appendSectionHeading(sb, "WORST WAITS IN SCOPE");
      sb.append("*** NO BOOKINGS MATCH THE CURRENT SCOPE ***").append(NEW_LINE);
      return sb.toString();
    }

    appendGroupedRows(sb, sorted, shown, scope);
    appendTruncationNote(sb, matched, scope.recordLimit);

    return sb.toString();
  }

  private void appendGroupedRows(
      StringBuilder sb, ListInterface<Reservation> sorted, int shown, ReportScope scope) {

    String[] headers = selectedHeaders(scope);

    if ("NO GROUPING".equalsIgnoreCase(scope.groupBy)) {
      appendSectionHeading(sb, "BOOKINGS IN SCOPE");
      sb.append(buildTxtTable(headers, buildRows(sorted, 1, shown, scope)));
      return;
    }

    ListInterface<String> groups = distinctGroupKeys(sorted, shown, scope.groupBy);

    for (int gi = 1; gi <= groups.getNumberOfEntries(); gi++) {
      String key = groups.getEntry(gi);

      ListInterface<Reservation> inGroup = new ArrayList<>();
      for (int i = 1; i <= shown; i++) {
        Reservation r = sorted.getEntry(i);
        if (r != null && key.equals(groupKeyOf(r, scope.groupBy))) {
          inGroup.add(r);
        }
      }

      appendSectionHeading(
          sb, scope.groupBy + ": " + key + "   (" + inGroup.getNumberOfEntries() + " row(s))");
      sb.append(buildTxtTable(headers, buildRows(inGroup, 1, inGroup.getNumberOfEntries(), scope)));
      sb.append(NEW_LINE);
    }
  }

  private ListInterface<String> distinctGroupKeys(
      ListInterface<Reservation> sorted, int shown, String groupBy) {

    ListInterface<String> keys = new ArrayList<>();
    for (int i = 1; i <= shown; i++) {
      Reservation r = sorted.getEntry(i);
      if (r == null) continue;

      String key = groupKeyOf(r, groupBy);
      if (!keys.contains(key)) {
        keys.add(key);
      }
    }
    return keys;
  }

  private String groupKeyOf(Reservation r, String groupBy) {
    if ("ROOM TYPE".equalsIgnoreCase(groupBy)) return r.getRoomType().name();
    if ("BOOKING STATUS".equalsIgnoreCase(groupBy)) return r.getStatus().name();

    LocalDateTime stamp =
        (r.getQueueArrivalTime() != null) ? r.getQueueArrivalTime() : r.getExpectedArrivalTime();
    return (stamp == null) ? "NO ARRIVAL DATE" : stamp.toLocalDate().format(DATE_FORMAT);
  }

  private String[] selectedHeaders(ReportScope scope) {
    int count = 1;
    for (boolean on : scope.columns) {
      if (on) count++;
    }

    String[] headers = new String[count];
    headers[0] = "No.";

    int next = 1;
    for (int i = 0; i < scope.columns.length; i++) {
      if (scope.columns[i]) {
        headers[next++] = COLUMN_NAMES[i];
      }
    }
    return headers;
  }

  private String[][] buildRows(
      ListInterface<Reservation> source, int from, int to, ReportScope scope) {

    int count = Math.max(0, to - from + 1);
    String[][] rows = new String[count][];

    for (int i = from; i <= to; i++) {
      Reservation r = source.getEntry(i);
      Guest g = (r == null) ? null : guestRepo.findById(r.getGuestId());
      rows[i - from] = buildRow(i - from + 1, r, g, scope);
    }
    return rows;
  }

  private String[] buildRow(int rowNumber, Reservation r, Guest g, ReportScope scope) {
    String[] all = new String[COLUMN_NAMES.length];

    if (r == null) {
      for (int i = 0; i < all.length; i++) {
        all[i] = "-";
      }
    } else {
      all[0] = r.getReservationId();
      all[1] = blankToDash(r.getConfirmationNumber());
      all[2] = blankToDash(r.getGuestId());
      all[3] = (g != null) ? g.getName() : "N/A";
      all[4] = (g != null) ? blankToDash(g.getPhoneNumber()) : "-";
      all[5] = r.getRoomType().name();
      all[6] = blankToDash(r.getRoomNumber());
      all[7] = r.getStatus().name();
      all[8] = sourceOf(r);
      all[9] = formatMinutes(waitMinutesOf(r));
      all[10] = hasArrived(r) ? formatClock(arrivalStampOf(r)) : formatDay(arrivalStampOf(r));
      all[11] = (r.getStayDays() != null) ? String.valueOf(r.getStayDays()) : "-";
      all[12] = String.valueOf((g != null) ? g.getStrikeCount() : 0);
    }

    int count = 1;
    for (boolean on : scope.columns) {
      if (on) count++;
    }

    String[] row = new String[count];
    row[0] = String.valueOf(rowNumber);

    int next = 1;
    for (int i = 0; i < scope.columns.length; i++) {
      if (scope.columns[i]) {
        row[next++] = all[i];
      }
    }
    return row;
  }

  // A walk-in arrives by queueing, a booking by check-in, one still due by its date.
  private LocalDateTime arrivalStampOf(Reservation r) {
    if (r.getQueueArrivalTime() != null) return r.getQueueArrivalTime();
    if (r.getCheckInTime() != null) return r.getCheckInTime();
    return r.getExpectedArrivalTime();
  }

  private String[][] buildRoomTypeSubtotals(ListInterface<Reservation> rows) {
    Room.RoomType[] types = Room.RoomType.values();
    String[][] table = new String[types.length][6];

    for (int t = 0; t < types.length; t++) {
      int inScope = 0;
      int inLine = 0;
      int onHold = 0;
      int checkedIn = 0;
      int closed = 0;

      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        Reservation r = rows.getEntry(i);
        if (r == null || r.getRoomType() != types[t]) continue;

        inScope++;
        if (r.getStatus() == Reservation.Status.WAITING) inLine++;
        else if (r.getStatus() == Reservation.Status.ALLOCATED) onHold++;
        else if (r.getStatus() == Reservation.Status.CHECKED_IN) checkedIn++;
        else if (r.getStatus() == Reservation.Status.NO_SHOW
            || r.getStatus() == Reservation.Status.CANCELLED) closed++;
      }

      table[t] =
          new String[] {
            types[t].name(),
            String.valueOf(inScope),
            String.valueOf(inLine),
            String.valueOf(onHold),
            String.valueOf(checkedIn),
            String.valueOf(closed)
          };
    }

    return table;
  }

  private String[][] buildPerformanceSummary(ListInterface<Reservation> rows) {
    Room.RoomType[] types = Room.RoomType.values();
    String[][] table = new String[types.length][7];

    for (int t = 0; t < types.length; t++) {
      table[t] = summariseRows(rows, types[t], types[t].name());
    }

    return table;
  }

  private String[] buildOverallSummary(ListInterface<Reservation> rows) {
    return summariseRows(rows, null, "ALL TYPES");
  }

  // Counted once it queued, checked in or no-showed. Still-awaited would dilute the rate.
  private String[] summariseRows(
      ListInterface<Reservation> rows, Room.RoomType roomType, String label) {

    int arrivals = 0;
    int served = 0;
    int noShows = 0;
    long totalWait = 0;
    long longestWait = 0;

    for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
      Reservation r = rows.getEntry(i);
      if (r == null) continue;
      if (roomType != null && r.getRoomType() != roomType) continue;
      if (!hasBeenResolved(r)) continue;

      arrivals++;

      // A booking checked straight in never queued, so it waited nothing rather than -1.
      long wait = Math.max(0, waitMinutesOf(r));
      totalWait += wait;
      if (wait > longestWait) {
        longestWait = wait;
      }

      if (r.getStatus() == Reservation.Status.CHECKED_IN) served++;
      if (r.getStatus() == Reservation.Status.NO_SHOW) noShows++;
    }

    long averageWait = (arrivals == 0) ? 0 : totalWait / arrivals;
    String noShowRate =
        (arrivals == 0) ? "-" : String.format("%.1f%%", (noShows * 100.0) / arrivals);

    return new String[] {
      label,
      String.valueOf(arrivals),
      formatMinutes(averageWait),
      formatMinutes(longestWait),
      String.valueOf(served),
      String.valueOf(noShows),
      noShowRate
    };
  }

  private boolean hasBeenResolved(Reservation r) {
    return r.getQueueArrivalTime() != null
        || r.getCheckInTime() != null
        || r.getStatus() == Reservation.Status.NO_SHOW;
  }

  // ================= SHARED TXT LAYOUT =================

  private void appendReportHeader(
      StringBuilder sb,
      String title,
      String scopeLabel,
      String sortLabel,
      String groupLabel,
      int matched,
      int exported) {

    sb.append(title).append(NEW_LINE);
    appendRule(sb, '=', title.length());
    sb.append("GENERATED : ").append(formatTimestamp(LocalDateTime.now())).append(NEW_LINE);
    sb.append("SCOPE     : ").append(scopeLabel).append(NEW_LINE);
    sb.append("SORTED BY : ").append(sortLabel).append(NEW_LINE);
    sb.append("GROUPED BY: ").append(groupLabel).append(NEW_LINE);
    sb.append("RECORDS   : ")
        .append(matched)
        .append(" matching, ")
        .append(exported)
        .append(" written")
        .append(NEW_LINE)
        .append(NEW_LINE);
  }

  private void appendSectionHeading(StringBuilder sb, String heading) {
    sb.append(heading).append(NEW_LINE);
    appendRule(sb, '-', heading.length());
  }

  private void appendRule(StringBuilder sb, char character, int length) {
    for (int i = 0; i < length; i++) {
      sb.append(character);
    }
    sb.append(NEW_LINE);
  }

  private void appendTruncationNote(StringBuilder sb, int matched, int recordLimit) {
    if (recordLimit == 0 || matched <= recordLimit) return;

    sb.append(NEW_LINE)
        .append("Showing the top ")
        .append(recordLimit)
        .append(" of ")
        .append(matched)
        .append(" matching records. Raise the record limit to export the rest.")
        .append(NEW_LINE);
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
    if (raw == null || raw.trim().isEmpty() || "-".equals(raw.trim())) return null;
    return parseDate(raw.trim());
  }

  private int exportedRowCount(int matched, int recordLimit) {
    return (recordLimit == 0) ? matched : Math.min(recordLimit, matched);
  }

  private String buildTxtTable(String[] headers, String[][] rows) {
    final String gap = "   ";
    if (headers == null) return "";
    int columnCount = headers.length;
    int[] widths = new int[columnCount];

    for (int c = 0; c < columnCount; c++) {
      widths[c] = (headers[c] == null) ? 0 : headers[c].length();
    }
    if (rows != null) {
      for (String[] row : rows) {
        if (row == null) continue;
        for (int c = 0; c < columnCount && c < row.length; c++) {
          int length = (row[c] == null) ? 0 : row[c].length();
          if (length > widths[c]) widths[c] = length;
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append(formatTxtRow(headers, widths, gap));

    int dividerLength = gap.length() * Math.max(0, columnCount - 1);
    for (int width : widths) {
      dividerLength += width;
    }
    appendRule(sb, '-', dividerLength);

    if (rows != null) {
      for (String[] row : rows) {
        sb.append(formatTxtRow(row, widths, gap));
      }
    }

    return sb.toString();
  }

  private String formatTxtRow(String[] fields, int[] widths, String gap) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < widths.length; i++) {
      String value = (fields != null && i < fields.length && fields[i] != null) ? fields[i] : "";
      sb.append(value);

      // The last column is left ragged so no line carries trailing spaces.
      if (i < widths.length - 1) {
        for (int p = value.length(); p < widths[i]; p++) {
          sb.append(' ');
        }
        sb.append(gap);
      }
    }

    sb.append(NEW_LINE);
    return sb.toString();
  }

  // ================= LABELS, FILTERS AND HELPERS =================

  private String searchLabel(ReportScope scope) {
    if (scope.searchTerm == null) return "None";
    return scope.searchField
        + " "
        + (scope.exactMatch ? "EXACT" : "CONTAINS")
        + " \""
        + scope.searchTerm
        + "\"";
  }

  private String periodLabel(ReportScope scope) {
    if (!"CUSTOM".equalsIgnoreCase(scope.periodFilter)) return scope.periodFilter;

    String from = (scope.fromDate == null) ? "open" : scope.fromDate.format(DATE_FORMAT);
    String to = (scope.toDate == null) ? "open" : scope.toDate.format(DATE_FORMAT);
    return from + " .. " + to;
  }

  private String strikeLabel(ReportScope scope) {
    if (scope.minStrikes < 0 && scope.maxStrikes < 0) return "Any";
    if (scope.minStrikes >= 0 && scope.maxStrikes >= 0) {
      return scope.minStrikes + " to " + scope.maxStrikes;
    }
    return (scope.minStrikes >= 0) ? scope.minStrikes + " or more" : scope.maxStrikes + " or fewer";
  }

  private String columnsLabel(ReportScope scope) {
    int chosen = 0;
    for (boolean on : scope.columns) {
      if (on) chosen++;
    }
    return chosen + " of " + COLUMN_NAMES.length + " shown";
  }

  private String buildScopeLabel(ReportScope scope) {
    StringBuilder label = new StringBuilder();
    label.append(periodLabel(scope));
    label
        .append("  |  ")
        .append(scope.roomTypeFilter == null ? "All Room Types" : scope.roomTypeFilter);

    if (scope.statusFilter != null) {
      label.append("  |  Status ").append(scope.statusFilter);
    }
    if (scope.sourceFilter != null) {
      label.append("  |  Source ").append(scope.sourceFilter);
    }
    if (scope.minWaitMinutes != null) {
      label.append("  |  Waited >= ").append(scope.minWaitMinutes).append("m");
    }
    if (scope.minStrikes >= 0 || scope.maxStrikes >= 0) {
      label.append("  |  Strikes ").append(strikeLabel(scope));
    }
    if (scope.roomNumberFilter != null) {
      label.append("  |  Room ").append(scope.roomNumberFilter);
    }
    if (scope.searchTerm != null) {
      label.append("  |  ").append(searchLabel(scope));
    }

    return label.toString();
  }

  private boolean matchesSearch(Reservation r, Guest g, ReportScope scope) {
    if (scope.searchTerm == null || scope.searchTerm.trim().isEmpty()) return true;

    String query = scope.searchTerm.trim().toLowerCase();
    boolean exact = scope.exactMatch;
    String field = scope.searchField;

    if (FIELD_GUEST_ID.equals(field)) return hit(g == null ? null : g.getGuestId(), query, exact);
    if (FIELD_NAME.equals(field)) return hit(g == null ? null : g.getName(), query, exact);
    if (FIELD_IC.equals(field)) return hit(g == null ? null : g.getIcNumber(), query, exact);
    if (FIELD_PASSPORT.equals(field)) {
      return hit(g == null ? null : g.getPassportNumber(), query, exact);
    }
    if (FIELD_PHONE.equals(field)) return hit(g == null ? null : g.getPhoneNumber(), query, exact);
    if (FIELD_EMAIL.equals(field)) return hit(g == null ? null : g.getEmail(), query, exact);
    if (FIELD_RES_ID.equals(field)) return hit(r.getReservationId(), query, exact);
    if (FIELD_CODE.equals(field)) return hit(r.getConfirmationNumber(), query, exact);

    return hit(r.getReservationId(), query, exact)
        || hit(r.getConfirmationNumber(), query, exact)
        || (g != null
            && (hit(g.getGuestId(), query, exact)
                || hit(g.getName(), query, exact)
                || hit(g.getIcNumber(), query, exact)
                || hit(g.getPassportNumber(), query, exact)
                || hit(g.getPhoneNumber(), query, exact)
                || hit(g.getEmail(), query, exact)));
  }

  private boolean hit(String value, String query, boolean exactMatch) {
    if (value == null) return false;
    String candidate = value.toLowerCase();
    return exactMatch ? candidate.equals(query) : candidate.contains(query);
  }

  private boolean matchesPeriod(Reservation r, ReportScope scope) {
    String period = scope.periodFilter;
    if ("ALL TIME".equalsIgnoreCase(period)) return true;

    LocalDateTime stamp = arrivalStampOf(r);
    if (stamp == null) stamp = r.getReservationTime();
    if (stamp == null) return false;

    LocalDate date = stamp.toLocalDate();
    LocalDate today = LocalDate.now();

    if ("CUSTOM".equalsIgnoreCase(period)) {
      if (scope.fromDate != null && date.isBefore(scope.fromDate)) return false;
      return scope.toDate == null || !date.isAfter(scope.toDate);
    }
    if ("TODAY".equalsIgnoreCase(period)) return date.isEqual(today);
    if ("YESTERDAY".equalsIgnoreCase(period)) return date.isEqual(today.minusDays(1));
    if ("LAST 30 DAYS".equalsIgnoreCase(period)) return !date.isBefore(today.minusDays(29));
    return !date.isBefore(today.minusDays(6));
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

  private String roomTypeNameFor(int choice) {
    if (choice == 1) return "LUXURY";
    if (choice == 2) return "SUITE";
    if (choice == 3) return "STANDARD";
    return null;
  }

  private String statusNameFor(int choice) {
    if (choice == 1) return "RESERVED";
    if (choice == 2) return "WAITING";
    if (choice == 3) return "ALLOCATED";
    if (choice == 4) return "CHECKED_IN";
    if (choice == 5) return "CHECKED_OUT";
    if (choice == 6) return "NO_SHOW";
    if (choice == 7) return "CANCELLED";
    return null;
  }

  private String sortAttributeFor(int choice, boolean isRegister) {
    if (choice == 1) return "WAIT DURATION";
    if (choice == 2) return "ARRIVAL TIME";
    if (choice == 3) return "GUEST NAME";
    if (choice == 4) return "ROOM TYPE";
    if (choice == 5) return "BOOKING STATUS";
    return isRegister ? "RESERVATION ID" : "STRIKE COUNT";
  }

  private long waitMinutesOf(Reservation r) {
    if (r.getQueueArrivalTime() == null) return -1;

    LocalDateTime end = (r.getAllocatedTime() != null) ? r.getAllocatedTime() : LocalDateTime.now();
    return Duration.between(r.getQueueArrivalTime(), end).toMinutes();
  }

  private int compareArrival(Reservation a, Reservation b) {
    LocalDateTime first = arrivalStampOf(a);
    LocalDateTime second = arrivalStampOf(b);
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

  private String blankToDash(String value) {
    return (value == null || value.isEmpty()) ? "-" : value;
  }

  private String formatMinutes(long minutes) {
    if (minutes < 0) return "-";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  // Only an actual arrival has a knowable minute, so an awaited one shows no clock.
  private boolean hasArrived(Reservation r) {
    return r.getQueueArrivalTime() != null || r.getCheckInTime() != null;
  }

  private String formatDay(LocalDateTime dateTime) {
    if (dateTime == null) return "-";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM"));
  }

  private String formatClock(LocalDateTime dateTime) {
    if (dateTime == null) return "-";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
  }

  private String formatTimestamp(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }
}
