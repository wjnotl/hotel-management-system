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
        else if (choice == 2) runRoomPerformanceReport();
        else if (choice == 3) runStayDurationReport();
        else if (choice == 4) return;
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
        DoublyLinkedHashMap<String, String> guestNameMap = buildGuestNameMap();
        ArrayList<ReportsView.CheckoutRowDTO> allDtos = buildCheckoutDTOs(guestNameMap);
        ArrayList<ReportsView.CheckoutRowDTO> filtered =
            filterAndSortCheckoutDTOs(
                allDtos, fromDate, toDate, paymentFilter, roomTypeFilter, sortCriteria);

        ReportsView.CheckoutSummaryDTO summary = buildCheckoutSummary(filtered, fromDate, toDate);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        GetMenuInputResult result =
            reportsView.renderCheckoutReport(
                filtered, summary, fromDate, toDate,
                paymentFilter, roomTypeFilter, sortCriteria, currentPage, PAGE_SIZE);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) { /* re-fetched */ }
        else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, paymentFilter, roomTypeFilter);
          fromDate      = dates[0];
          toDate        = dates[1];
          paymentFilter  = reportsView.getLastPaymentFilter();
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
          currentPage    = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleCheckoutSortMenu(sortCriteria);
          if (newSort != null) { sortCriteria = newSort; currentPage = 1; }
        } else if ("X".equalsIgnoreCase(raw)) {
          exportCheckoutReport(filtered, summary, fromDate, toDate);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // REPORT 2 — ROOM PERFORMANCE & REVENUE REPORT (occupancy + revenue combined)
  // =========================================================================

  private void runRoomPerformanceReport() {
    LocalDate fromDate = LocalDate.now().withDayOfMonth(1);
    LocalDate toDate = LocalDate.now();
    String roomTypeFilter = null;
    String sortCriteria = "TOTAL AMOUNT (HIGH -> LOW)";

    while (true) {
      try {
        // Section 1: live occupancy snapshot (ignores date range — always current)
        ArrayList<ReportsView.OccupancyRowDTO> occupancyRows = buildOccupancyDTOs(roomTypeFilter);
        ReportsView.OccupancySummaryDTO occupancySummary = buildOccupancySummary(occupancyRows);

        // Section 2: paid revenue in the selected date range, sorted
        DoublyLinkedHashMap<String, String> guestNameMap = buildGuestNameMap();
        ArrayList<ReportsView.CheckoutRowDTO> allCheckouts = buildCheckoutDTOs(guestNameMap);
        ArrayList<ReportsView.CheckoutRowDTO> revenueRows =
            filterAndSortCheckoutDTOs(
                allCheckouts, fromDate, toDate, "PAID", roomTypeFilter, sortCriteria);
        ReportsView.RevenueSummaryDTO revenueSummary =
            buildRevenueSummary(revenueRows, fromDate, toDate);

        GetMenuInputResult result =
            reportsView.renderRoomPerformanceReport(
                occupancyRows, occupancySummary,
                revenueRows, revenueSummary,
                fromDate, toDate, roomTypeFilter, sortCriteria);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) { /* re-fetched */ }
        else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, null, roomTypeFilter);
          fromDate       = dates[0];
          toDate         = dates[1];
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleCheckoutSortMenu(sortCriteria);
          if (newSort != null) sortCriteria = newSort;
        } else if ("X".equalsIgnoreCase(raw)) {
          exportRoomPerformanceReport(
              occupancyRows, occupancySummary, revenueRows, revenueSummary, fromDate, toDate);
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
                filtered, summary, fromDate, toDate, roomTypeFilter, sortCriteria,
                currentPage, PAGE_SIZE);

        String raw = result.input.trim();
        if ("E".equalsIgnoreCase(raw)) return;
        else if ("R".equalsIgnoreCase(raw)) { /* re-fetched */ }
        else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleFilterMenu(fromDate, toDate, null, roomTypeFilter);
          fromDate       = dates[0];
          toDate         = dates[1];
          roomTypeFilter = reportsView.getLastRoomTypeFilter();
          currentPage    = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleStaySortMenu(sortCriteria);
          if (newSort != null) { sortCriteria = newSort; currentPage = 1; }
        } else if ("X".equalsIgnoreCase(raw)) {
          exportStayReport(filtered, summary);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // FILTER MENUS
  // =========================================================================

  private LocalDate[] handleFilterMenu(LocalDate currentFrom, LocalDate currentTo,
      String currentPayment, String currentRoomType) {
    while (true) {
      try {
        int choice =
            reportsView.displayFilterMenu(currentFrom, currentTo, currentPayment, currentRoomType);
        if (choice == 1) {
          LocalDate[] dates = reportsView.promptDateRange(currentFrom, currentTo);
          currentFrom = dates[0];
          currentTo   = dates[1];
        } else if (choice == 2) {
          reportsView.setLastPaymentFilter(handlePaymentSubmenu(currentPayment));
          currentPayment = reportsView.getLastPaymentFilter();
        } else if (choice == 3) {
          reportsView.setLastRoomTypeFilter(handleRoomTypeSubmenu(currentRoomType));
          currentRoomType = reportsView.getLastRoomTypeFilter();
        } else if (choice == 4) {
          reportsView.setLastPaymentFilter(null);
          reportsView.setLastRoomTypeFilter(null);
          return new LocalDate[]{null, null};
        } else if (choice == 5) {
          reportsView.setLastPaymentFilter(currentPayment);
          reportsView.setLastRoomTypeFilter(currentRoomType);
          return new LocalDate[]{currentFrom, currentTo};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handlePaymentSubmenu(String current) {
    while (true) {
      try {
        int choice = reportsView.displayPaymentSubmenu(current);
        if (choice == 1) return "PAID";
        if (choice == 2) return "UNPAID";
        if (choice == 3) return null;
        if (choice == 4) return current;
      } catch (Exception e) { ConsoleUtil.printError(e.getMessage()); }
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
      } catch (Exception e) { ConsoleUtil.printError(e.getMessage()); }
    }
  }

  private String handleCheckoutSortMenu(String current) {
    while (true) {
      try {
        return reportsView.displayCheckoutSortMenu(current);
      } catch (Exception e) { ConsoleUtil.printError(e.getMessage()); }
    }
  }

  private String handleStaySortMenu(String current) {
    while (true) {
      try {
        return reportsView.displayStaySortMenu(current);
      } catch (Exception e) { ConsoleUtil.printError(e.getMessage()); }
    }
  }

  // =========================================================================
  // DATA PROCESSING
  //
  // ADT usage:
  //   DoublyLinkedHashMap — O(1) guestId → name lookups
  //   LinkedList          — sequential accumulation while building DTOs
  //   ArrayList           — final indexed list for paged view rendering
  // =========================================================================

  /** Builds an O(1) lookup map: guestId (lowercase) → guest name. */
  private DoublyLinkedHashMap<String, String> buildGuestNameMap() {
    DoublyLinkedHashMap<String, String> map = new DoublyLinkedHashMap<>();
    ListInterface<Guest> guests = guestRepo.getGuestList();
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      Guest g = guests.getEntry(i);
      if (g != null && g.getGuestId() != null)
        map.put(g.getGuestId().toLowerCase(), g.getName() != null ? g.getName() : "N/A");
    }
    return map;
  }

  /**
   * Checkout DTOs — billings whose checkOutDate is strictly before today.
   * (A checkout date of today means the guest may still be occupying the room.)
   */
  private ArrayList<ReportsView.CheckoutRowDTO> buildCheckoutDTOs(
      DoublyLinkedHashMap<String, String> guestNameMap) {

    ListInterface<Billing> allBillings = billingRepo.getBillingList();
    LocalDate today = LocalDate.now();
    LinkedList<ReportsView.CheckoutRowDTO> buffer = new LinkedList<>();

    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b == null || b.getCheckOutDate() == null) continue;
      if (!b.getCheckOutDate().isBefore(today)) continue;

      String guestName = (b.getGuestId() != null)
          ? guestNameMap.get(b.getGuestId().toLowerCase()) : null;
      if (guestName == null) guestName = "N/A";

      buffer.add(new ReportsView.CheckoutRowDTO(
          b.getBillingId(),
          b.getGuestId()    != null ? b.getGuestId()    : "N/A",
          guestName,
          b.getRoomNumber() != null ? b.getRoomNumber() : "N/A",
          b.getRoomType()   != null ? b.getRoomType().name() : "N/A",
          b.getCheckInDate()  != null ? b.getCheckInDate().format(DATE_FMT)  : "N/A",
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
      LocalDate fromDate, LocalDate toDate,
      String paymentFilter, String roomTypeFilter, String sort) {

    LinkedList<ReportsView.CheckoutRowDTO> buffer = new LinkedList<>();
    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ReportsView.CheckoutRowDTO dto = source.getEntry(i);
        if (dto == null) continue;
        boolean matchFrom = fromDate == null || dto.checkOutDateRaw == null
            || !dto.checkOutDateRaw.isBefore(fromDate);
        boolean matchTo = toDate == null || dto.checkOutDateRaw == null
            || !dto.checkOutDateRaw.isAfter(toDate);
        boolean matchPayment = paymentFilter == null
            || paymentFilter.equalsIgnoreCase(dto.paymentStatus);
        boolean matchType = roomTypeFilter == null
            || roomTypeFilter.equalsIgnoreCase(dto.roomType);
        if (matchFrom && matchTo && matchPayment && matchType) buffer.add(dto);
      }
    }

    if ("CHECK-OUT DATE (EARLIEST FIRST)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> compareDates(a.checkOutDateRaw, b.checkOutDateRaw));
    else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> nullSafeCompare(a.guestName, b.guestName));
    else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> nullSafeCompare(b.guestName, a.guestName));
    else if ("ROOM NO. (LOW -> HIGH)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> nullSafeCompare(a.roomNumber, b.roomNumber));
    else if ("TOTAL AMOUNT (HIGH -> LOW)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));
    else if ("TOTAL AMOUNT (LOW -> HIGH)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> Double.compare(a.totalAmount, b.totalAmount));
    else if ("NIGHTS (HIGH -> LOW)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> Long.compare(b.nights, a.nights));
    else
      buffer.sort((a, b) -> compareDates(b.checkOutDateRaw, a.checkOutDateRaw));

    ArrayList<ReportsView.CheckoutRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ReportsView.CheckoutSummaryDTO buildCheckoutSummary(
      ArrayList<ReportsView.CheckoutRowDTO> filtered, LocalDate from, LocalDate to) {

    int total = filtered.getNumberOfEntries();
    int paid = 0, unpaid = 0;
    double revenue = 0, luxuryRevenue = 0, suiteRevenue = 0, standardRevenue = 0;

    for (int i = 1; i <= total; i++) {
      ReportsView.CheckoutRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      if ("PAID".equalsIgnoreCase(dto.paymentStatus)) {
        paid++;
        revenue += dto.totalAmount;
        if ("LUXURY".equalsIgnoreCase(dto.roomType))        luxuryRevenue   += dto.totalAmount;
        else if ("SUITE".equalsIgnoreCase(dto.roomType))    suiteRevenue    += dto.totalAmount;
        else if ("STANDARD".equalsIgnoreCase(dto.roomType)) standardRevenue += dto.totalAmount;
      } else {
        unpaid++;
      }
    }

    String generated = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));
    String period = (from != null ? from.format(DATE_FMT) : "All")
        + "  —  " + (to != null ? to.format(DATE_FMT) : "All");

    return new ReportsView.CheckoutSummaryDTO(total, paid, unpaid, revenue,
        luxuryRevenue, suiteRevenue, standardRevenue, generated, period);
  }

  /**
   * Builds one OccupancyRowDTO per room type.
   * Tallies each room's status using its Room.Status enum; CLEANING and
   * INSPECTED are handled defensively by name in case the enum variant is
   * absent from an older local build.
   */
  private ArrayList<ReportsView.OccupancyRowDTO> buildOccupancyDTOs(String roomTypeFilter) {
    ListInterface<Room> allRooms = roomRepo.getRoomList();
    int[] dirty = new int[3], cleaning = new int[3], inspected = new int[3],
          vacantClean = new int[3], occupied = new int[3];

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
          case DIRTY:        dirty[t]++;       break;
          case VACANT_CLEAN: vacantClean[t]++; break;
          default:
            // Guard CLEANING / INSPECTED by name — safe if enum is absent on older builds
            String sName = r.getStatus().name();
            if ("CLEANING".equals(sName))  cleaning[t]++;
            else if ("INSPECTED".equals(sName)) inspected[t]++;
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
      double rate = (100.0 * occupied[t] / total);
      result.add(new ReportsView.OccupancyRowDTO(typeNames[t], dirty[t], cleaning[t],
          inspected[t], vacantClean[t], occupied[t], total, String.format("%.1f%%", rate)));
    }
    return result;
  }

  private ReportsView.OccupancySummaryDTO buildOccupancySummary(
      ArrayList<ReportsView.OccupancyRowDTO> rows) {
    int totalRooms = 0, totalOccupied = 0, totalDirty = 0, totalVacant = 0;
    for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
      ReportsView.OccupancyRowDTO r = rows.getEntry(i);
      if (r == null) continue;
      totalRooms    += r.total;
      totalOccupied += r.occupied;
      totalDirty    += r.dirty;
      totalVacant   += r.vacantClean;
    }
    double rate = (totalRooms > 0) ? (100.0 * totalOccupied / totalRooms) : 0.0;
    String generated = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));
    return new ReportsView.OccupancySummaryDTO(totalRooms, totalOccupied, totalDirty,
        totalVacant, String.format("%.1f%%", rate), generated);
  }

  /** Stay DTOs = all billing records (active and historical). */
  private ArrayList<ReportsView.StayRowDTO> buildStayDTOs(
      DoublyLinkedHashMap<String, String> guestNameMap) {

    ListInterface<Billing> allBillings = billingRepo.getBillingList();
    LinkedList<ReportsView.StayRowDTO> buffer = new LinkedList<>();

    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b == null || b.getCheckInDate() == null || b.getCheckOutDate() == null) continue;
      String guestName = (b.getGuestId() != null)
          ? guestNameMap.get(b.getGuestId().toLowerCase()) : null;
      if (guestName == null) guestName = "N/A";
      buffer.add(new ReportsView.StayRowDTO(
          b.getGuestId()    != null ? b.getGuestId()    : "N/A",
          guestName,
          b.getRoomNumber() != null ? b.getRoomNumber() : "N/A",
          b.getRoomType()   != null ? b.getRoomType().name() : "N/A",
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
      LocalDate fromDate, LocalDate toDate, String roomTypeFilter, String sort) {

    LinkedList<ReportsView.StayRowDTO> buffer = new LinkedList<>();
    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ReportsView.StayRowDTO dto = source.getEntry(i);
        if (dto == null) continue;
        boolean matchFrom = fromDate == null || dto.checkInDateRaw == null
            || !dto.checkInDateRaw.isBefore(fromDate);
        boolean matchTo = toDate == null || dto.checkOutDateRaw == null
            || !dto.checkOutDateRaw.isAfter(toDate);
        boolean matchType = roomTypeFilter == null
            || roomTypeFilter.equalsIgnoreCase(dto.roomType);
        if (matchFrom && matchTo && matchType) buffer.add(dto);
      }
    }

    if ("NIGHTS (LOW -> HIGH)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> Long.compare(a.nights, b.nights));
    else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> nullSafeCompare(a.guestName, b.guestName));
    else if ("CHECK-IN (LATEST FIRST)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> compareDates(b.checkInDateRaw, a.checkInDateRaw));
    else if ("CHECK-IN (EARLIEST FIRST)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> compareDates(a.checkInDateRaw, b.checkInDateRaw));
    else if ("TOTAL AMOUNT (HIGH -> LOW)".equalsIgnoreCase(sort))
      buffer.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));
    else
      buffer.sort((a, b) -> Long.compare(b.nights, a.nights));

    ArrayList<ReportsView.StayRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= buffer.getNumberOfEntries(); i++) result.add(buffer.getEntry(i));
    return result;
  }

  private ReportsView.StaySummaryDTO buildStaySummary(
      ArrayList<ReportsView.StayRowDTO> filtered) {

    int total = filtered.getNumberOfEntries();
    if (total == 0) {
      return new ReportsView.StaySummaryDTO(0, 0.0, 0, 0, 0, 0, 0, 0,
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a")));
    }

    long nightsSum = 0, shortest = Long.MAX_VALUE, longest = Long.MIN_VALUE;
    int one = 0, twoThree = 0, fourSeven = 0, eightPlus = 0;

    for (int i = 1; i <= total; i++) {
      ReportsView.StayRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      long n = dto.nights;
      nightsSum += n;
      if (n < shortest) shortest = n;
      if (n > longest)  longest  = n;
      if (n == 1) one++;
      else if (n <= 3) twoThree++;
      else if (n <= 7) fourSeven++;
      else eightPlus++;
    }

    return new ReportsView.StaySummaryDTO(total, (double) nightsSum / total,
        shortest, longest, one, twoThree, fourSeven, eightPlus,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a")));
  }

  private ReportsView.RevenueSummaryDTO buildRevenueSummary(
      ArrayList<ReportsView.CheckoutRowDTO> filtered, LocalDate from, LocalDate to) {

    int count = filtered.getNumberOfEntries();
    double total = 0, luxury = 0, suite = 0, standard = 0;
    long totalNights = 0;

    for (int i = 1; i <= count; i++) {
      ReportsView.CheckoutRowDTO dto = filtered.getEntry(i);
      if (dto == null) continue;
      total        += dto.totalAmount;
      totalNights  += dto.nights;
      if ("LUXURY".equalsIgnoreCase(dto.roomType))        luxury   += dto.totalAmount;
      else if ("SUITE".equalsIgnoreCase(dto.roomType))    suite    += dto.totalAmount;
      else if ("STANDARD".equalsIgnoreCase(dto.roomType)) standard += dto.totalAmount;
    }

    String generated = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a"));
    String period = (from != null ? from.format(DATE_FMT) : "All")
        + "  —  " + (to != null ? to.format(DATE_FMT) : "All");

    return new ReportsView.RevenueSummaryDTO(count, total, luxury, suite, standard,
        (count > 0 ? total / count : 0.0),
        (totalNights > 0 ? total / totalNights : 0.0),
        generated, period);
  }

  // =========================================================================
  // EXPORT HELPERS
  //
  // All three exports use TxtExportUtil.export() which writes the string to a
  // timestamped .txt file and returns the saved path for the success screen.
  // =========================================================================

  private void exportCheckoutReport(ArrayList<ReportsView.CheckoutRowDTO> filtered,
      ReportsView.CheckoutSummaryDTO summary, LocalDate from, LocalDate to) {
    String content = ConsoleUtil.getCapturedString();
    String path = TxtExportUtil.export("frontdesk/checkout_report", content);
    reportsView.showExportSuccess(path, filtered.getNumberOfEntries());
  }

  private void exportRoomPerformanceReport(
      ArrayList<ReportsView.OccupancyRowDTO> occupancyRows,
      ReportsView.OccupancySummaryDTO occupancySummary,
      ArrayList<ReportsView.CheckoutRowDTO> revenueRows,
      ReportsView.RevenueSummaryDTO revenueSummary,
      LocalDate from, LocalDate to) {
    String content = ConsoleUtil.getCapturedString();
    String path = TxtExportUtil.export("frontdesk/room_performance_report", content);
    reportsView.showExportSuccess(path, revenueRows.getNumberOfEntries());
  }

  private void exportStayReport(ArrayList<ReportsView.StayRowDTO> filtered,
      ReportsView.StaySummaryDTO summary) {
    String content = ConsoleUtil.getCapturedString();
    String path = TxtExportUtil.export("frontdesk/stay_duration_report", content);
    reportsView.showExportSuccess(path, filtered.getNumberOfEntries());
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
