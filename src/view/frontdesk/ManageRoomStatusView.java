package view.frontdesk;

import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class ManageRoomStatusView {

  public GetMenuInputResult renderRoomStatusScreen(
      ListInterface<Room> rooms,
      String roomNumberFilter,
      String roomTypeFilter,
      String roomStatusFilter,
      String sortCriteria,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE ROOM STATUS", 90);

    System.out.println(
        "ROOM NO. FILTER : [ " + (roomNumberFilter == null ? "None" : roomNumberFilter) + " ]");
    System.out.println(
        "ROOM TYPE       : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println(
        "ROOM STATUS     : [ " + (roomStatusFilter == null ? "ALL" : roomStatusFilter) + " ]");
    System.out.println("SORT CRITERIA   : [ " + sortCriteria + " ]\n");

    int total = (rooms == null) ? 0 : rooms.getNumberOfEntries();
    int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);

    int[] colWidths = {4, 12, 12, 14};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(colWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"NO.", "ROOM NO.", "ROOM TYPE", "STATUS"}, settings);

    if (total == 0 || rooms == null) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {51}).setHAlign(0, TableUtil.Align.CENTER);

      boolean hasFilters =
          roomNumberFilter != null || roomTypeFilter != null || roomStatusFilter != null;
      String emptyMsg =
          hasFilters ? "*** NO ROOMS FOUND FOR ACTIVE FILTERS ***" : "*** NO ROOMS IN SYSTEM ***";

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
      Room r = rooms.getEntry(i);
      if (r == null) continue;

      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            r.getRoomNumber(),
            r.getRoomType().name(),
            r.getStatus().name()
          },
          settings);
    }
    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);

    System.out.printf("Page %d / %d (Total Matches: %d)\n\n", currentPage, totalPages, total);
    System.out.println("[S] Search & Filter     [O] Change Sort Order   [R] Refresh Table");
    System.out.println(
        "[P] Prev Page           [N] Next Page           [E] Exit to Front DeskMenu\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String rangeStr = (maxOptionNum == 1) ? "1" : "1-" + maxOptionNum;

    return ConsoleUtil.getMenuInput(
        "Select a room number to manage (" + rangeStr + "): ",
        1,
        maxOptionNum,
        new char[] {'S', 'O', 'R', 'P', 'N', 'E'});
  }

  // --- FILTER MENUS ---
  public int displayFilterMainMenu(
      String roomNumberFilter, String roomTypeFilter, String roomStatusFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH & FILTER ROOMS");
    System.out.println(
        "Room Number : [ " + (roomNumberFilter == null ? "None" : roomNumberFilter) + " ]");
    System.out.println(
        "Room Type   : [ " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) + " ]");
    System.out.println(
        "Room Status : [ " + (roomStatusFilter == null ? "ALL" : roomStatusFilter) + " ]\n");

    System.out.println("1. Search by Room Number");
    System.out.println("2. Filter by Room Type");
    System.out.println("3. Filter by Room Status");
    System.out.println("4. Clear All Filters");
    System.out.println("5. Apply and Back to Room List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public String promptRoomNumberFilterInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH BY ROOM NUMBER");
    return ConsoleUtil.getStringInput("Enter Room Number [Enter/C to clear]: ");
  }

  public int displayRoomTypeSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM TYPE");
    System.out.println("Current Selected: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. LUXURY");
    System.out.println("2. SUITE");
    System.out.println("3. STANDARD");
    System.out.println("4. Show All\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayRoomStatusSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER BY ROOM STATUS");
    System.out.println("Current Selected: [ " + (current == null ? "ALL" : current) + " ]\n");
    System.out.println("1. DIRTY");
    System.out.println("2. CLEANING");
    System.out.println("3. INSPECTED");
    System.out.println("4. VACANT_CLEAN (Available)");
    System.out.println("5. OCCUPIED");
    System.out.println("6. Show All\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String displaySortMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE SORT ORDER");
    System.out.println("1. Room Number (Low -> High)");
    System.out.println("2. Room Type (A -> Z)");
    System.out.println("3. Back\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
    if (choice == 1) return "ROOM NUMBER (LOW -> HIGH)";
    if (choice == 2) return "ROOM TYPE (A -> Z)";
    return null;
  }

  // --- ROOM ACTION SUBMENU ---
  public int displayRoomActionSubmenu(Room room, Reservation linkedReservation, Guest linkedGuest) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE ROOM: " + (room != null ? room.getRoomNumber() : "N/A"));

    int[] kvWidths = {20, 40};
    TableUtil.TableSettings kvSettings =
        new TableUtil.TableSettings(kvWidths)
            .setHAlign(0, TableUtil.Align.LEFT)
            .setHAlign(1, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"Room Type", room != null ? room.getRoomType().name() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Status", room != null ? room.getStatus().name() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {
          "Confirmation No.",
          linkedReservation != null && linkedReservation.getConfirmationNumber() != null
              ? linkedReservation.getConfirmationNumber()
              : "None"
        },
        kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.MIDDLE);
    TableUtil.printTableRow(
        new String[] {"Guest", linkedGuest != null ? linkedGuest.getName() : "N/A"}, kvSettings);
    TableUtil.printTableBorder(kvSettings, TableUtil.BorderPosition.BOTTOM);

    System.out.println("\n-- Room Actions --");
    System.out.println("1. Change Room (move current reservation to another room)");
    System.out.println("2. Mark Room as  VACANT_CLEAN (Available) ");
    System.out.println("3. Mark Room as Occupied (Unavailable)");
    System.out.println("4. Back to Room List\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public String promptTargetRoomNumberInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE ROOM");
    return ConsoleUtil.getStringInput("Enter Target Room Number [C to cancel]: ");
  }
}
