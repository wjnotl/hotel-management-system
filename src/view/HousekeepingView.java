package view;

import adt.ListInterface;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class HousekeepingView {
  public String displayMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("Housekeeping & Task Log");
    System.out.println(" 1. Manage Cleaning Task Board");
    System.out.println(" 2. Manage Staff Assignments");
    System.out.println(" 3. Room Status Sync");
    System.out.println(" 4. Generate Housekeeping Reports");
    System.out.println(" 5. Settings & Configuration");
    System.out.println(" 6. Back to Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).input;
  }

  // --- SCREEN 1: MANAGE CLEANING TASK BOARD ---

  public GetMenuInputResult renderTaskBoardScreen(
      ListInterface<HousekeepingTask> list,
      ListInterface<HousekeepingStaff> staffList,
      String search,
      String taskTypeFilter,
      String statusFilter,
      String displayOrder,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE CLEANING TASK BOARD");

    // Banner Metadata
    System.out.println(
        " SEARCH QUERY   : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" TASK TYPE      : [ " + (taskTypeFilter == null ? "ALL" : taskTypeFilter) + " ]");
    System.out.println(" STATUS FILTER  : [ " + (statusFilter == null ? "ALL" : statusFilter) + " ]");
    System.out.println(" DISPLAY ORDER  : [ " + displayOrder + " ]");
    System.out.println("------------------------------------------------------");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(5), ROOM NO.(9), TASK TYPE(17), ASSIGNED STAFF(15), URGENT(9), STATUS(13)
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(new int[] {5, 9, 17, 15, 9, 13})
            .setHAlign(0, TableUtil.Align.CENTER) // NO.
            .setHAlign(1, TableUtil.Align.CENTER) // ROOM NO.
            .setHAlign(4, TableUtil.Align.CENTER) // URGENT
            .setHAlign(5, TableUtil.Align.CENTER); // STATUS

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "ROOM NO.", "TASK TYPE", "ASSIGNED STAFF", "URGENT", "STATUS"},
        settings);

    // --- EMPTY STATE HANDLING ---
    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      boolean hasActiveFilters =
          (search != null && !search.trim().isEmpty())
              || (taskTypeFilter != null && !taskTypeFilter.trim().isEmpty())
              || (statusFilter != null && !statusFilter.trim().isEmpty());

      String emptyMessage =
          hasActiveFilters
              ? "*** NO MATCHING TASKS FOUND FOR ACTIVE FILTERS ***"
              : "*** TASK BOARD IS CURRENTLY EMPTY ***";

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {75}).setHAlign(0, TableUtil.Align.CENTER);

      TableUtil.printTableRow(new String[] {emptyMessage}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("----------------------------------------------------------------------");
      System.out.println(" [A] Add Task           [U] Add Urgent Task     [D] Dequeue Next Task");
      System.out.println(" [S] Search / Filter    [O] Change Display Order [E] Exit to Menu\n");

      return ConsoleUtil.getMenuInput(
          "Enter a command: ", new char[] {'A', 'U', 'D', 'S', 'O', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      HousekeepingTask t = list.getEntry(i);
      if (t == null) continue;

      HousekeepingStaff staff = findStaff(staffList, t.getAssignedStaffId());

      int displayNum = i - startIndex + 1;
      String staffLabel = (staff != null) ? staff.getName() : "Unassigned";
      String urgentStr = t.getIsUrgent() ? "[!]" : "[ ]";

      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            t.getRoomNumber(),
            t.getTaskType().name(),
            staffLabel,
            urgentStr,
            t.getStatus().name()
          },
          settings);
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(" Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, totalMatches);
    System.out.println("----------------------------------------------------------------------");
    System.out.println(" [A] Add Task           [U] Add Urgent Task     [D] Dequeue Next Task");
    System.out.println(" [S] Search / Filter    [O] Change Display Order [E] Exit to Menu");
    System.out.println(" [N] Next Page          [P] Prev Page\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String promptText =
        (maxOptionNum == 1)
            ? "Enter a command or select task index number (1): "
            : "Enter a command or select a task index number (1-" + maxOptionNum + "): ";

    return ConsoleUtil.getMenuInput(
        promptText, 1, maxOptionNum, new char[] {'A', 'U', 'D', 'S', 'O', 'E', 'N', 'P'});
  }

  // --- ADD TASK PROMPTS ---

  public String promptRoomNumberInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD CLEANING TASK");
    System.out.println(" [Enter 'C' to Cancel and return to Task Board]\n");
    return ConsoleUtil.getStringInput(" Enter Room Number: ");
  }

  public int promptTaskTypeInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT TASK TYPE");
    System.out.println(" 1. Standard Clean");
    System.out.println(" 2. Deep Clean");
    System.out.println(" 3. Turnover");
    System.out.println(" 4. Maintenance Check\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  // --- FILTER SUBMENUS WITH EXITS ---

  public int displayFilterMainMenu(String search, String taskType, String status) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SEARCH MANAGEMENT");
    System.out.println(
        " Active Search   : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" Active Task Type: [ " + (taskType == null ? "ALL" : taskType) + " ]");
    System.out.println(" Active Status   : [ " + (status == null ? "ALL" : status) + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Text Search Submenu (Room No. / Staff)");
    System.out.println(" 2. Task Type Submenu");
    System.out.println(" 3. Task Status Submenu");
    System.out.println(" 4. Reset / Clear All Filters");
    System.out.println(" 5. Apply and Return to Task Board");
    System.out.println(" 6. Back / Exit Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displaySearchSubmenu(String currentQuery) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY SUBMENU");
    System.out.println(
        " Current Query: [ " + (currentQuery == null ? "None" : "\"" + currentQuery + "\"") + " ]\n");
    System.out.println(" 1. Enter / Change Search Term");
    System.out.println(" 2. Clear Search Term");
    System.out.println(" 3. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptSearchInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter search query (Room Number / Staff Name): ");
  }

  public int displayTaskTypeSubmenu(String currentType) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TASK TYPE FILTER SUBMENU");
    System.out.println(
        " Current Selected Type: [ " + (currentType == null ? "ALL" : currentType) + " ]\n");
    System.out.println(" 1. Filter: Standard Clean");
    System.out.println(" 2. Filter: Deep Clean");
    System.out.println(" 3. Filter: Turnover");
    System.out.println(" 4. Filter: Maintenance Check");
    System.out.println(" 5. Clear Task Type Filter (Show All)");
    System.out.println(" 6. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displayStatusSubmenu(String currentStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TASK STATUS FILTER SUBMENU");
    System.out.println(
        " Current Selected Status: [ " + (currentStatus == null ? "ALL" : currentStatus) + " ]\n");
    System.out.println(" 1. Show PENDING Only");
    System.out.println(" 2. Show IN_PROGRESS Only");
    System.out.println(" 3. Show COMPLETED Only");
    System.out.println(" 4. Show SKIPPED Only");
    System.out.println(" 5. Clear Status Filter (Show All)");
    System.out.println(" 6. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String displayDisplayOrderMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CHANGE DISPLAY ORDER");
    System.out.println(" 1. Queue Order (Front -> Back, urgent tasks first)");
    System.out.println(" 2. Room Number (Ascending)");
    System.out.println(" 3. Assigned Time (Oldest -> Newest)");
    System.out.println(" 4. Assigned Time (Newest -> Oldest)");
    System.out.println(" 5. Back to Task Board (Keep current order)\n");

    int choice = ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
    if (choice == 2) return "ROOM NUMBER (ASCENDING)";
    if (choice == 3) return "ASSIGNED TIME (OLDEST -> NEWEST)";
    if (choice == 4) return "ASSIGNED TIME (NEWEST -> OLDEST)";
    if (choice == 5) return null; // Keep current order
    return "QUEUE ORDER (FRONT -> BACK)";
  }

  // --- ROW ACTION SUBMENU ---

  public int displayTaskActionSubmenu(HousekeepingTask t) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TASK ACTION: " + t.getTaskId());
    System.out.println(" Room Number : " + t.getRoomNumber());
    System.out.println(" Task Type   : " + t.getTaskType().name());
    System.out.println(" Status      : " + t.getStatus().name());
    System.out.println(" Urgent      : " + (t.getIsUrgent() ? "YES" : "NO") + "\n");
    System.out.println(" 1. Assign to Staff Member");
    System.out.println(" 2. Mark In Progress");
    System.out.println(" 3. Mark Completed");
    System.out.println(" 4. Mark Skipped");
    System.out.println(" 5. Escalate to Front of Queue");
    System.out.println(" 6. Cancel Action and Return\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String promptStaffIdInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter Staff ID to assign: ");
  }

  // --- INTERNAL LOOKUP HELPERS ---

  private HousekeepingStaff findStaff(ListInterface<HousekeepingStaff> staffList, String staffId) {
    if (staffList == null || staffId == null) return null;
    for (int i = 1; i <= staffList.getNumberOfEntries(); i++) {
      HousekeepingStaff s = staffList.getEntry(i);
      if (s != null && staffId.equals(s.getStaffId())) return s;
    }
    return null;
  }
}
