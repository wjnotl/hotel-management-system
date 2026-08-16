package view.booking;

import adt.ListInterface;
import adt.QueueInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class WalkInQueueView {

  private static final int[] LINE_WIDTHS = {5, 5, 12, 22, 12, 10, 9};
  private static final int[] HOLD_WIDTHS = {5, 12, 22, 11, 12, 14};
  private static final int[] SPAN_WIDTH = {81};
  private static final int[] KV_WIDTHS = {22, 58};
  private static final int SCREEN_WIDTH = 83;

  public Room.RoomType displayQueueSelectionMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN QUEUE - SELECT LINE");
    System.out.println("1. Luxury Room Walk-In Queue");
    System.out.println("2. Suite Room Walk-In Queue");
    System.out.println("3. Standard Room Walk-In Queue");
    System.out.println("4. Back to Booking Menu\n");

    int choice = ConsoleUtil.getMenuInput("Choose a line to manage: ", 1, 4).getAsInt();
    if (choice == 1) return Room.RoomType.LUXURY;
    if (choice == 2) return Room.RoomType.SUITE;
    if (choice == 3) return Room.RoomType.STANDARD;
    return null;
  }

  public GetMenuInputResult renderQueueScreen(
      QueueInterface<Reservation> queue,
      ListInterface<Reservation> lineRows,
      ListInterface<Reservation> holds,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      ListInterface<Room> roomList,
      Room.RoomType roomType,
      int queueCapacity,
      int vacantRooms,
      int vipWaiting,
      int graceMinutes,
      String search,
      String tier,
      String sort,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN QUEUE - [" + roomType.name() + " ROOMS]", SCREEN_WIDTH);

    int waiting = queue.getNumberOfEntries();
    Reservation next = queue.peek();
    Guest nextGuest = (next != null) ? findGuest(guestList, next.getGuestId()) : null;

    System.out.println(
        "WAITING IN LINE   : "
            + waiting
            + " / "
            + queueCapacity
            + " array slots"
            + (queue.isFull() ? "   [FULL - next enqueue doubles the array]" : ""));
    System.out.println(
        "NEXT UP (peek)    : "
            + (next == null
                ? "Line is empty"
                : (nextGuest != null ? nextGuest.getName() : "Unknown")
                    + "  ("
                    + next.getReservationId()
                    + ")"));
    System.out.println("VACANT CLEAN ROOMS: " + vacantRooms);
    System.out.println(
        "VIP AHEAD OF LINE : " + vipWaiting + "   ->  " + verdictFor(vacantRooms, vipWaiting));
    System.out.println(
        "SEARCH QUERY      : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println("TIER FILTER       : [ " + (tier == null ? "ALL" : tier) + " ]");
    System.out.println("SORT CRITERIA     : [ " + sort + " ]\n");

    printLineTable(queue, lineRows, guestList, memberList, roomType, currentPage, pageSize);
    System.out.println();
    printHoldTable(holds, guestList, roomList, graceMinutes);

    int totalMatches = (lineRows == null) ? 0 : lineRows.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);
    System.out.printf(
        "\nPage %d / %d (Matches: %d)   Holds: %d\n\n",
        (totalPages == 0) ? 0 : currentPage,
        totalPages,
        totalMatches,
        (holds == null) ? 0 : holds.getNumberOfEntries());

    System.out.println("[A] Add Walk-In        [G] Allocate Next       [C] Check In Hold");
    System.out.println("[V] Override VIP Bypass                        [X] Close Queue");
    System.out.println("[S] Search / Filter    [O] Change Sort Order   [R] Refresh");
    System.out.println("[P] Prev Page          [N] Next Page           [E] Exit to Line Menu\n");

    char[] commands = {'A', 'G', 'V', 'C', 'S', 'O', 'X', 'P', 'N', 'R', 'E'};

    // With nothing on the page there is no valid row number, and an empty integer range
    // would be rejected by ConsoleUtil before the user ever sees the prompt.
    int rowsOnPage = countRowsOnPage(totalMatches, currentPage, pageSize);
    if (rowsOnPage <= 0) {
      return ConsoleUtil.getMenuInput("Enter a command: ", commands);
    }

    String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
    return ConsoleUtil.getMenuInput(
        "Enter a command or select a row (" + range + "): ", 1, rowsOnPage, commands);
  }

  // Rooms are held back one per waiting VIP rather than the whole type being frozen, so a line can
  // legitimately be servable while VIPs are still waiting. Printing the leftover count says that
  // out loud, otherwise the header reads as though the bypass was ignored.
  private String verdictFor(int vacantRooms, int vipWaiting) {
    if (vacantRooms == 0) {
      return "NO VACANT CLEAN ROOM OF THIS TYPE";
    }
    if (vacantRooms <= vipWaiting) {
      return "BLOCKED BY VIP BYPASS (needs [V] override)";
    }
    if (vipWaiting == 0) {
      return "LINE CAN BE SERVED (" + vacantRooms + " free, no VIP waiting)";
    }
    return "LINE CAN BE SERVED ("
        + (vacantRooms - vipWaiting)
        + " spare after "
        + vipWaiting
        + " VIP hold(s))";
  }

  private int countRowsOnPage(int totalMatches, int currentPage, int pageSize) {
    if (totalMatches == 0) return 0;
    int startIndex = (currentPage - 1) * pageSize + 1;
    if (startIndex > totalMatches) return 0;
    return Math.min(startIndex + pageSize - 1, totalMatches) - startIndex + 1;
  }

  @SuppressWarnings("null")
  private void printLineTable(
      QueueInterface<Reservation> queue,
      ListInterface<Reservation> lineRows,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      Room.RoomType roomType,
      int currentPage,
      int pageSize) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(LINE_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setTruncateAt(3, LINE_WIDTHS[3] - 2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(LINE_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER);

    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"THE LINE (FIFO ORDER)"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "POS", "RES ID", "GUEST NAME", "TIER", "WAITED", "STRIKES"},
        headerSettings);

    int totalMatches = (lineRows == null) ? 0 : lineRows.getNumberOfEntries();
    if (totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {"*** NOBODY IS STANDING IN THE " + roomType.name() + " LINE ***"},
          spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Reservation r = lineRows.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;

      // Read straight off the queue so the true place in line shows even when the table is
      // sorted by something else.
      int position = queue.getPosition(r);

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            (position == -1) ? "-" : String.valueOf(position),
            r.getReservationId(),
            (g != null) ? g.getName() : "N/A",
            (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
            formatWait(r.getQueueArrivalTime()),
            String.valueOf((g != null) ? g.getStrikeCount() : 0)
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  @SuppressWarnings("null")
  private void printHoldTable(
      ListInterface<Reservation> holds,
      ListInterface<Guest> guestList,
      ListInterface<Room> roomList,
      int graceMinutes) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(HOLD_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.RIGHT)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setTruncateAt(2, HOLD_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(HOLD_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"HOLDS AWAITING CHECK-IN (" + graceMinutes + " MINUTE GRACE)"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "RES ID", "GUEST NAME", "ROOM", "HELD FOR", "EXPIRES IN"},
        headerSettings);

    int total = (holds == null) ? 0 : holds.getNumberOfEntries();
    if (total == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(new String[] {"*** NO ROOMS ARE ON HOLD ***"}, spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    for (int i = 1; i <= total; i++) {
      Reservation r = holds.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());
      Room heldRoom = findRoomByConfirmation(roomList, r.getConfirmationNumber());

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i),
            r.getReservationId(),
            (g != null) ? g.getName() : "N/A",
            (heldRoom != null) ? heldRoom.getRoomNumber() : "UNLINKED",
            formatWait(r.getAllocatedTime()),
            formatRemaining(r.getAllocatedTime(), graceMinutes)
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  public void displaySweepNotice(int lapsedCount, int graceMinutes) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GRACE WINDOW LAPSED", SCREEN_WIDTH);

    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: HOLDS RELEASED"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Holds Lapsed", String.valueOf(lapsedCount), true);
    printKeyValue(
        kvSettings,
        "System Notice",
        "These guests did not check in within "
            + graceMinutes
            + " minutes. Each room has been released, a strike issued, and the guest sent to"
            + " the back of the line. A third strike closes the booking as a no-show.",
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public String promptAddWalkInInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REGISTER WALK-IN GUEST", SCREEN_WIDTH);
    System.out.println("[Enter 'C' to Cancel]\n");
    return ConsoleUtil.getStringInput("Enter Guest ID or Name: ");
  }

  // A walk-in is by definition someone who did not book, so "not found" is the expected case
  // for a first-time arrival rather than an error. The clerk is offered a guest file instead
  // of being sent back to retype the same search.
  public int displayGuestNotFoundScreen(String searchedTerm) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GUEST NOT ON RECORD", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] NO MATCHING GUEST FILE",
        "Searched Term",
        searchedTerm,
        "No registered guest matches this guest ID or name. A first-time walk-in has no file"
            + " yet, so one can be opened now before they join the line.");

    System.out.println("1. Register This Person As A New Guest");
    System.out.println("2. Search Again");
    System.out.println("3. Cancel\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  // The tier used to be printed as one more row on the confirmation screen, which let a
  // Diamond member be dropped into the ordinary line without anyone noticing. Priority is a
  // decision, so it is raised as its own screen before the line is ever offered.
  public int displayVipArrivalScreen(
      Guest guest,
      Member member,
      Room.RoomType roomType,
      int vacantRooms,
      int vipWaiting,
      int lineLength,
      boolean canServeNow) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PRIORITY GUEST DETECTED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] " + member.getTier().name() + " MEMBER AT THE COUNTER"},
        spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        member.getTier().name() + " (" + member.getMemberId() + ")",
        true);
    printKeyValue(kvSettings, "Strike Count", String.valueOf(guest.getStrikeCount()), true);
    printKeyValue(kvSettings, "Requested Type", roomType.name(), true);
    printKeyValue(kvSettings, "Vacant Clean Rooms", String.valueOf(vacantRooms), true);
    printKeyValue(kvSettings, "VIP Already Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(kvSettings, "Standard Line Length", String.valueOf(lineLength), false);

    System.out.println();

    if (canServeNow) {
      printNoticeBox(
          "RECOMMENDED ACTION",
          "Verdict",
          "SERVE NOW, DO NOT QUEUE",
          "This guest holds a loyalty tier, so the standard FIFO line is the wrong place for"
              + " them. A vacant clean "
              + roomType.name()
              + " room is free even after every waiting VIP is covered, so a room can be handed"
              + " over immediately without displacing anyone.");
    } else if (vacantRooms == 0) {
      printNoticeBox(
          "RECOMMENDED ACTION",
          "Verdict",
          "NO ROOM TO GIVE",
          "This guest holds a loyalty tier, but no vacant clean "
              + roomType.name()
              + " room exists at all. Place them on the VIP waitlist in the VIP module so the"
              + " priority score decides their turn, rather than the back of this line.");
    } else {
      printNoticeBox(
          "RECOMMENDED ACTION",
          "Verdict",
          "VIP BYPASS IN EFFECT",
          vipWaiting
              + " VIP guest(s) are already waiting against only "
              + vacantRooms
              + " vacant clean room(s), so every free room of this type is spoken for. Place"
              + " this guest on the VIP waitlist so the priority score ranks them against those"
              + " already waiting.");
    }

    if (canServeNow) {
      System.out.println("1. Assign A Room Now (Skip The Line)");
    } else {
      System.out.println("1. Assign A Room Now (UNAVAILABLE)");
    }
    System.out.println("2. Add To The Standard Walk-In Line Anyway");
    System.out.println("3. Cancel (Handle Through The VIP Module)\n");

    while (true) {
      try {
        return ConsoleUtil.getMenuInput("Choose an option: ", canServeNow ? 1 : 2, 3).getAsInt();
      } catch (IllegalArgumentException e) {
        System.out.println(
            "No room can be handed over right now. Choose 2 to queue them, or 3 to cancel.");
      }
    }
  }

  public boolean displayVipDirectAssignConfirmationScreen(
      Guest guest, Member member, Room room, int graceMinutes, int vacantRooms, int vipWaiting) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM PRIORITY ROOM ASSIGNMENT", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ROOM HAND-OVER"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest", guest.getName() + " (" + guest.getGuestId() + ")", true);
    printKeyValue(kvSettings, "Loyalty Tier", member.getTier().name(), true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Room Type", room.getRoomType().name(), true);
    printKeyValue(kvSettings, "Rooms Left After", String.valueOf(vacantRooms - 1), true);
    printKeyValue(kvSettings, "VIP Still Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(
        kvSettings,
        "Hold Window",
        graceMinutes + " minutes before the room is released and a strike is recorded",
        false);

    System.out.println();
    System.out.println("The guest never enters the line. The room is held immediately and the");
    System.out.println("booking appears under [C] Check In Held Guest.");
    System.out.println();

    return promptConfirm("Hand room " + room.getRoomNumber() + " to this guest now? (Y/N): ");
  }

  public void displayVipDirectAssignSuccessScreen(
      Reservation reservation, Guest guest, Member member, Room room, int graceMinutes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("PRIORITY ROOM ASSIGNED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ALLOCATED WITHOUT QUEUING"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", reservation.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", reservation.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", guest.getName(), true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (member != null && member.getTier() != null) ? member.getTier().name() : "NON-MEMBER",
        true);
    printKeyValue(kvSettings, "Room Number", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Room Status", room.getStatus().name(), true);
    printKeyValue(kvSettings, "Hold Expires In", graceMinutes + " minutes", false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayAlreadyInLineScreen(Guest guest, Reservation existing, int position) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALREADY IN LINE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] DUPLICATE QUEUE ENTRY REFUSED",
        "Target Guest",
        guest.getName() + " (" + guest.getGuestId() + ")",
        "This guest is already standing in this line at position "
            + position
            + " under reservation "
            + existing.getReservationId()
            + ". Serve that entry instead of creating a second one.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayAddWalkInConfirmationScreen(
      Guest g, Member m, Room.RoomType roomType, int projectedPosition, int waiting, int capacity) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM WALK-IN REGISTRATION", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST DETAILS"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", g.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", g.getName(), true);
    printKeyValue(
        kvSettings,
        "IC / Passport No",
        (g.getIcNumber() != null) ? g.getIcNumber() : g.getPassportNumber(),
        true);
    printKeyValue(
        kvSettings,
        "Phone Number",
        (g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A",
        true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
        true);
    printKeyValue(kvSettings, "Strike Count", String.valueOf(g.getStrikeCount()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"QUEUE PLACEMENT"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Target Line", roomType.name(), true);
    printKeyValue(kvSettings, "Currently Waiting", String.valueOf(waiting), true);
    printKeyValue(kvSettings, "Array Capacity", waiting + " / " + capacity + " slots used", true);
    printKeyValue(
        kvSettings,
        "Position On Joining",
        projectedPosition + " (joins the back of the line)",
        false);

    System.out.println();
    return promptConfirm("Register this walk-in into the " + roomType.name() + " line? (Y/N): ");
  }

  public void displayAddWalkInSuccessScreen(
      Reservation r, Guest g, int position, int waiting, int capacity) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("WALK-IN REGISTERED", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: ENQUEUED"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Place In Line", String.valueOf(position), true);
    printKeyValue(kvSettings, "Line Length", waiting + " / " + capacity + " slots used", false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public void displayNoVacantRoomScreen(Room.RoomType roomType, int vipWaiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NO ROOM AVAILABLE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] NOTHING TO ALLOCATE",
        "Vacant / VIP Waiting",
        "0 vacant  vs  " + vipWaiting + " VIP waiting",
        "There is no vacant clean "
            + roomType.name()
            + " room to give away, so there is nothing to override. Housekeeping must release a"
            + " room before this line can move.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayAllocationBlockedScreen(
      Room.RoomType roomType, int vacantRooms, int vipWaiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALLOCATION BLOCKED", SCREEN_WIDTH);

    printNoticeBox(
        "STATUS: [!] VIP BYPASS IN EFFECT",
        "Vacant / VIP Waiting",
        vacantRooms + " vacant  vs  " + vipWaiting + " VIP waiting",
        vipWaiting
            + " high tier member(s) are waiting for a "
            + roomType.name()
            + " room against only "
            + vacantRooms
            + " vacant clean room(s), so every free room of this type is already spoken for."
            + " Serve them from the VIP module, or use the [V] Override VIP Bypass command if a"
            + " supervisor authorises taking one of those rooms.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayNothingToOverrideScreen(Room.RoomType roomType, int vacantRooms) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NOTHING TO OVERRIDE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] NO VIP BYPASS IN EFFECT",
        "Vacant Clean Rooms",
        String.valueOf(vacantRooms),
        "No high tier member is being displaced, so there is nothing to authorise. The "
            + roomType.name()
            + " line can be served normally with the [G] Allocate Next command.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayOverrideAuthorisationScreen(
      Room.RoomType roomType, Reservation front, Guest guest, int vacantRooms, int vipWaiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SUPERVISOR OVERRIDE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"STATUS: [!] AUTHORISATION REQUIRED TO BYPASS THE VIP QUEUE"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Room Type", roomType.name(), true);
    printKeyValue(kvSettings, "Vacant Clean Rooms", String.valueOf(vacantRooms), true);
    printKeyValue(kvSettings, "High Tier Members Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(
        kvSettings,
        "Guest To Be Served",
        ((guest != null) ? guest.getName() : "N/A") + "  (" + front.getReservationId() + ")",
        true);
    printKeyValue(kvSettings, "Waited", formatWait(front.getQueueArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Consequence",
        "One room that a high tier member is entitled to under the bypass rule will be given to"
            + " the front of the standard line instead.",
        false);

    System.out.println();
    return promptConfirm("Authorise this override? (Y/N): ");
  }

  public boolean displayAllocateConfirmationScreen(
      Reservation r,
      Guest g,
      Member m,
      Room room,
      int graceMinutes,
      int vipWaiting,
      boolean overridden) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM ROOM ALLOCATION", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"FRONT OF THE LINE"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
        true);
    printKeyValue(kvSettings, "Joined Line At", formatTime(r.getQueueArrivalTime()), true);
    printKeyValue(kvSettings, "Waited", formatWait(r.getQueueArrivalTime()), false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"ROOM & HOLD"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(
        kvSettings,
        "Room To Assign",
        room.getRoomNumber() + " (" + room.getRoomType().name() + ")",
        true);
    printKeyValue(kvSettings, "Rate Per Night", String.format("RM %.2f", room.getPrice()), true);
    printKeyValue(kvSettings, "VIP Waiting", String.valueOf(vipWaiting), true);
    printKeyValue(
        kvSettings,
        "Grace Window",
        graceMinutes + " minutes, then the room is released and a strike is issued",
        !overridden);

    if (overridden) {
      printKeyValue(
          kvSettings,
          "[!] Bypass Override",
          "This room is being taken ahead of "
              + vipWaiting
              + " waiting high tier member(s) on supervisor authority.",
          false);
    }

    System.out.println();
    return promptConfirm("Dequeue this guest and hold the room? (Y/N): ");
  }

  public void displayAllocateSuccessScreen(
      Reservation r, Guest g, Room room, int remaining, boolean overridden) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM HELD", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          overridden
              ? "STATUS: DEQUEUED AND HELD (VIP BYPASS OVERRIDDEN)"
              : "STATUS: DEQUEUED AND HELD"
        },
        spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Assigned Room", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Remaining In Line", remaining + " guest(s)", false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public Integer promptHoldSelection(
      ListInterface<Reservation> holds,
      ListInterface<Guest> guestList,
      ListInterface<Room> roomList,
      int graceMinutes) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK IN A HELD ROOM", SCREEN_WIDTH);

    // Opening this screen clears the queue screen the holds were listed on, so the table is
    // reprinted here. Asking for a number with nothing on screen to read is how the clerk ends up
    // checking in the wrong guest.
    printHoldTable(holds, guestList, roomList, graceMinutes);
    System.out.println();

    int holdCount = (holds == null) ? 0 : holds.getNumberOfEntries();
    return ConsoleUtil.getIntegerInput(
        "Enter the NO. of the hold to check in [1 - " + holdCount + "] (blank or 'C' to cancel): ",
        1,
        holdCount);
  }

  public Integer promptStayDays(Reservation r, Guest g, Room room) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK IN - DURATION OF STAY", SCREEN_WIDTH);
    System.out.println(" Reservation : " + r.getReservationId());
    System.out.println(" Guest       : " + ((g != null) ? g.getName() : "N/A"));
    System.out.println(" Room        : " + ((room != null) ? room.getRoomNumber() : "N/A"));
    System.out.println("------------------------------------------------------");
    System.out.println(" Press Enter or 'C' to cancel and return\n");

    return ConsoleUtil.getIntegerInput(" Enter duration of stay in nights [1 - 30]: ", 1, 30);
  }

  public void displayCheckInSuccessScreen(Reservation r, Guest g, Room room, int stayDays) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-IN COMPLETE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: CHECKED IN"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room", (room != null) ? room.getRoomNumber() : "N/A", true);
    printKeyValue(kvSettings, "Nights Booked", String.valueOf(stayDays), false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCloseQueueConfirmationScreen(Room.RoomType roomType, int waiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CLOSE THE LINE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] END OF BUSINESS CYCLE",
        "Guests Still Waiting",
        String.valueOf(waiting),
        "Closing the line cancels every guest still standing in it and empties the queue in one"
            + " operation. Completed and held bookings are not affected.");
    return promptConfirm("Cancel all " + waiting + " waiting guest(s) and clear the line? (Y/N): ");
  }

  public void displayCloseQueueSuccessScreen(Room.RoomType roomType, int closed) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("LINE CLOSED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: QUEUE CLEARED",
        "Bookings Cancelled",
        String.valueOf(closed),
        "The " + roomType.name() + " line is now empty and ready for the next business cycle.");
    ConsoleUtil.printContinueMessage();
  }

  public int displayRowActionSubmenu(Reservation r) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("QUEUE ACTION: " + r.getReservationId(), SCREEN_WIDTH);
    System.out.println("1. View Booking Details");
    System.out.println("2. Cancel Reservation (leaves the line)");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public void displayReservationDetailScreen(
      Reservation r, Guest g, Member m, int position, int lineLength) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING DETAILS", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Room Type", r.getRoomType().name(), true);
    printKeyValue(kvSettings, "Status", r.getStatus().name(), true);
    printKeyValue(kvSettings, "Booked At", formatTime(r.getReservationTime()), true);
    printKeyValue(kvSettings, "Joined Line At", formatTime(r.getQueueArrivalTime()), true);
    printKeyValue(kvSettings, "Waited So Far", formatWait(r.getQueueArrivalTime()), true);
    printKeyValue(
        kvSettings,
        "Place In Line",
        (position == -1) ? "Not in the line" : position + " of " + lineLength,
        false);

    System.out.println();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST"}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", (g != null) ? g.getGuestId() : r.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "Phone Number",
        (g != null && g.getPhoneNumber() != null) ? g.getPhoneNumber() : "N/A",
        true);
    printKeyValue(
        kvSettings,
        "Email Address",
        (g != null && g.getEmail() != null) ? g.getEmail() : "N/A",
        true);
    printKeyValue(
        kvSettings,
        "Loyalty Tier",
        (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER",
        true);
    printKeyValue(
        kvSettings, "Strike Count", String.valueOf((g != null) ? g.getStrikeCount() : 0), false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCancelConfirmationScreen(Reservation r, Guest g, int position) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM CANCELLATION", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] REMOVE FROM THE LINE",
        "Target Booking",
        r.getReservationId() + "  -  " + ((g != null) ? g.getName() : "N/A"),
        "This guest is at position "
            + position
            + ". Removing them closes the gap, so everyone behind moves up one place.");
    return promptConfirm("Cancel this reservation and remove it from the line? (Y/N): ");
  }

  public int displayFilterMainMenu(String search, String tier) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & LINE FILTERS", SCREEN_WIDTH);
    System.out.println("Active Search : [ " + (search == null ? "None" : search) + " ]");
    System.out.println("Active Tier   : [ " + (tier == null ? "ALL" : tier) + " ]\n");
    System.out.println("1. Text Search Submenu");
    System.out.println("2. Loyalty Tier Submenu");
    System.out.println("3. Reset All Filters");
    System.out.println("4. Apply and Return");
    System.out.println("5. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY", SCREEN_WIDTH);
    System.out.println(
        "Current Search: [ " + (currentQuery == null ? "None" : currentQuery) + " ]\n");
    System.out.println("Matches on guest name, reservation ID, confirmation code or phone.\n");
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
    ConsoleUtil.printTitleBox("TIER FILTER", SCREEN_WIDTH);
    System.out.println(
        "Current Selected Tier: [ " + (currentTier == null ? "ALL" : currentTier) + " ]\n");
    System.out.println("1. DIAMOND");
    System.out.println("2. GOLD");
    System.out.println("3. SILVER");
    System.out.println("4. NON-MEMBER");
    System.out.println("5. Show All");
    System.out.println("6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER", SCREEN_WIDTH);
    System.out.println("Sorting only reorders this table. The queue itself stays FIFO.\n");
    System.out.println("1. Queue Position (FIFO, the real order)");
    System.out.println("2. Wait Time (Longest -> Shortest)");
    System.out.println("3. Wait Time (Shortest -> Longest)");
    System.out.println("4. Guest Name (A -> Z)");
    System.out.println("5. Guest Name (Z -> A)");
    System.out.println("6. Strike Count (Highest -> Lowest)");
    System.out.println("7. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
    if (choice == 1) return "QUEUE POSITION (FIFO)";
    if (choice == 2) return "WAIT TIME (LONGEST -> SHORTEST)";
    if (choice == 3) return "WAIT TIME (SHORTEST -> LONGEST)";
    if (choice == 4) return "GUEST NAME (A -> Z)";
    if (choice == 5) return "GUEST NAME (Z -> A)";
    if (choice == 6) return "STRIKES (HIGHEST -> LOWEST)";
    return null;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = new TableUtil.TableSettings(KV_WIDTHS);
    TableUtil.TableSettings spanSettings =
        new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil wraps a cell at the full column width but only prints width - 2 characters and
  // hard-truncates the rest, so long values are pre-wrapped to the printable width here and
  // emitted one row at a time. Without this every wrapped line loses its last two characters.
  private void printKeyValue(
      TableUtil.TableSettings settings, String key, String value, boolean moreRowsFollow) {
    ListInterface<String> lines = TextUtil.wrapText(value, KV_WIDTHS[1] - 2);

    for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
      TableUtil.printTableRow(new String[] {(i == 1) ? key : "", lines.getEntry(i)}, settings);
    }

    TableUtil.printTableBorder(
        settings,
        moreRowsFollow ? TableUtil.BorderPosition.MIDDLE : TableUtil.BorderPosition.BOTTOM);
  }

  private boolean promptConfirm(String prompt) {
    while (true) {
      String choice = ConsoleUtil.getStringInput(prompt);
      if (choice != null) {
        if ("Y".equalsIgnoreCase(choice.trim())) return true;
        if ("N".equalsIgnoreCase(choice.trim())) return false;
      }
      System.out.println("Invalid input. Please enter 'Y' or 'N'.");
    }
  }

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }

  private String formatWait(LocalDateTime from) {
    if (from == null) return "N/A";

    long minutes = Duration.between(from, LocalDateTime.now()).toMinutes();
    if (minutes < 0) return "0m";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  private String formatRemaining(LocalDateTime allocatedTime, int graceMinutes) {
    if (allocatedTime == null) return "N/A";

    long secondsLeft =
        (graceMinutes * 60L) - Duration.between(allocatedTime, LocalDateTime.now()).toSeconds();
    if (secondsLeft <= 0) return "LAPSED";
    return (secondsLeft / 60) + "m " + String.format("%02ds", secondsLeft % 60);
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) return g;
    }
    return null;
  }

  private Room findRoomByConfirmation(ListInterface<Room> roomList, String confirmationNumber) {
    if (roomList == null || confirmationNumber == null) return null;
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room room = roomList.getEntry(i);
      if (room != null && confirmationNumber.equals(room.getReservationConfirmationNumber())) {
        return room;
      }
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
