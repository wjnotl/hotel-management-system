package view.frontdesk;

import adt.ArrayList;
import entity.Billing;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ManageReservationView {

  // =========================================================================
  // DTO — view only renders pre-processed data, never entity lookups
  // =========================================================================

  public static class ReservationRowDTO {
    public final String billingId;
    public final String guestId;
    public final String guestName;
    public final String roomNumber;
    public final String roomType;
    public final String reservationId;
    public final String confirmationNumber;
    public final String checkInDate;
    public final String checkOutDate;
    public final String paymentStatus;
    public final String stayStatus;

    public ReservationRowDTO(
        String billingId,
        String guestId,
        String guestName,
        String roomNumber,
        String roomType,
        String reservationId,
        String confirmationNumber,
        String checkInDate,
        String checkOutDate,
        String paymentStatus,
        String stayStatus) {
      this.billingId = billingId;
      this.guestId = orNA(guestId);
      this.guestName = orNA(guestName);
      this.roomNumber = orNA(roomNumber);
      this.roomType = orNA(roomType);
      this.reservationId = orNA(reservationId);
      this.confirmationNumber = orNA(confirmationNumber);
      this.checkInDate = orNA(checkInDate);
      this.checkOutDate = orNA(checkOutDate);
      this.paymentStatus = orNA(paymentStatus);
      this.stayStatus = orNA(stayStatus);
    }

    private static String orNA(String s) {
      return (s != null && !s.isEmpty()) ? s : "N/A";
    }
  }

  // =========================================================================
  // MAIN LIST SCREEN
  // =========================================================================

  public GetMenuInputResult renderReservationScreen(
      ArrayList<ReservationRowDTO> list,
      String searchQuery,
      String roomTypeFilter,
      String paymentStatusFilter,
      String stayStatusFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE RESERVATION", 135);

    System.out.println(
        "SEARCH FILTER   : [ "
            + (searchQuery == null || searchQuery.isEmpty() ? "None" : "\"" + searchQuery + "\"")
            + " ]");
    System.out.println(
        "ROOM TYPE       : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println(
        "PAYMENT STATUS  : [ "
            + (paymentStatusFilter == null ? "ALL" : paymentStatusFilter)
            + " ]");
    System.out.println(
        "STAY STATUS     : [ " + (stayStatusFilter == null ? "ALL" : stayStatusFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]");
    System.out.println();

    int total = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 10, 18, 8, 9, 12, 12, 9, 13};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.LEFT)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.CENTER)
            .setHAlign(9, TableUtil.Align.CENTER)
            .setTruncate(3);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "RES. ID", "GUEST ID", "GUEST NAME", "ROOM NO.",
          "ROOM TYPE", "CHECK-IN", "CHECK-OUT", "PAYMENT", "STATUS"
        },
        settings);

    if (total == 0 || list == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {132}).setHAlign(0, TableUtil.Align.CENTER);
      boolean hasFilters =
          searchQuery != null
              || roomTypeFilter != null
              || paymentStatusFilter != null
              || stayStatusFilter != null;
      String msg =
          hasFilters
              ? "*** NO RESERVATIONS MATCH ACTIVE FILTERS ***"
              : "*** NO RESERVATION RECORDS FOUND ***";
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
      ReservationRowDTO dto = list.getEntry(i);
      if (dto == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            dto.reservationId,
            dto.guestId,
            dto.guestName,
            dto.roomNumber,
            dto.roomType,
            formatDate(dto.checkInDate),
            formatDate(dto.checkOutDate),
            dto.paymentStatus,
            dto.stayStatus
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

  // =========================================================================
  // FILTER MENU
  // =========================================================================

  public int displayFilterMenu(
      String search, String roomType, String paymentStatus, String stayStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & FILTER", 60);
    System.out.println(
        "Search Query   : [ "
            + (search == null || search.isEmpty() ? "None" : "\"" + search + "\"")
            + " ]");
    System.out.println("Room Type      : [ " + (roomType == null ? "ALL" : roomType) + " ]");
    System.out.println(
        "Payment Status : [ " + (paymentStatus == null ? "ALL" : paymentStatus) + " ]");
    System.out.println("Stay Status    : [ " + (stayStatus == null ? "ALL" : stayStatus) + " ]\n");
    System.out.println("1. Search)");
    System.out.println("2. Filter by Room Type");
    System.out.println("3. Filter by Payment Status");
    System.out.println("4. Filter by Stay Status");
    System.out.println("5. Clear All Filters");
    System.out.println("6. Apply & Back\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 6).getAsInt();
  }

  public int displayStayStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY STAY STATUS", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. CHECKED_IN   (currently staying)");
    System.out.println("2. CHECKED_OUT  (already left)");
    System.out.println("3. PENDING      (no billing yet)");
    System.out.println("4. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  public String promptSearchInput(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH", 60);
    System.out.println(
        "Current: [ "
            + (current == null || current.isEmpty() ? "None" : "\"" + current + "\"")
            + " ]");
    System.out.println();
    System.out.println(
        "Can Search by Guest ID, Guest Name, Room No., Res. ID, or Confirmation No.");
    System.out.println("Press Enter to clear/ C to cancel.\n");
    String input = ConsoleUtil.getStringInput("Search: ");
    if (input == null || "C".equalsIgnoreCase(input.trim())) return current;
    return input.trim().isEmpty() ? null : input.trim();
  }

  public int displayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM TYPE", 50);
    System.out.println("Current: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  public int displayPaymentStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY PAYMENT STATUS", 50);
    System.out.println("Current: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. PAID Only");
    System.out.println("2. UNPAID Only");
    System.out.println("3. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 3).getAsInt();
  }

  // =========================================================================
  // SORT MENU
  // =========================================================================

  /** Returns selected sort string, or null if cancelled. */
  public String displaySortMenu(String currentSort) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT ORDER", 60);
    System.out.println("Current : [ " + currentSort + " ]\n");
    System.out.println("1. Guest Name      (A -> Z)");
    System.out.println("2. Guest Name      (Z -> A)");
    System.out.println("3. Room Number     (Low -> High)");
    System.out.println("4. Room Number     (High -> Low)");
    System.out.println("5. Check-in        (Earliest First)");
    System.out.println("6. Check-in        (Latest First)");
    System.out.println("7. Payment Status");
    System.out.println("8. Cancel\n");

    int choice = ConsoleUtil.getMenuInput("Choose option: ", 1, 8).getAsInt();
    switch (choice) {
      case 1:
        return "GUEST NAME (A -> Z)";
      case 2:
        return "GUEST NAME (Z -> A)";
      case 3:
        return "ROOM NUMBER (LOW -> HIGH)";
      case 4:
        return "ROOM NUMBER (HIGH -> LOW)";
      case 5:
        return "CHECK-IN (EARLIEST FIRST)";
      case 6:
        return "CHECK-IN (LATEST FIRST)";
      case 7:
        return "PAYMENT STATUS";
      default:
        return null;
    }
  }

  // =========================================================================
  // CREATE BILLING DTO
  // =========================================================================

  public static class CreateBillingInputDTO {
    public final LocalDate checkInDate;
    public final LocalDate checkOutDate;

    public CreateBillingInputDTO(LocalDate checkInDate, LocalDate checkOutDate) {
      this.checkInDate = checkInDate;
      this.checkOutDate = checkOutDate;
    }
  }

  // =========================================================================
  // NO BILLING NOTICE — shown when room is OCCUPIED but has no billing record
  // Returns: 1 = Create Billing, 2 = Back
  // =========================================================================

  public int displayNoBillingNotice(ReservationRowDTO dto) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NO BILLING RECORD — " + dto.roomNumber, 72);

    int[] kvWidths = {22, 46};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"Guest ID", dto.guestId}, kvSettings);
    printKvRow("Guest Name", dto.guestName, kvSettings);
    printKvRow("Room Number", dto.roomNumber, kvSettings);
    printKvRow("Room Type", dto.roomType, kvSettings);
    printKvRow("Reservation ID", dto.reservationId, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println("  This guest has no billing record yet.");
    System.out.println("  You can create one now to process payment and check-out.\n");
    System.out.println("1. Create Billing Record");
    System.out.println("2. Back\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 2).getAsInt();
  }

  // =========================================================================
  // CREATE BILLING PROMPTS
  // =========================================================================

  public CreateBillingInputDTO promptCreateBilling(
      ReservationRowDTO dto, LocalDate defaultCheckIn, LocalDate defaultCheckOut, double rate) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CREATE BILLING RECORD", 68);
    System.out.println("Guest  : " + dto.guestName + "  [" + dto.guestId + "]");
    System.out.println("Room   : " + dto.roomNumber + "  (" + dto.roomType + ")");
    System.out.printf("Rate   : RM %.2f / night  (fixed room rate)%n", rate);
    System.out.println();
    System.out.println("Format: YYYY-MM-DD   |   blank = use suggested value   |   C = cancel\n");

    LocalDate checkIn = promptDate("Check-in  date [" + defaultCheckIn + "]: ", defaultCheckIn);
    if (checkIn == null) return null;

    LocalDate checkOut = promptDate("Check-out date [" + defaultCheckOut + "]: ", defaultCheckOut);
    if (checkOut == null) return null;

    if (!checkOut.isAfter(checkIn)) {
      ConsoleUtil.printError("Check-out must be after check-in. Billing creation cancelled.");
      return null;
    }

    long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    double total = nights * rate * (1 + entity.Billing.SST_RATE);
    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            String.format(
                "Create billing: %d night(s) × RM %.2f = RM %.2f (incl. 8%% SST)?",
                nights, rate, total));
    if (!confirmed) return null;

    return new CreateBillingInputDTO(checkIn, checkOut);
  }

  public void displayBillingCreated(entity.Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BILLING RECORD CREATED", 68);
    System.out.println("Billing ID  : " + billing.getBillingId());
    System.out.println("Room        : " + billing.getRoomNumber());
    System.out.printf("Total (RM)  : %.2f%n", billing.getTotalAmount());
    System.out.println("Status      : UNPAID\n");
    ConsoleUtil.printContinueMessage("Press Enter to continue...");
  }

  // =========================================================================
  // GUEST ACTIONS SUBMENU
  // =========================================================================

  public int displayGuestActionsSubmenu(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "RESERVATION: "
            + (guest != null ? guest.getName() : "GUEST")
            + " ["
            + (billing != null ? billing.getRoomNumber() : "N/A")
            + "]",
        80);

    int[] kvWidths = {22, 50};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    String checkInOut =
        (billing != null)
            ? formatDate(billing.getCheckInDate())
                + "  ->  "
                + formatDate(billing.getCheckOutDate())
            : "N/A";

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "Guest", guest != null ? guest.getName() + "  (" + guest.getGuestId() + ")" : "N/A"
        },
        kvSettings);
    printKvRow(
        "Room No. / Type",
        billing != null
            ? billing.getRoomNumber()
                + "  ("
                + (billing.getRoomType() != null ? billing.getRoomType().name() : "N/A")
                + ")"
            : "N/A",
        kvSettings);
    printKvRow("Check-in / Check-out", checkInOut, kvSettings);
    printKvRow("Payment Status", billing != null ? billing.getStatus().name() : "N/A", kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("1. Calculate Room Charges");
    System.out.println("2. Record Payment");
    System.out.println("3. Print Receipt");
    System.out.println("4. Stay Extension");
    System.out.println("5. Complete Check-Out");
    System.out.println("6. Back to Reservation List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  // =========================================================================
  // ROOM CHARGES
  // =========================================================================

  public void displayRoomCharges(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM CHARGES: " + billing.getBillingId(), 68);

    int[] kvWidths = {24, 42};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "Room No. / Type",
          billing.getRoomNumber()
              + "  ("
              + (billing.getRoomType() != null ? billing.getRoomType().name() : "N/A")
              + ")"
        },
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
    printKvRow("TOTAL DUE (RM)", String.format("%.2f", billing.getTotalAmount()), kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // PAYMENT RECORDED
  // =========================================================================

  public void displayPaymentRecorded(Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PAYMENT RECORDED", 60);
    System.out.println("Billing " + billing.getBillingId() + " marked as PAID.");
    System.out.printf("Amount Collected (RM): %.2f%n%n", billing.getTotalAmount());
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // RECEIPT
  // =========================================================================

  public void displayReceipt(Guest guest, Billing billing) {
    new ManageGuestView().displayReceipt(guest, billing);
  }

  // =========================================================================
  // STAY EXTENSION
  // =========================================================================

  public Integer promptStayExtension(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STAY EXTENSION", 68);

    int[] kvWidths = {24, 42};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Guest", guest != null ? guest.getName() : "N/A"}, kvSettings);
    printKvRow("Room No.", billing != null ? billing.getRoomNumber() : "N/A", kvSettings);
    printKvRow(
        "Current Check-out",
        formatDate(billing != null ? billing.getCheckOutDate() : null),
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    System.out.println("Enter the number of extra days to extend the stay.");
    System.out.println("Enter 0 or C to cancel.\n");

    String input = ConsoleUtil.getStringInput("Extra days (1-30): ");
    if (input == null || "C".equalsIgnoreCase(input.trim()) || "0".equals(input.trim())) {
      return null;
    }
    try {
      int days = Integer.parseInt(input.trim());
      if (days < 1 || days > 30) {
        ConsoleUtil.printError("Please enter a value between 1 and 30.");
        return null;
      }
      LocalDate newCheckOut =
          (billing != null && billing.getCheckOutDate() != null)
              ? billing.getCheckOutDate().plusDays(days)
              : null;
      boolean confirmed =
          ConsoleUtil.showConfirmMessage(
              "Extend stay by "
                  + days
                  + " day(s)?"
                  + (newCheckOut != null ? "  New check-out: " + formatDate(newCheckOut) : ""));
      if (!confirmed) return null;
      return days;
    } catch (NumberFormatException e) {
      ConsoleUtil.printError("Invalid input. Extension cancelled.");
      return null;
    }
  }

  public void displayStayExtensionSuccess(Guest guest, Billing billing, int extraDays) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STAY EXTENDED", 68);
    System.out.println(
        "Stay extended by "
            + extraDays
            + " day(s) for "
            + (guest != null ? guest.getName() : "guest")
            + ".");
    System.out.println("New check-out date : " + formatDate(billing.getCheckOutDate()));
    System.out.println("Payment Status     : UNPAID  (reset — new total must be settled)\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // CHECK-OUT SUCCESS
  // =========================================================================

  public void displayCheckOutSuccess(Guest guest, Billing billing, Room room) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-OUT COMPLETE", 72);

    int[] kvWidths = {24, 46};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Guest Name", guest != null ? guest.getName() : "N/A"}, kvSettings);
    printKvRow("Guest ID", guest != null ? guest.getGuestId() : "N/A", kvSettings);
    printKvRow("Room Number", room != null ? room.getRoomNumber() : "N/A", kvSettings);
    printKvRow("Room Status", "DIRTY  (Pending Housekeeping)", kvSettings);
    printKvRow("Check-out Date", formatDate(billing.getCheckOutDate()), kvSettings);
    printKvRow("Total Paid (RM)", String.format("%.2f", billing.getTotalAmount()), kvSettings);
    printKvRow("Reservation", "Status → CHECKED_OUT  |  Confirmation cleared", kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nGuest has been removed from the active reservation list.\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  private LocalDate promptDate(String prompt, LocalDate defaultVal) {
    String raw = ConsoleUtil.getStringInput(prompt);
    if (raw == null || "C".equalsIgnoreCase(raw.trim())) return null;
    if (raw.trim().isEmpty()) return defaultVal;
    try {
      return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      ConsoleUtil.printError("Invalid date format. Using suggested value.");
      return defaultVal;
    }
  }

  private void printKvRow(String label, String value, TableUtil.TableSettings settings) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {label, value}, settings);
  }

  private String formatDate(LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
  }

  private String formatDate(String dateStr) {
    if (dateStr == null || "N/A".equals(dateStr)) return "N/A";
    try {
      return LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    } catch (Exception e) {
      return dateStr;
    }
  }
}
