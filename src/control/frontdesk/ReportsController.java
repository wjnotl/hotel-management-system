package control.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.RoomRepo;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TxtExportUtil;
import view.frontdesk.ReportsView;

public class ReportsController {
  private static final int PAGE_SIZE = 10;

  private final ReportsView reportsView = new ReportsView();
  private final BillingRepo billingRepo = new BillingRepo();
  private final GuestRepo guestRepo = new GuestRepo();
  private final RoomRepo roomRepo = new RoomRepo();

  public void start() {
    while (true) {
      try {
        GetMenuInputResult result = reportsView.displayReportHubMenu();
        int choice = result.getAsInt();

        if (choice == 1) {
          runDailyCheckoutReport();
        } else if (choice == 2) {
          runRoomOccupancyReport();
        } else if (choice == 3) {
          runStayDurationReport();
        } else if (choice == 4) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // ================= DAILY CHECKOUT REPORT =================

  private void runDailyCheckoutReport() {
    String dateFilter = LocalDate.now().toString(); // default: today
    String paymentStatusFilter = null; // null = ALL
    String sortCriteria = "ROOM NO. (LOW -> HIGH)";
    int currentPage = 1;

    while (true) {
      try {
        ListInterface<Billing> completedCheckouts = getCompletedCheckouts();
        ListInterface<Billing> filteredList =
            filterAndSortCheckouts(
                completedCheckouts, dateFilter, paymentStatusFilter, sortCriteria);

        int total = filteredList.getNumberOfEntries();
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (currentPage > totalPages) {
          currentPage = Math.max(1, totalPages);
        }

        double totalRevenue = 0.0;
        for (int i = 1; i <= total; i++) {
          Billing b = filteredList.getEntry(i);
          if (b != null && b.getStatus() == Billing.Status.PAID) {
            totalRevenue += b.getTotalAmount();
          }
        }

        GetMenuInputResult result =
            reportsView.renderDailyCheckoutReport(
                filteredList,
                guestRepo.getGuestList(),
                dateFilter,
                paymentStatusFilter,
                sortCriteria,
                totalRevenue,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("D".equalsIgnoreCase(result.input)) {
          dateFilter = handleDateSubmenu(dateFilter);
          currentPage = 1;
        } else if ("F".equalsIgnoreCase(result.input)) {
          paymentStatusFilter = handlePaymentStatusSubmenu(paymentStatusFilter);
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          sortCriteria = handleSortSubmenu(sortCriteria);
        } else if ("N".equalsIgnoreCase(result.input)) {
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if ("C".equalsIgnoreCase(result.input)) {
          exportDailyCheckoutReportToTxt(filteredList);
        }
        // "R" (Refresh) falls through and simply redraws with fresh repo data.
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Exports the FULL filtered result set (not just the current page) to TXT.
  private void exportDailyCheckoutReportToTxt(ListInterface<Billing> filteredList) {
    ListInterface<Billing> source = (filteredList == null) ? new ArrayList<>() : filteredList;
    int total = source.getNumberOfEntries();

    String[] headers = {
      "No.",
      "Guest ID",
      "Guest Name",
      "Room No.",
      "Room Type",
      "Check-In",
      "Check-Out",
      "Nights",
      "Total (RM)",
      "Payment Status"
    };
    String[][] rows = new String[total][];

    for (int i = 1; i <= total; i++) {
      Billing b = source.getEntry(i);
      Guest g = guestRepo.findById(b.getGuestId());
      rows[i - 1] =
          new String[] {
            String.valueOf(i),
            b.getGuestId(),
            (g != null ? g.getName() : "N/A"),
            b.getRoomNumber(),
            b.getRoomType().name(),
            String.valueOf(b.getCheckInDate()),
            String.valueOf(b.getCheckOutDate()),
            String.valueOf(b.getNumberOfNights()),
            String.format("%.2f", b.getTotalAmount()),
            b.getStatus().name()
          };
    }

    String content = buildTxtTable(headers, rows);
    String path = TxtExportUtil.export("frontdesk/checkout_report", content);
    reportsView.showExportSuccess(path, total);
  }

  private String handleDateSubmenu(String currentDate) {
    String input = reportsView.promptDateFilterInput();
    if (input == null || input.trim().isEmpty()) {
      return currentDate;
    }

    String trimmed = input.trim();
    if ("T".equalsIgnoreCase(trimmed)) {
      return LocalDate.now().toString();
    }
    if ("A".equalsIgnoreCase(trimmed)) {
      return null; // ALL DATES
    }

    try {
      return LocalDate.parse(trimmed).toString();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date! Please use the format yyyy-MM-dd.");
    }
  }

  private String handlePaymentStatusSubmenu(String currentStatus) {
    while (true) {
      int choice = reportsView.displayPaymentStatusSubmenu(currentStatus);
      if (choice == 1) return "PAID";
      if (choice == 2) return "UNPAID";
      if (choice == 3) return null;
    }
  }

  private String handleSortSubmenu(String currentSort) {
    int choice = reportsView.displayCheckoutSortMenu();
    if (choice == 1) return "ROOM NO. (LOW -> HIGH)";
    if (choice == 2) return "GUEST NAME (A -> Z)";
    if (choice == 3) return "TOTAL AMOUNT (HIGH -> LOW)";
    return currentSort;
  }

  // A "completed" checkout = a billing record with a check-out date that is no longer the
  // active/current stay for an OCCUPIED room (i.e. Complete Check-Out has actually been done).
  private ListInterface<Billing> getCompletedCheckouts() {
    ListInterface<Billing> all = billingRepo.getBillingList();
    ListInterface<Room> rooms = roomRepo.getRoomList();
    ListInterface<Billing> completed = new ArrayList<>();

    if (all == null || all.isEmpty()) return completed;

    ListInterface<String> activeBillingIds = new ArrayList<>();
    if (rooms != null) {
      for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
        Room room = rooms.getEntry(i);
        if (room == null || room.getStatus() != Room.Status.OCCUPIED) continue;

        Billing latest = findLatestBillingForRoom(all, room.getRoomNumber());
        if (latest != null) {
          activeBillingIds.add(latest.getBillingId());
        }
      }
    }

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b == null || b.getCheckOutDate() == null) continue;
      if (activeBillingIds.contains(b.getBillingId())) continue;
      completed.add(b);
    }

    return completed;
  }

  private Billing findLatestBillingForRoom(ListInterface<Billing> all, String roomNumber) {
    Billing latest = null;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b == null || !roomNumber.equalsIgnoreCase(b.getRoomNumber())) continue;
      if (latest == null || b.getCreatedAt().isAfter(latest.getCreatedAt())) {
        latest = b;
      }
    }
    return latest;
  }

  private ListInterface<Billing> filterAndSortCheckouts(
      ListInterface<Billing> source,
      String dateFilter,
      String paymentStatusFilter,
      String sortCriteria) {

    if (source == null || source.isEmpty()) return new ArrayList<>();

    ListInterface<Billing> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Billing b = source.getEntry(i);
      if (b == null) continue;

      boolean matchDate =
          dateFilter == null
              || (b.getCheckOutDate() != null && dateFilter.equals(b.getCheckOutDate().toString()));
      boolean matchPayment =
          paymentStatusFilter == null || paymentStatusFilter.equalsIgnoreCase(b.getStatus().name());

      if (matchDate && matchPayment) {
        filtered.add(b);
      }
    }

    if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sortCriteria)) {
      filtered.sort(
          (b1, b2) -> {
            Guest g1 = guestRepo.findById(b1.getGuestId());
            Guest g2 = guestRepo.findById(b2.getGuestId());
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return n1.compareToIgnoreCase(n2);
          });
    } else if ("TOTAL AMOUNT (HIGH -> LOW)".equalsIgnoreCase(sortCriteria)) {
      filtered.sort((b1, b2) -> Double.compare(b2.getTotalAmount(), b1.getTotalAmount()));
    } else {
      // Default: ROOM NO. (LOW -> HIGH)
      filtered.sort((b1, b2) -> b1.getRoomNumber().compareToIgnoreCase(b2.getRoomNumber()));
    }

    return filtered;
  }

