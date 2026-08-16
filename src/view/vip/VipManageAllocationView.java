package view.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class VipManageAllocationView {

  public GetMenuInputResult renderAllocationScreen(
      ListInterface<AllocationEntry> list,
      ListInterface<Reservation> reservationList,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      String search,
      String tier,
      String sort,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE ALLOCATION (PENDING CHECK-IN)");

    System.out.println(
        "SEARCH QUERY   : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println("TIER FILTER    : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("SORT CRITERIA  : [ " + sort + " ]");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    boolean hasActiveFilters = (search != null || tier != null);

    int[] columnWidths = {4, 11, 22, 10, 15, 18};

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "TIER", "ROOM ASSIGNED", "GRACE TIMER"},
        headerSettings);

    // WHEN 0 ALLOCATION MATCHES RETURNED:
    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      // Width 85 matches total grid width (4+11+22+10+15+18 = 80 + 5 internal walls)
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {85}).setHAlign(0, TableUtil.Align.CENTER);

      String emptyMsg =
          hasActiveFilters
              ? "*** NO PENDING ALLOCATIONS FOUND FOR ACTIVE FILTERS ***"
              : "*** NO ROOM ALLOCATIONS PENDING ***";

      TableUtil.printTableRow(new String[] {emptyMsg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("Page 0 / 0 (Total Allocated Matches: 0)\n");

      // SCENARIO 1: Filters Active -> Allow staff to adjust/clear filters or refresh
      if (hasActiveFilters) {
        System.out.println("[S] Search / Filter    [R] Refresh Table       [E] Exit to VIP Menu\n");
        return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'R', 'E'});
      }

      // SCENARIO 2: Holding Bay is completely empty -> Remove search, filter, and
      // sort options
      else {
        System.out.println("[R] Refresh Table      [E] Exit to VIP Menu\n");
        return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'R', 'E'});
      }
    }

    // NORMAL TABLE DISPLAY (When pending allocations > 0)
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      AllocationEntry entry = list.getEntry(i);
      if (entry == null) continue;

      Reservation reservation =
          reservationList.find(
              r -> entry.getReservationId().equalsIgnoreCase(r.getReservationId()));

      Guest guest = (reservation != null) ? findGuest(guestList, reservation.getGuestId()) : null;
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? findMember(memberList, guest.getMemberId())
              : null;

      int displayNum = i - startIndex + 1;
      String resId = entry.getReservationId();
      String guestName = (guest != null) ? guest.getName() : "N/A";
      String tierStr = (member != null) ? member.getTier().name() : "NON-MEMBER";
      String roomAssigned = "Room " + entry.getAssignedRoomNumber();
      String graceTimer = formatTimerCountdown(entry.getExpirationTimestamp());

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum), resId, guestName, tierStr, roomAssigned, graceTimer
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(
        "Page %d / %d (Total Allocated Matches: %d)\n\n", currentPage, totalPages, totalMatches);
    System.out.println("[S] Search Guests      [O] Change Sort Order   [R] Refresh Table");

    StringBuilder navLine = new StringBuilder();
    ArrayList<Character> validList = new ArrayList<>();
    validList.add('S');
    validList.add('O');
    validList.add('R');
    validList.add('E');

    if (currentPage > 1) {
      navLine.append("[P] Prev Page          ");
      validList.add('P');
    }
    if (currentPage < totalPages) {
      navLine.append("[N] Next Page          ");
      validList.add('N');
    }

    if (navLine.length() > 0) {
      System.out.println(navLine.toString().trim() + "           [E] Exit to VIP Menu\n");
    } else {
      System.out.println("[E] Exit to VIP Menu\n");
    }

    char[] validChars = new char[validList.getNumberOfEntries()];
    for (int i = 1; i <= validList.getNumberOfEntries(); i++) {
      validChars[i - 1] = validList.getEntry(i);
    }

    int maxOptionNum = endIndex - startIndex + 1;
    String rangeStr = (maxOptionNum == 1) ? "1" : "1-" + maxOptionNum;
    String promptText = "Select a pending guest number to handle (" + rangeStr + "): ";

    return ConsoleUtil.getMenuInput(promptText, 1, maxOptionNum, validChars);
  }

  public int displayAllocationDetailScreen(
      AllocationEntry entry, Reservation r, Guest g, Member m) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALLOCATION DETAILS & SETTLEMENT");

    int[] fullWidth = {81};
    int[] kvWidths = {22, 58};

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"HOLDING BAY ALLOCATION SUMMARY"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Reservation ID", (r != null ? r.getReservationId() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest Name", (g != null ? g.getName() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Phone Number", (g != null ? g.getPhoneNumber() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Membership Tier", (m != null ? m.getTier().name() : "NON-MEMBER")},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Assigned Room", "Room " + (entry != null ? entry.getAssignedRoomNumber() : "N/A")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Current Strikes", (g != null ? g.getStrikeCount() : 0) + " strikes"},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Grace Countdown",
          formatTimerCountdown(entry != null ? entry.getExpirationTimestamp() : 0)
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nSelect an action to proceed:");
    System.out.println(
        "1. Confirm Allocate (Guest arrived, prompt stay duration & complete check-in)");
    System.out.println("2. Cancel Allocation (Guest no-show / resolve penalty path)");
    System.out.println("3. Go Back to Allocation List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayCancelResolutionMenu(Guest g, Member m, int maxStrikes) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CANCEL ALLOCATION RESOLUTION PATHS");

    int[] fullWidth = {81};
    int[] kvWidths = {20, 60};

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"NO-SHOW EVICTION PROCESSING"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {
          "Target Profile",
          (g != null ? g.getName() : "N/A") + " (" + (g != null ? g.getGuestId() : "N/A") + ")"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Member Tier", (m != null ? m.getTier().name() : "NON-MEMBER")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Strike Count", String.valueOf(g != null ? g.getStrikeCount() : 0) + " / " + maxStrikes
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nSelect how to handle the no-show guest:");
    System.out.println("1. Issue Strike & Send Back to Waitlist Queue");
    System.out.println(
        "   -> Increments strike count by 1. Re-enters queue using calculated score.");
    System.out.println(
        "   -> Note: Reaching " + maxStrikes + " strikes triggers automatic eviction lockout.\n");
    System.out.println("2. Issue Strike ONLY (Do NOT Re-queue)");
    System.out.println(
        "   -> Increments strike count by 1. Cancels reservation without re-entering queue.\n");
    System.out.println("3. Re-queue ONLY (No Strike Issued)");
    System.out.println(
        "   -> Returns reservation to waitlist queue without penalizing with a strike.\n");
    System.out.println("4. Evict & Remove Guest Entirely From System");
    System.out.println("   -> Cancels reservation permanently and frees the room.\n");
    System.out.println("5. Back to Allocation Detail Screen\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public void displayCheckInSuccessScreen(Reservation r, Guest g, Room room) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-IN COMPLETE");

    int[] fullWidth = {81};
    int[] kvWidths = {20, 60};

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: GUEST CHECKED-IN TO ROOM"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Reservation ID", r.getReservationId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest Name", (g != null ? g.getName() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Room Number", (room != null ? room.getRoomNumber() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Room Status", "OCCUPIED"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public int displayFilterMainMenu(String search, String tier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & ALLOCATION FILTERS");
    System.out.println("Active Search  : [ " + (search == null ? "None" : search) + " ]");
    System.out.println("Active Tier    : [ " + (tier == null ? "ALL" : tier) + " ]\n");

    System.out.println("1. Text Search Submenu");
    System.out.println("2. Loyalty Tier Submenu");
    System.out.println("3. Reset All Filters");
    System.out.println("4. Apply and Return");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY");
    System.out.println("Searchable Fields: Reservation ID, Guest Name, Room Number");
    System.out.println(
        "Current Search   : [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    System.out.println("1. Enter Search Term");
    System.out.println("2. Clear Search Term");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY");
    System.out.println("Searchable Fields: Reservation ID, Guest Name, Room Number");
    System.out.println(
        "Current Search   : [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    return ConsoleUtil.getStringInput("Enter search term (Res ID / Guest Name / Room No): ");
  }

  public void displayRequeuedWithoutStrikeScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: GUEST RE-QUEUED WITHOUT STRIKE");
    System.out.println(
        "Guest "
            + (guest != null ? guest.getName() : "N/A")
            + " was re-queued in the waitlist without a strike penalty.");
    System.out.println("Room holding reservation has been released.\n");
    ConsoleUtil.printContinueMessage();
  }

  public int displayTierSubmenu(String currentTier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER ALLOCATIONS BY TIER");
    System.out.println(
        "Current Selected Tier: [ " + (currentTier == null ? "ALL" : currentTier) + " ]\n");

    System.out.println("1. DIAMOND ONLY");
    System.out.println("2. GOLD ONLY");
    System.out.println("3. SILVER ONLY");
    System.out.println("4. Show All Tiers\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Grace Timer (Low -> High | Expiring Soonest)");
    System.out.println("2. Grace Timer (High -> Low | Most Time Left)");
    System.out.println("3. VIP Tier Rank (Diamond -> Silver)");
    System.out.println("4. VIP Tier Rank (Silver -> Diamond)");
    System.out.println("5. Guest Name (A -> Z)");
    System.out.println("6. Guest Name (Z -> A)");
    System.out.println("7. Room Number (Low -> High)");
    System.out.println("8. Room Number (High -> Low)");
    System.out.println("9. Reservation ID (Low -> High)");
    System.out.println("10. Reservation ID (High -> Low)");
    System.out.println("11. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 11).getAsInt();
    if (choice == 1) return "TIME REMAINING (LOW -> HIGH)";
    if (choice == 2) return "TIME REMAINING (HIGH -> LOW)";
    if (choice == 3) return "TIER RANK (DIAMOND -> SILVER)";
    if (choice == 4) return "TIER RANK (SILVER -> DIAMOND)";
    if (choice == 5) return "GUEST NAME (A -> Z)";
    if (choice == 6) return "GUEST NAME (Z -> A)";
    if (choice == 7) return "ROOM NUMBER (LOW -> HIGH)";
    if (choice == 8) return "ROOM NUMBER (HIGH -> LOW)";
    if (choice == 9) return "RESERVATION ID (LOW -> HIGH)";
    if (choice == 10) return "RESERVATION ID (HIGH -> LOW)";
    return null;
  }

  public Integer promptStayDuration(AllocationEntry entry, Guest guest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ALLOCATION & CHECK-IN");
    System.out.println(" Target Guest   : " + (guest != null ? guest.getName() : "N/A"));
    System.out.println(" Room Assigned  : Room " + entry.getAssignedRoomNumber());
    System.out.println("------------------------------------------------------");
    System.out.println(" Press ENTER or 'C' to Cancel & Return\n");

    return ConsoleUtil.getIntegerInput(
        " Enter Duration of Stay (Number of Days/Nights) [1 - 30]: ", 1, 30);
  }

  public void displayEvictionLockoutScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: EVICTION LOCKOUT ENFORCED");
    System.out.println(
        "Guest "
            + (guest != null ? guest.getName() : "N/A")
            + " has accumulated "
            + (guest != null ? guest.getStrikeCount() : 0)
            + " strikes (MAX LIMIT REACHED).");
    System.out.println("Cannot re-enter queue. Reservation evicted and room freed.\n");
    ConsoleUtil.printContinueMessage();
  }

  public void displayStrikeIssuedScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: STRIKE ISSUED & RE-QUEUED");
    System.out.println(
        "Guest "
            + (guest != null ? guest.getName() : "N/A")
            + " strike count is now "
            + (guest != null ? guest.getStrikeCount() : 0)
            + ".");
    System.out.println("Reservation re-entered waitlist queue.\n");
    ConsoleUtil.printContinueMessage();
  }

  public void displayStrikeIssuedWithoutRequeueScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: STRIKE ISSUED (RESERVATION CANCELLED)");
    System.out.println(
        "Guest "
            + (guest != null ? guest.getName() : "N/A")
            + " strike count is now "
            + (guest != null ? guest.getStrikeCount() : 0)
            + ".");
    System.out.println("Reservation cancelled and room freed (NOT re-queued in waitlist).\n");
    ConsoleUtil.printContinueMessage();
  }

  public void displayEvictionCompletedScreen() {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: EVICTION COMPLETED");
    System.out.println("Booking record marked as NO_SHOW and removed from active system.\n");
    ConsoleUtil.printContinueMessage();
  }

  private String formatTimerCountdown(long expirationMs) {
    long diffMs = expirationMs - System.currentTimeMillis();
    if (diffMs <= 0) {
      return "00:00 (EXPIRED)";
    }
    long totalSec = diffMs / 1000;
    long mins = totalSec / 60;
    long secs = totalSec % 60;
    return String.format("%02d:%02d LEFT", mins, secs);
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) return g;
    }
    return null;
  }

  private Member findMember(ListInterface<Member> memberList, String memberId) {
    if (memberList == null || memberId == null) return null;
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equalsIgnoreCase(m.getMemberId())) return m;
    }
    return null;
  }
}
