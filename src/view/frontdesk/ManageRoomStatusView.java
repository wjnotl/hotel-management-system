package view.frontdesk;

import adt.ArrayList;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ManageRoomStatusView {

  // =========================================================================
  // DTOs — view never touches raw entities
  // =========================================================================

  public static class RoomRowDTO {
    public final String roomNumber;
    public final String roomType;
    public final String status;
    public final String pricePerNight;
    public final String guestName;
    public final String confirmationNumber;
    public final String reservationId;

    public RoomRowDTO(
        String roomNumber,
        String roomType,
        String status,
        String pricePerNight,
        String guestName,
        String confirmationNumber) {
      this(roomNumber, roomType, status, pricePerNight, guestName, confirmationNumber, null);
    }

    public RoomRowDTO(
        String roomNumber,
        String roomType,
        String status,
        String pricePerNight,
        String guestName,
        String confirmationNumber,
        String reservationId) {
      this.roomNumber = orNA(roomNumber);
      this.roomType = orNA(roomType);
      this.status = orNA(status);
      this.pricePerNight = orNA(pricePerNight);
      this.guestName = orNA(guestName);
      this.confirmationNumber = orNA(confirmationNumber);
      this.reservationId = orNA(reservationId);
    }

    private static String orNA(String s) {
      return (s != null && !s.isEmpty()) ? s : "N/A";
    }
  }

  public static class RoomDetailDTO {
    public final String roomNumber;
    public final String roomType;
    public final String status;
    public final String pricePerNight;
    public final String reservationId;
    public final String confirmationNumber;
    public final String guestInfo;

    public RoomDetailDTO(
        String roomNumber,
        String roomType,
        String status,
        String pricePerNight,
        String reservationId,
        String confirmationNumber,
        String guestInfo) {
      this.roomNumber = orNA(roomNumber);
      this.roomType = orNA(roomType);
      this.status = orNA(status);
      this.pricePerNight = orNA(pricePerNight);
      this.reservationId = orNA(reservationId);
      this.confirmationNumber = orNA(confirmationNumber);
      this.guestInfo = orNA(guestInfo);
    }

    private static String orNA(String s) {
      return (s != null && !s.isEmpty()) ? s : "N/A";
    }
  }

  // =========================================================================
  // MAIN ROOM LIST SCREEN
  // =========================================================================

  public GetMenuInputResult renderRoomStatusScreen(
      ArrayList<RoomRowDTO> rooms,
      String searchQuery,
      String roomTypeFilter,
      String roomStatusFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE ROOM STATUS", 121);

    System.out.println(
        "SEARCH FILTER   : [ "
            + (searchQuery == null || searchQuery.isEmpty() ? "None" : "\"" + searchQuery + "\"")
            + " ]");
    System.out.println(
        "ROOM TYPE       : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println(
        "ROOM STATUS     : [ " + (roomStatusFilter == null ? "ALL" : roomStatusFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]");
    System.out.println();

    int total = (rooms == null) ? 0 : rooms.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 10, 14, 14, 12, 20, 14};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.RIGHT)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.LEFT)
            .setHAlign(7, TableUtil.Align.CENTER)
            .setTruncate(6);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "ROOM NO.", "ROOM TYPE", "STATUS", "PRICE/NIGHT", "RES. ID", "GUEST NAME", "CONFIRM NO."
        },
        settings);

    if (total == 0 || rooms == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {119}).setHAlign(0, TableUtil.Align.CENTER);
      boolean hasFilters =
          searchQuery != null || roomTypeFilter != null || roomStatusFilter != null;
      String msg =
          hasFilters ? "*** NO ROOMS MATCH ACTIVE FILTERS ***" : "*** NO ROOMS IN SYSTEM ***";
      TableUtil.printTableRow(new String[] {msg}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      System.out.println("\nPage 0 / 0 (Total: 0)\n");
      System.out.println("[S] Search & Filter     [O] Change Sort     [R] Refresh     [E] Exit\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'O', 'R', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      RoomRowDTO dto = rooms.getEntry(i);
      if (dto == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            dto.roomNumber,
            dto.roomType,
            dto.status,
            dto.pricePerNight,
            dto.reservationId,
            dto.guestName,
            dto.confirmationNumber
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Search & Filter     [O] Change Sort     [R] Refresh");
    System.out.println("[P] Prev Page           [N] Next Page       [E] Exit to Front Desk\n");

    int maxDisplayNum = endIndex - startIndex + 1;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Select row or command (" + rangeStr + "): ",
        1,
        maxDisplayNum,
        new char[] {'S', 'O', 'R', 'N', 'P', 'E'});
  }

  // showing available room table
  public GetMenuInputResult renderAvailableRoomsTable(
      ArrayList<RoomRowDTO> rooms, RoomDetailDTO currentRoom, int currentPage, int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT TARGET ROOM", 80);

    System.out.println(
        "Moving from : Room "
            + currentRoom.roomNumber
            + "  ("
            + currentRoom.roomType
            + ")  —  "
            + currentRoom.guestInfo);
    System.out.println("Only VACANT_CLEAN rooms are shown below.\n");

    int total = (rooms == null) ? 0 : rooms.getNumberOfEntries();
    int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));

    int[] colWidths = {4, 10, 10, 14, 12};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.RIGHT);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM NO.", "ROOM TYPE", "STATUS", "PRICE/NIGHT"}, settings);

    if (total == 0 || rooms == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {53}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO AVAILABLE ROOMS ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
      System.out.println("\nPress C to cancel.\n");
      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'C'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);

    for (int i = startIndex; i <= endIndex; i++) {
      RoomRowDTO dto = rooms.getEntry(i);
      if (dto == null) continue;
      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum), dto.roomNumber, dto.roomType, dto.status, dto.pricePerNight
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("\nPage %d / %d (Total Available: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[P] Prev Page     [N] Next Page     [C] Cancel\n");

    int maxDisplayNum = endIndex - startIndex + 1;
    String rangeStr = (maxDisplayNum == 1) ? "1" : "1-" + maxDisplayNum;
    return ConsoleUtil.getMenuInput(
        "Select row to move into (" + rangeStr + "): ",
        1,
        maxDisplayNum,
        new char[] {'N', 'P', 'C'});
  }

  // filter menu
  public int displayFilterMenu(String search, String roomType, String roomStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & FILTER ROOMS", 68);
    System.out.println(
        "Search Query   : [ "
            + (search == null || search.isEmpty() ? "None" : "\"" + search + "\"")
            + " ]");
    System.out.println("Room Type      : [ " + (roomType == null ? "ALL" : roomType) + " ]");
    System.out.println("Room Status    : [ " + (roomStatus == null ? "ALL" : roomStatus) + " ]\n");
    System.out.println("1.Search");
    System.out.println("2. Filter by Room Type");
    System.out.println("3. Filter by Room Status");
    System.out.println("4. Clear All Filters");
    System.out.println("5. Apply & Back\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 5).getAsInt();
  }

  public String promptSearchInput(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH ROOMS", 68);
    System.out.println(
        "Current : [ "
            + (current == null || current.isEmpty() ? "None" : "\"" + current + "\"")
            + " ]");
    System.out.println();
    System.out.println("Can Search by Room Number, Guest Name, or Confirmation Number");
    System.out.println("Press Enter to clear/ C to cancel.\n");
    String input = ConsoleUtil.getStringInput("Search: ");
    if (input == null || "C".equalsIgnoreCase(input.trim())) return current;
    return input.trim().isEmpty() ? null : input.trim();
  }

  public int displayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM TYPE", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 4).getAsInt();
  }

  public int displayRoomStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM STATUS", 50);
    System.out.println("Current : [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. DIRTY");
    System.out.println("2. CLEANING");
    System.out.println("3. INSPECTED");
    System.out.println("4. VACANT_CLEAN  (Available)");
    System.out.println("5. OCCUPIED");
    System.out.println("6. Show All\n");
    return ConsoleUtil.getMenuInput("Choose option: ", 1, 6).getAsInt();
  }

  // sorting menu
  public String displaySortMenu(String currentSort) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT ORDER", 60);
    System.out.println("Current : [ " + currentSort + " ]\n");
    System.out.println("1. Room Number  (Low -> High)");
    System.out.println("2. Room Number  (High -> Low)");
    System.out.println("3. Room Type    (A -> Z)");
    System.out.println("4. Room Type    (Z -> A)");
    System.out.println("5. Status       (A -> Z)");
    System.out.println("6. Status       (Z -> A)");
    System.out.println("7. Price        (Low -> High)");
    System.out.println("8. Price        (High -> Low)");
    System.out.println("9. Cancel\n");

    int choice = ConsoleUtil.getMenuInput("Choose option: ", 1, 9).getAsInt();
    switch (choice) {
      case 1:
        return "ROOM NUMBER (LOW -> HIGH)";
      case 2:
        return "ROOM NUMBER (HIGH -> LOW)";
      case 3:
        return "ROOM TYPE (A -> Z)";
      case 4:
        return "ROOM TYPE (Z -> A)";
      case 5:
        return "STATUS (A -> Z)";
      case 6:
        return "STATUS (Z -> A)";
      case 7:
        return "PRICE (LOW -> HIGH)";
      case 8:
        return "PRICE (HIGH -> LOW)";
      default:
        return null;
    }
  }

  // Room action submenu
  public int displayRoomActionSubmenu(RoomDetailDTO dto) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE ROOM: " + dto.roomNumber, 80);

    int[] kvWidths = {22, 52};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"Room Type", dto.roomType}, kvSettings);
    printKvRow("Status", dto.status, kvSettings);
    printKvRow("Price / Night (RM)", dto.pricePerNight, kvSettings);
    printKvRow("Reservation ID", dto.reservationId, kvSettings);
    printKvRow("Confirmation No.", dto.confirmationNumber, kvSettings);
    printKvRow("Guest", dto.guestInfo, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\n-- Room Actions --");
    System.out.println("1. Change Room          (move reservation to another room)");
    System.out.println("2. Mark as VACANT_CLEAN (Available)");
    System.out.println("3. Mark as OCCUPIED     (manual override)");
    System.out.println("4. Mark as DIRTY        (flag for housekeeping)");
    System.out.println("5. Back to Room List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // =========================================================================
  // SUCCESS SCREENS
  // =========================================================================

  public void displayChangeRoomSuccess(
      String oldRoomNumber, String newRoomNumber, String oldStatus, String confirmationNumber) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM CHANGED", 72);

    int[] kvWidths = {24, 46};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"Confirmation No.", confirmationNumber}, kvSettings);
    printKvRow("Moved From", "Room " + oldRoomNumber + "  →  Now DIRTY", kvSettings);
    printKvRow("Moved To", "Room " + newRoomNumber + "  →  Now OCCUPIED", kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\nOld room marked DIRTY — pending housekeeping inspection.\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  public void displayStatusChangeSuccess(
      String roomNumber, String roomType, String newStatusLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STATUS UPDATED", 60);
    System.out.println("Room " + roomNumber + "  (" + roomType + ")  →  " + newStatusLabel + "\n");
    ConsoleUtil.printContinueMessage("Press Enter to return...");
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  private void printKvRow(String label, String value, TableUtil.TableSettings settings) {
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(new String[] {label, value}, settings);
  }
}
