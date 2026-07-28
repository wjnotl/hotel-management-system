package view;

import adt.ListInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
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

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).input;
  }

  public Room.RoomType displayWaitlistQueueSelectionMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE WAITLIST - SELECT QUEUE");
    System.out.println("1. Luxury Room Waitlist Queue");
    System.out.println("2. Suite Room Waitlist Queue");
    System.out.println("3. Standard Room Waitlist Queue");
    System.out.println("4. Back to VIP Main Menu\n");

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

    System.out.println("ACTIVE ROOM QUEUE: [ " + roomType.name() + " ]");
    System.out.println(
        "SEARCH QUERY     : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println("TIER FILTER      : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("BOILING FILTER   : [ " + (boiling == null ? "ALL" : boiling) + " ]");
    System.out.println("SORT CRITERIA    : [ " + sort + " ]");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(4), RES ID(11), GUEST NAME(18), PHONE NO.(14), TIER(10), BOILING(9), STRIKES(8),
    // SCORE(7)
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(new int[] {4, 11, 18, 14, 10, 9, 8, 7})
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.RIGHT)
            .setTruncate(2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(new int[] {4, 11, 18, 14, 10, 9, 8, 7})
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "RES ID", "GUEST NAME", "PHONE NO.", "TIER", "BOILING", "STRIKES", "SCORE"
        },
        headerSettings);

    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {89}).setHAlign(0, TableUtil.Align.CENTER);

      TableUtil.printTableRow(
          new String[] {"*** NO GUESTS WAITING IN " + roomType.name() + " QUEUE ***"},
          emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("Page 0 / 0 (Total Matches: 0)\n");
      System.out.println("[A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
      System.out.println("[S] Search / Filter    [O] Change Sort Order   [E] Exit to Queue Menu\n");

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
      Member m =
          (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;

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
    System.out.printf(
        "Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, totalMatches);
    System.out.println("[A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
    System.out.println("[S] Search / Filter    [O] Change Sort Order   [E] Exit to Queue Menu");
    System.out.println("[P] Prev Page          [N] Next Page\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String rangeStr = (maxOptionNum == 1) ? "1" : "1-" + maxOptionNum;
    String promptText = "Enter a command or select index (" + rangeStr + "): ";

    return ConsoleUtil.getMenuInput(
        promptText, 1, maxOptionNum, new char[] {'A', 'Q', 'R', 'S', 'O', 'E', 'N', 'P'});
  }

  public String promptAddGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD GUEST TO WAITLIST");
    System.out.println("[Enter 'C' to Cancel]\n");
    return ConsoleUtil.getStringInput("Enter Guest ID or Name: ");
  }

  public boolean displayAddGuestConfirmationScreen(
      Guest g, Member m, Room.RoomType roomType, int baseScore) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ADD GUEST TO WAITLIST");

    System.out.println("GUEST DETAILS");
    System.out.println("Guest ID            : " + g.getGuestId());
    System.out.println("Guest Name          : " + g.getName());
    System.out.println(
        "IC / Passport No    : "
            + (g.getIcNumber() != null ? g.getIcNumber() : g.getPassportNumber()));
    System.out.println(
        "Phone Number        : " + (g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A"));
    System.out.println("Email Address       : " + (g.getEmail() != null ? g.getEmail() : "N/A"));

    System.out.println("\nLOYALTY & PRIORITY");
    System.out.println(
        "Loyalty Member ID   : " + (g.getMemberId() != null ? g.getMemberId() : "N/A"));
    System.out.println("Loyalty Tier        : " + (m != null ? m.getTier().name() : "NON-MEMBER"));
    System.out.println("Strike Count        : " + g.getStrikeCount());
    System.out.println("Target Queue        : " + roomType.name());
    System.out.println("Calculated Score    : " + baseScore);

    System.out.println();
    return promptConfirm("Add this guest to the " + roomType.name() + " waitlist queue? (Y/N): ");
  }

  public boolean displayDequeueConfirmationScreen(Reservation r, Guest g, Member m, Room room) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM VIP ROOM ALLOCATION");

    System.out.println("RESERVATION & BOOKING DETAILS");
    System.out.println("Reservation ID      : " + r.getReservationId());
    System.out.println("Confirmation Code   : " + r.getConfirmationNumber());
    System.out.println("Requested Room Type : " + r.getRoomType().name());
    System.out.println("Queue Arrival Time  : " + formatTime(r.getQueueArrivalTime()));

    System.out.println("\nGUEST & LOYALTY DETAILS");
    System.out.println("Guest ID            : " + (g != null ? g.getGuestId() : r.getGuestId()));
    System.out.println("Guest Name          : " + (g != null ? g.getName() : "N/A"));
    System.out.println(
        "IC / Passport No    : "
            + (g != null
                ? (g.getIcNumber() != null ? g.getIcNumber() : g.getPassportNumber())
                : "N/A"));
    System.out.println(
        "Phone Number        : "
            + (g != null && g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A"));
    System.out.println(
        "Loyalty Member ID   : "
            + (g != null && g.getMemberId() != null ? g.getMemberId() : "N/A"));
    System.out.println("Loyalty Tier        : " + (m != null ? m.getTier().name() : "NON-MEMBER"));
    System.out.println("Strike Count        : " + (g != null ? g.getStrikeCount() : 0));

    System.out.println("\nROOM ALLOCATION HOLD");
    System.out.println("Priority Score      : " + r.getPriorityScore());
    System.out.println(
        "Boiling Status      : " + (r.getIsBoiling() ? "BOILING [!]" : "NORMAL [ ]"));
    System.out.println(
        "Assigned Room No    : " + room.getRoomNumber() + " (" + room.getRoomType().name() + ")");
    System.out.println("Hold Expiration     : 15 Minutes");

    System.out.println();
    return promptConfirm("Assign room & create allocation entry? (Y/N): ");
  }

  public boolean displayCancelConfirmationScreen(Reservation r, Guest g, Member m) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM CANCEL RESERVATION");

    System.out.println("RESERVATION DETAILS");
    System.out.println("Reservation ID      : " + r.getReservationId());
    System.out.println("Confirmation Code   : " + r.getConfirmationNumber());
    System.out.println("Requested Room Type : " + r.getRoomType().name());
    System.out.println("Queue Arrival Time  : " + formatTime(r.getQueueArrivalTime()));

    System.out.println("\nGUEST DETAILS");
    System.out.println("Guest ID            : " + (g != null ? g.getGuestId() : r.getGuestId()));
    System.out.println("Guest Name          : " + (g != null ? g.getName() : "N/A"));
    System.out.println(
        "Phone Number        : "
            + (g != null && g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A"));
    System.out.println("Loyalty Tier        : " + (m != null ? m.getTier().name() : "NON-MEMBER"));
    System.out.println("Priority Score      : " + r.getPriorityScore());
    System.out.println("Strike Count        : " + (g != null ? g.getStrikeCount() : 0));

    System.out.println();
    return promptConfirm(
        "Are you sure you want to remove this reservation from the waitlist? (Y/N): ");
  }

  public void displayDequeueSuccessScreen(
      Reservation r,
      Guest g,
      Member m,
      String assignedRoom,
      int remainingWaitlistCount,
      AllocationEntry entry) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALLOCATION ENTRY CREATED");

    System.out.println("STATUS          : ROOM HELD IN MANAGE ALLOCATION");
    System.out.println("RESERVATION ID  : " + r.getReservationId());
    System.out.println("CONFIRMATION NO : " + entry.getReservationConfirmationNumber());
    System.out.println("GUEST NAME      : " + (g != null ? g.getName() : "N/A"));
    System.out.println("ASSIGNED ROOM   : " + entry.getAssignedRoomNumber());
    System.out.println("REMAINING IN QUEUE : " + remainingWaitlistCount + " Guests");

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public int displayMaxStrikeWarningScreen(Guest g, Member m) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WARNING: MAX STRIKE LIMIT EXCEEDED");

    System.out.println("STATUS          : [!] ACCOUNT FLAGGED / EVICTION LOCKOUT");
    System.out.println("Target Guest    : " + g.getName() + " (" + g.getGuestId() + ")");
    System.out.println("Member Tier     : " + (m != null ? m.getTier().name() : "NON-MEMBER"));
    System.out.println("Strikes Today   : " + g.getStrikeCount() + " / 3");
    System.out.println("Notice          : Guest has exceeded maximum no-show strikes.");
    System.out.println("                  Authorization required to grant waitlist entry.");

    System.out.println("\n1. Authorize Override (Reset Strikes to 0)");
    System.out.println("2. Enforce Eviction Lockout (Deny Waitlist Access)");
    System.out.println("3. Cancel and Return\n");

    return ConsoleUtil.getMenuInput("Choose an action: ", 1, 3).getAsInt();
  }

  public boolean displayUnassignedTierWarningScreen(Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CRITICAL PROFILE WARNING");

    System.out.println("STATUS          : [X] UNASSIGNED / NULL MEMBERSHIP TIER");
    System.out.println("Target Guest    : " + g.getName() + " (" + g.getGuestId() + ")");
    System.out.println("Loyalty Member  : " + (g.getMemberId() != null ? g.getMemberId() : "NONE"));
    System.out.println("Notice          : Cannot resolve VIP priority ranking without a tier.");
    System.out.println(
        "                  Guest will be assigned default NON-MEMBER priority score (1000).");

    System.out.println();
    return promptConfirm("Proceed with default NON-MEMBER priority score? (Y/N): ");
  }

  public void displayGuestNotFoundErrorScreen(String searchedTerm) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VALIDATION ERROR");

    System.out.println("STATUS          : [X] CUSTOMER NOT FOUND");
    System.out.println("Searched Term   : " + searchedTerm);
    System.out.println(
        "Notice          : The identifier typed does not match any registered guest.");
    System.out.println("                  Please verify the reference code or card ID.");

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to try again...");
  }

  public int displayFilterMainMenu(String search, String tier, String boiling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & QUEUE FILTERS");
    System.out.println("Active Search  : [ " + (search == null ? "None" : search) + " ]");
    System.out.println("Active Tier    : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("Active Boiling : [ " + (boiling == null ? "ALL" : boiling) + " ]");
    System.out.println("1. Text Search Submenu");
    System.out.println("2. Loyalty Tier Submenu");
    System.out.println("3. Boiling Status Submenu");
    System.out.println("4. Reset All Filters");
    System.out.println("5. Apply and Return");
    System.out.println("6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY");
    System.out.println(
        "Current Search: [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    System.out.println("1. Enter Search Term");
    System.out.println("2. Clear Search Term");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput() {
    return ConsoleUtil.getStringInput("\nEnter search term: ");
  }

  public int displayTierSubmenu(String currentTier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TIER FILTER");
    System.out.println(
        "Current Selected Tier: [ " + (currentTier == null ? "ALL" : currentTier) + " ]\n");
    System.out.println("1. DIAMOND");
    System.out.println("2. GOLD");
    System.out.println("3. SILVER");
    System.out.println("4. Show All");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayBoilingSubmenu(String currentBoiling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOILING STATUS FILTER");
    System.out.println(
        "Current Selected Status: [ " + (currentBoiling == null ? "ALL" : currentBoiling) + " ]\n");
    System.out.println("1. Boiling Only ([!])");
    System.out.println("2. Normal Only ([ ])");
    System.out.println("3. Show All");
    System.out.println("4. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Priority Score (High -> Low)");
    System.out.println("2. Priority Score (Low -> High)");
    System.out.println("3. Strike Count (Lowest -> Highest)");
    System.out.println("4. Strike Count (Highest -> Lowest)");
    System.out.println("5. VIP Tier Rank (Diamond -> Silver)");
    System.out.println("6. VIP Tier Rank (Silver -> Diamond)");
    System.out.println("7. Live Wait Time (Longest -> Shortest)");
    System.out.println("8. Live Wait Time (Shortest -> Longest)");
    System.out.println("9. Back\n");

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
    System.out.println("1. Dequeue & Assign Room");
    System.out.println("2. Cancel Reservation");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  private boolean promptConfirm(String prompt) {
    while (true) {
      String choice = ConsoleUtil.getStringInput(prompt);
      if (choice != null) {
        choice = choice.trim().toUpperCase();
        if ("Y".equalsIgnoreCase(choice)) return true;
        if ("N".equalsIgnoreCase(choice)) return false;
      }
      System.out.println("Invalid input. Please enter 'Y' or 'N'.");
    }
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    return dateTime.format(formatter);
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null
          && (guestId.equalsIgnoreCase(g.getGuestId()) || guestId.equalsIgnoreCase(g.getName())))
        return g;
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
