package view.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import util.ConsoleUtil;
import util.TableUtil;

public class ManageGuestView {

  // =========================================================================
  // DTO — view only renders pre-processed data
  // =========================================================================

  public static class GuestRowDTO {
    public final String guestId;
    public final String name;
    public final String icOrPassport;
    public final String phone;
    public final String memberLevel;

    public GuestRowDTO(
        String guestId, String name, String icOrPassport, String phone, String memberLevel) {
      this.guestId = guestId;
      this.name = name;
      this.icOrPassport = icOrPassport != null ? icOrPassport : "N/A";
      this.phone = phone != null ? phone : "N/A";
      this.memberLevel = memberLevel != null ? memberLevel : "NON-MEMBER";
    }
  }

  // Guest Table
  public ConsoleUtil.GetMenuInputResult renderGuestTable(
      ListInterface<GuestRowDTO> guests,
      String searchQuery,
      String memberLevelFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE GUESTS", 90);

    System.out.println(
        "SEARCH FILTER   : [ "
            + (searchQuery == null || searchQuery.isEmpty() ? "None" : "\"" + searchQuery + "\"")
            + " ]");
    System.out.println(
        "MEMBER LEVEL    : [ " + (memberLevelFilter == null ? "ALL" : memberLevelFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]");
    System.out.println();

    int total = (guests == null) ? 0 : guests.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 22, 18, 16, 14};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.LEFT)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "GUEST ID", "NAME", "IC / PASSPORT", "PHONE", "MEMBER LEVEL"},
        settings);

    if (total == 0 || guests == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {99}).setHAlign(0, TableUtil.Align.CENTER);
      boolean hasFilters =
          (searchQuery != null && !searchQuery.isEmpty()) || memberLevelFilter != null;
      String msg =
          hasFilters ? "*** NO GUESTS MATCH ACTIVE FILTERS ***" : "*** NO GUESTS FOUND ***";
      TableUtil.printTableRow(new String[] {msg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[S] Search & Filter     [O] Change Sort     [R] Refresh     [E] Exit\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'O', 'R', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      GuestRowDTO g = guests.getEntry(i);
      if (g == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum), g.guestId, g.name, g.icOrPassport, g.phone, g.memberLevel
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Search & Filter     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page           [N] Next Page       [E] Exit to Front Desk\n");

    int maxDisplayNum = endIndex - startIndex + 1;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Select row or command (" + rangeStr + "): ",
        1,
        maxDisplayNum,
        new char[] {'S', 'O', 'R', 'N', 'P', 'E'});
  }

  // Filter Menu
  public int displayFilterMenu(String search, String memberLevel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & FILTER GUESTS", 68);
    System.out.println(
        "Search Query   : [ "
            + (search == null || search.isEmpty() ? "None" : "\"" + search + "\"")
            + " ]");
    System.out.println(
        "Member Level   : [ " + (memberLevel == null ? "ALL" : memberLevel) + " ]\n");
    System.out.println("1. Search");
    System.out.println("2. Filter by Member Level");
    System.out.println("3. Clear All Filters");
    System.out.println("4. Apply & Back\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  // search
  public String promptSearchQuery(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH GUESTS", 68);
    System.out.println(
        "Current : [ "
            + (currentQuery == null || currentQuery.isEmpty() ? "None" : "\"" + currentQuery + "\"")
            + " ]");
    System.out.println();
    System.out.println("Can Seacrh by Guest ID, Name, IC Number, Passport or Phone Number");
    System.out.println("Press Enter to clean/ C to cancel.\n");
    String input = ConsoleUtil.getStringInput("Search: ");
    if (input == null || "C".equalsIgnoreCase(input.trim())) return currentQuery;
    return input.trim().isEmpty() ? null : input.trim();
  }

  // filter by member level
  public int displayMemberLevelSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY MEMBER LEVEL", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. DIAMOND");
    System.out.println("2. GOLD");
    System.out.println("3. SILVER");
    System.out.println("4. NON-MEMBER");
    System.out.println("5. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 5).getAsInt();
  }

  // Sorting Menu
  public String displaySortMenu(String currentSort) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT GUESTS", 60);
    System.out.println("Current Sort : [ " + currentSort + " ]\n");
    System.out.println("1. Name (A -> Z)");
    System.out.println("2. Name (Z -> A)");
    System.out.println("3. Guest ID (Low -> High)");
    System.out.println("4. Guest ID (High -> Low)");
    System.out.println("5. Cancel\n");

    int choice = ConsoleUtil.getMenuInput("Choose option: ", 1, 5).getAsInt();
    switch (choice) {
      case 1:
        return "NAME (A -> Z)";
      case 2:
        return "NAME (Z -> A)";
      case 3:
        return "GUEST ID (LOW -> HIGH)";
      case 4:
        return "GUEST ID (HIGH -> LOW)";
      default:
        return null;
    }
  }

  // Guest Action Submenu
  public int displayGuestActionSubmenu(Guest guest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "GUEST ACTIONS: " + guest.getName() + " [" + guest.getGuestId() + "]");
    System.out.println("1. View Guest Details");
    System.out.println("2. View Billing History");
    System.out.println("3. View Reservation & Room History");
    System.out.println("4. Back to Manage Guest List\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // Action 1 — GUEST DETAILS
  public void displayGuestDetails(
      Guest guest,
      Member member,
      Reservation latestReservation,
      Billing latestBilling,
      int totalBookings) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST DETAILS", 104);
    // Guest Profile
    System.out.println(" GUEST PROFILE\n");

    int[] profileWidths = {12, 20, 18, 18, 22, 12};
    TableUtil.TableSettings profileSettings =
        new TableUtil.TableSettings(profileWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(profileSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"GUEST ID", "NAME", "IC NUMBER", "PASSPORT", "EMAIL", "PHONE"},
        profileSettings);
    TableUtil.printTableBorder(profileSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          guest.getGuestId(),
          guest.getName(),
          orNA(guest.getIcNumber()),
          orNA(guest.getPassportNumber()),
          orNA(guest.getEmail()),
          orNA(guest.getPhoneNumber())
        },
        profileSettings);
    TableUtil.printTableBorder(profileSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // Member Information
    System.out.println(" MEMBER INFORMATION\n");

    String memberId = orNA(guest.getMemberId());
    String memberLevel =
        (member != null && member.getTier() != null)
            ? member.getTier().name()
            : (guest.getMemberId() != null ? "N/A" : "NON-MEMBER");
    String memberPoints = (member != null) ? String.valueOf(member.getPoints()) : "N/A";

    int[] memberWidths = {20, 20, 20};
    TableUtil.TableSettings memberSettings =
        new TableUtil.TableSettings(memberWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(memberSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"MEMBER ID", "MEMBER LEVEL", "LOYALTY POINTS"}, memberSettings);
    TableUtil.printTableBorder(memberSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {memberId, memberLevel, memberPoints}, memberSettings);
    TableUtil.printTableBorder(memberSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // lastest booking
    System.out.println(" LATEST BOOKING  (Total Bookings: " + totalBookings + ")\n");

    String bookingId = (latestReservation != null) ? latestReservation.getReservationId() : "N/A";
    String confNum =
        (latestReservation != null) ? orNA(latestReservation.getConfirmationNumber()) : "N/A";
    String roomType =
        (latestReservation != null && latestReservation.getRoomType() != null)
            ? latestReservation.getRoomType().name()
            : "N/A";
    String resStatus = (latestReservation != null) ? latestReservation.getStatus().name() : "N/A";
    String roomNo = (latestBilling != null) ? orNA(latestBilling.getRoomNumber()) : "N/A";
    String checkIn = (latestBilling != null) ? formatDate(latestBilling.getCheckInDate()) : "N/A";
    String checkOut = (latestBilling != null) ? formatDate(latestBilling.getCheckOutDate()) : "N/A";
    String billStatus = (latestBilling != null) ? latestBilling.getStatus().name() : "N/A";

    int[] bookingWidths = {13, 13, 12, 14, 10, 14, 14, 10};
    TableUtil.TableSettings bookingSettings =
        new TableUtil.TableSettings(bookingWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(bookingSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "BOOKING ID", "CONFIRM NO.", "ROOM TYPE", "RES. STATUS",
          "ROOM NO.", "CHECK-IN", "CHECK-OUT", "BILL STATUS"
        },
        bookingSettings);
    TableUtil.printTableBorder(bookingSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          bookingId, confNum, roomType, resStatus, roomNo, checkIn, checkOut, billStatus
        },
        bookingSettings);
    TableUtil.printTableBorder(bookingSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    if (latestReservation == null) {
      System.out.println("  (No booking records found for this guest yet.)\n");
    }

    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // billing history
  public ConsoleUtil.GetMenuInputResult displayBillingHistory(
      Guest guest,
      ListInterface<Billing> historyNewToOld,
      LocalDate fromDate,
      LocalDate toDate,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "BILLING HISTORY: " + guest.getName() + " [" + guest.getGuestId() + "]", 110);

    System.out.println(
        "DATE FILTER (CHECK-IN)  : FROM [ "
            + (fromDate != null ? fromDate : "Any")
            + " ]  TO [ "
            + (toDate != null ? toDate : "Any")
            + " ]");
    System.out.println();

    ListInterface<Billing> history =
        (historyNewToOld != null) ? historyNewToOld : new ArrayList<>();
    int total = history.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 12, 9, 10, 16, 16, 8, 12};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.RIGHT);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "BILLING ID", "ROOM NO.", "ROOM TYPE",
          "CHECK-IN", "CHECK-OUT", "STATUS", "TOTAL (RM)"
        },
        settings);

    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {108}).setHAlign(0, TableUtil.Align.CENTER);
      String msg =
          (fromDate != null || toDate != null)
              ? "*** NO BILLING RECORDS MATCH DATE FILTER ***"
              : "*** NO BILLING RECORDS FOUND ***";
      TableUtil.printTableRow(new String[] {msg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[F] Date Filter     [C] Return\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'F', 'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = history.getEntry(i);
      if (b == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            b.getBillingId(),
            orNA(b.getRoomNumber()),
            (b.getRoomType() != null) ? b.getRoomType().name() : "N/A",
            formatDate(b.getCheckInDate()),
            formatDate(b.getCheckOutDate()),
            b.getStatus().name(),
            String.format("%.2f", b.getTotalAmount())
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("Select a row number to view the receipt.");
    System.out.println("[F] Date Filter     [P] Prev Page     [N] Next Page     [C] Return\n");

    int maxDisplayNum = endIndex - startIndex + 1;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Enter row or command (" + rangeStr + "): ",
        1,
        maxDisplayNum,
        new char[] {'F', 'N', 'P', 'C'});
  }

  public LocalDate[] promptDateFilter(LocalDate currentFrom, LocalDate currentTo) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("DATE FILTER — BILLING HISTORY", 60);
    System.out.println(
        "Current: FROM [ "
            + (currentFrom != null ? currentFrom : "Any")
            + " ]  TO [ "
            + (currentTo != null ? currentTo : "Any")
            + " ]");
    System.out.println();
    System.out.println("Format: YYYY-MM-DD   |   blank = keep current   |   '-' = clear\n");

    LocalDate from = promptDate("From date: ", currentFrom);
    LocalDate to = promptDate("To date  : ", currentTo);
    return new LocalDate[] {from, to};
  }

  private LocalDate promptDate(String prompt, LocalDate current) {
    String raw = ConsoleUtil.getStringInput(prompt);
    if (raw == null || raw.trim().isEmpty()) return current;
    if ("-".equals(raw.trim())) return null;
    try {
      return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      ConsoleUtil.printError("Invalid date format. Keeping previous value.");
      return current;
    }
  }

  // receipt
  public void displayReceipt(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("RECEIPT: " + billing.getBillingId(), 68);

    int[] kvWidths = {24, 42};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"Guest Name", guest.getName()}, kvSettings);
    printKvRow("Guest ID", guest.getGuestId(), kvSettings);
    printKvRow(
        "Room No. / Type",
        billing.getRoomNumber()
            + " ("
            + (billing.getRoomType() != null ? billing.getRoomType().name() : "N/A")
            + ")",
        kvSettings);
    printKvRow("Check-in Date", formatDate(billing.getCheckInDate()), kvSettings);
    printKvRow("Check-out Date", formatDate(billing.getCheckOutDate()), kvSettings);
    printKvRow("Nights Stayed", String.valueOf(billing.getNumberOfNights()), kvSettings);
    printKvRow("Rate / Night (RM)", String.format("%.2f", billing.getRatePerNight()), kvSettings);
    printKvRow("Subtotal (RM)", String.format("%.2f", billing.getSubtotal()), kvSettings);
    printKvRow(
        "SST (" + (int) (Billing.SST_RATE * 100) + "%) (RM)",
        String.format("%.2f", billing.getSstAmount()),
        kvSettings);
    printKvRow("TOTAL (RM)", String.format("%.2f", billing.getTotalAmount()), kvSettings);
    printKvRow("Billing Status", billing.getStatus().name(), kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // reservation history
  public ConsoleUtil.GetMenuInputResult displayReservationHistory(
      Guest guest, ListInterface<Reservation> historyNewToOld, int currentPage, int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "RESERVATION HISTORY: " + guest.getName() + " [" + guest.getGuestId() + "]", 93);

    ListInterface<Reservation> history =
        (historyNewToOld != null) ? historyNewToOld : new ArrayList<>();
    int total = history.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 12, 12, 12, 14, 22};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "CONFIRM NO.", "ROOM TYPE", "STATUS", "RESERVATION TIME"},
        settings);

    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {91}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO RESERVATION HISTORY FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      return ConsoleUtil.getMenuInput("Press 'C' to return: ", new char[] {'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = history.getEntry(i);
      if (r == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            r.getReservationId(),
            orNA(r.getConfirmationNumber()),
            (r.getRoomType() != null) ? r.getRoomType().name() : "N/A",
            r.getStatus().name(),
            formatDateTime(r.getReservationTime())
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("Select a row number to view the assigned room details.");
    System.out.println("[P] Prev Page     [N] Next Page     [C] Return\n");

    int maxDisplayNum = endIndex - startIndex + 1;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Enter row or command (" + rangeStr + "): ", 1, maxDisplayNum, new char[] {'N', 'P', 'C'});
  }

  public void displayAssignedRoomDetail(Guest guest, Reservation reservation, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM ASSIGNMENT DETAIL: " + reservation.getReservationId(), 80);

    int[] kvWidths = {24, 50};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    System.out.println(" RESERVATION INFO\n");
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Reservation ID", reservation.getReservationId()}, kvSettings);
    printKvRow("Confirmation No.", orNA(reservation.getConfirmationNumber()), kvSettings);
    printKvRow(
        "Room Type",
        reservation.getRoomType() != null ? reservation.getRoomType().name() : "N/A",
        kvSettings);
    printKvRow("Status", reservation.getStatus().name(), kvSettings);
    printKvRow("Reservation Time", formatDateTime(reservation.getReservationTime()), kvSettings);
    printKvRow("Check-out Time", formatDateTime(reservation.getCheckOutTime()), kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println(" ASSIGNED ROOM & BILLING\n");

    if (billing == null) {
      System.out.println("  (No room assignment / billing record linked to this reservation.)\n");
    } else {
      TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
      TableUtil.printTableRow(new String[] {"Billing ID", billing.getBillingId()}, kvSettings);
      printKvRow("Room No.", orNA(billing.getRoomNumber()), kvSettings);
      printKvRow(
          "Room Type",
          billing.getRoomType() != null ? billing.getRoomType().name() : "N/A",
          kvSettings);
      printKvRow("Check-in Date", formatDate(billing.getCheckInDate()), kvSettings);
      printKvRow("Check-out Date", formatDate(billing.getCheckOutDate()), kvSettings);
      printKvRow("Nights Stayed", String.valueOf(billing.getNumberOfNights()), kvSettings);
      printKvRow("Rate / Night (RM)", String.format("%.2f", billing.getRatePerNight()), kvSettings);
      printKvRow("Total (RM)", String.format("%.2f", billing.getTotalAmount()), kvSettings);
      printKvRow("Billing Status", billing.getStatus().name(), kvSettings);
      TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  /**
   * Prints a MIDDLE separator then a data row. Used for every row AFTER the first (which uses TOP +
   * first row directly).
   */
  private void printKvRow(String label, String value, TableUtil.TableSettings settings) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {label, value}, settings);
  }

  private String orNA(String s) {
    return (s != null && !s.isEmpty()) ? s : "N/A";
  }

  private String formatDate(LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
  }

  private String formatDateTime(LocalDateTime dt) {
    if (dt == null) return "N/A";
    return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a"));
  }
}