  // ================= ROOM OCCUPANCY & STATUS REPORT =================

  private void runRoomOccupancyReport() {
    while (true) {
      try {
        ListInterface<Room> rooms = roomRepo.getRoomList();

        String[] roomTypeNames = {"LUXURY", "SUITE", "STANDARD"};
        String[] statusNames = {"DIRTY", "CLEANING", "INSPECTED", "VACANT_CLEAN", "OCCUPIED"};
        int[][] statusCountsByType = new int[roomTypeNames.length][statusNames.length];
        int totalRooms = 0;
        int totalOccupied = 0;

        if (rooms != null) {
          for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r == null || r.getRoomType() == null || r.getStatus() == null) continue;

            statusCountsByType[r.getRoomType().ordinal()][r.getStatus().ordinal()]++;
            totalRooms++;
            if (r.getStatus() == Room.Status.OCCUPIED) {
              totalOccupied++;
            }
          }
        }

        GetMenuInputResult result =
            reportsView.renderRoomOccupancyReport(
                statusCountsByType, roomTypeNames, statusNames, totalRooms, totalOccupied);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("C".equalsIgnoreCase(result.input)) {
          exportRoomOccupancyReportToTxt(
              statusCountsByType, roomTypeNames, statusNames, totalRooms);
        }
        // "R" (Refresh) falls through and simply redraws with fresh repo data.
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void exportRoomOccupancyReportToTxt(
      int[][] statusCountsByType, String[] roomTypeNames, String[] statusNames, int totalRooms) {

    String[] headers = new String[statusNames.length + 2];
    headers[0] = "Room Type";
    for (int s = 0; s < statusNames.length; s++) {
      headers[s + 1] = statusNames[s];
    }
    headers[statusNames.length + 1] = "Total";

    String[][] rows = new String[roomTypeNames.length + 1][];
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
      rows[t] = row;
    }

    String[] totalRow = new String[statusNames.length + 2];
    totalRow[0] = "TOTAL";
    for (int s = 0; s < statusNames.length; s++) {
      totalRow[s + 1] = String.valueOf(colTotals[s]);
    }
    totalRow[statusNames.length + 1] = String.valueOf(totalRooms);
    rows[roomTypeNames.length] = totalRow;

    String content = buildTxtTable(headers, rows);
    String path = TxtExportUtil.export("frontdesk/room_occupancy_report", content);
    reportsView.showExportSuccess(path, roomTypeNames.length + 1);
  }

