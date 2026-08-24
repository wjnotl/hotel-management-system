package view.vip;

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

public class VipManageWaitlistView {
  public Room.RoomType displayWaitlistQueueSelectionMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE WAITLIST - SELECT QUEUE");
    System.out.println("1. Luxury Room Waitlist Queue");
    System.out.println("2. Suite Room Waitlist Queue");
    System.out.println("3. Standard Room Waitlist Queue");
    System.out.println("4. Back to VIP Menu\n");

    int choice = ConsoleUtil.getMenuInput("Choose a queue to manage: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  public static class WaitlistRowDTO {
    private final String reservationId;
    private final String guestName;
    private final String phoneNumber;
    private final String tier;
    private final String waitTime;
    private final boolean isBoiling;
    private final int strikes;
    private final int priorityScore;

    public WaitlistRowDTO(
        String reservationId,
        String guestName,
        String phoneNumber,
        String tier,
        String waitTime,
        boolean isBoiling,
        int strikes,
        int priorityScore) {
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.phoneNumber = phoneNumber;
      this.tier = tier;
      this.waitTime = waitTime;
      this.isBoiling = isBoiling;
      this.strikes = strikes;
      this.priorityScore = priorityScore;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getPhoneNumber() {
      return phoneNumber;
    }

    public String getTier() {
      return tier;
    }

    public String getWaitTime() {
      return waitTime;
    }

    public boolean getIsBoiling() {
      return isBoiling;
    }

    public int getStrikes() {
      return strikes;
    }

    public int getPriorityScore() {
      return priorityScore;
    }
  }

  public static class GuestDisambiguationRowDTO {
    private final String displayNum;
    private final String guestId;
    private final String name;
    private final String icOrPass;
    private final String phone;
    private final String tierStr;

    public GuestDisambiguationRowDTO(
        String displayNum,
        String guestId,
        String name,
        String icOrPass,
        String phone,
        String tierStr) {
      this.displayNum = displayNum;
      this.guestId = guestId;
      this.name = name;
      this.icOrPass = icOrPass;
      this.phone = phone;
      this.tierStr = tierStr;
    }

    public String getDisplayNum() {
      return displayNum;
    }

    public String getGuestId() {
      return guestId;
    }

    public String getName() {
      return name;
    }

    public String getIcOrPass() {
      return icOrPass;
    }

    public String getPhone() {
      return phone;
    }

    public String getTierStr() {
      return tierStr;
    }
  }

  public GetMenuInputResult renderWaitlistScreen(
      ListInterface<WaitlistRowDTO> pageSlice,
      Room.RoomType roomType,
      String search,
      String tier,
      String boiling,
      String sort,
      int currentPage,
      int totalPages,
      int totalMatches,
      int rowsOnPage) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE WAITLIST - [" + roomType.name() + " ROOMS]");

    System.out.println("ACTIVE ROOM QUEUE: [ " + roomType.name() + " ]");
    System.out.println(
        "SEARCH QUERY     : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println("TIER FILTER      : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("BOILING FILTER   : [ " + (boiling == null ? "ALL" : boiling) + " ]");
    System.out.println("SORT CRITERIA    : [ " + sort + " ]");

    boolean hasActiveFilters = (search != null || tier != null || boiling != null);

    String validStr;
    if (pageSlice == null || totalMatches == 0) {
      validStr = "A" + (hasActiveFilters ? "S" : "") + "RE";
    } else {
      validStr = "AQRESO" + (currentPage > 1 ? "P" : "") + (currentPage < totalPages ? "N" : "");
    }
    char[] validChars = validStr.toCharArray();

    int[] columnWidths = {4, 11, 16, 13, 10, 10, 8, 7, 6};

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.RIGHT)
            .setTruncate(2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setHAlign(8, TableUtil.Align.CENTER)
            .setTruncate(2);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.",
          "RES ID",
          "GUEST NAME",
          "PHONE NO.",
          "TIER",
          "WAIT TIME",
          "BOILING",
          "STRIKES",
          "SCORE"
        },
        headerSettings);

    if (pageSlice == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {109}).setHAlign(0, TableUtil.Align.CENTER);

      String emptyMsg =
          hasActiveFilters
              ? "*** NO GUESTS MATCH ACTIVE SEARCH / FILTERS IN " + roomType.name() + " QUEUE ***"
              : "*** NO GUESTS WAITING IN " + roomType.name() + " QUEUE ***";

