package control.frontdesk;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.LinkedList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.RoomRepo;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TxtExportUtil;
import view.frontdesk.ReportsView;

public class ReportsController {

  private static final int PAGE_SIZE = 10;
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

  private final ReportsView reportsView = new ReportsView();
  private final BillingRepo billingRepo;
  private final GuestRepo guestRepo;
  private final RoomRepo roomRepo;

  public ReportsController(BillingRepo billingRepo, GuestRepo guestRepo, RoomRepo roomRepo) {
    this.billingRepo = billingRepo;
    this.guestRepo = guestRepo;
    this.roomRepo = roomRepo;
  }

  // =========================================================================
  // ENTRY POINT
  // =========================================================================

  public void start() {
    while (true) {
      try {
        int choice = reportsView.displayReportHubMenu();
        if (choice == 1) runCheckoutReport();
        else if (choice == 2) runOccupancyReport();
        else if (choice == 3) runStayDurationReport();
        else if (choice == 4) runRevenueReport();
        else if (choice == 5) return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // REPORT 1 — GUEST CHECK-OUT REPORT
  // =========================================================================

  private void runCheckoutReport() {
    LocalDate fromDate = LocalDate.now();
    LocalDate toDate = LocalDate.now();
    String paymentFilter = null;
    String roomTypeFilter = null;
    String sortCriteria = "CHECK-OUT DATE (LATEST FIRST)";
    int currentPage = 1;

    while (true) {
      try {
        // Build guestId → name map once per loop
        DoublyLinkedHashMap<String, String> guestNameMap = buildGuestNameMap();

        ArrayList<ReportsView.CheckoutRowDTO> allDtos = buildCheckoutDTOs(guestNameMap);
        ArrayList<ReportsView.CheckoutRowDTO> filtered =
            filterAndSortCheckoutDTOs(
                allDtos, fromDate, toDate, paymentFilter, roomTypeFilter, sortCriteria);

        // Summary stats (computed in controller, not view)
        ReportsView.CheckoutSummaryDTO summary = buildCheckoutSummary(filtered, fromDate, toDate);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        GetMenuInputResult result =
            reportsView.renderCheckoutReport(
                filtered,
                summary,
                fromDate,
                toDate,
                paymentFilter,
                roomTypeFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        String raw = result.input.trim();

        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) {
          /* re-fetched */
        } else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, paymentFilter, roomTypeFilter);
          fromDate = dates[0];
          toDate = dates[1];
          paymentFilter = reportsView.getLastPaymentFilter();
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleCheckoutSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("X".equalsIgnoreCase(raw)) {
          exportCheckoutReport(filtered, summary, fromDate, toDate);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // REPORT 2 — ROOM OCCUPANCY & STATUS REPORT
  // =========================================================================

  private void runOccupancyReport() {
    String roomTypeFilter = null;
    while (true) {
      try {
        ArrayList<ReportsView.OccupancyRowDTO> rows = buildOccupancyDTOs(roomTypeFilter);
        ReportsView.OccupancySummaryDTO summary = buildOccupancySummary(rows);

        GetMenuInputResult result =
            reportsView.renderOccupancyReport(rows, summary, roomTypeFilter);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) {
          /* re-fetched */
        } else if ("T".equalsIgnoreCase(raw)) {
          roomTypeFilter = handleRoomTypeSubmenu(roomTypeFilter);
        } else if ("X".equalsIgnoreCase(raw)) {
          exportOccupancyReport(rows, summary);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // REPORT 3 — STAY DURATION ANALYSIS
  // =========================================================================

  private void runStayDurationReport() {
    LocalDate fromDate = null;
    LocalDate toDate = null;
    String roomTypeFilter = null;
    String sortCriteria = "NIGHTS (HIGH -> LOW)";
    int currentPage = 1;

    while (true) {
      try {
        DoublyLinkedHashMap<String, String> guestNameMap = buildGuestNameMap();
        ArrayList<ReportsView.StayRowDTO> allDtos = buildStayDTOs(guestNameMap);
        ArrayList<ReportsView.StayRowDTO> filtered =
            filterAndSortStayDTOs(allDtos, fromDate, toDate, roomTypeFilter, sortCriteria);

        ReportsView.StaySummaryDTO summary = buildStaySummary(filtered);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        GetMenuInputResult result =
            reportsView.renderStayReport(
                filtered,
                summary,
                fromDate,
                toDate,
                roomTypeFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) {
          /* re-fetched */
        } else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, null, roomTypeFilter);
          fromDate = dates[0];
          toDate = dates[1];
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleStaySortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("X".equalsIgnoreCase(raw)) {
          exportStayReport(filtered, summary);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // REPORT 4 — REVENUE SUMMARY REPORT
  // =========================================================================

  private void runRevenueReport() {
    LocalDate fromDate = LocalDate.now().withDayOfMonth(1); // default: this month
    LocalDate toDate = LocalDate.now();
    String roomTypeFilter = null;

    while (true) {
      try {
        DoublyLinkedHashMap<String, String> guestNameMap = buildGuestNameMap();
        ArrayList<ReportsView.CheckoutRowDTO> allDtos = buildCheckoutDTOs(guestNameMap);
        ArrayList<ReportsView.CheckoutRowDTO> filtered =
            filterAndSortCheckoutDTOs(
                allDtos, fromDate, toDate, "PAID", roomTypeFilter, "CHECK-OUT DATE (LATEST FIRST)");

        ReportsView.RevenueSummaryDTO summary = buildRevenueSummary(filtered, fromDate, toDate);

        GetMenuInputResult result =
            reportsView.renderRevenueReport(filtered, summary, fromDate, toDate, roomTypeFilter);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) {
          /* re-fetched */
        } else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, null, roomTypeFilter);
          fromDate = dates[0];
          toDate = dates[1];
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
        } else if ("X".equalsIgnoreCase(raw)) {
          exportRevenueReport(filtered, summary, fromDate, toDate);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // FILTER MENUS
  // =========================================================================

  private LocalDate[] handleFilterMenu(
      LocalDate currentFrom, LocalDate currentTo, String currentPayment, String currentRoomType) {

    while (true) {
      try {
        int choice =
            reportsView.displayFilterMenu(currentFrom, currentTo, currentPayment, currentRoomType);

        if (choice == 1) {
          LocalDate[] dates = promptDateRange(currentFrom, currentTo);
          currentFrom = dates[0];
          currentTo = dates[1];
        } else if (choice == 2) {
          reportsView.setLastPaymentFilter(handlePaymentSubmenu(currentPayment));
          currentPayment = reportsView.getLastPaymentFilter();
        } else if (choice == 3) {
          reportsView.setLastRoomTypeFilter(handleRoomTypeSubmenu(currentRoomType));
          currentRoomType = reportsView.getLastRoomTypeFilter();
        } else if (choice == 4) {
          // Clear all — reset to null/default
          reportsView.setLastPaymentFilter(null);
          reportsView.setLastRoomTypeFilter(null);
          return new LocalDate[] {null, null};
        } else if (choice == 5) {
          // Apply & back
          reportsView.setLastPaymentFilter(currentPayment);
          reportsView.setLastRoomTypeFilter(currentRoomType);
          return new LocalDate[] {currentFrom, currentTo};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private LocalDate[] promptDateRange(LocalDate currentFrom, LocalDate currentTo) {
    LocalDate[] result = reportsView.promptDateRange(currentFrom, currentTo);
    return result;
  }

  private String handlePaymentSubmenu(String current) {
    while (true) {
      try {
        int choice = reportsView.displayPaymentSubmenu(current);
        if (choice == 1) return "PAID";
        if (choice == 2) return "UNPAID";
        if (choice == 3) return null;
        if (choice == 4) return current; // Cancel
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleRoomTypeSubmenu(String current) {
    while (true) {
      try {
        int choice = reportsView.displayRoomTypeSubmenu(current);
        if (choice == 1) return "LUXURY";
        if (choice == 2) return "SUITE";
        if (choice == 3) return "STANDARD";
        if (choice == 4) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleCheckoutSortMenu(String current) {
    while (true) {
      try {
        String selected = reportsView.displayCheckoutSortMenu(current);
        return selected;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleStaySortMenu(String current) {
    while (true) {
      try {
        String selected = reportsView.displayStaySortMenu(current);
        return selected;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // DATA PROCESSING — DTOs built here, view gets clean strings
  //
  // ADT usage:
  //   DoublyLinkedHashMap — O(1) guestId→name, billingId→billing lookups
  //   LinkedList          — sequential DTO accumulation during build
  //   ArrayList           — final paged list for view (index access)
  // =========================================================================

  /** O(1) guest name map: guestId.lower → guest name */
  private DoublyLinkedHashMap<String, String> buildGuestNameMap() {
    DoublyLinkedHashMap<String, String> map = new DoublyLinkedHashMap<>();
    ListInterface<Guest> guests = guestRepo.getGuestList();
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      Guest g = guests.getEntry(i);
      if (g != null && g.getGuestId() != null) {
        map.put(g.getGuestId().toLowerCase(), g.getName() != null ? g.getName() : "N/A");
      }
    }
    return map;
  }

  /**
   * Checkout DTOs = billings where the guest has already left. A billing qualifies only when
   * checkOutDate is BEFORE today — a billing due today means the guest may still be in the room.
   */
  private ArrayList<ReportsView.CheckoutRowDTO> buildCheckoutDTOs(
      DoublyLinkedHashMap<String, String> guestNameMap) {

    ListInterface<Billing> allBillings = billingRepo.getBillingList();
    LocalDate today = LocalDate.now();

    LinkedList<ReportsView.CheckoutRowDTO> buffer = new LinkedList<>();

    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b == null || b.getCheckOutDate() == null) continue;

      // Only include guests who have already checked out (checkOut strictly before today)
      if (!b.getCheckOutDate().isBefore(today)) continue;

      String guestName =
          (b.getGuestId() != null) ? guestNameMap.get(b.getGuestId().toLowerCase()) : null;
      if (guestName == null) guestName = "N/A";

      buffer.add(
          new ReportsView.CheckoutRowDTO(
              b.getBillingId(),
              b.getGuestId() != null ? b.getGuestId() : "N/A",
              guestName,
              b.getRoomNumber() != null ? b.getRoomNumber() : "N/A",
              b.getRoomType() != null ? b.getRoomType().name() : "N/A",
              b.getCheckInDate() != null ? b.getCheckInDate().format(DATE_FMT) : "N/A",
              b.getCheckOutDate() != null ? b.getCheckOutDate().format(DATE_FMT) : "N/A",
              b.getCheckInDate(),
              b.getCheckOutDate(),
              b.getNumberOfNights(),
              b.getTotalAmount(),
              b.getStatus().name()));
    }

    ArrayList<ReportsView.CheckoutRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ArrayList<ReportsView.CheckoutRowDTO> filterAndSortCheckoutDTOs(
      ArrayList<ReportsView.CheckoutRowDTO> source,
      LocalDate fromDate,
      LocalDate toDate,
      String paymentFilter,
      String roomTypeFilter,
      String sort) {

    LinkedList<ReportsView.CheckoutRowDTO> buffer = new LinkedList<>();
    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ReportsView.CheckoutRowDTO dto = source.getEntry(i);
        if (dto == null) continue;

        boolean matchFrom =
            fromDate == null
                || dto.checkOutDateRaw == null
                || !dto.checkOutDateRaw.isBefore(fromDate);
        boolean matchTo =
            toDate == null || dto.checkOutDateRaw == null || !dto.checkOutDateRaw.isAfter(toDate);
        boolean matchPayment =
            paymentFilter == null || paymentFilter.equalsIgnoreCase(dto.paymentStatus);
        boolean matchType = roomTypeFilter == null || roomTypeFilter.equalsIgnoreCase(dto.roomType);

        if (matchFrom && matchTo && matchPayment && matchType) buffer.add(dto);
      }
    }

    if ("CHECK-OUT DATE (EARLIEST FIRST)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> compareDates(a.checkOutDateRaw, b.checkOutDateRaw));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> nullSafeCompare(a.guestName, b.guestName));
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> nullSafeCompare(b.guestName, a.guestName));
    } else if ("ROOM NO. (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> nullSafeCompare(a.roomNumber, b.roomNumber));
    } else if ("TOTAL AMOUNT (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));
    } else if ("TOTAL AMOUNT (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> Double.compare(a.totalAmount, b.totalAmount));
    } else if ("NIGHTS (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> Long.compare(b.nights, a.nights));
    } else {
      // Default: CHECK-OUT DATE (LATEST FIRST)
      buffer.sort((a, b) -> compareDates(b.checkOutDateRaw, a.checkOutDateRaw));
    }

    ArrayList<ReportsView.CheckoutRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ReportsView.CheckoutSummaryDTO buildCheckoutSummary(
      ArrayList<ReportsView.CheckoutRowDTO> filtered, LocalDate from, LocalDate to) {

    int total = filtered.getNumberOfEntries();
    int paid = 0;
    int unpaid = 0;
    double revenue = 0.0;
    double luxuryRevenue = 0.0;
    double suiteRevenue = 0.0;
    double standardRevenue = 0.0;

    for (int i = 1; i <= total; i++) {
      ReportsView.CheckoutRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      if ("PAID".equalsIgnoreCase(dto.paymentStatus)) {
        paid++;
        revenue += dto.totalAmount;
        if ("LUXURY".equalsIgnoreCase(dto.roomType)) luxuryRevenue += dto.totalAmount;
        else if ("SUITE".equalsIgnoreCase(dto.roomType)) suiteRevenue += dto.totalAmount;
        else if ("STANDARD".equalsIgnoreCase(dto.roomType)) standardRevenue += dto.totalAmount;
      } else {
        unpaid++;
      }
    }

    String generated =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));
    String period =
        (from != null ? from.format(DATE_FMT) : "All")
            + "  —  "
            + (to != null ? to.format(DATE_FMT) : "All");

    return new ReportsView.CheckoutSummaryDTO(
        total,
        paid,
        unpaid,
        revenue,
        luxuryRevenue,
        suiteRevenue,
        standardRevenue,
        generated,
        period);
  }

  /** Build one DTO row per room type + grand total row for occupancy report. */
  private ArrayList<ReportsView.OccupancyRowDTO> buildOccupancyDTOs(String roomTypeFilter) {
    ListInterface<Room> allRooms = roomRepo.getRoomList();

    // Tally by type and status
    int[] dirty = new int[3];
    int[] cleaning = new int[3];
    int[] inspected = new int[3];
    int[] vacantClean = new int[3];
    int[] occupied = new int[3];

    for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
      Room r = allRooms.getEntry(i);
      if (r == null || r.getRoomType() == null || r.getStatus() == null) continue;
      if (roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase(r.getRoomType().name()))
        continue;

      int t = r.getRoomType().ordinal();
      if (r.getIsOccupied()) {
        occupied[t]++;
      } else {
        switch (r.getStatus()) {
          case DIRTY:
            dirty[t]++;
            break;
          case VACANT_CLEAN:
            vacantClean[t]++;
            break;
          default:
            break;
        }
      }
    }

    String[] typeNames = {"LUXURY", "SUITE", "STANDARD"};
    ArrayList<ReportsView.OccupancyRowDTO> result = new ArrayList<>();

    for (int t = 0; t < 3; t++) {
      if (roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase(typeNames[t])) continue;
      int total = dirty[t] + cleaning[t] + inspected[t] + vacantClean[t] + occupied[t];
      if (total == 0) continue;
      double rate = (total > 0) ? (100.0 * occupied[t] / total) : 0.0;
      result.add(
          new ReportsView.OccupancyRowDTO(
              typeNames[t],
              dirty[t],
              cleaning[t],
              inspected[t],
              vacantClean[t],
              occupied[t],
              total,
              String.format("%.1f%%", rate)));
    }
    return result;
  }

  private ReportsView.OccupancySummaryDTO buildOccupancySummary(
      ArrayList<ReportsView.OccupancyRowDTO> rows) {
    int totalRooms = 0;
    int totalOccupied = 0;
    int totalDirty = 0;
    int totalVacant = 0;

    for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
      ReportsView.OccupancyRowDTO r = rows.getEntry(i);
      if (r == null) continue;
      totalRooms += r.total;
      totalOccupied += r.occupied;
      totalDirty += r.dirty;
      totalVacant += r.vacantClean;
    }

    double rate = (totalRooms > 0) ? (100.0 * totalOccupied / totalRooms) : 0.0;
    String generated =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));

    return new ReportsView.OccupancySummaryDTO(
        totalRooms,
        totalOccupied,
        totalDirty,
        totalVacant,
        String.format("%.1f%%", rate),
        generated);
  }

  /** Stay DTOs = all billing records (both active and historical). */
  private ArrayList<ReportsView.StayRowDTO> buildStayDTOs(
      DoublyLinkedHashMap<String, String> guestNameMap) {

    ListInterface<Billing> allBillings = billingRepo.getBillingList();
    LinkedList<ReportsView.StayRowDTO> buffer = new LinkedList<>();

    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b == null || b.getCheckInDate() == null || b.getCheckOutDate() == null) continue;

      String guestName =
          (b.getGuestId() != null) ? guestNameMap.get(b.getGuestId().toLowerCase()) : null;
      if (guestName == null) guestName = "N/A";

      buffer.add(
          new ReportsView.StayRowDTO(
              b.getGuestId() != null ? b.getGuestId() : "N/A",
              guestName,
              b.getRoomNumber() != null ? b.getRoomNumber() : "N/A",
              b.getRoomType() != null ? b.getRoomType().name() : "N/A",
              b.getCheckInDate().format(DATE_FMT),
              b.getCheckOutDate().format(DATE_FMT),
              b.getCheckInDate(),
              b.getCheckOutDate(),
              b.getNumberOfNights(),
              b.getTotalAmount(),
              b.getStatus().name()));
    }

    ArrayList<ReportsView.StayRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ArrayList<ReportsView.StayRowDTO> filterAndSortStayDTOs(
      ArrayList<ReportsView.StayRowDTO> source,
      LocalDate fromDate,
      LocalDate toDate,
      String roomTypeFilter,
      String sort) {

    LinkedList<ReportsView.StayRowDTO> buffer = new LinkedList<>();
    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ReportsView.StayRowDTO dto = source.getEntry(i);
        if (dto == null) continue;

        boolean matchFrom =
            fromDate == null
                || dto.checkInDateRaw == null
                || !dto.checkInDateRaw.isBefore(fromDate);
        boolean matchTo =
            toDate == null || dto.checkOutDateRaw == null || !dto.checkOutDateRaw.isAfter(toDate);
        boolean matchType = roomTypeFilter == null || roomTypeFilter.equalsIgnoreCase(dto.roomType);

        if (matchFrom && matchTo && matchType) buffer.add(dto);
      }
    }

    if ("NIGHTS (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> Long.compare(a.nights, b.nights));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> nullSafeCompare(a.guestName, b.guestName));
    } else if ("CHECK-IN (LATEST FIRST)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> compareDates(b.checkInDateRaw, a.checkInDateRaw));
    } else if ("CHECK-IN (EARLIEST FIRST)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> compareDates(a.checkInDateRaw, b.checkInDateRaw));
    } else if ("TOTAL AMOUNT (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      buffer.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));
    } else {
      // Default: NIGHTS (HIGH -> LOW)
      buffer.sort((a, b) -> Long.compare(b.nights, a.nights));
    }

    ArrayList<ReportsView.StayRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ReportsView.StaySummaryDTO buildStaySummary(ArrayList<ReportsView.StayRowDTO> filtered) {
    int total = filtered.getNumberOfEntries();
    if (total == 0) {
      return new ReportsView.StaySummaryDTO(
          0,
          0.0,
          0,
          0,
          0,
          0,
          0,
          0,
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a")));
    }

    long nightsSum = 0;
    long shortest = Long.MAX_VALUE;
    long longest = Long.MIN_VALUE;
    int one = 0, twoThree = 0, fourSeven = 0, eightPlus = 0;

    for (int i = 1; i <= total; i++) {
      ReportsView.StayRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      long n = dto.nights;
      nightsSum += n;
      if (n < shortest) shortest = n;
      if (n > longest) longest = n;
      if (n == 1) one++;
      else if (n <= 3) twoThree++;
      else if (n <= 7) fourSeven++;
      else eightPlus++;
    }

    double avg = (double) nightsSum / total;
    String generated =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));

    return new ReportsView.StaySummaryDTO(
        total, avg, shortest, longest, one, twoThree, fourSeven, eightPlus, generated);
  }

  private ReportsView.RevenueSummaryDTO buildRevenueSummary(
      ArrayList<ReportsView.CheckoutRowDTO> filtered, LocalDate from, LocalDate to) {

    int count = filtered.getNumberOfEntries();
    double total = 0, luxury = 0, suite = 0, standard = 0;
    long totalNights = 0;

    for (int i = 1; i <= count; i++) {
      ReportsView.CheckoutRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      total += dto.totalAmount;
      totalNights += dto.nights;
      if ("LUXURY".equalsIgnoreCase(dto.roomType)) luxury += dto.totalAmount;
      else if ("SUITE".equalsIgnoreCase(dto.roomType)) suite += dto.totalAmount;
      else if ("STANDARD".equalsIgnoreCase(dto.roomType)) standard += dto.totalAmount;
    }

    double avgPerStay = (count > 0) ? total / count : 0.0;
    double avgPerNight = (totalNights > 0) ? total / totalNights : 0.0;
    String generated =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));
    String period =
        (from != null ? from.format(DATE_FMT) : "All")
            + "  —  "
            + (to != null ? to.format(DATE_FMT) : "All");

    return new ReportsView.RevenueSummaryDTO(
        count, total, luxury, suite, standard, avgPerStay, avgPerNight, generated, period);
  }

  // =========================================================================
  // EXPORT HELPERS
  // =========================================================================

  private void exportCheckoutReport(
      ArrayList<ReportsView.CheckoutRowDTO> filtered,
      ReportsView.CheckoutSummaryDTO summary,
      LocalDate from,
      LocalDate to) {

    int total = filtered.getNumberOfEntries();
    String[] headers = {
      "No.", "Guest ID", "Guest Name", "Room No.", "Room Type",
      "Check-In", "Check-Out", "Nights", "Total (RM)", "Payment"
    };
    String[][] rows = new String[total][];
    for (int i = 1; i <= total; i++) {
      ReportsView.CheckoutRowDTO d = filtered.getEntry(i);
      rows[i - 1] =
          new String[] {
            String.valueOf(i),
            d.guestId,
            d.guestName,
            d.roomNumber,
            d.roomType,
            d.checkInDate,
            d.checkOutDate,
            String.valueOf(d.nights),
            String.format("%.2f", d.totalAmount),
            d.paymentStatus
          };
    }
    StringBuilder sb = new StringBuilder();
    sb.append("GUEST CHECK-OUT REPORT\n");
    sb.append("Generated : ").append(summary.generatedAt).append("\n");
    sb.append("Period    : ").append(summary.period).append("\n");
    sb.append("Total Checkouts: ")
        .append(summary.totalCheckouts)
        .append("  |  Paid: ")
        .append(summary.paidCount)
        .append("  |  Unpaid: ")
        .append(summary.unpaidCount)
        .append("  |  Revenue: RM ")
        .append(String.format("%.2f", summary.totalRevenue))
        .append("\n\n");
    sb.append(buildTxtTable(headers, rows));
    String path = TxtExportUtil.export("frontdesk/checkout_report", sb.toString());
    reportsView.showExportSuccess(path, total);
  }

  private void exportOccupancyReport(
      ArrayList<ReportsView.OccupancyRowDTO> rows, ReportsView.OccupancySummaryDTO summary) {

    int total = rows.getNumberOfEntries();
    String[] headers = {
      "Room Type", "DIRTY", "CLEANING", "INSPECTED", "VACANT_CLEAN", "OCCUPIED", "TOTAL", "OCC%"
    };
    String[][] data = new String[total][];
    for (int i = 1; i <= total; i++) {
      ReportsView.OccupancyRowDTO r = rows.getEntry(i);
      data[i - 1] =
          new String[] {
            r.roomType,
            String.valueOf(r.dirty),
            String.valueOf(r.cleaning),
            String.valueOf(r.inspected),
            String.valueOf(r.vacantClean),
            String.valueOf(r.occupied),
            String.valueOf(r.total),
            r.occupancyRate
          };
    }
    StringBuilder sb = new StringBuilder();
    sb.append("ROOM OCCUPANCY & STATUS REPORT\n");
    sb.append("Generated        : ").append(summary.generatedAt).append("\n");
    sb.append("Overall Rate     : ").append(summary.overallOccupancyRate).append("\n");
    sb.append("Total Rooms      : ")
        .append(summary.totalRooms)
        .append("  |  Occupied: ")
        .append(summary.totalOccupied)
        .append("  |  Dirty: ")
        .append(summary.totalDirty)
        .append("  |  Available: ")
        .append(summary.totalVacantClean)
        .append("\n\n");
    sb.append(buildTxtTable(headers, data));
    String path = TxtExportUtil.export("frontdesk/occupancy_report", sb.toString());
    reportsView.showExportSuccess(path, total);
  }

  private void exportStayReport(
      ArrayList<ReportsView.StayRowDTO> filtered, ReportsView.StaySummaryDTO summary) {

    int total = filtered.getNumberOfEntries();
    String[] headers = {
      "No.", "Guest ID", "Guest Name", "Room No.", "Room Type",
      "Check-In", "Check-Out", "Nights", "Total (RM)", "Status"
    };
    String[][] rows = new String[total][];
    for (int i = 1; i <= total; i++) {
      ReportsView.StayRowDTO d = filtered.getEntry(i);
      rows[i - 1] =
          new String[] {
            String.valueOf(i),
            d.guestId,
            d.guestName,
            d.roomNumber,
            d.roomType,
            d.checkInDate,
            d.checkOutDate,
            String.valueOf(d.nights),
            String.format("%.2f", d.totalAmount),
            d.status
          };
    }
    StringBuilder sb = new StringBuilder();
    sb.append("GUEST STAY DURATION ANALYSIS REPORT\n");
    sb.append("Generated : ").append(summary.generatedAt).append("\n");
    sb.append(
        String.format(
            "Total Stays: %d  |  Avg: %.1f nights  |  " + "Shortest: %d  |  Longest: %d nights\n",
            summary.totalStays, summary.avgNights, summary.shortestNights, summary.longestNights));
    sb.append(
        String.format(
            "Distribution -> 1 Night: %d  |  2-3: %d  |  4-7: %d  |  8+: %d\n\n",
            summary.oneNight,
            summary.twoThreeNights,
            summary.fourSevenNights,
            summary.eightPlusNights));
    sb.append(buildTxtTable(headers, rows));
    String path = TxtExportUtil.export("frontdesk/stay_duration_report", sb.toString());
    reportsView.showExportSuccess(path, total);
  }

  private void exportRevenueReport(
      ArrayList<ReportsView.CheckoutRowDTO> filtered,
      ReportsView.RevenueSummaryDTO summary,
      LocalDate from,
      LocalDate to) {

    int total = filtered.getNumberOfEntries();
    String[] headers = {
      "No.",
      "Guest ID",
      "Guest Name",
      "Room No.",
      "Room Type",
      "Check-In",
      "Check-Out",
      "Nights",
      "Total (RM)"
    };
    String[][] rows = new String[total][];
    for (int i = 1; i <= total; i++) {
      ReportsView.CheckoutRowDTO d = filtered.getEntry(i);
      rows[i - 1] =
          new String[] {
            String.valueOf(i),
            d.guestId,
            d.guestName,
            d.roomNumber,
            d.roomType,
            d.checkInDate,
            d.checkOutDate,
            String.valueOf(d.nights),
            String.format("%.2f", d.totalAmount)
          };
    }
    StringBuilder sb = new StringBuilder();
    sb.append("REVENUE SUMMARY REPORT\n");
    sb.append("Generated  : ").append(summary.generatedAt).append("\n");
    sb.append("Period     : ").append(summary.period).append("\n");
    sb.append(
        String.format(
            "Total Revenue: RM %.2f  |  Stays: %d  |  Avg/Stay: RM %.2f  |  Avg/Night: RM %.2f\n",
            summary.totalRevenue,
            summary.totalStays,
            summary.avgRevenuePerStay,
            summary.avgRevenuePerNight));
    sb.append(
        String.format(
            "By Type -> Luxury: RM %.2f  |  Suite: RM %.2f  |  Standard: RM %.2f\n\n",
            summary.luxuryRevenue, summary.suiteRevenue, summary.standardRevenue));
    sb.append(buildTxtTable(headers, rows));
    String path = TxtExportUtil.export("frontdesk/revenue_report", sb.toString());
    reportsView.showExportSuccess(path, total);
  }

  // =========================================================================
  // TXT TABLE BUILDER
  // =========================================================================

  private String buildTxtTable(String[] headers, String[][] rows) {
    final String GAP = "   ";
    int cols = headers != null ? headers.length : 0;
    int[] widths = new int[cols];
    if (headers != null)
      for (int c = 0; c < cols; c++) widths[c] = headers[c] != null ? headers[c].length() : 0;
    if (rows != null)
      for (String[] row : rows)
        if (row != null)
          for (int c = 0; c < cols && c < row.length; c++)
            if (row[c] != null && row[c].length() > widths[c]) widths[c] = row[c].length();

    StringBuilder sb = new StringBuilder();
    sb.append(formatTxtRow(headers, widths, GAP));
    int divLen = 0;
    for (int w : widths) divLen += w;
    divLen += GAP.length() * Math.max(0, cols - 1);
    for (int i = 0; i < divLen; i++) sb.append('-');
    sb.append(System.lineSeparator());
    if (rows != null) for (String[] row : rows) sb.append(formatTxtRow(row, widths, GAP));
    return sb.toString();
  }

  private String formatTxtRow(String[] fields, int[] widths, String gap) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < widths.length; i++) {
      String v = (fields != null && i < fields.length && fields[i] != null) ? fields[i] : "";
      sb.append(v);
      if (i < widths.length - 1) {
        for (int p = v.length(); p < widths[i]; p++) sb.append(' ');
        sb.append(gap);
      }
    }
    sb.append(System.lineSeparator());
    return sb.toString();
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  private int nullSafeCompare(String a, String b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareToIgnoreCase(b);
  }

  private int compareDates(LocalDate a, LocalDate b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareTo(b);
  }
}
