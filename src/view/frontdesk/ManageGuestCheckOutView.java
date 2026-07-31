package view.frontdesk;

import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ManageGuestCheckOutView {
  private final GuestInformationView guestInformationView = new GuestInformationView();
  // Reused so the check-out receipt matches the one printed from Guest Information exactly.
  public GetMenuInputResult renderCheckOutScreen(
      ListInterface<Billing> activeStays,
      ListInterface<Guest> guestList,
      String guestIdFilter,
      String guestNameFilter,
      String roomNumberFilter,
      String paymentStatusFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE GUEST CHECK-OUT", 90);

    System.out.println(
        "GUEST ID FILTER   : [ " + (guestIdFilter == null ? "None" : guestIdFilter) + " ]");
    System.out.println(
        "GUEST NAME FILTER : [ " + (guestNameFilter == null ? "None" : guestNameFilter) + " ]");
    System.out.println(
        "ROOM NO. FILTER   : [ " + (roomNumberFilter == null ? "None" : roomNumberFilter) + " ]");
    System.out.println(
        "PAYMENT STATUS    : [ " + (paymentStatusFilter == null ? "ALL" : paymentStatusFilter) + " ]");
    System.out.println("SORT CRITERIA     : [ " + sortCriteria + " ]\n");

    int total = (activeStays == null) ? 0 : activeStays.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    int[] colWidths = {4, 10, 22, 10, 14, 14};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "GUEST ID", "GUEST NAME", "ROOM NO.", "CHECK-IN", "PAYMENT"},
        settings);

    // Guard covers both the "no data" case (total == 0) and, explicitly, a null list,
    // so the static analyzer can see activeStays is never null past this point.
    if (total == 0 || activeStays == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {74}).setHAlign(0, TableUtil.Align.CENTER);

      boolean hasFilters =
          guestIdFilter != null
              || guestNameFilter != null
              || roomNumberFilter != null
              || paymentStatusFilter != null;
      String emptyMsg =
          hasFilters
              ? "*** NO GUESTS FOUND FOR ACTIVE FILTERS ***"
              : "*** NO GUESTS CURRENTLY CHECKED IN ***";

      TableUtil.printTableRow(new String[] {emptyMsg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("\nPage 0 / 0 (Total Matches: 0)\n");
      System.out.println("[S] Search & Filter     [O] Change Sort Order   [R] Refresh Table");
      System.out.println("[E] Exit to Front Desk Menu\n");

      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'O', 'R', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      Billing b = activeStays.getEntry(i);
      if (b == null) continue;
      Guest g = findGuest(guestList, b.getGuestId());

      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            b.getGuestId(),
            (g != null ? g.getName() : "N/A"),
            b.getRoomNumber(),
            formatDate(b.getCheckInDate()),
            b.getStatus().name()
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf(
        "Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Search & Filter     [O] Change Sort Order   [R] Refresh Table");
    System.out.println("[P] Prev Page           [N] Next Page           [E] Exit to Front Desk Menu\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String rangeStr = (maxOptionNum == 1) ? "1" : "1-" + maxOptionNum;

    return ConsoleUtil.getMenuInput(
        "Select a guest number to process (" + rangeStr + "): ",
        1,
        maxOptionNum,
        new char[] {'S', 'O', 'R', 'P', 'N', 'E'});
  }

  public int displayFilterMainMenu(
      String guestIdFilter, String guestNameFilter, String roomNumberFilter, String paymentStatusFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & FILTER GUESTS");
    System.out.println("Guest ID       : [ " + (guestIdFilter == null ? "None" : guestIdFilter) + " ]");
    System.out.println("Guest Name     : [ " + (guestNameFilter == null ? "None" : guestNameFilter) + " ]");
    System.out.println("Room Number    : [ " + (roomNumberFilter == null ? "None" : roomNumberFilter) + " ]");
    System.out.println(
        "Payment Status : [ " + (paymentStatusFilter == null ? "ALL" : paymentStatusFilter) + " ]\n");

    System.out.println("1. Search by Guest ID");
    System.out.println("2. Search by Guest Name");
    System.out.println("3. Search by Room Number");
    System.out.println("4. Filter by Payment Status");
    System.out.println("5. Clear All Filters");
    System.out.println("6. Apply and Back to Guest List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String promptGuestIdFilterInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH BY GUEST ID");
    return ConsoleUtil.getStringInput("Enter Guest ID [Enter/C to clear]: ");
  }

  public String promptGuestNameFilterInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH BY GUEST NAME");
    return ConsoleUtil.getStringInput("Enter Guest Name [Enter/C to clear]: ");
  }

  public String promptRoomNumberFilterInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH BY ROOM NUMBER");
    return ConsoleUtil.getStringInput("Enter Room Number [Enter/C to clear]: ");
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

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Guest Name (A -> Z)");
    System.out.println("2. Room Number (Low -> High)");
    System.out.println("3. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
    if (choice == 1) return "GUEST NAME (A -> Z)";
    if (choice == 2) return "ROOM NUMBER (LOW -> HIGH)";
    return null;
  }

  public int displayGuestCheckOutSubmenu(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox(
        "CHECK-OUT: "
            + (guest != null ? guest.getName() : "GUEST")
            + " ["
            + (billing != null ? billing.getRoomNumber() : "N/A")
            + "]");

    int[] kvWidths = {20, 40};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "Guest",
          guest != null ? guest.getName() + " (" + guest.getGuestId() + ")" : "N/A"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Room No.", billing != null ? billing.getRoomNumber() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Check-in / Check-out",
          billing != null
              ? formatDate(billing.getCheckInDate()) + " -> " + formatDate(billing.getCheckOutDate())
              : "N/A"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Payment Status", billing != null ? billing.getStatus().name() : "N/A"},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\n-- Payment Processing --");
    System.out.println("1. Calculate Room Charges");
    System.out.println("2. Record Payment");
    System.out.println("3. Print Receipt");
    System.out.println("\n-- Check-Out Actions --");
    System.out.println("4. Complete Check-Out");
    System.out.println("5. Back to Guest List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public void displayRoomCharges(Guest guest, Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM CHARGES: " + billing.getBillingId());

    int[] kvWidths = {22, 40};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "Room No. / Type", billing.getRoomNumber() + " (" + billing.getRoomType().name() + ")"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Check-in", formatDate(billing.getCheckInDate())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Check-out (Today)", formatDate(billing.getCheckOutDate())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Nights Stayed", String.valueOf(billing.getNumberOfNights())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Rate / Night (RM)", String.format("%.2f", billing.getRatePerNight())},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Subtotal (RM)", String.format("%.2f", billing.getSubtotal())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "SST (" + (int) (Billing.SST_RATE * 100) + "%) (RM)",
          String.format("%.2f", billing.getSstAmount())
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"TOTAL DUE (RM)", String.format("%.2f", billing.getTotalAmount())},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public void displayPaymentRecorded(Billing billing) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PAYMENT RECORDED");
    System.out.println("Billing " + billing.getBillingId() + " marked as PAID.");
    System.out.printf("Amount Collected (RM): %.2f\n\n", billing.getTotalAmount());
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public void displayReceipt(Guest guest, Billing billing) {
    guestInformationView.displayReceipt(guest, billing);
  }

  public void displayCheckOutSuccess(Guest guest, Billing billing, Room room) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-OUT COMPLETE");

    int[] fullWidth = {81};
    int[] kvWidths = {20, 60};
    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: GUEST CHECKED-OUT"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Guest Name", guest != null ? guest.getName() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Room Number", room != null ? room.getRoomNumber() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Room Status", "DIRTY (Pending Housekeeping)"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Total Paid (RM)", String.format("%.2f", billing.getTotalAmount())},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nGuest has been removed from the active stay list.\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  private String formatDate(LocalDate date) {
    if (date == null) return "N/A";
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
