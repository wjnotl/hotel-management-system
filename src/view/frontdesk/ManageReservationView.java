package view.frontdesk;

import entity.Billing;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ManageReservationView {

  // DTO
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

  // manage reservation list
  public GetMenuInputResult renderReservationScreen(
      ReservationRowDTO[] pageRows,
      int totalCount,
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

    int total = totalCount;
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

    if (total == 0 || pageRows == null) {
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

    for (int i = 0; i < pageRows.length; i++) {
      ReservationRowDTO dto = pageRows[i];
      if (dto == null) continue;
      int displayNum = i + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            dto.reservationId,
            dto.guestId,
            dto.guestName,
            dto.roomNumber,
            dto.roomType,
            dto.checkInDate,
            dto.checkOutDate,
            dto.paymentStatus,
            dto.stayStatus
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Search & Filter     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page           [N] Next Page       [E] Exit to Front Desk\n");

    int maxDisplayNum = pageRows.length;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Select row or command (" + rangeStr + "): ",
        1,
        maxDisplayNum,
        new char[] {'S', 'O', 'R', 'N', 'P', 'E'});
  }

  // filter menu
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
    System.out.println("1. Search");
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

  // sort menu
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

  // no billing notice
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

  // create billing record
  public String[] promptCreateBillingDatesRaw(
      ReservationRowDTO dto, LocalDate defaultCheckIn, LocalDate defaultCheckOut, double rate) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CREATE BILLING RECORD", 68);
    System.out.println("Guest  : " + dto.guestName + "  [" + dto.guestId + "]");
    System.out.println("Room   : " + dto.roomNumber + "  (" + dto.roomType + ")");
    System.out.printf("Rate   : RM %.2f / night  (fixed room rate)%n", rate);
    System.out.println();
    System.out.println(
        "Format: YYYY-MM-DD   |   Press 'Enter' = use suggested value   |   C = cancel\n");

    String checkIn = ConsoleUtil.getStringInput("Check-in  date [" + defaultCheckIn + "]: ");
    String checkOut = ConsoleUtil.getStringInput("Check-out date [" + defaultCheckOut + "]: ");
    return new String[] {checkIn, checkOut};
  }

  public boolean confirmCreateBilling(long nights, double rate, double total) {
    return ConsoleUtil.showConfirmMessage(
        String.format(
            "Create billing: %d night(s) × RM %.2f = RM %.2f (incl. 8%% SST)?",
            nights, rate, total));
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

  // guest action submenu
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

  // room change
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
    printKvRow("Total Nights", String.valueOf(billing.getNumberOfNights()), kvSettings);
    printKvRow("Nights Paid", String.valueOf(billing.getPaidNights()), kvSettings);
    printKvRow("Nights Due", String.valueOf(billing.getOutstandingNights()), kvSettings);
    printKvRow(
        "Subtotal (RM)", String.format("%.2f", billing.getOutstandingSubtotal()), kvSettings);
    printKvRow("SST (8%) (RM)", String.format("%.2f", billing.getOutstandingSST()), kvSettings);
    printKvRow("TOTAL DUE (RM)", String.format("%.2f", billing.getOutstandingTotal()), kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // PAYMENT RECORDED
  // =========================================================================

  public void displayPaymentRecorded(Billing billing, double amountCollected) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PAYMENT RECORDED", 60);
    System.out.println("Billing " + billing.getBillingId() + " marked as PAID.");
    System.out.printf("Amount Collected (RM): %.2f%n%n", amountCollected);
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

  /** Returns the raw typed string, or null/blank/"0"/"C" to cancel; the controller parses it. */
  public String promptStayExtensionInput(Guest guest, Billing billing) {
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

    return ConsoleUtil.getStringInput("Extra days (1-30): ");
  }

  public boolean confirmStayExtension(int days, LocalDate newCheckOut) {
    return ConsoleUtil.showConfirmMessage(
        "Extend stay by "
            + days
            + " day(s)?"
            + (newCheckOut != null ? "  New check-out: " + formatDate(newCheckOut) : ""));
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
    printKvRow("Reservation", "Status → CHECKED_OUT", kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nGuest has been removed from the active reservation list.\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  private void printKvRow(String label, String value, TableUtil.TableSettings settings) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {label, value}, settings);
  }

  private String formatDate(LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
  }
}
