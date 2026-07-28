package view;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class VipView {

  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println(" 1. Manage Waitlist");
    System.out.println(" 2. Manage Allocation");
    System.out.println(" 3. Settings & Configurations");
    System.out.println(" 4. Generate Analytics Report");
    System.out.println(" 5. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).input;
  }

  public Room.RoomType displayWaitlistQueueSelectionMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE WAITLIST - SELECT QUEUE");
    System.out.println(" 1. Luxury Room Waitlist Queue");
    System.out.println(" 2. Suite Room Waitlist Queue");
    System.out.println(" 3. Standard Room Waitlist Queue");
    System.out.println(" 4. Back to VIP Main Menu\n");

    int choice = ConsoleUtil.getMenuInput("Choose a queue to manage: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  public GetMenuInputResult renderWaitlistScreen(
      ListInterface<Reservation> list,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      Room.RoomType roomType,
      String search,
      String tier,
      String boiling,
      String sort,
      int currentPage,
      int pageSize) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE WAITLIST - [" + roomType.name() + " ROOMS]");

    System.out.println(" ACTIVE ROOM QUEUE: [ " + roomType.name() + " ]");
    System.out.println(
        " SEARCH QUERY     : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" TIER FILTER      : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println(" BOILING FILTER   : [ " + (boiling == null ? "ALL" : boiling) + " ]");
    System.out.println(" SORT CRITERIA    : [ " + sort + " ]");
    System.out.println("----------------------------------------------------------------------");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(4), RES ID(11), GUEST NAME(18), PHONE NO.(14), TIER(10), BOILING(9),
    // STRIKES(8), SCORE(7)
    int[] colWidths = {4, 11, 18, 14, 10, 9, 8, 7};
    TableUtil.TableSettings headerSettings = new TableUtil.TableSettings(colWidths);
    for (int i = 0; i < colWidths.length; i++) {
      headerSettings.setHAlign(i, TableUtil.Align.CENTER);
    }

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.RIGHT)
            .setTruncate(2);

    TableUtil.printTableBorder(headerSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "RES ID", "GUEST NAME", "PHONE NO.", "TIER", "BOILING", "STRIKES", "SCORE"
        },
        headerSettings);

    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(headerSettings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {88}).setHAlign(0, TableUtil.Align.CENTER);

      TableUtil.printTableRow(
          new String[] {"*** NO GUESTS WAITING IN " + roomType.name() + " QUEUE ***"},
          emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("----------------------------------------------------------------------");
      System.out.println(" [A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
      System.out.println(
          " [S] Search / Filter    [O] Change Sort Order   [E] Exit to Queue Menu\n");

      return ConsoleUtil.getMenuInput(
          "Enter a command: ", new char[] {'A', 'Q', 'R', 'S', 'O', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = list.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());
      Member m = (g != null) ? findMember(memberList, g.getMemberId()) : null;

      int displayNum = i - startIndex + 1;
      String resId = r.getReservationId();
      String guestName = (g != null) ? g.getName() : "N/A";
      String phoneNo = (g != null && g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A";
      String tierStr = (m != null) ? m.getTier().name() : "NON-MEMBER";
      String boilingStr = r.getIsBoiling() ? "[!]" : "[ ]";
      int strikes = (g != null) ? g.getStrikeCount() : 0;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            resId,
            guestName,
            phoneNo,
            tierStr,
            boilingStr,
            String.valueOf(strikes),
            String.valueOf(r.getPriorityScore())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(" Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, totalMatches);
    System.out.println("----------------------------------------------------------------------");
    System.out.println(" [A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
    System.out.println(" [S] Search / Filter    [O] Change Sort Order   [E] Exit to Queue Menu");
    System.out.println(" [P] Prev Page          [N] Next Page\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String rangeStr = (maxOptionNum == 1) ? "1" : "1-" + maxOptionNum;
    String promptText = "Enter a command or select index (" + rangeStr + "): ";

    return ConsoleUtil.getMenuInput(
        promptText, 1, maxOptionNum, new char[] {'A', 'Q', 'R', 'S', 'O', 'E', 'N', 'P'});
  }

  public String promptAddGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD GUEST TO WAITLIST");
    System.out.println(" [Enter 'C' to Cancel]\n");
    return ConsoleUtil.getStringInput(" Enter Customer ID or Booking Ref: ");
  }

  public boolean displayDequeueConfirmationScreen(
      Reservation r, Guest g, Member m, String autoAssignedRoom) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM VIP ROOM ALLOCATION");

    System.out.println("  GUEST DETAILS");
    System.out.println("  ----------------------------------------------------");
    System.out.println("  Reservation ID  : " + r.getReservationId());
    System.out.println("  Guest ID        : " + (g != null ? g.getGuestId() : r.getGuestId()));
    System.out.println("  Guest Name      : " + (g != null ? g.getName() : "N/A"));
    System.out.println("  Loyalty Tier    : " + (m != null ? m.getTier().name() : "NON-MEMBER"));
    System.out.println("  Priority Score  : " + r.getPriorityScore());

    System.out.println("\n  ROOM ALLOCATION");
    System.out.println("  ----------------------------------------------------");
    System.out.println("  Assigned Room   : " + autoAssignedRoom);
    System.out.println("  Booking Ref     : " + r.getConfirmationNumber());

    System.out.println("\n------------------------------------------------------");
    return ConsoleUtil.showConfirmMessage("Confirm room assignment and check-in?");
  }

  public void displayDequeueSuccessScreen(
      Reservation r, Guest g, Member m, String assignedRoom, int remainingWaitlistCount) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("DEQUEUE COMPLETE");

    System.out.println("  STATUS          : ALLOCATED & CHECKED IN");
    System.out.println("  RESERVATION ID  : " + r.getReservationId());
    System.out.println("  GUEST NAME      : " + (g != null ? g.getName() : "N/A"));
    System.out.println("  ASSIGNED ROOM   : " + assignedRoom);
    System.out.println("  REMAINING IN QUEUE : " + remainingWaitlistCount + " Guests");

    System.out.println("\n------------------------------------------------------");
    ConsoleUtil.printContinueMessage("  Press Enter to return...");
  }

  public int displayFilterMainMenu(String search, String tier, String boiling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & QUEUE FILTERS");
    System.out.println(" Active Search  : [ " + (search == null ? "None" : search) + " ]");
    System.out.println(" Active Tier    : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println(" Active Boiling : [ " + (boiling == null ? "ALL" : boiling) + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Text Search Submenu");
    System.out.println(" 2. Loyalty Tier Submenu");
    System.out.println(" 3. Boiling Status Submenu");
    System.out.println(" 4. Reset All Filters");
    System.out.println(" 5. Apply and Return");
    System.out.println(" 6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY");
    System.out.println(
        " Current Search: [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    System.out.println(" 1. Enter Search Term");
    System.out.println(" 2. Clear Search Term");
    System.out.println(" 3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput() {
    return ConsoleUtil.getStringInput("\nEnter search term: ");
  }

  public int displayTierSubmenu(String currentTier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TIER FILTER");
    System.out.println(
        " Current Selected Tier: [ " + (currentTier == null ? "ALL" : currentTier) + " ]\n");
    System.out.println(" 1. DIAMOND");
    System.out.println(" 2. GOLD");
    System.out.println(" 3. SILVER");
    System.out.println(" 4. Show All");
    System.out.println(" 5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayBoilingSubmenu(String currentBoiling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOILING STATUS FILTER");
    System.out.println(
        " Current Selected Status: [ "
            + (currentBoiling == null ? "ALL" : currentBoiling)
            + " ]\n");
    System.out.println(" 1. Boiling Only ([!])");
    System.out.println(" 2. Normal Only ([ ])");
    System.out.println(" 3. Show All");
    System.out.println(" 4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println(" 1. Priority Score (High -> Low)");
    System.out.println(" 2. Priority Score (Low -> High)");
    System.out.println(" 3. Strike Count (Lowest -> Highest)");
    System.out.println(" 4. Strike Count (Highest -> Lowest)");
    System.out.println(" 5. VIP Tier Rank (Diamond -> Silver)");
    System.out.println(" 6. VIP Tier Rank (Silver -> Diamond)");
    System.out.println(" 7. Live Wait Time (Longest -> Shortest)");
    System.out.println(" 8. Live Wait Time (Shortest -> Longest)");
    System.out.println(" 9. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 9).getAsInt();
    if (choice == 1) return "SCORE (HIGH -> LOW)";
    if (choice == 2) return "SCORE (LOW -> HIGH)";
    if (choice == 3) return "STRIKES (LOWEST -> HIGHEST)";
    if (choice == 4) return "STRIKES (HIGHEST -> LOWEST)";
    if (choice == 5) return "TIER RANK (DIAMOND -> SILVER)";
    if (choice == 6) return "TIER RANK (SILVER -> DIAMOND)";
    if (choice == 7) return "WAIT TIME (LONGEST -> SHORTEST)";
    if (choice == 8) return "WAIT TIME (SHORTEST -> LONGEST)";
    return null;
  }

  public int displayGuestActionSubmenu(Reservation r) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE ACTION: " + r.getReservationId());
    System.out.println(" 1. Dequeue & Assign Room");
    System.out.println(" 2. Cancel Reservation");
    System.out.println(" 3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equals(g.getGuestId())) return g;
    }
    return null;
  }

  private Member findMember(ListInterface<Member> memberList, String memberId) {
    if (memberList == null || memberId == null) return null;
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equals(m.getMemberId())) return m;
    }
    return null;
  }
}
