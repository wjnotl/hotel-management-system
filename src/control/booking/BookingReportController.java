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
import repo.BookingSettingsRepo;
import repo.GuestRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import util.TxtExportUtil;
import view.booking.BookingReportView;

public class BookingReportController {
  private static final int ARRIVAL_REGISTER = 1;
  private static final int QUEUE_PERFORMANCE = 2;
  private static final int ROOM_UTILISATION = 3;

  private static final String REGISTER_TITLE = "DAILY ARRIVAL REGISTER";
  private static final String PERFORMANCE_TITLE = "QUEUE PERFORMANCE & NO-SHOW ANALYSIS";
  private static final String UTILISATION_TITLE = "ROOM UTILISATION & FORECAST";
  private static final String NEW_LINE = System.lineSeparator();

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
  private final BookingSettingsRepo bookingSettingsRepo;

  public BookingReportController(
      StandardReservationRepo standardReservationRepo,
      GuestRepo guestRepo,
      RoomRepo roomRepo,
      BookingSettingsRepo bookingSettingsRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.guestRepo = guestRepo;
    this.roomRepo = roomRepo;
    this.bookingSettingsRepo = bookingSettingsRepo;
  }

  private BookingSettings settings() {
    return bookingSettingsRepo.getSettings();
  }

  public void startReportManagement() {
    while (true) {
      try {
        int choice = reportView.displayReportHubMenu();
        if (choice == 0) {
          return;
        }

        if (choice == ROOM_UTILISATION) {
          runUtilisationReport();
        } else {
          runReportPipeline(choice);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Everything the clerk can narrow, order or reshape before the file is written. Held in one
  // object so a reset is a single assignment and the whole scope travels to the export in one
  // piece rather than as a dozen parameters.
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

        if ("B".equals(command)) {
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
          int picked = reportView.displayRoomTypeSubmenu(scope.roomTypeFilter);
          if (picked > 0) scope.roomTypeFilter = roomTypeNameFor(picked);
        } else if ("3".equals(command)) {
          if (isRegister) {
            int picked = reportView.displayStatusSubmenu(scope.statusFilter);
            if (picked > 0) scope.statusFilter = statusNameFor(picked);
          } else {
            scope.minWaitMinutes = reportView.promptMinimumWait(scope.minWaitMinutes);
          }
        } else if ("4".equals(command)) {
          int picked = reportView.displaySourceSubmenu(scope.sourceFilter);
          if (picked == 1) scope.sourceFilter = "WALK-IN";
          else if (picked == 2) scope.sourceFilter = "ADVANCE";
          else if (picked == 3) scope.sourceFilter = null;
        } else if ("5".equals(command)) {
          handlePeriodSubmenu(scope);
        } else if ("6".equals(command)) {
          int[] range = reportView.promptStrikeRange(scope.minStrikes, scope.maxStrikes);
          scope.minStrikes = range[0];
          scope.maxStrikes = range[1];
        } else if ("7".equals(command)) {
          scope.roomNumberFilter = reportView.promptRoomNumber(scope.roomNumberFilter);
        } else if ("8".equals(command)) {
          int attribute = reportView.displaySortAttributeSubmenu(scope.sortAttribute, isRegister);
          if (attribute > 0) {
            scope.sortAttribute = sortAttributeFor(attribute, isRegister);
            int direction = reportView.displaySortDirectionSubmenu(scope.sortDirection);
            if (direction == 1) scope.sortDirection = "DESCENDING";
            else if (direction == 2) scope.sortDirection = "ASCENDING";
          }
        } else if ("9".equals(command)) {
          int picked = reportView.displayGroupBySubmenu(scope.groupBy);
          if (picked == 1) scope.groupBy = "NO GROUPING";
          else if (picked == 2) scope.groupBy = "ROOM TYPE";
          else if (picked == 3) scope.groupBy = "BOOKING STATUS";
          else if (picked == 4) scope.groupBy = "ARRIVAL DAY";
        } else if ("10".equals(command)) {
          handleColumnSelection(scope);
        } else if ("11".equals(command)) {
          int picked = reportView.displayRecordLimitSubmenu(scope.recordLimit);
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
      int choice =
          reportView.displaySearchSubmenu(
              scope.searchField, scope.searchTerm, scope.exactMatch ? "EXACT" : "CONTAINS");

      if (choice == 0) return;

      if (choice == 1) {
        int picked = reportView.displaySearchFieldSubmenu(scope.searchField);
        if (picked > 0) scope.searchField = fieldNameFor(picked);
      } else if (choice == 2) {
        scope.searchTerm = reportView.promptSearchTerm(scope.searchField, scope.searchTerm);
      } else if (choice == 3) {
        scope.exactMatch = !scope.exactMatch;
      } else if (choice == 4) {
        scope.searchTerm = null;
        return;
      }
    }
  }

  private void handlePeriodSubmenu(ReportScope scope) {
    int picked = reportView.displayPeriodSubmenu(periodLabel(scope));
    if (picked == 0) return;

    if (picked == 6) {
      LocalDate[] range = reportView.promptDateRange(scope.fromDate, scope.toDate);
      scope.fromDate = range[0];
      scope.toDate = range[1];
      scope.periodFilter = (range[0] == null && range[1] == null) ? "ALL TIME" : "CUSTOM";
      return;
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
      int picked = reportView.displayColumnSelection(COLUMN_NAMES, scope.columns);
      if (picked == 0) return;

      if (picked == -1) {
        for (int i = 0; i < scope.columns.length; i++) {
          scope.columns[i] = true;
        }
        continue;
      }

      scope.columns[picked - 1] = !scope.columns[picked - 1];
    }
  }

  // The report body goes to a .txt file, so the sort runs once here and the same ordered list
  // backs both the file and the binary search offered afterwards. Returns true to leave the
  // report, false to go back to the scope screen.
  private boolean exportReport(
      int reportType, ListInterface<Reservation> matched, ReportScope scope) {

    boolean isRegister = (reportType == ARRIVAL_REGISTER);
    String title = isRegister ? REGISTER_TITLE : PERFORMANCE_TITLE;
    String sortLabel = scope.sortAttribute + " (" + scope.sortDirection + ")";
    String scopeLabel = buildScopeLabel(scope);

    ListInterface<Reservation> sorted = sortReservations(matched, scope);
    String path = writeReportFile(isRegister, sorted, title, scopeLabel, sortLabel, scope);

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
                "RESERVATION ID".equalsIgnoreCase(scope.sortAttribute)
                    && "ASCENDING".equalsIgnoreCase(scope.sortDirection));

        if ("B".equalsIgnoreCase(result.input)) {
          return true;
        } else if ("S".equalsIgnoreCase(result.input)) {
          return false;
        } else if ("R".equalsIgnoreCase(result.input)) {
          path = writeReportFile(isRegister, sorted, title, scopeLabel, sortLabel, scope);
        } else if ("F".equalsIgnoreCase(result.input)) {
          handleBinarySearch(sorted, scope);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String writeReportFile(
      boolean isRegister,
      ListInterface<Reservation> sorted,
      String title,
      String scopeLabel,
      String sortLabel,
      ReportScope scope) {

    String content =
        isRegister
            ? buildArrivalRegisterTxt(sorted, title, scopeLabel, sortLabel, scope)
            : buildPerformanceReportTxt(sorted, title, scopeLabel, sortLabel, scope);

    return TxtExportUtil.export(
        isRegister ? "booking/daily_arrival_register" : "booking/queue_performance_report",
        content);
  }

  // Binary search is only valid on the key the list is actually ordered by.
  private void handleBinarySearch(ListInterface<Reservation> sorted, ReportScope scope) {
    if (!"RESERVATION ID".equalsIgnoreCase(scope.sortAttribute)
        || !"ASCENDING".equalsIgnoreCase(scope.sortDirection)) {
      ConsoleUtil.printError(
          "Binary search needs the register sorted by RESERVATION ID (ASCENDING). Change the"
              + " sort order first.");
      return;
    }

    String target = reportView.promptReservationIdSearch();
    if (target == null || target.trim().isEmpty() || "B".equalsIgnoreCase(target.trim())) {
      return;
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
      // Every id is "RES-" plus a five digit number, so alphabetical order and numeric order are
      // the same and a plain string comparison is safe here.
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
        needle, found, guest, foundAt, comparisons, sorted.getNumberOfEntries());
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

  // Grouping splits the limited rows into one titled section per value, each with its own count.
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
    return (stamp == null)
        ? "NO ARRIVAL DATE"
        : stamp.toLocalDate().format(ConsoleUtil.DATE_FORMAT);
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
      all[10] = formatClock(arrivalStampOf(r));
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

  private LocalDateTime arrivalStampOf(Reservation r) {
    return (r.getQueueArrivalTime() != null) ? r.getQueueArrivalTime() : r.getExpectedArrivalTime();
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

  // A booking counts as an arrival once it has actually stood in a line, so RESERVED bookings
  // that never turned up are left out of the wait and no-show maths entirely.
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
      if (r.getQueueArrivalTime() == null) continue;

      arrivals++;

      long wait = waitMinutesOf(r);
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

  // ================= ROOM UTILISATION & FORECAST =================

  private void runUtilisationReport() {
    LocalDate startDate = LocalDate.now();
    int horizonDays = 14;
    String roomTypeFilter = null;

    while (true) {
      try {
        standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);

        String command =
            reportView.displayUtilisationPanel(
                horizonDays, roomTypeFilter, startDate, countLiveBookings());

        if ("B".equals(command)) {
          return;
        } else if ("R".equals(command)) {
          startDate = LocalDate.now();
          horizonDays = 14;
          roomTypeFilter = null;
        } else if ("X".equals(command)) {
          String path =
              TxtExportUtil.export(
                  "booking/room_utilisation_forecast",
                  buildUtilisationTxt(startDate, horizonDays, roomTypeFilter));

          ConsoleUtil.GetMenuInputResult result =
              reportView.showExportReceipt(
                  UTILISATION_TITLE,
                  startDate.format(ConsoleUtil.DATE_FORMAT)
                      + " for "
                      + horizonDays
                      + " nights  |  "
                      + ((roomTypeFilter == null) ? "All Room Types" : roomTypeFilter),
                  "CALENDAR DATE (ASCENDING)",
                  "NIGHT",
                  horizonDays,
                  horizonDays,
                  path,
                  false,
                  false);

          if ("B".equalsIgnoreCase(result.input)) return;
        } else if ("1".equals(command)) {
          startDate = reportView.promptStartDate(startDate);
        } else if ("2".equals(command)) {
          Integer picked = reportView.promptHorizon(horizonDays);
          if (picked != null) horizonDays = picked;
        } else if ("3".equals(command)) {
          int picked = reportView.displayRoomTypeSubmenu(roomTypeFilter);
          if (picked > 0) roomTypeFilter = roomTypeNameFor(picked);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int countLiveBookings() {
    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    int count = 0;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && r.getOccupancyStartDate() != null) {
        count++;
      }
    }
    return count;
  }

  private String buildUtilisationTxt(LocalDate startDate, int horizonDays, String roomTypeFilter) {
    Room.RoomType[] allTypes = Room.RoomType.values();

    ListInterface<Room.RoomType> types = new ArrayList<>();
    for (Room.RoomType type : allTypes) {
      if (roomTypeFilter == null || roomTypeFilter.equalsIgnoreCase(type.name())) {
        types.add(type);
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append(UTILISATION_TITLE).append(NEW_LINE);
    appendRule(sb, '=', UTILISATION_TITLE.length());
    sb.append("GENERATED : ").append(formatTimestamp(LocalDateTime.now())).append(NEW_LINE);
    sb.append("FROM      : ").append(startDate.format(ConsoleUtil.DATE_FORMAT)).append(NEW_LINE);
    sb.append("HORIZON   : ").append(horizonDays).append(" night(s)").append(NEW_LINE);
    sb.append("ROOM TYPE : ")
        .append((roomTypeFilter == null) ? "All room types" : roomTypeFilter)
        .append(NEW_LINE)
        .append(NEW_LINE);

    for (int t = 1; t <= types.getNumberOfEntries(); t++) {
      Room.RoomType type = types.getEntry(t);
      int total = standardReservationRepo.getTotalRoomsOfType(roomRepo, type);

      appendSectionHeading(sb, type.name() + "   (" + total + " room(s) in the house)");

      String[][] rows = new String[horizonDays][];
      for (int d = 0; d < horizonDays; d++) {
        LocalDate date = startDate.plusDays(d);
        int committed = standardReservationRepo.countCommittedOn(type, date);
        int free = Math.max(0, total - committed);
        String occupancy =
            (total == 0) ? "-" : String.format("%.0f%%", (committed * 100.0) / total);

        rows[d] =
            new String[] {
              date.format(ConsoleUtil.DATE_FORMAT),
              date.getDayOfWeek().name().substring(0, 3),
              String.valueOf(total),
              String.valueOf(committed),
              String.valueOf(free),
              occupancy,
              (free == 0) ? "FULLY BOOKED" : ""
            };
      }

      sb.append(
          buildTxtTable(
              new String[] {"Date", "Day", "Rooms", "Committed", "Free", "Occupancy", "Note"},
              rows));
      sb.append(NEW_LINE);
    }

    sb.append("Committed counts advance bookings, guests still in their rooms and rooms currently")
        .append(NEW_LINE)
        .append("on hold, across every module. Checkout day is a turnover day and is counted free.")
        .append(NEW_LINE);

    return sb.toString();
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

  private int exportedRowCount(int matched, int recordLimit) {
    return (recordLimit == 0) ? matched : Math.min(recordLimit, matched);
  }

  // Pads every column to the widest value it holds so the file lines up in a plain text editor.
  private String buildTxtTable(String[] headers, String[][] rows) {
    final String gap = "   ";
    int columnCount = (headers == null) ? 0 : headers.length;
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

    String from =
        (scope.fromDate == null) ? "open" : scope.fromDate.format(ConsoleUtil.DATE_FORMAT);
    String to = (scope.toDate == null) ? "open" : scope.toDate.format(ConsoleUtil.DATE_FORMAT);
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

  private String formatClock(LocalDateTime dateTime) {
    if (dateTime == null) return "-";
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
  }

  private String formatTimestamp(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }
}
