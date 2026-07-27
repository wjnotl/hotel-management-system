package view;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import util.ConsoleUtil;
import util.TableUtil;

public class VipView {

  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP Priority Room Allocation");
    System.out.println("1. Manage Waitlist");
    System.out.println("2. Manage Allocation");
    System.out.println("3. Settings & Configurations");
    System.out.println("4. Generate Analytics Report");
    System.out.println("5. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Select option: ", 1, 5).input;
  }

  // --- SCREEN 1: MANAGE WAITLIST ---

  public String renderWaitlistScreen(
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
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    if (totalMatches == 0) {
      TableUtil.printTableRow(
          new String[] {"-", "-", "*** NO MATCHING GUESTS FOUND ***", "-", "-", "-", "-", "-"},
          settings);
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("----------------------------------------------------------------------");
      System.out.println(" [A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
      System.out.println(" [S] Search / Filter    [O] Change Sort Order   [E] Exit to Menu\n");

      return ConsoleUtil.getMenuInput(
              "Enter a command: ", new char[] {'A', 'Q', 'R', 'S', 'O', 'E'})
          .input;
    }

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = list.getEntry(i);
      Guest g = findGuest(guestList, r.getGuestId());
      Member m = (g != null) ? findMember(memberList, g.getMemberId()) : null;

      int displayNum = i - startIndex + 1;
      String guestId = (g != null) ? g.getGuestId() : r.getGuestId();
      String guestName = (g != null) ? g.getName() : "N/A";
      String phoneNo = (g != null && g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A";
      String tierStr = (m != null) ? m.getTier().name() : "NON-MEMBER";
      int strikes = (g != null) ? g.getStrikeCount() : 0;
      String boilingStr = r.getIsBoiling() ? "[✓]" : "[ ]";

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
    return ConsoleUtil.getMenuInput(
            "Enter a command or select a guest index number (1-" + maxOptionNum + "): ",
            1,
            maxOptionNum,
            new char[] {'A', 'Q', 'R', 'S', 'O', 'E', 'N', 'P'})
        .input;
  }

  // --- SUB-MENUS & PROMPTS ---

  public String promptAddGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD NEW GUEST");
    System.out.println(" Fetching profile records from Loyalty System...\n");
    return ConsoleUtil.getStringInput(" Enter the Customer ID or Booking Reference Number: ");
  }

  public String[] displayFilterMenu(
      String currentSearch, String currentTier, String currentStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SEARCH WAITLIST");
    System.out.println(" 1. Search by Guest Name / ID / Phone");
    System.out.println(" 2. Filter by Membership Tier");
    System.out.println(" 3. Filter by Waiting State (Boiling)");
    System.out.println(" 4. Clear All Waitlist Filters");
    System.out.println(" 5. Back to Waitlist View\n");

    int choice = ConsoleUtil.getMenuInput("Choose a search option (1-5): ", 1, 5).getAsInt();
    String[] result = new String[] {currentSearch, currentTier, currentStatus};

    if (choice == 1) {
      String query = ConsoleUtil.getStringInput("Enter search query (leave blank to clear): ");
      result[0] = query.isEmpty() ? null : query;
    } else if (choice == 2) {
      System.out.println(
          "\n 1. Diamond Only\n 2. Gold Only\n 3. Silver Only\n 4. Clear Tier Filter\n");
      int tChoice = ConsoleUtil.getMenuInput("Choose tier option (1-4): ", 1, 4).getAsInt();
      if (tChoice == 1) result[1] = "DIAMOND";
      else if (tChoice == 2) result[1] = "GOLD";
      else if (tChoice == 3) result[1] = "SILVER";
      else result[1] = null;
    } else if (choice == 3) {
      System.out.println("\n 1. Boiling Status Only\n 2. Normal Status Only\n 3. Clear Filter\n");
      int sChoice = ConsoleUtil.getMenuInput("Choose state option (1-3): ", 1, 3).getAsInt();
      if (sChoice == 1) result[2] = "BOILING";
      else if (sChoice == 2) result[2] = "NORMAL";
      else result[2] = null;
    } else if (choice == 4) {
      return new String[] {null, null, null};
    }

    return result;
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println(" 1. Priority Score (High to Low)");
    System.out.println(" 2. Live Waiting Time (Longest to Shortest)");
    System.out.println(" 3. Tier Rank Hierarchy (Diamond -> Gold -> Silver)\n");

    int choice = ConsoleUtil.getMenuInput("Choose a sorting option (1-3): ", 1, 3).getAsInt();
    if (choice == 2) return "WAIT TIME (LONGEST -> SHORTEST)";
    if (choice == 3) return "TIER RANK (DIAMOND -> SILVER)";
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

    return ConsoleUtil.getMenuInput("Choose an action (1-3): ", 1, 3).getAsInt();
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
