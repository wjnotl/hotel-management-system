package view.frontdesk;

import adt.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ReportsView {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // Retained between filter menu calls so the controller can read them back
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
  // DTOs — all data pre-computed in controller; view just formats strings
  // =========================================================================

  public static class CheckoutRowDTO {
    public final String billingId;
    public final String guestId;
    public final String guestName;
    public final String roomNumber;
    public final String roomType;
    public final String checkInDate;
    public final String checkOutDate;
    public final LocalDate checkInDateRaw;
    public final LocalDate checkOutDateRaw;
    public final long nights;
    public final double totalAmount;
    public final String paymentStatus;

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
    public final int totalCheckouts;
    public final int paidCount;
    public final int unpaidCount;
    public final double totalRevenue;
    public final double luxuryRevenue;
    public final double suiteRevenue;
    public final double standardRevenue;
    public final String generatedAt;
    public final String period;

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
    public final String roomType;
    public final int dirty, cleaning, inspected, vacantClean, occupied, total;
    public final String occupancyRate;

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
    public final String overallOccupancyRate;
    public final String generatedAt;

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
    public final int totalStays;
    public final double avgNights;
    public final long shortestNights, longestNights;
    public final int oneNight, twoThreeNights, fourSevenNights, eightPlusNights;
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
    System.out.println(
        "  Generated : "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"))
            + "\n");
    System.out.println("1. Guest Check-Out Report");
    System.out.println("2. Room Occupancy & Status Report");
    System.out.println("3. Guest Stay Duration Analysis");
    System.out.println("4. Revenue Summary Report");
    System.out.println("5. Back to Front-Desk Menu\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
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

    System.out.println("  Generated : " + summary.generatedAt);
    System.out.println("  Period    : " + summary.period);
    System.out.println();
    System.out.println("DATE RANGE      : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "PAYMENT STATUS  : [ " + (paymentFilter == null ? "ALL" : paymentFilter) + " ]");
    System.out.println(
        "ROOM TYPE       : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]");
    System.out.println();

    // Summary panel
    printSummaryPanel(
        new String[][] {
          {"Total Check-outs", String.valueOf(summary.totalCheckouts)},
          {"Paid", String.valueOf(summary.paidCount)},
          {"Unpaid", String.valueOf(summary.unpaidCount)},
          {"Total Revenue", "RM " + String.format("%.2f", summary.totalRevenue)},
          {"Luxury Rev.", "RM " + String.format("%.2f", summary.luxuryRevenue)},
          {"Suite Rev.", "RM " + String.format("%.2f", summary.suiteRevenue)},
          {"Standard Rev.", "RM " + String.format("%.2f", summary.standardRevenue)},
        });
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
          107,
          (fromDate != null || paymentFilter != null || roomTypeFilter != null)
              ? "*** NO CHECK-OUTS MATCH ACTIVE FILTERS ***"
              : "*** NO CHECK-OUT RECORDS FOUND ***");
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[S] Filter & Sort     [R] Refresh     [X] Export     [E] Back\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      CheckoutRowDTO dto = list.getEntry(i);
      if (dto == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
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

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Filter & Sort     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page         [N] Next Page       [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'S', 'O', 'R', 'N', 'P', 'X', 'E'});
  }

  // =========================================================================
  // REPORT 2 — ROOM OCCUPANCY & STATUS REPORT
  // =========================================================================

  public GetMenuInputResult renderOccupancyReport(
      ArrayList<OccupancyRowDTO> rows, OccupancySummaryDTO summary, String roomTypeFilter) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM OCCUPANCY & STATUS REPORT", 84);

    System.out.println("  Generated : " + summary.generatedAt);
    System.out.println(
        "  ROOM TYPE : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println();

    int total = (rows == null) ? 0 : rows.getNumberOfEntries();

    // Main breakdown table
    int[] colWidths = {12, 7, 10, 11, 14, 10, 7, 7};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "ROOM TYPE",
          "DIRTY",
          "CLEANING",
          "INSPECTED",
          "VACANT_CLEAN",
          "OCCUPIED",
          "TOTAL",
          "OCC %"
        },
        settings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    if (total == 0 || rows == null) {
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {81}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO ROOM DATA FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      for (int i = 1; i <= total; i++) {
        OccupancyRowDTO r = rows.getEntry(i);
        if (r == null) continue;
        TableUtil.printTableRow(
            new String[] {
              r.roomType,
              String.valueOf(r.dirty),
              String.valueOf(r.cleaning),
              String.valueOf(r.inspected),
              String.valueOf(r.vacantClean),
              String.valueOf(r.occupied),
              String.valueOf(r.total),
              r.occupancyRate
            },
            settings);
        if (i < total) TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
      }
      // Grand total row
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
      TableUtil.printTableRow(
          new String[] {
            "TOTAL",
            String.valueOf(sumDirty(rows)),
            String.valueOf(sumCleaning(rows)),
            String.valueOf(sumInspected(rows)),
            String.valueOf(sumVacant(rows)),
            String.valueOf(summary.totalOccupied),
            String.valueOf(summary.totalRooms),
            summary.overallOccupancyRate
          },
          settings);
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.println();

    // Summary panel
    printSummaryPanel(
        new String[][] {
          {"Total Rooms", String.valueOf(summary.totalRooms)},
          {"Occupied", String.valueOf(summary.totalOccupied)},
          {"Dirty / Cleaning", summary.totalDirty + " / " + sumCleaning(rows)},
          {"Available", String.valueOf(summary.totalVacantClean)},
          {"Occupancy Rate", summary.overallOccupancyRate},
        });

    System.out.println();
    System.out.println("[T] Filter Room Type     [R] Refresh     [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'T', 'R', 'X', 'E'});
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

    System.out.println("  Generated : " + summary.generatedAt);
    System.out.println();
    System.out.println("DATE RANGE    : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "ROOM TYPE     : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA : [ " + sortCriteria + " ]");
    System.out.println();

    // Summary panel
    printSummaryPanel(
        new String[][] {
          {"Total Stays", String.valueOf(summary.totalStays)},
          {"Avg Nights", String.format("%.1f", summary.avgNights)},
          {"Shortest", summary.shortestNights + " night(s)"},
          {"Longest", summary.longestNights + " night(s)"},
          {"1 Night", String.valueOf(summary.oneNight)},
          {"2-3 Nights", String.valueOf(summary.twoThreeNights)},
          {"4-7 Nights", String.valueOf(summary.fourSevenNights)},
          {"8+ Nights", String.valueOf(summary.eightPlusNights)},
        });
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
          107,
          (fromDate != null || toDate != null || roomTypeFilter != null)
              ? "*** NO STAYS MATCH ACTIVE FILTERS ***"
              : "*** NO STAY RECORDS FOUND ***");
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[S] Filter & Sort     [R] Refresh     [X] Export     [E] Back\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      StayRowDTO dto = list.getEntry(i);
      if (dto == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
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

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Filter & Sort     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page         [N] Next Page       [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'S', 'O', 'R', 'N', 'P', 'X', 'E'});
  }

  // =========================================================================
  // REPORT 4 — REVENUE SUMMARY REPORT
  // =========================================================================

  public GetMenuInputResult renderRevenueReport(
      ArrayList<CheckoutRowDTO> list,
      RevenueSummaryDTO summary,
      LocalDate fromDate,
      LocalDate toDate,
      String roomTypeFilter) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REVENUE SUMMARY REPORT", 104);

    System.out.println("  Generated : " + summary.generatedAt);
    System.out.println("  Period    : " + summary.period);
    System.out.println();
    System.out.println("DATE RANGE  : [ FROM " + fmt(fromDate) + "  TO  " + fmt(toDate) + " ]");
    System.out.println(
        "ROOM TYPE   : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("(Only PAID stays are included in revenue figures)");
    System.out.println();

    // Summary panel
    printSummaryPanel(
        new String[][] {
          {"Total Stays", String.valueOf(summary.totalStays)},
          {"Total Revenue", "RM " + String.format("%.2f", summary.totalRevenue)},
          {"Avg / Stay", "RM " + String.format("%.2f", summary.avgRevenuePerStay)},
          {"Avg / Night", "RM " + String.format("%.2f", summary.avgRevenuePerNight)},
          {"Luxury Revenue", "RM " + String.format("%.2f", summary.luxuryRevenue)},
          {"Suite Revenue", "RM " + String.format("%.2f", summary.suiteRevenue)},
          {"Standard Revenue", "RM " + String.format("%.2f", summary.standardRevenue)},
        });
    System.out.println();

    int total = (list == null) ? 0 : list.getNumberOfEntries();

    int[] colWidths = {4, 10, 22, 9, 10, 12, 12, 7, 12};
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
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
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
        settings);

    if (total == 0 || list == null) {
      printEmptyTable(settings, 95, "*** NO PAID STAYS IN SELECTED PERIOD ***");
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[S] Change Filters     [R] Refresh     [X] Export     [E] Back\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    for (int i = 1; i <= total; i++) {
      CheckoutRowDTO dto = list.getEntry(i);
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
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nTotal Paid Stays: %d\n\n", total);
    System.out.println("[S] Change Filters     [R] Refresh     [X] Export     [E] Back\n");
    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'X', 'E'});
  }

  // =========================================================================
  // FILTER MENU — shared across all reports
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

  public LocalDate[] promptDateRange(LocalDate currentFrom, LocalDate currentTo) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET DATE RANGE", 68);
    System.out.println(
        "Current : FROM [ " + fmt(currentFrom) + " ]  TO [ " + fmt(currentTo) + " ]");
    System.out.println();
    System.out.println(
        "Format: YYYY-MM-DD   |   blank = keep current   |   '-' = clear (All Dates)\n");
    LocalDate from = promptSingleDate("From date: ", currentFrom);
    LocalDate to = promptSingleDate("To date  : ", currentTo);
    return new LocalDate[] {from, to};
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

  /** Returns selected sort string, or null if cancelled. */
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

  /** Returns selected sort string, or null if cancelled. */
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

  /** Prints a 2-column label|value summary panel. */
  private void printSummaryPanel(String[][] pairs) {
    int[] kvWidths = {22, 36};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    for (int i = 0; i < pairs.length; i++) {
      TableUtil.printTableRow(new String[] {pairs[i][0], pairs[i][1]}, kvSettings);
      if (i < pairs.length - 1)
        TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    }
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);
  }

  private void printEmptyTable(TableUtil.TableSettings settings, int emptyWidth, String msg) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
    TableUtil.TableSettings emptySettings =
        new TableUtil.TableSettings(new int[] {emptyWidth}).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.printTableRow(new String[] {msg}, emptySettings);
    TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
  }

  private LocalDate promptSingleDate(String prompt, LocalDate current) {
    String raw = ConsoleUtil.getStringInput(prompt);
    if (raw == null || raw.trim().isEmpty()) return current;
    if ("-".equals(raw.trim())) return null;
    try {
      return LocalDate.parse(raw.trim(), INPUT_FMT);
    } catch (DateTimeParseException e) {
      ConsoleUtil.printError("Invalid date. Keeping: " + fmt(current));
      return current;
    }
  }

  private String fmt(LocalDate date) {
    return (date == null) ? "All Dates" : date.format(DATE_FMT);
  }

  // Occupancy table column sum helpers
  private int sumDirty(ArrayList<OccupancyRowDTO> rows) {
    int s = 0;
    if (rows != null)
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        OccupancyRowDTO r = rows.getEntry(i);
        if (r != null) s += r.dirty;
      }
    return s;
  }

  private int sumCleaning(ArrayList<OccupancyRowDTO> rows) {
    int s = 0;
    if (rows != null)
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        OccupancyRowDTO r = rows.getEntry(i);
        if (r != null) s += r.cleaning;
      }
    return s;
  }

  private int sumInspected(ArrayList<OccupancyRowDTO> rows) {
    int s = 0;
    if (rows != null)
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        OccupancyRowDTO r = rows.getEntry(i);
        if (r != null) s += r.inspected;
      }
    return s;
  }

  private int sumVacant(ArrayList<OccupancyRowDTO> rows) {
    int s = 0;
    if (rows != null)
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        OccupancyRowDTO r = rows.getEntry(i);
        if (r != null) s += r.vacantClean;
      }
    return s;
  }

  private static String orNA(String s) {
    return (s != null && !s.isEmpty()) ? s : "N/A";
  }
}