  // ================= GUEST STAY DURATION ANALYSIS REPORT =================

  private void runStayDurationReport() {
    String roomTypeFilter = null; // null = ALL
    String sortCriteria = "NIGHTS (HIGH -> LOW)";
    int currentPage = 1;

    while (true) {
      try {
        ListInterface<Billing> allStays = billingRepo.getBillingList();
        ListInterface<Billing> filteredList =
            filterAndSortStays(allStays, roomTypeFilter, sortCriteria);

        int total = filteredList.getNumberOfEntries();
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (currentPage > totalPages) {
          currentPage = Math.max(1, totalPages);
        }

        long nightsSum = 0;
        long shortest = Long.MAX_VALUE;
        long longest = Long.MIN_VALUE;
        int oneNightCount = 0;
        int twoToThreeCount = 0;
        int fourToSevenCount = 0;
        int eightPlusCount = 0;

        for (int i = 1; i <= total; i++) {
          Billing b = filteredList.getEntry(i);
          if (b == null) continue;

          long nights = b.getNumberOfNights();
          nightsSum += nights;
          if (nights < shortest) shortest = nights;
          if (nights > longest) longest = nights;

          if (nights == 1) {
            oneNightCount++;
          } else if (nights >= 2 && nights <= 3) {
            twoToThreeCount++;
          } else if (nights >= 4 && nights <= 7) {
            fourToSevenCount++;
          } else if (nights >= 8) {
            eightPlusCount++;
          }
        }

        double averageNights = (total == 0) ? 0.0 : (double) nightsSum / total;
        if (total == 0) {
          shortest = 0;
          longest = 0;
        }

        GetMenuInputResult result =
            reportsView.renderStayDurationReport(
                filteredList,
                guestRepo.getGuestList(),
                roomTypeFilter,
                sortCriteria,
                averageNights,
                shortest,
                longest,
                oneNightCount,
                twoToThreeCount,
                fourToSevenCount,
                eightPlusCount,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("T".equalsIgnoreCase(result.input)) {
          roomTypeFilter = handleStayRoomTypeSubmenu(roomTypeFilter);
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          sortCriteria = handleStaySortSubmenu(sortCriteria);
        } else if ("N".equalsIgnoreCase(result.input)) {
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if ("C".equalsIgnoreCase(result.input)) {
          exportStayDurationReportToTxt(filteredList);
        }
        // "R" (Refresh) falls through and simply redraws with fresh repo data.
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Exports the FULL filtered result set (not just the current page) to TXT.
  private void exportStayDurationReportToTxt(ListInterface<Billing> filteredList) {
    ListInterface<Billing> source = (filteredList == null) ? new ArrayList<>() : filteredList;
    int total = source.getNumberOfEntries();

    String[] headers = {
      "No.", "Guest ID", "Guest Name", "Room No.", "Room Type", "Check-In", "Check-Out", "Nights"
    };
    String[][] rows = new String[total][];

    for (int i = 1; i <= total; i++) {
      Billing b = source.getEntry(i);
      Guest g = guestRepo.findById(b.getGuestId());
      rows[i - 1] =
          new String[] {
            String.valueOf(i),
            b.getGuestId(),
            (g != null ? g.getName() : "N/A"),
            b.getRoomNumber(),
            b.getRoomType().name(),
            String.valueOf(b.getCheckInDate()),
            String.valueOf(b.getCheckOutDate()),
            String.valueOf(b.getNumberOfNights())
          };
    }

    String content = buildTxtTable(headers, rows);
    String path = TxtExportUtil.export("frontdesk/stay_duration_report", content);
    reportsView.showExportSuccess(path, total);
  }

  private String handleStayRoomTypeSubmenu(String currentType) {
    int choice = reportsView.displayStayRoomTypeSubmenu(currentType);
    if (choice == 1) return "LUXURY";
    if (choice == 2) return "SUITE";
    if (choice == 3) return "STANDARD";
    return null;
  }

  private String handleStaySortSubmenu(String currentSort) {
    int choice = reportsView.displayStaySortMenu();
    if (choice == 1) return "NIGHTS (HIGH -> LOW)";
    if (choice == 2) return "NIGHTS (LOW -> HIGH)";
    if (choice == 3) return "GUEST NAME (A -> Z)";
    return currentSort;
  }

  private ListInterface<Billing> filterAndSortStays(
      ListInterface<Billing> source, String roomTypeFilter, String sortCriteria) {

    if (source == null || source.isEmpty()) return new ArrayList<>();

    ListInterface<Billing> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Billing b = source.getEntry(i);
      if (b == null || b.getCheckInDate() == null || b.getCheckOutDate() == null) continue;

      boolean matchType =
          roomTypeFilter == null
              || (b.getRoomType() != null
                  && roomTypeFilter.equalsIgnoreCase(b.getRoomType().name()));

      if (matchType) {
        filtered.add(b);
      }
    }

    if ("NIGHTS (LOW -> HIGH)".equalsIgnoreCase(sortCriteria)) {
      filtered.sort((b1, b2) -> Long.compare(b1.getNumberOfNights(), b2.getNumberOfNights()));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sortCriteria)) {
      filtered.sort(
          (b1, b2) -> {
            Guest g1 = guestRepo.findById(b1.getGuestId());
            Guest g2 = guestRepo.findById(b2.getGuestId());
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return n1.compareToIgnoreCase(n2);
          });
    } else {
      // Default: NIGHTS (HIGH -> LOW)
      filtered.sort((b1, b2) -> Long.compare(b2.getNumberOfNights(), b1.getNumberOfNights()));
    }

    return filtered;
  }

  // ================= TXT EXPORT FORMATTING (frontdesk's own format) =================
  // TxtExportUtil just writes whatever string we hand it to disk, so how the report looks is
  // entirely up to this module. This lays each row out as columns padded to the widest value
  // in that column, with a header row and a "-----" divider underneath.
  private String buildTxtTable(String[] headers, String[][] rows) {
    final String gap = "   ";
    int columnCount = (headers != null) ? headers.length : 0;
    int[] widths = new int[columnCount];

    if (headers != null) {
      for (int c = 0; c < columnCount; c++) {
        widths[c] = headers[c] == null ? 0 : headers[c].length();
      }
    }
    if (rows != null) {
      for (String[] row : rows) {
        if (row == null) continue;
        for (int c = 0; c < columnCount && c < row.length; c++) {
          int len = (row[c] == null) ? 0 : row[c].length();
          if (len > widths[c]) widths[c] = len;
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append(formatTxtRow(headers, widths, gap));

    int dividerLen = 0;
    for (int w : widths) dividerLen += w;
    dividerLen += gap.length() * Math.max(0, columnCount - 1);
    for (int i = 0; i < dividerLen; i++) sb.append('-');
    sb.append(System.lineSeparator());

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
      boolean isLastColumn = (i == widths.length - 1);
      if (isLastColumn) {
        sb.append(value);
      } else {
        sb.append(value);
        for (int p = value.length(); p < widths[i]; p++) sb.append(' ');
        sb.append(gap);
      }
    }
    sb.append(System.lineSeparator());
    return sb.toString();
  }
}
