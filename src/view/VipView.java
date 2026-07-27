package view;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
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

  // --- SCREEN 1: MANAGE WAITLIST ---

  public GetMenuInputResult renderWaitlistScreen(
      ListInterface<Reservation> list,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      String search,
      String tier,
      String status,
      String sort,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE QUEUE WAITLIST");

    // Banner Metadata
    System.out.println(
        " SEARCH QUERY   : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" TIER FILTER    : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println(" STATUS FILTER  : [ " + (status == null ? "ALL" : status) + " ]");
    System.out.println(" SORT CRITERIA  : [ " + sort + " ]");
    System.out.println("------------------------------------------------------");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(5), GUEST ID(10), GUEST NAME(18), PHONE NO.(15), TIER(10), STRIKES(9),
    // BOILING(9), SCORE(8)
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(new int[] {5, 10, 18, 15, 10, 9, 9, 8})
            .setHAlign(0, TableUtil.Align.CENTER) // NO.
            .setHAlign(1, TableUtil.Align.CENTER) // GUEST ID
            .setHAlign(4, TableUtil.Align.CENTER) // TIER
            .setHAlign(5, TableUtil.Align.CENTER) // STRIKES
            .setHAlign(6, TableUtil.Align.CENTER) // BOILING
            .setHAlign(7, TableUtil.Align.RIGHT) // SCORE
            .setTruncate(2); // Truncate long names

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "GUEST ID", "GUEST NAME", "PHONE NO.", "TIER", "STRIKES", "BOILING", "SCORE"
        },
        settings);

    // --- CLEAN SPANNED EMPTY STATE HANDLING ---
    if (list == null || totalMatches == 0) {
      // 1. Cap off the 8 header columns cleanly with upward T-junctions (╠ ╩ ╣)
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      // 2. Differentiate between empty DB vs no filter matches
      boolean hasActiveFilters =
          (search != null && !search.trim().isEmpty())
              || (tier != null && !"ALL".equalsIgnoreCase(tier.trim()))
              || (status != null && !"ALL".equalsIgnoreCase(status.trim()));

      String emptyMessage =
          hasActiveFilters
              ? "*** NO MATCHING GUESTS FOUND FOR ACTIVE FILTERS ***"
              : "*** WAITLIST IS CURRENTLY EMPTY ***";

      // 3. Single 91-character spanned width row (84 total col width + 7 inner border chars)
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {91}).setHAlign(0, TableUtil.Align.CENTER);

      TableUtil.printTableRow(new String[] {emptyMessage}, emptySettings);

      // 4. Clean bottom box border without any orphan column T-ticks (╚ ═ ╝)
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("----------------------------------------------------------------------");
      System.out.println(" [A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
      System.out.println(" [S] Search / Filter    [O] Change Sort Order   [E] Exit to Menu\n");

      return ConsoleUtil.getMenuInput(
          "Enter a command: ", new char[] {'A', 'Q', 'R', 'S', 'O', 'E'});
    }

    // Standard multi-column middle border for non-empty tables
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = list.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());
      Member m = (g != null) ? findMember(memberList, g.getMemberId()) : null;

      int displayNum = i - startIndex + 1;
      String guestId = (g != null) ? g.getGuestId() : r.getGuestId();
      String guestName = (g != null) ? g.getName() : "N/A";
      String phoneNo = (g != null && g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A";
      String tierStr = (m != null) ? m.getTier().name() : "NON-MEMBER";
      int strikes = (g != null) ? g.getStrikeCount() : 0;
      String boilingStr = r.getIsBoiling() ? "[!]" : "[ ]";

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            guestId,
            guestName,
            phoneNo,
            tierStr,
            String.valueOf(strikes),
            boilingStr,
            String.valueOf(r.getPriorityScore())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(" Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, totalMatches);
    System.out.println("----------------------------------------------------------------------");
    System.out.println(" [A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
    System.out.println(" [S] Search / Filter    [O] Change Sort Order   [E] Exit to Menu");
    System.out.println(" [N] Next Page          [P] Prev Page\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String promptText =
        (maxOptionNum == 1)
            ? "Enter a command or select guest index number (1): "
            : "Enter a command or select a guest index number (1-" + maxOptionNum + "): ";

    return ConsoleUtil.getMenuInput(
        promptText, 1, maxOptionNum, new char[] {'A', 'Q', 'R', 'S', 'O', 'E', 'N', 'P'});
  }

  // --- SUB-MENUS & PROMPTS WITH EXITS ---

  public String promptAddGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD NEW GUEST");
    System.out.println(" Fetching profile records from Loyalty System...\n");
    System.out.println(" [Enter 'C' to Cancel and return to Waitlist]\n");
    return ConsoleUtil.getStringInput(" Enter Customer ID or Booking Reference Number: ");
  }

  // --- FILTER SUBMENUS WITH EXITS ---

  public int displayFilterMainMenu(String search, String tier, String status) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SEARCH MANAGEMENT");
    System.out.println(
        " Active Search : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" Active Tier   : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println(" Active Status : [ " + (status == null ? "ALL" : status) + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Text Search Submenu (Name / ID / Phone)");
    System.out.println(" 2. Loyalty Tier Submenu");
    System.out.println(" 3. Waiting State Submenu (Boiling / Normal)");
    System.out.println(" 4. Reset / Clear All Filters");
    System.out.println(" 5. Apply and Return to Waitlist");
    System.out.println(" 6. Back / Exit Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY SUBMENU");
    System.out.println(
        " Current Query: [ "
            + (currentQuery == null ? "None" : "\"" + currentQuery + "\"")
            + " ]\n");
    System.out.println(" 1. Enter / Change Search Term");
    System.out.println(" 2. Clear Search Term");
    System.out.println(" 3. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput(
        "Enter search query (Guest Name / ID / Phone / Booking Ref): ");
  }

  public int displayTierSubmenu(String currentTier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("LOYALTY TIER FILTER SUBMENU");
    System.out.println(
        " Current Selected Tier: [ " + (currentTier == null ? "ALL" : currentTier) + " ]\n");
    System.out.println(" 1. Filter: DIAMOND Tier");
    System.out.println(" 2. Filter: GOLD Tier");
    System.out.println(" 3. Filter: SILVER Tier");
    System.out.println(" 4. Clear Tier Filter (Show All)");
    System.out.println(" 5. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayStatusSubmenu(String currentStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WAITING STATE FILTER SUBMENU");
    System.out.println(
        " Current Selected Status: [ " + (currentStatus == null ? "ALL" : currentStatus) + " ]\n");
    System.out.println(" 1. Show BOILING Guests Only (High Urgency)");
    System.out.println(" 2. Show NORMAL Guests Only");
    System.out.println(" 3. Clear Status Filter (Show All)");
    System.out.println(" 4. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println(" 1. Priority Score (High to Low)");
    System.out.println(" 2. Live Waiting Time (Longest to Shortest)");
    System.out.println(" 3. Tier Rank Hierarchy (Diamond -> Gold -> Silver)");
    System.out.println(" 4. Back to Waitlist (Keep current sort)\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
    if (choice == 2) return "WAIT TIME (LONGEST -> SHORTEST)";
    if (choice == 3) return "TIER RANK (DIAMOND -> SILVER)";
    if (choice == 4) return null; // Keep current sort
    return "PRIORITY SCORE (HIGH -> LOW)";
  }

  public int displayGuestActionSubmenu(Reservation r) {
    return displayGuestActionSubmenu(r.getGuestId(), r.getPriorityScore());
  }

  public int displayGuestActionSubmenu(String guestId, int currentScore) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE ACTION: " + guestId);
    System.out.println(" Target Guest ID: " + guestId);
    System.out.println(" Priority Score : " + currentScore + "\n");
    System.out.println(" 1. Force Assign (Manually push to allocation stage)");
    System.out.println(" 2. Delete from Queue (Remove entirely from system)");
    System.out.println(" 3. Cancel Action and Return\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  // --- INTERNAL PROFILE LOOKUP HELPERS ---

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