      TableUtil.printTableRow(new String[] {emptyMsg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println("Page 0 / 0 (Total Matches: 0)\n");

      if (hasActiveFilters) {
        System.out.println("[A] Add Guest          [S] Search / Filter     [R] Refresh Table");
        System.out.println("[E] Exit to Queue Menu\n");
      } else {
        System.out.println(
            "[A] Add Guest          [R] Refresh Table       [E] Exit to Queue Menu\n");
      }

      return ConsoleUtil.getMenuInput("Enter a command: ", validChars);
    }

    // Normal table display
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    for (int i = 1; i <= pageSlice.getNumberOfEntries(); i++) {
      WaitlistRowDTO item = pageSlice.getEntry(i);
      if (item == null) continue;

      int displayNum = i;
      String boilingStr = item.getIsBoiling() ? "[!]" : "[ ]";

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            item.getReservationId(),
            item.getGuestName(),
            item.getPhoneNumber(),
            item.getTier(),
            item.getWaitTime(),
            boilingStr,
            String.valueOf(item.getStrikes()),
            String.valueOf(item.getPriorityScore())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(
        "Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, totalMatches);
    System.out.println("[A] Add Guest          [Q] Quick Assign Top    [R] Refresh Table");
    System.out.println("[S] Search / Filter    [O] Change Sort Order   [E] Exit to Queue Menu");

    StringBuilder navLine = new StringBuilder();
    boolean hasPrev = (currentPage > 1);
    boolean hasNext = (currentPage < totalPages);

    if (hasPrev) navLine.append("[P] Prev Page          ");
    if (hasNext) navLine.append("[N] Next Page          ");

    if (navLine.length() > 0) {
      System.out.println(navLine.toString().trim() + "\n");
    } else {
      System.out.println();
    }

    String rangeStr = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    String promptText = "Enter a command or select index (" + rangeStr + "): ";

    return ConsoleUtil.getMenuInput(promptText, 1, rowsOnPage, validChars);
  }

  public String promptAddGuestInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD GUEST TO WAITLIST");
    System.out.println("[Enter 'C' to Cancel]\n");
    return ConsoleUtil.getStringInput("Enter Guest ID, Name, IC/Passport, or Phone: ");
  }

  public boolean displayAddGuestConfirmationScreen(
      Guest g, Member m, Room.RoomType roomType, int baseScore) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ADD GUEST TO WAITLIST", 83);

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    // Guest Details
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST DETAILS"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Guest ID", g.getGuestId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Guest Name", g.getName()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "IC / Passport No", (g.getIcNumber() != null ? g.getIcNumber() : g.getPassportNumber())
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Phone Number", (g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A")},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Email Address", (g.getEmail() != null ? g.getEmail() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // Loyalty & Priority
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"LOYALTY & PRIORITY"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Loyalty Member ID", (g.getMemberId() != null ? g.getMemberId() : "N/A")},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Loyalty Tier", (m != null ? m.getTier().name() : "NON-MEMBER")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Strike Count", String.valueOf(g.getStrikeCount())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Target Queue", roomType.name()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Calculated Score", String.valueOf(baseScore)}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    GetMenuInputResult input =
        ConsoleUtil.getMenuInput(
            "Add this guest to the " + roomType.name() + " waitlist queue? (Y/N): ",
            new char[] {'Y', 'N'});
    return "Y".equalsIgnoreCase(input.input);
  }

  public boolean displayDequeueConfirmationScreen(
      Reservation r, Guest g, Member m, Room room, int graceMins) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ROOM ASSIGNMENT", 83);

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    // Reservation & Booking Details
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION & BOOKING DETAILS"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Reservation ID", r.getReservationId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Requested Room Type", r.getRoomType().name()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Queue Arrival Time", formatTime(r.getQueueArrivalTime())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // Guest & Loyalty Details
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST & LOYALTY DETAILS"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Guest ID", (g != null ? g.getGuestId() : r.getGuestId())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest Name", (g != null ? g.getName() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "IC / Passport No",
          (g != null ? (g.getIcNumber() != null ? g.getIcNumber() : g.getPassportNumber()) : "N/A")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Phone Number", (g != null && g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Loyalty Member ID", (g != null && g.getMemberId() != null ? g.getMemberId() : "N/A")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Loyalty Tier", (m != null ? m.getTier().name() : "NON-MEMBER")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Strike Count", String.valueOf(g != null ? g.getStrikeCount() : 0)},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // Room Allocation Hold
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ROOM ALLOCATION HOLD"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Priority Score", String.valueOf(r.getPriorityScore())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Boiling Status", (r.getIsBoiling() ? "BOILING [!]" : "NORMAL [ ]")},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Assigned Room No", room.getRoomNumber() + " (" + room.getRoomType().name() + ")"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {"Hold Expiration", graceMins + " Mins"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    GetMenuInputResult input =
        ConsoleUtil.getMenuInput("Assign room & hold for guest? (Y/N): ", new char[] {'Y', 'N'});
    return "Y".equalsIgnoreCase(input.input);
  }

  public boolean displayCancelConfirmationScreen(Reservation r, Guest g, Member m) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM CANCEL RESERVATION", 83);

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    // Reservation Details
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION DETAILS"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Reservation ID", r.getReservationId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Requested Room Type", r.getRoomType().name()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Queue Arrival Time", formatTime(r.getQueueArrivalTime())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();

    // Guest Details
    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST DETAILS"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Guest ID", (g != null ? g.getGuestId() : r.getGuestId())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest Name", (g != null ? g.getName() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Phone Number", (g != null && g.getPhoneNumber() != null ? g.getPhoneNumber() : "N/A")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Loyalty Tier", (m != null ? m.getTier().name() : "NON-MEMBER")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Priority Score", String.valueOf(r.getPriorityScore())}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Strike Count", String.valueOf(g != null ? g.getStrikeCount() : 0)},
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    GetMenuInputResult input =
        ConsoleUtil.getMenuInput(
            "Are you sure you want to remove this reservation from the waitlist? (Y/N): ",
            new char[] {'Y', 'N'});
    return "Y".equalsIgnoreCase(input.input);
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

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ROOM HELD IN MANAGE ALLOCATION"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Reservation ID", r.getReservationId()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest Name", (g != null ? g.getName() : "N/A")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Assigned Room", entry.getAssignedRoomNumber()}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Remaining in Queue", remainingWaitlistCount + " Guests"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public int displayMaxStrikeWarningScreen(Guest g, Member m, int maxStrikes) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WARNING: MAX STRIKE LIMIT EXCEEDED");

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] ACCOUNT FLAGGED / EVICTION LOCKOUT"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Target Guest", g.getName() + " (" + g.getGuestId() + ")"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Member Tier", (m != null ? m.getTier().name() : "NON-MEMBER")}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Strikes Today", g.getStrikeCount() + " / " + maxStrikes}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "System Notice",
          "Guest has exceeded maximum no-show strikes. Authorization required to grant waitlist"
              + " entry."
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\n1. Authorize Override (Reset Strikes to 0)");
    System.out.println("2. Cancel / Go Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt();
  }

  public boolean displayStrikeOverrideConfirmationScreen(Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM STRIKE OVERRIDE");
    System.out.println(
        " Are you sure you want to authorize strike reset for "
            + (g != null ? g.getName() : "Guest")
            + "?");
    System.out.println(
        " This will reset the guest's strike count from "
            + (g != null ? g.getStrikeCount() : 0)
            + " to 0.\n");
    GetMenuInputResult input =
        ConsoleUtil.getMenuInput(
            "Authorize override and reset strikes to 0? (Y/N): ", new char[] {'Y', 'N'});
    return "Y".equalsIgnoreCase(input.input);
  }

  public void displayNonMemberDeniedScreen(Guest g) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ENTRY DENIED - NON-MEMBER");

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [X] ENTRY DENIED - NON-MEMBER PROFILE"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(
        new String[] {"Target Guest", g.getName() + " (" + g.getGuestId() + ")"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Loyalty Member", (g.getMemberId() != null ? g.getMemberId() : "NONE (Not a Member)")
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "System Policy",
          "VIP Waitlist access is strictly reserved for Loyalty Members (Diamond, Gold, Silver). "
              + "Non-members cannot join the VIP waitlist queue."
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public void displayGuestNotFoundErrorScreen(String searchedTerm) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VALIDATION ERROR", 83);

    int[] kvWidths = {20, 60};
    int[] fullWidth = {83};

    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.TableSettings fullSettings =
        new TableUtil.TableSettings(fullWidth).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(fullSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: [X] CUSTOMER NOT FOUND"}, fullSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);

    TableUtil.printTableRow(new String[] {"Searched Term", searchedTerm}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "System Notice",
          "The identifier typed does not match any registered guest. Please verify the reference"
              + " code or card ID."
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println();
    ConsoleUtil.printContinueMessage("Press Enter to try again...");
  }

  public int displayFilterMainMenu(String search, String tier, String boiling) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & QUEUE FILTERS");
    System.out.println("Active Search  : [ " + (search == null ? "None" : search) + " ]");
    System.out.println("Active Tier    : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("Active Boiling : [ " + (boiling == null ? "ALL" : boiling) + " ]\n");
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
    System.out.println("Searchable Fields: Reservation ID, Guest Name, Phone Number");
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
    System.out.println("Searchable Fields: Reservation ID, Guest Name, Phone Number");
    System.out.println(
        "Current Search   : [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    return ConsoleUtil.getStringInput("Enter search term (Res ID / Guest Name / Phone No): ");
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
    System.out.println("9. Reservation ID (Low -> High)");
    System.out.println("10. Reservation ID (High -> Low)");
    System.out.println("11. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 11).getAsInt();
    if (choice == 1) return "SCORE (HIGH -> LOW)";
    if (choice == 2) return "SCORE (LOW -> HIGH)";
    if (choice == 3) return "STRIKES (LOWEST -> HIGHEST)";
    if (choice == 4) return "STRIKES (HIGHEST -> LOWEST)";
    if (choice == 5) return "TIER RANK (DIAMOND -> SILVER)";
    if (choice == 6) return "TIER RANK (SILVER -> DIAMOND)";
    if (choice == 7) return "WAIT TIME (LONGEST -> SHORTEST)";
    if (choice == 8) return "WAIT TIME (SHORTEST -> LONGEST)";
    if (choice == 9) return "RESERVATION ID (LOW -> HIGH)";
    if (choice == 10) return "RESERVATION ID (HIGH -> LOW)";
    return null;
  }

  public int displayGuestActionSubmenu(Reservation r) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WAITLIST ACTION: " + r.getReservationId());
    System.out.println("1. Assign Room & Hold");
    System.out.println("2. Cancel Reservation");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public void displayStrikeResetOverrideScreen(Guest guest) {
    ConsoleUtil.clearScreen();
    System.out.println(
        ">> OVERRIDE AUTHORIZED: Strike count reset to 0 for " + guest.getName() + ".\n");
    ConsoleUtil.printContinueMessage();
  }

  public void displayAddGuestSuccessScreen(String resId, Guest guest, Room.RoomType roomType) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: SUCCESS");
    System.out.println(
        "Reservation "
            + resId
            + " created for "
            + guest.getName()
            + " in "
            + roomType.name()
            + " queue.\n");
    ConsoleUtil.printContinueMessage();
  }

  public void displayCancelSuccessScreen(String resId) {
    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: SUCCESS");
    System.out.println("Reservation " + resId + " has been removed from the waitlist.\n");
    ConsoleUtil.printContinueMessage();
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    return dateTime.format(formatter);
  }

  public GetMenuInputResult displayGuestDisambiguationScreen(
      ListInterface<GuestDisambiguationRowDTO> pageSlice,
      String searchQuery,
      int currentPage,
      int totalPages,
      int totalMatches,
      int rowsOnPage) {
    if (pageSlice == null || pageSlice.isEmpty()) return null;

    String validStr = "C" + (currentPage > 1 ? "P" : "") + (currentPage < totalPages ? "N" : "");
    char[] validChars = validStr.toCharArray();

    int[] columnWidths = {4, 10, 18, 18, 14, 18};

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
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

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MULTIPLE GUEST MATCHES FOUND");

    System.out.println("Search Term: \"" + searchQuery + "\"");
    if (totalMatches > pageSlice.getNumberOfEntries()) {
      System.out.println(
          "(Tip: If there are too many results, enter a more specific search query)");
    }
    System.out.println();

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "GUEST ID", "GUEST NAME", "IC / PASSPORT NO.", "PHONE NO.", "MEMBER TIER"
        },
        headerSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    for (int i = 1; i <= pageSlice.getNumberOfEntries(); i++) {
      GuestDisambiguationRowDTO dto = pageSlice.getEntry(i);
      if (dto == null) continue;

      TableUtil.printTableRow(
          new String[] {
            dto.getDisplayNum(),
            dto.getGuestId(),
            dto.getName(),
            dto.getIcOrPass(),
            dto.getPhone(),
            dto.getTierStr()
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(
        "Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, totalMatches);

    StringBuilder navLine = new StringBuilder();
    boolean hasPrev = (currentPage > 1);
    boolean hasNext = (currentPage < totalPages);

    if (hasPrev) navLine.append("[P] Previous Page    ");
    if (hasNext) navLine.append("[N] Next Page        ");
    navLine.append("[C] Cancel / Refine Search");
    System.out.println(navLine.toString());
    System.out.println();

    String rangeStr = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    String promptText = "Select guest index (" + rangeStr + ") or command: ";
    return ConsoleUtil.getMenuInput(promptText, 1, rowsOnPage, validChars);
  }
}
