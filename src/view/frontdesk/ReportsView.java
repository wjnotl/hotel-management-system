package view.frontdesk;

import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ReportsView {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  // --- REPORT HUB ---

  public GetMenuInputResult displayReportHubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FRONT-DESK REPORT HUB", 60);

    System.out.println("Select an operational performance report to generate:\n");

    System.out.println("1. Guest Check-Out Performance Report");
    System.out.println("2. Room Occupancy & Status Report");
    System.out.println("3. Guest Stay Duration Analysis Report");
    System.out.println("4. Back to Front-Desk Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4);
  }

  // --- GUEST CHECK-OUT PERFORMANCE REPORT ---

  public GetMenuInputResult renderDailyCheckoutReport(
      ListInterface<Billing> checkouts,
      ListInterface<Guest> guestList,
      String dateFilter,
      String paymentStatusFilter,
      String sortCriteria,
      double totalRevenue,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST CHECK-OUT PERFORMANCE REPORT", 96);

    System.out.println(
        "CHECK-OUT DATE  : [ " + (dateFilter == null ? "ALL DATES" : dateFilter) + " ]");
    System.out.println(
        "PAYMENT STATUS  : [ "
            + (paymentStatusFilter == null ? "ALL" : paymentStatusFilter)
            + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]\n");

    int total = (checkouts == null) ? 0 : checkouts.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    int[] colWidths = {4, 10, 20, 10, 12, 8, 12, 10};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.RIGHT)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.",
          "GUEST ID",
          "GUEST NAME",
          "ROOM NO.",
          "ROOM TYPE",
          "NIGHTS",
          "TOTAL (RM)",
          "PAYMENT"
        },
        settings);

    if (total == 0 || checkouts == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {86}).setHAlign(0, TableUtil.Align.CENTER);

      boolean hasFilters = dateFilter != null || paymentStatusFilter != null;
      String emptyMsg =
          hasFilters
              ? "*** NO CHECK-OUTS FOUND FOR ACTIVE FILTERS ***"
              : "*** NO CHECK-OUTS RECORDED ***";

      TableUtil.printTableRow(new String[] {emptyMsg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      System.out.println("[D] Change Date         [F] Filter Payment Status");
      System.out.println("[O] Change Sort Order   [C] Export to TXT       [R] Refresh Table");
      System.out.println("[E] Exit to Reports Menu\n");

      return ConsoleUtil.getMenuInput(
          "Enter a command: ", new char[] {'D', 'F', 'O', 'C', 'R', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = checkouts.getEntry(i);
      if (b == null) continue;
      Guest g = findGuest(guestList, b.getGuestId());

      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            b.getGuestId(),
            (g != null ? g.getName() : "N/A"),
            b.getRoomNumber(),
            b.getRoomType().name(),
            String.valueOf(b.getNumberOfNights()),
            String.format("%.2f", b.getTotalAmount()),
            b.getStatus().name()
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, total);
    System.out.printf(
        "Guests Checked Out: %d   |   Total Revenue Collected (RM): %.2f\n\n", total, totalRevenue);

    System.out.println("[D] Change Date         [F] Filter Payment Status");
    System.out.println("[O] Change Sort Order   [P] Prev Page            [N] Next Page");
    System.out.println(
        "[C] Export to TXT       [R] Refresh Table        [E] Exit to Reports Menu\n");

    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'D', 'F', 'O', 'P', 'N', 'C', 'R', 'E'});
  }

  public String promptDateFilterInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY CHECK-OUT DATE");
    System.out.println(
        "Format: yyyy-MM-dd (e.g. " + java.time.LocalDate.now().format(DATE_FMT) + ")\n");
    return ConsoleUtil.getStringInput(
        "Enter Check-Out Date [T for Today / A for All Dates / Enter to keep current]: ");
  }

  public int displayPaymentStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY PAYMENT STATUS");
    System.out.println("Current Selected: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. PAID Only");
    System.out.println("2. UNPAID Only");
    System.out.println("3. Show All\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayCheckoutSortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Room Number (Low -> High)");
    System.out.println("2. Guest Name (A -> Z)");
    System.out.println("3. Total Amount (High -> Low)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  // --- ROOM OCCUPANCY & STATUS REPORT ---

  public GetMenuInputResult renderRoomOccupancyReport(
      int[][] statusCountsByType, // [roomTypeOrdinal][statusOrdinal]
      String[] roomTypeNames,
      String[] statusNames,
      int totalRooms,
      int totalOccupied) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM OCCUPANCY & STATUS REPORT", 96);

    int[] colWidths = {12, 8, 10, 11, 14, 10, 8};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "ROOM TYPE", "DIRTY", "CLEANING", "INSPECTED", "VACANT_CLEAN", "OCCUPIED", "TOTAL"
        },
        settings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int[] colTotals = new int[statusNames.length];

    for (int t = 0; t < roomTypeNames.length; t++) {
      int rowTotal = 0;
      String[] row = new String[statusNames.length + 2];
      row[0] = roomTypeNames[t];
      for (int s = 0; s < statusNames.length; s++) {
        int count = statusCountsByType[t][s];
        row[s + 1] = String.valueOf(count);
        rowTotal += count;
        colTotals[s] += count;
      }
      row[statusNames.length + 1] = String.valueOf(rowTotal);
      TableUtil.printTableRow(row, settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    String[] totalRow = new String[statusNames.length + 2];
    totalRow[0] = "TOTAL";
    for (int s = 0; s < statusNames.length; s++) {
      totalRow[s + 1] = String.valueOf(colTotals[s]);
    }
    totalRow[statusNames.length + 1] = String.valueOf(totalRooms);
    TableUtil.printTableRow(totalRow, settings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    double occupancyRate = (totalRooms == 0) ? 0.0 : (100.0 * totalOccupied / totalRooms);
    System.out.printf(
        "\nOverall Occupancy Rate: %d / %d rooms occupied (%.1f%%)\n\n",
        totalOccupied, totalRooms, occupancyRate);

    System.out.println(
        "[C] Export to TXT       [R] Refresh Table       [E] Exit to Reports Menu\n");

    return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'C', 'R', 'E'});
  }

  // --- GUEST STAY DURATION ANALYSIS REPORT ---

  public GetMenuInputResult renderStayDurationReport(
      ListInterface<Billing> stays,
      ListInterface<Guest> guestList,
      String roomTypeFilter,
      String sortCriteria,
      double averageNights,
      long shortestNights,
      long longestNights,
      int oneNightCount,
      int twoToThreeCount,
      int fourToSevenCount,
      int eightPlusCount,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST STAY DURATION ANALYSIS REPORT", 96);

    System.out.println(
        "ROOM TYPE FILTER : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println("SORT CRITERIA    : [ " + sortCriteria + " ]\n");

    int total = (stays == null) ? 0 : stays.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    int[] colWidths = {4, 10, 20, 10, 12, 12, 12, 8};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
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
          "NIGHTS"
        },
        settings);

    if (total == 0 || stays == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {88}).setHAlign(0, TableUtil.Align.CENTER);

      String emptyMsg =
          roomTypeFilter != null
              ? "*** NO STAYS FOUND FOR ACTIVE FILTERS ***"
              : "*** NO STAY RECORDS FOUND ***";

      TableUtil.printTableRow(new String[] {emptyMsg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      System.out.println("[T] Filter Room Type    [O] Change Sort Order   [C] Export to TXT");
      System.out.println("[R] Refresh Table       [E] Exit to Reports Menu\n");

      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'T', 'O', 'C', 'R', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = stays.getEntry(i);
      if (b == null) continue;
      Guest g = findGuest(guestList, b.getGuestId());

      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            b.getGuestId(),
            (g != null ? g.getName() : "N/A"),
            b.getRoomNumber(),
            b.getRoomType().name(),
            formatDate(b.getCheckInDate()),
            formatDate(b.getCheckOutDate()),
            String.valueOf(b.getNumberOfNights())
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);

    System.out.printf(
        "Average Stay: %.1f nights   |   Shortest: %d nights   |   Longest: %d nights\n",
        averageNights, shortestNights, longestNights);
    System.out.printf(
        "Distribution -> 1 Night: %d   |   2-3 Nights: %d   |   4-7 Nights: %d   |   8+ Nights:"
            + " %d\n\n",
        oneNightCount, twoToThreeCount, fourToSevenCount, eightPlusCount);

    System.out.println("[T] Filter Room Type    [O] Change Sort Order");
    System.out.println("[P] Prev Page           [N] Next Page           [C] Export to TXT");
    System.out.println("[R] Refresh Table       [E] Exit to Reports Menu\n");

    return ConsoleUtil.getMenuInput(
        "Enter a command: ", new char[] {'T', 'O', 'P', 'N', 'C', 'R', 'E'});
  }

  // --- TXT EXPORT ---

  public void showExportSuccess(String filePath, int rowCount) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EXPORT SUCCESSFUL");
    System.out.println("Rows exported : " + rowCount);
    System.out.println("Saved to      : " + filePath + "\n");
    ConsoleUtil.printContinueMessage();
  }

  public int displayStayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM TYPE");
    System.out.println("Current Selected: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayStaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Nights (High -> Low)");
    System.out.println("2. Nights (Low -> High)");
    System.out.println("3. Guest Name (A -> Z)");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  // --- HELPERS ---

  private String formatDate(java.time.LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DATE_FMT);
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
