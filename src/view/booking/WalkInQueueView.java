package view.booking;

import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;
import util.TextUtil;

public class WalkInQueueView {

  // Column widths must sum to 96 - 3n to frame to the same width as the spanned heading above.
  private static final int[] LINE_WIDTHS = {5, 5, 12, 22, 12, 10, 9};
  private static final int[] HOLD_WIDTHS = {5, 12, 24, 11, 12, 14};
  private static final int[] SPAN_WIDTH = {93};
  private static final int[] KV_WIDTHS = {22, 68};
  private static final int SCREEN_WIDTH = 83;
  private static final int HOLDS_PREVIEW_ROWS = 5;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static class LineRowDTO {
    private final String position;
    private final String reservationId;
    private final String guestName;
    private final String phoneNumber;
    private final String waited;
    private final int strikes;

    public LineRowDTO(
        String position,
        String reservationId,
        String guestName,
        String phoneNumber,
        String waited,
        int strikes) {
      this.position = position;
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.phoneNumber = phoneNumber;
      this.waited = waited;
      this.strikes = strikes;
    }

    public String getPosition() {
      return position;
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

    public String getWaited() {
      return waited;
    }

    public int getStrikes() {
      return strikes;
    }
  }

  public static class HoldRowDTO {
    private final String reservationId;
    private final String guestName;
    private final String roomNumber;
    private final String heldFor;
    private final String expiresIn;

    public HoldRowDTO(
        String reservationId,
        String guestName,
        String roomNumber,
        String heldFor,
        String expiresIn) {
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.roomNumber = roomNumber;
      this.heldFor = heldFor;
      this.expiresIn = expiresIn;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getRoomNumber() {
      return roomNumber;
    }

    public String getHeldFor() {
      return heldFor;
    }

    public String getExpiresIn() {
      return expiresIn;
    }
  }

  public Room.RoomType displayQueueSelectionMenu() {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("WALK-IN QUEUE - SELECT LINE", SCREEN_WIDTH);
        System.out.println("1. Luxury Room Walk-In Queue");
        System.out.println("2. Suite Room Walk-In Queue");
        System.out.println("3. Standard Room Walk-In Queue");
        System.out.println("4. Back to Walk-In & Booking Menu\n");

        int choice = ConsoleUtil.getMenuInput("Choose a line to manage: ", 1, 4).getAsInt();
        if (choice == 1) return Room.RoomType.LUXURY;
        if (choice == 2) return Room.RoomType.SUITE;
        if (choice == 3) return Room.RoomType.STANDARD;
        return null;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public GetMenuInputResult renderQueueScreen(
      ListInterface<LineRowDTO> lineRows,
      ListInterface<HoldRowDTO> holds,
      Room.RoomType roomType,
      String nextUp,
      int waiting,
      int queueCapacity,
      boolean queueFull,
      int vacantRooms,
      int arrivingToday,
      int vipWaiting,
      int graceMinutes,
      String searchField,
      String searchTerm,
      String matchMode,
      Integer minWaitMinutes,
      String sort,
      int currentPage,
      int pageSize,
      boolean enforceVipBypass) {
    while (true) {
      try {

        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("WALK-IN QUEUE - [" + roomType.name() + " ROOMS]", SCREEN_WIDTH);

        System.out.println(
            "WAITING IN LINE   : "
                + waiting
                + " / "
                + queueCapacity
                + " array slots"
                + (queueFull ? "   [FULL - next enqueue doubles the array]" : ""));
        System.out.println("NEXT UP (peek)    : " + nextUp);
        System.out.println(
            "ROOMS             : "
                + vacantRooms
                + " vacant clean, "
                + arrivingToday
                + " held for arrivals today");
        System.out.println(
            "VIP AHEAD OF LINE : "
                + vipWaiting
                + "   ->  "
                + verdictFor(vacantRooms - arrivingToday, vipWaiting, enforceVipBypass));
        System.out.println(
            "SEARCH            : "
                + (searchTerm == null
                    ? "[ None ]"
                    : "[ " + searchField + " " + matchMode + " \"" + searchTerm + "\" ]"));
        System.out.println(
            "MIN WAIT FILTER   : [ "
                + (minWaitMinutes == null ? "None" : minWaitMinutes + " minutes")
                + " ]");
        System.out.println("SORT CRITERIA     : [ " + sort + " ]\n");

        printLineTable(lineRows, roomType, currentPage, pageSize);
        System.out.println();
        printHoldTable(holds, graceMinutes, 1, HOLDS_PREVIEW_ROWS, true);

        int totalMatches = (lineRows == null) ? 0 : lineRows.getNumberOfEntries();
        int totalPages =
            (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);
        System.out.printf(
            "%nPage %d / %d (Matches: %d)   Holds: %d%n%n",
            (totalPages == 0) ? 0 : currentPage,
            totalPages,
            totalMatches,
            (holds == null) ? 0 : holds.getNumberOfEntries());

        System.out.println("[A] Add Walk-In        [G] Allocate Next       [C] Check In Hold");
        System.out.println("[S] Search / Filter    [O] Change Sort Order   [R] Refresh");
        System.out.println("[P] Prev Page          [N] Next Page           [X] Close Queue");
        System.out.println("[E] Exit to Line Menu\n");
        System.out.println("Pick a row number to view, allocate or cancel that booking.\n");

        char[] commands = {'A', 'G', 'C', 'S', 'O', 'X', 'P', 'N', 'R', 'E'};

        int rowsOnPage = countRowsOnPage(totalMatches, currentPage, pageSize);
        if (rowsOnPage <= 0) {
          return ConsoleUtil.getMenuInput("Enter a command: ", commands);
        }

        String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
        return ConsoleUtil.getMenuInput(
            "Enter a command or select a row (" + range + "): ", 1, rowsOnPage, commands);
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Rooms are held back one per waiting VIP rather than the whole type being frozen, so a line can
  // legitimately be servable while VIPs are still waiting.
  private String verdictFor(int freeToCounter, int vipWaiting, boolean enforceVipBypass) {
    if (freeToCounter <= 0) {
      return "NO ROOM FREE FOR THIS LINE";
    }
    if (!enforceVipBypass) {
      return "LINE CAN BE SERVED (" + freeToCounter + " free, VIP bypass switched off)";
    }
    if (freeToCounter <= vipWaiting) {
      return "BLOCKED BY VIP BYPASS (override from a row)";
    }
    if (vipWaiting == 0) {
      return "LINE CAN BE SERVED (" + freeToCounter + " free, no VIP waiting)";
    }
    return "LINE CAN BE SERVED ("
        + (freeToCounter - vipWaiting)
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

  private void printLineTable(
      ListInterface<LineRowDTO> lineRows, Room.RoomType roomType, int currentPage, int pageSize) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(LINE_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setTruncateAt(3, LINE_WIDTHS[3] - 2)
            .setTruncateAt(4, LINE_WIDTHS[4] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(LINE_WIDTHS);
    TableUtil.TableSettings spanSettings = spanSettings();

    TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"THE LINE (FIFO ORDER)"}, spanSettings);
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.SPAN_OPEN);
    TableUtil.printTableRow(
        new String[] {"NO.", "POS", "RES ID", "GUEST NAME", "PHONE", "WAITED", "STRIKES"},
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
      LineRowDTO row = lineRows.getEntry(i);
      if (row == null) continue;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(i - startIndex + 1),
            row.getPosition(),
            row.getReservationId(),
            row.getGuestName(),
            row.getPhoneNumber(),
            row.getWaited(),
            String.valueOf(row.getStrikes())
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  private void printHoldTable(
      ListInterface<HoldRowDTO> holds, int graceMinutes, int page, int pageSize, boolean preview) {

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(HOLD_WIDTHS)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.RIGHT)
            .setHAlign(5, TableUtil.Align.RIGHT)
            .setTruncateAt(2, HOLD_WIDTHS[2] - 2);

    TableUtil.TableSettings headerSettings = centeredHeader(HOLD_WIDTHS);
    TableUtil.TableSettings spanSettings = spanSettings();

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

    int startIndex = (page - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      HoldRowDTO row = holds.getEntry(i);
      if (row == null) continue;

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(preview ? i : i - startIndex + 1),
            row.getReservationId(),
            row.getGuestName(),
            row.getRoomNumber(),
            row.getHeldFor(),
            row.getExpiresIn()
          },
          settings);
    }

    if (preview && total > endIndex) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.printTableRow(
          new String[] {
            "*** " + (total - endIndex) + " MORE HOLD(S), OPEN [C] CHECK IN HOLD TO PAGE THEM ***"
          },
          spanSettings);
      TableUtil.printTableBorder(spanSettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      return;
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
  }

  public void displaySweepNotice(
      int lapsedCount, int graceMinutes, int maxStrikes, boolean requeueOnLapse) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GRACE WINDOW LAPSED", SCREEN_WIDTH);

    String outcome =
        requeueOnLapse
            ? "the guest sent to the back of the line. Strike "
                + maxStrikes
                + " closes the booking as a no-show."
            : "the booking closed as a no-show, because the house rule sends a lapsed hold"
                + " straight out rather than back into the line.";

    printNoticeBox(
        "STATUS: HOLDS RELEASED",
        "Holds Lapsed",
        String.valueOf(lapsedCount),
        "These guests did not check in within the grace window recorded on their hold, which is"
            + " currently "
            + graceMinutes
            + " minutes. Each room has been released, a strike issued, and "
            + outcome);

    ConsoleUtil.printContinueMessage();
  }

  public void displayNoVacantRoomScreen(Room.RoomType roomType, int arrivingToday, int vipWaiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("NO ROOM AVAILABLE", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] NOTHING TO ALLOCATE",
        "Rooms Free / Promised",
        "0 free to this line, " + arrivingToday + " held for today's arrivals",
        "There is no vacant clean "
            + roomType.name()
            + " room this line may take. Housekeeping must release a room, or an advance booking"
            + " arriving today has to be served or cancelled, before this line can move. "
            + vipWaiting
            + " VIP guest(s) are also waiting for this type.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayBypassBlockedScreen(
      Room.RoomType roomType, int freeToCounter, int vipWaiting) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ALLOCATION BLOCKED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [!] VIP BYPASS IN EFFECT",
        "Free / VIP Waiting",
        freeToCounter + " free  vs  " + vipWaiting + " VIP waiting",
        vipWaiting
            + " high tier member(s) are waiting for a "
            + roomType.name()
            + " room against only "
            + freeToCounter
            + " free room(s), so every one of them is already spoken for. Serve them from the VIP"
            + " module. The supervisor override is switched off under Settings & Configuration,"
            + " so no room can be taken from the bypass here.");
    ConsoleUtil.printContinueMessage();
  }

  public void displayNonFrontBlockedScreen(int position) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FIFO ORDER ENFORCED", SCREEN_WIDTH);
    printNoticeBox(
        "STATUS: [X] ONLY THE FRONT MAY BE SERVED",
        "Place In Line",
        String.valueOf(position),
        "Serving out of order is switched off under Settings & Configuration, so a room can only"
            + " go to position 1. Use [G] Allocate Next, or turn the override back on.");
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayAllocationOverrideScreen(
      Room.RoomType roomType,
      Reservation target,
      Guest guest,
      int position,
      int lineLength,
      int skippedCount,
      String skippedNames,
      int freeToCounter,
      int vipWaiting,
      boolean fifoSkip,
      boolean vipBypass) {
    while (true) {
      try {

        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("SUPERVISOR OVERRIDE", SCREEN_WIDTH);

        TableUtil.TableSettings kvSettings = kvSettings();

        TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
        TableUtil.printTableRow(
            new String[] {"STATUS: [!] AUTHORISATION REQUIRED"}, spanSettings());
        TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
        printKeyValue(kvSettings, "Room Type", roomType.name(), true);
        printKeyValue(
            kvSettings,
            "Guest To Be Served",
            ((guest != null) ? guest.getName() : "N/A") + "  (" + target.getReservationId() + ")",
            true);
        printKeyValue(kvSettings, "Place In Line", position + " of " + lineLength, true);
        printKeyValue(kvSettings, "Waited", formatWait(target.getQueueArrivalTime()), true);
        printKeyValue(kvSettings, "Rooms Free To This Line", String.valueOf(freeToCounter), true);
        printKeyValue(kvSettings, "High Tier Members Waiting", String.valueOf(vipWaiting), false);

        System.out.println();

        if (fifoSkip) {
          printNoticeBox(
              "REASON 1: FIFO ORDER WILL BE BROKEN",
              "Guests Skipped",
              String.valueOf(skippedCount),
              "This guest is not at the front of the line. Serving them now takes the room ahead of"
                  + " "
                  + skippedNames
                  + ", who all arrived earlier.");
        }

        if (vipBypass) {
          printNoticeBox(
              "REASON 2: VIP BYPASS WILL BE OVERRIDDEN",
              "Vacant / VIP Waiting",
              freeToCounter + " free  vs  " + vipWaiting + " VIP waiting",
              "Every free room of this type is spoken for by a high tier member under the bypass"
                  + " rule. One of those rooms will be given to the standard line instead.");
        }

        System.out.println("1. Authorise And Allocate The Room");
        System.out.println("2. Do Not Allocate\n");

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public boolean displayAllocateConfirmationScreen(
      Reservation r, Guest g, Room room, int graceMinutes, int vipWaiting, int position) {
    while (true) {
      try {

        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("CONFIRM ROOM ALLOCATION", SCREEN_WIDTH);

        TableUtil.TableSettings kvSettings = kvSettings();

        TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
        TableUtil.printTableRow(new String[] {"GUEST BEING SERVED"}, spanSettings());
        TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
        printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
        printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
        printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
        printKeyValue(
            kvSettings, "Phone Number", (g != null) ? blankToNa(g.getPhoneNumber()) : "N/A", true);
        printKeyValue(kvSettings, "Place In Line", String.valueOf(position), true);
        printKeyValue(kvSettings, "Joined Line At", formatTime(r.getQueueArrivalTime()), true);
        printKeyValue(kvSettings, "Waited", formatWait(r.getQueueArrivalTime()), false);

        System.out.println();

        TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
        TableUtil.printTableRow(new String[] {"ROOM & HOLD"}, spanSettings());
        TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
        printKeyValue(
            kvSettings,
            "Room To Assign",
            room.getRoomNumber() + " (" + room.getRoomType().name() + ")",
            true);
        printKeyValue(
            kvSettings, "Rate Per Night", String.format("RM %.2f", room.getPrice()), true);
        printKeyValue(kvSettings, "VIP Waiting", String.valueOf(vipWaiting), true);
        printKeyValue(
            kvSettings,
            "Grace Window",
            graceMinutes + " minutes, then the room is released and a strike is issued",
            false);

        System.out.println();
        System.out.println("1. Dequeue This Guest And Hold The Room");
        System.out.println("2. Do Not Allocate\n");

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void displayAllocateSuccessScreen(
      Reservation r, Guest g, Room room, int remaining, boolean overridden) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM HELD", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          overridden ? "STATUS: DEQUEUED AND HELD (OVERRIDE USED)" : "STATUS: DEQUEUED AND HELD"
        },
        spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Assigned Room", room.getRoomNumber(), true);
    printKeyValue(kvSettings, "Remaining In Line", remaining + " guest(s)", false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  // Returns the 1-based index into the whole hold list, or null when the clerk backed out.
  public Integer promptHoldSelection(
      ListInterface<HoldRowDTO> holds, int graceMinutes, int pageSize) {

    int page = 1;

    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("CHECK IN A HELD ROOM", SCREEN_WIDTH);

        int total = (holds == null) ? 0 : holds.getNumberOfEntries();
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);
        page = Math.min(Math.max(page, 1), Math.max(totalPages, 1));

        printHoldTable(holds, graceMinutes, page, pageSize, false);

        int rowsOnPage = countRowsOnPage(total, page, pageSize);
        System.out.printf(
            "%nPage %d / %d (Holds: %d)%n%n", (totalPages == 0) ? 0 : page, totalPages, total);
        System.out.println("[P] Prev Page   [N] Next Page   [E] Exit\n");

        char[] commands = {'P', 'N', 'E'};

        GetMenuInputResult result;
        if (rowsOnPage <= 0) {
          result = ConsoleUtil.getMenuInput("Enter a command: ", commands);
        } else {
          String range = (rowsOnPage == 1) ? "1" : "1-" + rowsOnPage;
          result =
              ConsoleUtil.getMenuInput(
                  "Pick the hold to check in (" + range + "): ", 1, rowsOnPage, commands);
        }

        if ("E".equalsIgnoreCase(result.input)) return null;

        if ("N".equalsIgnoreCase(result.input)) {
          if (page < totalPages) {
            page++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
          continue;
        }
        if ("P".equalsIgnoreCase(result.input)) {
          if (page > 1) {
            page--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
          continue;
        }

        return (page - 1) * pageSize + result.getAsInt();
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public Integer promptStayDays(
      Reservation r, Guest g, Room room, int maxStayNights, int availableNights) {
    while (true) {
      try {

        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("CHECK IN - DURATION OF STAY", SCREEN_WIDTH);
        System.out.println(" Reservation : " + r.getReservationId());
        System.out.println(" Guest       : " + ((g != null) ? g.getName() : "N/A"));
        System.out.println(" Room        : " + ((room != null) ? room.getRoomNumber() : "N/A"));
        System.out.println("------------------------------------------------------");

        if (availableNights < maxStayNights) {
          System.out.println(
              " The calendar allows "
                  + availableNights
                  + " night(s) from today before this room type is fully booked.");
        }

        System.out.println(" Type 'C' to cancel and return\n");

        return requireInt(
            " Enter duration of stay in nights [1 - " + availableNights + "]: ",
            1,
            availableNights);
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void displayCheckInSuccessScreen(
      Reservation r, Guest g, Room room, int stayDays, java.time.LocalDate dueOut) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHECK-IN COMPLETE", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"STATUS: CHECKED IN"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Reservation ID", r.getReservationId(), true);
    printKeyValue(kvSettings, "Confirmation Code", r.getConfirmationNumber(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(kvSettings, "Room", (room != null) ? room.getRoomNumber() : "N/A", true);
    printKeyValue(kvSettings, "Nights Booked", String.valueOf(stayDays), true);
    printKeyValue(
        kvSettings,
        "Due To Check Out",
        (dueOut != null) ? dueOut.format(DATE_FORMAT) : "N/A",
        false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCloseQueueConfirmationScreen(Room.RoomType roomType, int waiting) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("CLOSE THE LINE", SCREEN_WIDTH);
        printNoticeBox(
            "STATUS: [!] END OF BUSINESS CYCLE",
            "Guests Still Waiting",
            String.valueOf(waiting),
            "Closing the "
                + roomType.name()
                + " line cancels every guest still standing in it and empties the queue in one"
                + " operation. Completed and held bookings are not affected.");

        System.out.println("1. Cancel All " + waiting + " Waiting Guest(s) And Clear The Line");
        System.out.println("2. Leave The Line Alone\n");

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
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

  public int displayRowActionSubmenu(Reservation r, Guest g, int position, int lineLength) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("QUEUE ACTION: " + r.getReservationId(), SCREEN_WIDTH);

        System.out.println("Guest    : " + ((g != null) ? g.getName() : "N/A"));
        System.out.println("Position : " + position + " of " + lineLength);
        System.out.println("Waited   : " + formatWait(r.getQueueArrivalTime()) + "\n");

        System.out.println("1. View Booking Details");
        System.out.println("2. Allocate A Room To This Guest");
        System.out.println("3. Cancel Reservation (leaves the line)");
        System.out.println("4. Back\n");

        if (position > 1) {
          System.out.println("Serving this row skips " + (position - 1) + " guest(s) who arrived");
          System.out.println("earlier, so it has to be authorised.\n");
        }

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void displayReservationDetailScreen(Reservation r, Guest g, int position, int lineLength) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("BOOKING DETAILS", SCREEN_WIDTH);

    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"RESERVATION"}, spanSettings());
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

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"GUEST"}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, "Guest ID", (g != null) ? g.getGuestId() : r.getGuestId(), true);
    printKeyValue(kvSettings, "Guest Name", (g != null) ? g.getName() : "N/A", true);
    printKeyValue(
        kvSettings,
        "IC / Passport No",
        (g == null)
            ? "N/A"
            : (g.getIcNumber() != null) ? g.getIcNumber() : blankToNa(g.getPassportNumber()),
        true);
    printKeyValue(
        kvSettings, "Phone Number", (g != null) ? blankToNa(g.getPhoneNumber()) : "N/A", true);
    printKeyValue(kvSettings, "Email Address", (g != null) ? blankToNa(g.getEmail()) : "N/A", true);
    printKeyValue(
        kvSettings, "Strike Count", String.valueOf((g != null) ? g.getStrikeCount() : 0), false);

    System.out.println();
    ConsoleUtil.printContinueMessage();
  }

  public boolean displayCancelConfirmationScreen(Reservation r, Guest g, int position) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("CONFIRM CANCELLATION", SCREEN_WIDTH);
        printNoticeBox(
            "STATUS: [!] REMOVE FROM THE LINE",
            "Target Booking",
            r.getReservationId() + "  -  " + ((g != null) ? g.getName() : "N/A"),
            "This guest is at position "
                + position
                + ". Removing them closes the gap, so everyone behind moves up one place.");

        System.out.println("1. Cancel This Reservation And Remove It From The Line");
        System.out.println("2. Keep The Reservation\n");

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt() == 1;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public int displayFilterMainMenu(
      String searchField, String searchTerm, String matchMode, Integer minWaitMinutes) {
    while (true) {
      try {

        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("SEARCH & LINE FILTERS", SCREEN_WIDTH);
        System.out.println("Search Field : [ " + searchField + " ]");
        System.out.println("Search Term  : [ " + (searchTerm == null ? "None" : searchTerm) + " ]");
        System.out.println("Match Mode   : [ " + matchMode + " ]");
        System.out.println(
            "Minimum Wait : [ "
                + (minWaitMinutes == null ? "None" : minWaitMinutes + " minutes")
                + " ]\n");

        System.out.println("1. Change Search Field");
        System.out.println("2. Change Search Term");
        System.out.println(
            "3. Switch Match Mode To " + ("EXACT".equals(matchMode) ? "CONTAINS" : "EXACT"));
        System.out.println("4. Minimum Wait Threshold");
        System.out.println("5. Reset All Filters");
        System.out.println("6. Apply And Return");
        System.out.println("7. Back (discard these changes)\n");

        return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public int displaySearchFieldSubmenu(String current) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("SEARCH FIELD", SCREEN_WIDTH);
        System.out.println("Current: [ " + current + " ]\n");
        System.out.println("Searching one field at a time keeps a partial phone number from");
        System.out.println("pulling in every IC that happens to contain the same digits.\n");
        System.out.println(" 1. Guest Name");
        System.out.println(" 2. Guest ID");
        System.out.println(" 3. IC Number");
        System.out.println(" 4. Passport Number");
        System.out.println(" 5. Phone Number");
        System.out.println(" 6. Email Address");
        System.out.println(" 7. Reservation ID");
        System.out.println(" 8. Confirmation Code");
        System.out.println(" 9. All Of The Above");
        System.out.println("10. Back\n");

        int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 10).getAsInt();
        return (choice == 10) ? 0 : choice;
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public String promptSearchTerm(String fieldLabel, String current) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("SEARCH TERM", SCREEN_WIDTH);
        System.out.println("Field   : [ " + fieldLabel + " ]");
        System.out.println("Current : [ " + (current == null ? "None" : current) + " ]\n");
        System.out.println("Type '-' to clear the term.");
        System.out.println("E - Exit and keep the current term\n");

        return requireText(
            "Search term: ",
            "Search term cannot be empty! Type '-' to clear it or 'E' to go back.");
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public Integer promptMinimumWait(Integer current) {
    while (true) {
      try {
        ConsoleUtil.clearScreen();
        ConsoleUtil.printTitleBox("MINIMUM WAIT THRESHOLD", SCREEN_WIDTH);
        System.out.println(
            "Current: [ " + (current == null ? "None" : current + " minutes") + " ]\n");
        System.out.println("Only guests who have waited at least this long stay on the screen.");
        System.out.println("Enter 0 to clear the threshold.");
        System.out.println("Type 'C' to keep the current threshold\n");

        return requireInt("Minimum wait in minutes [0 - 1440]: ", 0, 1440);
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public String displaySortMenu() {
    while (true) {
      try {
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
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }

  private TableUtil.TableSettings kvSettings() {
    return new TableUtil.TableSettings(KV_WIDTHS);
  }

  private TableUtil.TableSettings spanSettings() {
    return new TableUtil.TableSettings(SPAN_WIDTH).setHAlign(0, TableUtil.Align.CENTER);
  }

  private TableUtil.TableSettings centeredHeader(int[] widths) {
    TableUtil.TableSettings settings = new TableUtil.TableSettings(widths);
    for (int i = 0; i < widths.length; i++) {
      settings.setHAlign(i, TableUtil.Align.CENTER);
    }
    return settings;
  }

  private void printNoticeBox(String heading, String key, String value, String notice) {
    TableUtil.TableSettings kvSettings = kvSettings();

    TableUtil.printTableBorder(spanSettings(), TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {heading}, spanSettings());
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.SPAN_OPEN);
    printKeyValue(kvSettings, key, value, true);
    printKeyValue(kvSettings, "System Notice", notice, false);
    System.out.println();
  }

  // TableUtil prints only width - 2 chars per cell, so long values are pre-wrapped here.
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

  private String formatTime(LocalDateTime dateTime) {
    if (dateTime == null) return "N/A";
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
  }

  private String formatWait(LocalDateTime from) {
    if (from == null) return "N/A";

    long minutes = java.time.Duration.between(from, LocalDateTime.now()).toMinutes();
    if (minutes < 0) return "0m";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  // Blank is an error rather than a silent cancel, so Enter never quietly discards what the clerk
  // was part way through. 'C' stays the explicit way out.
  private Integer requireInt(String prompt, int min, int max) {
    ConsoleUtil.GetMenuInputResult result =
        ConsoleUtil.getMenuInput(prompt, min, max, new char[] {'C'});
    return result.isNumber ? Integer.valueOf(result.getAsInt()) : null;
  }

  // A blank line is rejected here rather than in the controller, so the error redraws this screen
  // instead of the menu above it.
  private String requireText(String prompt, String emptyMessage) {
    String typed = ConsoleUtil.getStringInput(prompt);
    if (typed == null || typed.trim().isEmpty()) {
      throw new IllegalArgumentException(emptyMessage);
    }
    return typed;
  }
}
