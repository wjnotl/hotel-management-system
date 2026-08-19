package view.frontdesk;

import adt.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ReportsView {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

  private String lastPaymentFilter = null;
  private String lastRoomTypeFilter = null;

  public String getLastPaymentFilter() {
    return lastPaymentFilter;
  }

  public String getLastRoomTypeFilter() {
    return lastRoomTypeFilter;
  }

  public void setLastPaymentFilter(String v) {
    lastPaymentFilter = v;
  }

  public void setLastRoomTypeFilter(String v) {
    lastRoomTypeFilter = v;
  }

  // =========================================================================
  // DTOs
  // =========================================================================

  public static class CheckoutRowDTO {
    public final String billingId, guestId, guestName, roomNumber, roomType;
    public final String checkInDate, checkOutDate, paymentStatus;
    public final LocalDate checkInDateRaw, checkOutDateRaw;
    public final long nights;
    public final double totalAmount;

    public CheckoutRowDTO(
        String billingId,
        String guestId,
        String guestName,
        String roomNumber,
        String roomType,
        String checkInDate,
        String checkOutDate,
        LocalDate checkInDateRaw,
        LocalDate checkOutDateRaw,
        long nights,
        double totalAmount,
        String paymentStatus) {
      this.billingId = billingId;
      this.guestId = orNA(guestId);
      this.guestName = orNA(guestName);
      this.roomNumber = orNA(roomNumber);
      this.roomType = orNA(roomType);
      this.checkInDate = orNA(checkInDate);
      this.checkOutDate = orNA(checkOutDate);
      this.checkInDateRaw = checkInDateRaw;
      this.checkOutDateRaw = checkOutDateRaw;
      this.nights = nights;
      this.totalAmount = totalAmount;
      this.paymentStatus = orNA(paymentStatus);
    }
  }

  public static class CheckoutSummaryDTO {
    public final int totalCheckouts, paidCount, unpaidCount;
    public final double totalRevenue, luxuryRevenue, suiteRevenue, standardRevenue;
    public final String generatedAt, period;

    public CheckoutSummaryDTO(
        int totalCheckouts,
        int paidCount,
        int unpaidCount,
        double totalRevenue,
        double luxuryRevenue,
        double suiteRevenue,
        double standardRevenue,
        String generatedAt,
        String period) {
      this.totalCheckouts = totalCheckouts;
      this.paidCount = paidCount;
      this.unpaidCount = unpaidCount;
      this.totalRevenue = totalRevenue;
      this.luxuryRevenue = luxuryRevenue;
      this.suiteRevenue = suiteRevenue;
      this.standardRevenue = standardRevenue;
      this.generatedAt = generatedAt;
      this.period = period;
    }
  }

  public static class OccupancyRowDTO {
    public final String roomType, occupancyRate;
    public final int dirty, cleaning, inspected, vacantClean, occupied, total;

    public OccupancyRowDTO(
        String roomType,
        int dirty,
        int cleaning,
        int inspected,
        int vacantClean,
        int occupied,
        int total,
        String occupancyRate) {
      this.roomType = roomType;
      this.dirty = dirty;
      this.cleaning = cleaning;
      this.inspected = inspected;
      this.vacantClean = vacantClean;
      this.occupied = occupied;
      this.total = total;
      this.occupancyRate = occupancyRate;
    }
  }

  public static class OccupancySummaryDTO {
    public final int totalRooms, totalOccupied, totalDirty, totalVacantClean;
    public final String overallOccupancyRate, generatedAt;

    public OccupancySummaryDTO(
        int totalRooms,
        int totalOccupied,
        int totalDirty,
        int totalVacantClean,
        String overallOccupancyRate,
        String generatedAt) {
      this.totalRooms = totalRooms;
      this.totalOccupied = totalOccupied;
      this.totalDirty = totalDirty;
      this.totalVacantClean = totalVacantClean;
      this.overallOccupancyRate = overallOccupancyRate;
      this.generatedAt = generatedAt;
    }
  }

  public static class StayRowDTO {
    public final String guestId, guestName, roomNumber, roomType;
    public final String checkInDate, checkOutDate, status;
    public final LocalDate checkInDateRaw, checkOutDateRaw;
    public final long nights;
    public final double totalAmount;

    public StayRowDTO(
        String guestId,
        String guestName,
        String roomNumber,
        String roomType,
        String checkInDate,
        String checkOutDate,
        LocalDate checkInDateRaw,
        LocalDate checkOutDateRaw,
        long nights,
        double totalAmount,
        String status) {
      this.guestId = orNA(guestId);
      this.guestName = orNA(guestName);
      this.roomNumber = orNA(roomNumber);
      this.roomType = orNA(roomType);
      this.checkInDate = orNA(checkInDate);
      this.checkOutDate = orNA(checkOutDate);
      this.checkInDateRaw = checkInDateRaw;
      this.checkOutDateRaw = checkOutDateRaw;
      this.nights = nights;
      this.totalAmount = totalAmount;
      this.status = orNA(status);
    }
  }

  public static class StaySummaryDTO {
    public final int totalStays, oneNight, twoThreeNights, fourSevenNights, eightPlusNights;
    public final double avgNights;
    public final long shortestNights, longestNights;
    public final String generatedAt;

    public StaySummaryDTO(
        int totalStays,
        double avgNights,
        long shortestNights,
        long longestNights,
        int oneNight,
        int twoThreeNights,
        int fourSevenNights,
        int eightPlusNights,
        String generatedAt) {
      this.totalStays = totalStays;
      this.avgNights = avgNights;
      this.shortestNights = shortestNights;
      this.longestNights = longestNights;
      this.oneNight = oneNight;
      this.twoThreeNights = twoThreeNights;
      this.fourSevenNights = fourSevenNights;
      this.eightPlusNights = eightPlusNights;
      this.generatedAt = generatedAt;
    }
  }

  public static class RevenueSummaryDTO {
    public final int totalStays;
    public final double totalRevenue, luxuryRevenue, suiteRevenue, standardRevenue;
    public final double avgRevenuePerStay, avgRevenuePerNight;
    public final String generatedAt, period;

    public RevenueSummaryDTO(
        int totalStays,
        double totalRevenue,
        double luxuryRevenue,
        double suiteRevenue,
        double standardRevenue,
        double avgRevenuePerStay,
        double avgRevenuePerNight,
        String generatedAt,
        String period) {
      this.totalStays = totalStays;
      this.totalRevenue = totalRevenue;
      this.luxuryRevenue = luxuryRevenue;
      this.suiteRevenue = suiteRevenue;
      this.standardRevenue = standardRevenue;
      this.avgRevenuePerStay = avgRevenuePerStay;
      this.avgRevenuePerNight = avgRevenuePerNight;
      this.generatedAt = generatedAt;
      this.period = period;
    }
  }

  // =========================================================================
  // REPORT HUB
  // =========================================================================

  public int displayReportHubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FRONT-DESK REPORT HUB", 60);
    System.out.println("1. Guest Check-Out Report");
    System.out.println("2. Room Performance & Revenue Report");
    System.out.println("3. Guest Stay Duration Analysis");
    System.out.println("4. Back to Front-Desk Menu\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  // =========================================================================
  // REPORT 1 — GUEST CHECK-OUT REPORT
  // =========================================================================

  public GetMenuInputResult renderCheckoutReport(
      ArrayList<CheckoutRowDTO> list,
      CheckoutSummaryDTO summary,
      LocalDate fromDate,
      LocalDate toDate,
      String paymentFilter,
      String roomTypeFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST CHECK-OUT REPORT", 104);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println();
    System.out.println("DATE RANGE      : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "PAYMENT STATUS  : [ " + (paymentFilter == null ? "ALL" : paymentFilter) + " ]");
    System.out.println(
        "ROOM TYPE       : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]");
    System.out.println();

    int total = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 22, 9, 10, 12, 12, 7, 12, 9};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.RIGHT)
            .setHAlign(9, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "GUEST ID", "GUEST NAME", "ROOM NO.", "ROOM TYPE",
          "CHECK-IN", "CHECK-OUT", "NIGHTS", "TOTAL (RM)", "PAYMENT"
        },
        settings);

    if (total == 0 || list == null) {
      printEmptyTable(
          settings,
          134,
          (fromDate != null || paymentFilter != null || roomTypeFilter != null)
              ? "*** NO CHECK-OUTS MATCH ACTIVE FILTERS ***"
              : "*** NO CHECK-OUT RECORDS FOUND ***");
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println(
          "Total Check-outs : "
              + summary.totalCheckouts
              + "   |   Paid : "
              + summary.paidCount
              + "   |   Unpaid : "
              + summary.unpaidCount);
      System.out.println(
          "Total Revenue    : RM "
              + String.format("%.2f", summary.totalRevenue)
              + "   (Luxury: RM "
              + String.format("%.2f", summary.luxuryRevenue)
              + "  |  Suite: RM "
              + String.format("%.2f", summary.suiteRevenue)
              + "  |  Standard: RM "
              + String.format("%.2f", summary.standardRevenue)
              + ")");
      System.out.println();
      ConsoleUtil.stopRecording();
      System.out.println("[S] Filter & Sort     [R] Refresh     [X] Export     [E] Back\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    for (int i = startIndex; i <= endIndex; i++) {
      CheckoutRowDTO dto = list.getEntry(i);
      if (dto == null) continue;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            dto.guestId,
            dto.guestName,
            dto.roomNumber,
            dto.roomType,
            dto.checkInDate,
            dto.checkOutDate,
            String.valueOf(dto.nights),
            String.format("%.2f", dto.totalAmount),
            dto.paymentStatus
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println(
        "Total Check-outs : "
            + summary.totalCheckouts
            + "   |   Paid : "
            + summary.paidCount
            + "   |   Unpaid : "
            + summary.unpaidCount);
    System.out.println(
        "Total Revenue    : RM "
            + String.format("%.2f", summary.totalRevenue)
            + "   (Luxury: RM "
            + String.format("%.2f", summary.luxuryRevenue)
            + "  |  Suite: RM "
            + String.format("%.2f", summary.suiteRevenue)
            + "  |  Standard: RM "
            + String.format("%.2f", summary.standardRevenue)
            + ")");
    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);

    ConsoleUtil.stopRecording();

    System.out.println("[S] Filter & Sort     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page         [N] Next Page       [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'S', 'O', 'R', 'N', 'P', 'X', 'E'});
  }

  // =========================================================================
  // REPORT 2 — ROOM PERFORMANCE & REVENUE REPORT
  // =========================================================================

  public GetMenuInputResult renderRoomPerformanceReport(
      ArrayList<OccupancyRowDTO> occupancyRows,
      OccupancySummaryDTO occupancySummary,
      ArrayList<CheckoutRowDTO> revenueRows,
      RevenueSummaryDTO revenueSummary,
      LocalDate fromDate,
      LocalDate toDate,
      String roomTypeFilter,
      String sortCriteria) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM PERFORMANCE & REVENUE REPORT", 104);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println();
    System.out.println("DATE RANGE    : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "ROOM TYPE     : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA : [ " + sortCriteria + " ]");
    System.out.println();

    // ── SECTION 1: OCCUPANCY ─────────────────────────────────────────────
    System.out.println(
        "──────────────────── SECTION 1 : CURRENT OCCUPANCY SNAPSHOT ────────────────────");
    System.out.println();

    int occTotal = (occupancyRows == null) ? 0 : occupancyRows.getNumberOfEntries();
    int[] occWidths = {12, 10, 14, 7, 7, 7};
    TableUtil.TableSettings occSettings =
        new TableUtil.TableSettings(occWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(occSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"ROOM TYPE", "OCCUPIED", "VACANT_CLEAN", "DIRTY", "TOTAL", "OCC %"},
        occSettings);
    TableUtil.printTableBorder(occSettings, TableUtil.BorderPosition.HEADER_CLOSE);

    if (occTotal == 0 || occupancyRows == null) {
      TableUtil.TableSettings es =
          new TableUtil.TableSettings(new int[] {72}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO ROOM DATA ***"}, es);
      TableUtil.printTableBorder(es, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      for (int i = 1; i <= occTotal; i++) {
        OccupancyRowDTO r = occupancyRows.getEntry(i);
        if (r == null) continue;
        TableUtil.printTableRow(
            new String[] {
              r.roomType,
              String.valueOf(r.occupied),
              String.valueOf(r.vacantClean),
              String.valueOf(r.dirty),
              String.valueOf(r.total),
              r.occupancyRate
            },
            occSettings);
        if (i < occTotal) TableUtil.printTableBorder(occSettings, TableUtil.BorderPosition.MIDDLE);
      }
      TableUtil.printTableBorder(occSettings, TableUtil.BorderPosition.MIDDLE);
      TableUtil.printTableRow(
          new String[] {
            "TOTAL",
            String.valueOf(occupancySummary.totalOccupied),
            String.valueOf(occupancySummary.totalVacantClean),
            String.valueOf(occupancySummary.totalDirty),
            String.valueOf(occupancySummary.totalRooms),
            occupancySummary.overallOccupancyRate
          },
          occSettings);
      TableUtil.printTableBorder(occSettings, TableUtil.BorderPosition.BOTTOM);
    }
    System.out.println();

    // ── SECTION 2: REVENUE ───────────────────────────────────────────────
    System.out.println(
        "─────────────── SECTION 2 : REVENUE BREAKDOWN  (PAID STAYS IN PERIOD) ───────────────");
    System.out.println();

    int revTotal = (revenueRows == null) ? 0 : revenueRows.getNumberOfEntries();
    int[] revWidths = {4, 10, 22, 9, 10, 12, 12, 7, 12};
    TableUtil.TableSettings revSettings =
        new TableUtil.TableSettings(revWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.RIGHT)
            .setTruncate(2);

    TableUtil.printTableBorder(revSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.",
          "GUEST ID",
          "GUEST NAME",
          "ROOM NO.",
          "ROOM TYPE",
          "CHECK-IN",
          "CHECK-OUT",
          "NIGHTS",
          "TOTAL (RM)"
        },
        revSettings);

    if (revTotal == 0 || revenueRows == null) {
      printEmptyTable(revSettings, 122, "*** NO PAID STAYS IN SELECTED PERIOD ***");
    } else {
      TableUtil.printTableBorder(revSettings, TableUtil.BorderPosition.MIDDLE);
      for (int i = 1; i <= revTotal; i++) {
        CheckoutRowDTO dto = revenueRows.getEntry(i);
        if (dto == null) continue;
        TableUtil.printTableRow(
            new String[] {
              String.valueOf(i),
              dto.guestId,
              dto.guestName,
              dto.roomNumber,
              dto.roomType,
              dto.checkInDate,
              dto.checkOutDate,
              String.valueOf(dto.nights),
              String.format("%.2f", dto.totalAmount)
            },
            revSettings);
      }
      TableUtil.printTableBorder(revSettings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.println(
        "\nTotal Stays   : "
            + revenueSummary.totalStays
            + "   |   Total Revenue : RM "
            + String.format("%.2f", revenueSummary.totalRevenue));
    System.out.println(
        "Avg / Stay    : RM "
            + String.format("%.2f", revenueSummary.avgRevenuePerStay)
            + "   |   Avg / Night : RM "
            + String.format("%.2f", revenueSummary.avgRevenuePerNight));
    System.out.println(
        "Luxury : RM "
            + String.format("%.2f", revenueSummary.luxuryRevenue)
            + "   |   Suite : RM "
            + String.format("%.2f", revenueSummary.suiteRevenue)
            + "   |   Standard : RM "
            + String.format("%.2f", revenueSummary.standardRevenue));
    System.out.printf("Total Paid Stays : %d%n%n", revTotal);

    ConsoleUtil.stopRecording();

    System.out.println("[S] Filter   [O] Change Sort   [X] Export   [R] Refresh   [E] Back\n");
    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'O', 'X', 'R', 'E'});
  }

  // =========================================================================
  // REPORT 3 — STAY DURATION ANALYSIS
  // =========================================================================

  public GetMenuInputResult renderStayReport(
      ArrayList<StayRowDTO> list,
      StaySummaryDTO summary,
      LocalDate fromDate,
      LocalDate toDate,
      String roomTypeFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST STAY DURATION ANALYSIS", 104);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println();
    System.out.println("DATE RANGE    : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "ROOM TYPE     : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA : [ " + sortCriteria + " ]");
    System.out.println();

    int total = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 22, 9, 10, 12, 12, 7, 12, 9};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.RIGHT)
            .setHAlign(9, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "GUEST ID", "GUEST NAME", "ROOM NO.", "ROOM TYPE",
          "CHECK-IN", "CHECK-OUT", "NIGHTS", "TOTAL (RM)", "STATUS"
        },
        settings);

    if (total == 0 || list == null) {
      printEmptyTable(
          settings,
          134,
          (fromDate != null || toDate != null || roomTypeFilter != null)
              ? "*** NO STAYS MATCH ACTIVE FILTERS ***"
              : "*** NO STAY RECORDS FOUND ***");
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      printStaySummaryLine(summary);
      ConsoleUtil.stopRecording();
      System.out.println("[S] Filter & Sort     [R] Refresh     [X] Export     [E] Back\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    for (int i = startIndex; i <= endIndex; i++) {
      StayRowDTO dto = list.getEntry(i);
      if (dto == null) continue;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            dto.guestId,
            dto.guestName,
            dto.roomNumber,
            dto.roomType,
            dto.checkInDate,
            dto.checkOutDate,
            String.valueOf(dto.nights),
            String.format("%.2f", dto.totalAmount),
            dto.status
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    printStaySummaryLine(summary);
    System.out.printf("Page %d / %d (Total: %d)%n%n", currentPage, totalPages, total);

    ConsoleUtil.stopRecording();

    System.out.println("[S] Filter & Sort     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page         [N] Next Page       [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'S', 'O', 'R', 'N', 'P', 'X', 'E'});
  }

  // =========================================================================
  // FILTER MENUS
  // =========================================================================

  public int displayFilterMenu(
      LocalDate fromDate, LocalDate toDate, String paymentFilter, String roomTypeFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SORT OPTIONS", 68);
    System.out.println("Date Range     : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "Payment Status : [ " + (paymentFilter == null ? "ALL" : paymentFilter) + " ]");
    System.out.println(
        "Room Type      : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]\n");
    System.out.println("1. Set Date Range");
    System.out.println("2. Filter by Payment Status");
    System.out.println("3. Filter by Room Type");
    System.out.println("4. Clear All Filters");
    System.out.println("5. Apply & Back\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 5).getAsInt();
  }

  /** Returns the raw typed strings for [from, to]; parsing/validation is done by the controller. */
  public String[] promptDateRangeRaw(LocalDate currentFrom, LocalDate currentTo) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET DATE RANGE", 68);
    System.out.println(
        "Current : FROM [ " + fmt(currentFrom) + " ]  TO [ " + fmt(currentTo) + " ]");
    System.out.println();
    System.out.println(
        "Format: YYYY-MM-DD   |   blank = keep current   |   '-' = clear (All Dates)\n");
    String from = ConsoleUtil.getStringInput("From date: ");
    String to = ConsoleUtil.getStringInput("To date  : ");
    return new String[] {from, to};
  }

  public int displayPaymentSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY PAYMENT STATUS", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. PAID Only");
    System.out.println("2. UNPAID Only");
    System.out.println("3. Show All");
    System.out.println("4. Cancel\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  public int displayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM TYPE", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  // =========================================================================
  // SORT MENUS
  // =========================================================================

  public String displayCheckoutSortMenu(String currentSort) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT ORDER", 60);
    System.out.println("Current : [ " + currentSort + " ]\n");
    System.out.println("1. Check-out Date (Latest First)");
    System.out.println("2. Check-out Date (Earliest First)");
    System.out.println("3. Guest Name (A -> Z)");
    System.out.println("4. Guest Name (Z -> A)");
    System.out.println("5. Room Number (Low -> High)");
    System.out.println("6. Total Amount (High -> Low)");
    System.out.println("7. Total Amount (Low -> High)");
    System.out.println("8. Nights (High -> Low)");
    System.out.println("9. Cancel\n");
    int choice = ConsoleUtil.getMenuInput("Choose option: ", 1, 9).getAsInt();
    switch (choice) {
      case 1:
        return "CHECK-OUT DATE (LATEST FIRST)";
      case 2:
        return "CHECK-OUT DATE (EARLIEST FIRST)";
      case 3:
        return "GUEST NAME (A -> Z)";
      case 4:
        return "GUEST NAME (Z -> A)";
      case 5:
        return "ROOM NO. (LOW -> HIGH)";
      case 6:
        return "TOTAL AMOUNT (HIGH -> LOW)";
      case 7:
        return "TOTAL AMOUNT (LOW -> HIGH)";
      case 8:
        return "NIGHTS (HIGH -> LOW)";
      default:
        return null;
    }
  }

  public String displayStaySortMenu(String currentSort) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT ORDER", 60);
    System.out.println("Current : [ " + currentSort + " ]\n");
    System.out.println("1. Nights (High -> Low)");
    System.out.println("2. Nights (Low -> High)");
    System.out.println("3. Guest Name (A -> Z)");
    System.out.println("4. Check-in (Latest First)");
    System.out.println("5. Check-in (Earliest First)");
    System.out.println("6. Total Amount (High -> Low)");
    System.out.println("7. Cancel\n");
    int choice = ConsoleUtil.getMenuInput("Choose option: ", 1, 7).getAsInt();
    switch (choice) {
      case 1:
        return "NIGHTS (HIGH -> LOW)";
      case 2:
        return "NIGHTS (LOW -> HIGH)";
      case 3:
        return "GUEST NAME (A -> Z)";
      case 4:
        return "CHECK-IN (LATEST FIRST)";
      case 5:
        return "CHECK-IN (EARLIEST FIRST)";
      case 6:
        return "TOTAL AMOUNT (HIGH -> LOW)";
      default:
        return null;
    }
  }

  // =========================================================================
  // EXPORT SUCCESS
  // =========================================================================

  public void showExportSuccess(String filePath, int rowCount) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EXPORT SUCCESSFUL", 60);
    System.out.println("Rows exported : " + rowCount);
    System.out.println("Saved to      : " + filePath + "\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // SHARED HELPERS
  // =========================================================================

  private void printStaySummaryLine(StaySummaryDTO summary) {
    System.out.println(
        "Total Stays : "
            + summary.totalStays
            + "   |   Avg : "
            + String.format("%.1f", summary.avgNights)
            + " nights"
            + "   |   Shortest : "
            + summary.shortestNights
            + " night(s)"
            + "   |   Longest : "
            + summary.longestNights
            + " night(s)");
    System.out.println(
        "Distribution — 1 Night : "
            + summary.oneNight
            + "   |   2-3 Nights : "
            + summary.twoThreeNights
            + "   |   4-7 Nights : "
            + summary.fourSevenNights
            + "   |   8+ Nights : "
            + summary.eightPlusNights);
    System.out.println();
  }

  private void printEmptyTable(TableUtil.TableSettings settings, int emptyWidth, String msg) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
    TableUtil.TableSettings es =
        new TableUtil.TableSettings(new int[] {emptyWidth}).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.printTableRow(new String[] {msg}, es);
    TableUtil.printTableBorder(es, TableUtil.BorderPosition.PLAIN_BOTTOM);
  }

  private String fmt(LocalDate date) {
    return (date == null) ? "All Dates" : date.format(DATE_FMT);
  }

  private static String orNA(String s) {
    return (s != null && !s.isEmpty()) ? s : "N/A";
  }
}
