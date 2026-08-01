package view;

import adt.ListInterface;
import entity.HousekeepingSettings;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLogEntry;
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
    System.out.println(
        " TASK TYPE      : [ " + (taskTypeFilter == null ? "ALL" : taskTypeFilter) + " ]");
    System.out.println(
        " STATUS FILTER  : [ "
            + (statusFilter == null ? "ACTIVE ONLY (excl. Completed/Skipped)" : statusFilter)
            + " ]");
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
    System.out.println(
        " Active Status   : [ "
            + (status == null ? "ACTIVE ONLY (excl. Completed/Skipped)" : status)
            + " ]");
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
        " Current Selected Status: [ "
            + (currentStatus == null ? "ACTIVE ONLY (excl. Completed/Skipped)" : currentStatus)
            + " ]\n");
    System.out.println(" 1. Show PENDING Only");
    System.out.println(" 2. Show ASSIGNED Only");
    System.out.println(" 3. Show IN_PROGRESS Only");
    System.out.println(" 4. Show COMPLETED Only");
    System.out.println(" 5. Show SKIPPED Only");
    System.out.println(" 6. Clear Status Filter (Active Tasks Only, hides Completed/Skipped)");
    System.out.println(" 7. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
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
    System.out.println(" Room Number     : " + t.getRoomNumber());
    System.out.println(" Task Type       : " + t.getTaskType().name());
    System.out.println(" Status          : " + t.getStatus().name());
    System.out.println(
        " Assigned Staff  : " + (t.getAssignedStaffId() == null ? "None" : t.getAssignedStaffId()));
    System.out.println(" Urgent          : " + (t.getIsUrgent() ? "YES" : "NO") + "\n");
    System.out.println(" 1. Assign to Staff Member");
    System.out.println(" 2. Start Cleaning (requires assigned staff)");
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

  // --- SCREEN 2: STAFF ASSIGNMENTS ---

  public GetMenuInputResult renderStaffRosterScreen(
      ListInterface<HousekeepingStaff> list,
      HousekeepingSettings settings,
      String search,
      String shiftFilter,
      String availabilityFilter,
      int currentPage,
      int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("MANAGE STAFF ASSIGNMENTS");

    System.out.println(
        " SEARCH QUERY    : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(
        " SHIFT FILTER    : [ " + (shiftFilter == null ? "ALL" : shiftFilter) + " ]");
    System.out.println(
        " AVAILABILITY    : [ " + (availabilityFilter == null ? "ALL" : availabilityFilter) + " ]");
    System.out.println("------------------------------------------------------");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(4), STAFF NAME(14), SHIFT(22), ASSIGNED ROOMS(14), STATUS(10)
    TableUtil.TableSettings settingsTable =
        new TableUtil.TableSettings(new int[] {4, 14, 22, 14, 10})
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setTruncate(2)
            .setTruncate(3);

    TableUtil.printTableBorder(settingsTable, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"NO.", "STAFF NAME", "SHIFT", "ASSIGNED ROOMS", "STATUS"}, settingsTable);

    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(settingsTable, TableUtil.BorderPosition.HEADER_CLOSE);

      boolean hasActiveFilters =
          (search != null && !search.trim().isEmpty())
              || (shiftFilter != null && !shiftFilter.trim().isEmpty())
              || (availabilityFilter != null && !availabilityFilter.trim().isEmpty());

      String emptyMessage =
          hasActiveFilters
              ? "*** NO MATCHING STAFF FOUND FOR ACTIVE FILTERS ***"
              : "*** NO STAFF ON RECORD ***";

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {64}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {emptyMessage}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("------------------------------------------------------");
      System.out.println(" [S] Search / Filter    [E] Exit to Menu");
      System.out.println(" [A] Add Staff\n");

      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'E', 'A'});
    }

    TableUtil.printTableBorder(settingsTable, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      HousekeepingStaff s = list.getEntry(i);
      if (s == null) continue;

      int displayNum = i - startIndex + 1;
      TableUtil.printTableRow(
          new String[] {
            String.valueOf(displayNum),
            s.getName(),
            s.getShift().name() + " (" + getShiftScheduleText(settings, s.getShift()) + ")",
            formatAssignedRooms(s.getAssignedRoomNumbers()),
            s.getAvailability().name()
          },
          settingsTable);
    }

    TableUtil.printTableBorder(settingsTable, TableUtil.BorderPosition.BOTTOM);
    System.out.printf(" Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, totalMatches);
    System.out.println("------------------------------------------------------");
    System.out.println(" [S] Search / Filter    [E] Exit to Menu");
    System.out.println(" [N] Next Page          [P] Prev Page");
    System.out.println(" [A] Add Staff\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String promptText =
        (maxOptionNum == 1)
            ? "Enter a command or select a staff index number (1): "
            : "Enter a command or select a staff index number (1-" + maxOptionNum + "): ";

    return ConsoleUtil.getMenuInput(
        promptText, 1, maxOptionNum, new char[] {'S', 'E', 'N', 'P', 'A'});
  }

  private String getShiftScheduleText(
      HousekeepingSettings settings, HousekeepingStaff.Shift shift) {
    if (settings == null) return "";
    switch (shift) {
      case MORNING:
        return settings.getMorningShiftSchedule();
      case AFTERNOON:
        return settings.getAfternoonShiftSchedule();
      default:
        return settings.getNightShiftSchedule();
    }
  }

  private String formatAssignedRooms(ListInterface<String> rooms) {
    if (rooms == null || rooms.isEmpty()) return "None";

    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      if (i > 1) sb.append(", ");
      sb.append(rooms.getEntry(i));
    }
    return sb.toString();
  }

  // --- ADD STAFF ---

  public String promptNewStaffName() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ADD NEW STAFF MEMBER");
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter Staff Name: ");
  }

  public int displayShiftSelectionMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT SHIFT");
    System.out.println(" 1. MORNING");
    System.out.println(" 2. AFTERNOON");
    System.out.println(" 3. NIGHT");
    System.out.println(" 4. Cancel\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public int displayStaffFilterMainMenu(
      String search, String shiftFilter, String availabilityFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SEARCH MANAGEMENT");
    System.out.println(
        " Active Search      : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(
        " Active Shift       : [ " + (shiftFilter == null ? "ALL" : shiftFilter) + " ]");
    System.out.println(
        " Active Availability: [ "
            + (availabilityFilter == null ? "ALL" : availabilityFilter)
            + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Text Search Submenu (Staff Name)");
    System.out.println(" 2. Shift Submenu");
    System.out.println(" 3. Availability Submenu");
    System.out.println(" 4. Reset / Clear All Filters");
    System.out.println(" 5. Apply and Return to Staff Roster");
    System.out.println(" 6. Back / Exit Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int displayStaffSearchSubmenu(String currentQuery) {
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

  public String promptStaffSearchInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter search query (Staff Name): ");
  }

  public int displayShiftFilterSubmenu(String currentShift) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SHIFT FILTER SUBMENU");
    System.out.println(
        " Current Selected Shift: [ " + (currentShift == null ? "ALL" : currentShift) + " ]\n");
    System.out.println(" 1. Show MORNING Only");
    System.out.println(" 2. Show AFTERNOON Only");
    System.out.println(" 3. Show NIGHT Only");
    System.out.println(" 4. Clear Shift Filter (Show All)");
    System.out.println(" 5. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public int displayAvailabilityFilterSubmenu(String currentAvailability) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("AVAILABILITY FILTER SUBMENU");
    System.out.println(
        " Current Selected Status: [ "
            + (currentAvailability == null ? "ALL" : currentAvailability)
            + " ]\n");
    System.out.println(" 1. Show AVAILABLE Only");
    System.out.println(" 2. Show ON_TASK Only");
    System.out.println(" 3. Show OFF_DUTY Only");
    System.out.println(" 4. Clear Availability Filter (Show All)");
    System.out.println(" 5. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // --- ROW ACTION SUBMENU (STAFF) ---

  public int displayStaffActionSubmenu(HousekeepingStaff staff) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STAFF ACTION: " + staff.getStaffId());
    System.out.println(" Staff Name      : " + staff.getName());
    System.out.println(" Shift           : " + staff.getShift().name());
    System.out.println(" Availability    : " + staff.getAvailability().name());
    System.out.println(
        " Assigned Rooms  : " + formatAssignedRooms(staff.getAssignedRoomNumbers()) + "\n");
    System.out.println(" 1. Auto-Assign Next Queued Task (requires AVAILABLE)");
    System.out.println(" 2. Reassign a Room to Another Staff Member");
    System.out.println(
        " 3. "
            + (staff.getAvailability() == HousekeepingStaff.Availability.OFF_DUTY
                ? "Mark Available"
                : "Mark Off Duty / On Break"));
    System.out.println(" 4. View Today's Task History");
    System.out.println(" 5. Edit Shift");
    System.out.println(" 6. Cancel Action and Return\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public String promptRoomNumberForReassign() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter Room Number to reassign: ");
  }

  public String promptTargetStaffIdInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter Staff ID to reassign to: ");
  }

  public void renderStaffTaskHistoryScreen(
      HousekeepingStaff staff, ListInterface<HousekeepingTask> history) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TODAY'S TASK HISTORY: " + staff.getName());

    if (history == null || history.isEmpty()) {
      System.out.println(" *** NO TASKS ASSIGNED TO THIS STAFF MEMBER TODAY ***\n");
    } else {
      for (int i = 1; i <= history.getNumberOfEntries(); i++) {
        HousekeepingTask t = history.getEntry(i);
        if (t == null) continue;

        System.out.println(
            " "
                + t.getTaskId()
                + " | Room "
                + t.getRoomNumber()
                + " | "
                + t.getTaskType().name()
                + " | "
                + t.getStatus().name());
      }
      System.out.println();
    }

    ConsoleUtil.printContinueMessage();
  }

  // --- SCREEN 3: ROOM STATUS SYNC ---

  public GetMenuInputResult renderRoomStatusScreen(
      ListInterface<Room> list, String search, String statusFilter, int currentPage, int pageSize) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM STATUS SYNC");

    System.out.println(
        " SEARCH QUERY  : [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(
        " STATUS FILTER : [ " + (statusFilter == null ? "ALL" : statusFilter) + " ]");
    System.out.println("------------------------------------------------------");

    int totalMatches = (list == null) ? 0 : list.getNumberOfEntries();
    int totalPages = (totalMatches == 0) ? 0 : (int) Math.ceil((double) totalMatches / pageSize);

    // Columns: NO.(5), ROOM NO.(9), ROOM TYPE(12), STATUS(13)
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(new int[] {5, 9, 12, 13})
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(new String[] {"NO.", "ROOM NO.", "ROOM TYPE", "STATUS"}, settings);

    if (list == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);

      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {41}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"*** NO MATCHING ROOMS FOUND ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);

      System.out.println(" Page 0 / 0 (Total Matches: 0)");
      System.out.println("------------------------------------------------------");
      System.out.println(" [S] Search / Filter    [E] Exit to Menu\n");

      return ConsoleUtil.getMenuInput("Enter a command: ", new char[] {'S', 'E'});
    }

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, totalMatches);

    for (int i = startIndex; i <= endIndex; i++) {
      Room r = list.getEntry(i);
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
    System.out.printf(" Page %d / %d (Total Matches: %d)\n", currentPage, totalPages, totalMatches);
    System.out.println("------------------------------------------------------");
    System.out.println(" [S] Search / Filter    [E] Exit to Menu");
    System.out.println(" [N] Next Page          [P] Prev Page\n");

    int maxOptionNum = endIndex - startIndex + 1;
    String promptText =
        (maxOptionNum == 1)
            ? "Enter a command or select room index number (1): "
            : "Enter a command or select a room index number (1-" + maxOptionNum + "): ";

    return ConsoleUtil.getMenuInput(promptText, 1, maxOptionNum, new char[] {'S', 'E', 'N', 'P'});
  }

  public int displayRoomFilterMainMenu(String search, String statusFilter) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SEARCH MANAGEMENT");
    System.out.println(
        " Active Search: [ " + (search == null ? "None" : "\"" + search + "\"") + " ]");
    System.out.println(" Active Status: [ " + (statusFilter == null ? "ALL" : statusFilter) + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Enter / Change Room Number Search");
    System.out.println(" 2. Filter by Status");
    System.out.println(" 3. Reset / Clear All Filters");
    System.out.println(" 4. Apply and Return to Room Status Sync");
    System.out.println(" 5. Back / Exit Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public String promptRoomSearchInput() {
    System.out.println("\n [Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter Room Number to search: ");
  }

  public int displayRoomStatusFilterSubmenu(String currentStatus) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STATUS FILTER SUBMENU");
    System.out.println(
        " Current Selected Status: [ " + (currentStatus == null ? "ALL" : currentStatus) + " ]\n");
    System.out.println(" 1. Show DIRTY Only");
    System.out.println(" 2. Show CLEANING Only");
    System.out.println(" 3. Show INSPECTED Only");
    System.out.println(" 4. Show VACANT_CLEAN Only");
    System.out.println(" 5. Show OCCUPIED Only");
    System.out.println(" 6. Clear Status Filter (Show All)");
    System.out.println(" 7. Back to Filter Management\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
  }

  public int displayRoomActionSubmenu(Room room, boolean hasUndoHistory) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ROOM ACTION: " + room.getRoomNumber());
    System.out.println(" Room Type   : " + room.getRoomType().name());
    System.out.println(" Status      : " + room.getStatus().name());
    System.out.println(
        " Undo Available: " + (hasUndoHistory ? "YES" : "NO (no prior change on record)") + "\n");
    System.out.println(" 1. Update Status");
    System.out.println(" 2. Undo Last Status Change");
    System.out.println(" 3. View Status History");
    System.out.println(" 4. Flag Room for Maintenance");
    System.out.println(" 5. Request Supervisor Inspection");
    System.out.println(" 6. Cancel Action and Return\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6).getAsInt();
  }

  public int promptNewStatusInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT NEW ROOM STATUS");
    System.out.println(" 1. Dirty");
    System.out.println(" 2. Cleaning");
    System.out.println(" 3. Inspected");
    System.out.println(" 4. Vacant / Clean");
    System.out.println(" 5. Occupied\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  public void renderRoomHistoryScreen(
      String roomNumber, ListInterface<RoomStatusLogEntry> history) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STATUS HISTORY: ROOM " + roomNumber);

    if (history == null || history.isEmpty()) {
      System.out.println(" *** NO HISTORY RECORDED FOR THIS ROOM YET ***\n");
    } else {
      for (int i = 1; i <= history.getNumberOfEntries(); i++) {
        RoomStatusLogEntry entry = history.getEntry(i);
        if (entry == null) continue;

        String line = " " + entry.getChangedAt() + " - ";
        if (entry.getNote() != null) {
          line +=
              entry.getNote()
                  + (entry.getToStatus() != null
                      ? " (reverted to " + entry.getToStatus() + ")"
                      : "");
        } else {
          line += entry.getFromStatus() + " -> " + entry.getToStatus();
        }
        System.out.println(line);
      }
      System.out.println();
    }

    ConsoleUtil.printContinueMessage();
  }

  // --- SCREEN 5: SETTINGS & CONFIGURATION ---

  public int displaySettingsMainMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SETTINGS & CONFIGURATION");
    System.out.println(" 1. Task Rules Manager");
    System.out.println(" 2. Staff Configuration");
    System.out.println(" 3. Back to Housekeeping Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public int displayTaskRulesMenu(HousekeepingSettings settings) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("TASK RULES MANAGER");
    System.out.println(" Standard Cleaning Time Estimates (minutes):");
    System.out.println("   STANDARD : " + settings.getCleanTimeStandardMinutes());
    System.out.println("   SUITE    : " + settings.getCleanTimeSuiteMinutes());
    System.out.println("   LUXURY   : " + settings.getCleanTimeLuxuryMinutes());
    System.out.println();
    System.out.println(" Overdue Threshold: " + settings.getOverdueThresholdMinutes() + " minutes");
    System.out.println();
    System.out.println(" Queue-Jump Eligible Task Types:");
    System.out.println(
        "   STANDARD_CLEAN    : "
            + (settings.isQueueJumpStandardClean() ? "Allowed" : "Not Allowed"));
    System.out.println(
        "   DEEP_CLEAN        : " + (settings.isQueueJumpDeepClean() ? "Allowed" : "Not Allowed"));
    System.out.println(
        "   TURNOVER          : " + (settings.isQueueJumpTurnover() ? "Allowed" : "Not Allowed"));
    System.out.println(
        "   MAINTENANCE_CHECK : "
            + (settings.isQueueJumpMaintenanceCheck() ? "Allowed" : "Not Allowed"));
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Edit Standard Cleaning Time Estimates");
    System.out.println(" 2. Edit Overdue Threshold");
    System.out.println(" 3. Configure Queue-Jump Task Types");
    System.out.println(" 4. Back to Settings Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4).getAsInt();
  }

  public void printEditCleaningTimesHeader() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EDIT STANDARD CLEANING TIME ESTIMATES");
    System.out.println(
        " Current values shown in brackets. Leave blank or type 'C' to keep a value"
            + " unchanged.\n");
  }

  public void printEditOverdueThresholdHeader(HousekeepingSettings settings) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EDIT OVERDUE THRESHOLD");
    System.out.println(" Current Threshold: " + settings.getOverdueThresholdMinutes() + " minutes");
    System.out.println(" Leave blank or type 'C' to keep this unchanged.\n");
  }

  // Config-only for now — not yet enforced anywhere on the Task Board (Add Urgent Task still
  // accepts any task type regardless of what's toggled here).
  public boolean[] promptQueueJumpTypes(HousekeepingSettings settings) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIGURE QUEUE-JUMP TASK TYPES");
    System.out.println(" Toggle which task types are allowed to jump the queue (Add Urgent Task).");
    System.out.println(
        " Config only for now \u2014 not yet enforced on the Task Board's Add Urgent Task"
            + " action.\n");

    boolean standardClean =
        ConsoleUtil.showConfirmMessage(
            "Allow STANDARD_CLEAN to jump the queue? (currently "
                + (settings.isQueueJumpStandardClean() ? "Allowed" : "Not Allowed")
                + ")");
    boolean deepClean =
        ConsoleUtil.showConfirmMessage(
            "Allow DEEP_CLEAN to jump the queue? (currently "
                + (settings.isQueueJumpDeepClean() ? "Allowed" : "Not Allowed")
                + ")");
    boolean turnover =
        ConsoleUtil.showConfirmMessage(
            "Allow TURNOVER to jump the queue? (currently "
                + (settings.isQueueJumpTurnover() ? "Allowed" : "Not Allowed")
                + ")");
    boolean maintenanceCheck =
        ConsoleUtil.showConfirmMessage(
            "Allow MAINTENANCE_CHECK to jump the queue? (currently "
                + (settings.isQueueJumpMaintenanceCheck() ? "Allowed" : "Not Allowed")
                + ")");

    return new boolean[] {standardClean, deepClean, turnover, maintenanceCheck};
  }

  public int displayStaffConfigMenu(HousekeepingSettings settings) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STAFF CONFIGURATION");
    System.out.println(" Shift Schedules:");
    System.out.println("   MORNING   : " + settings.getMorningShiftSchedule());
    System.out.println("   AFTERNOON : " + settings.getAfternoonShiftSchedule());
    System.out.println("   NIGHT     : " + settings.getNightShiftSchedule());
    System.out.println();
    System.out.println(" Max Rooms per Staff per Shift:");
    System.out.println("   MORNING   : " + settings.getMaxRoomsMorning());
    System.out.println("   AFTERNOON : " + settings.getMaxRoomsAfternoon());
    System.out.println("   NIGHT     : " + settings.getMaxRoomsNight());
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Set Shift Schedules");
    System.out.println(" 2. Set Max Rooms per Staff per Shift");
    System.out.println(" 3. Back to Settings Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public void printEditShiftSchedulesHeader() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET SHIFT SCHEDULES");
    System.out.println(
        " Current values shown in brackets. Leave blank or type 'C' to keep a value"
            + " unchanged.\n");
  }

  public void printEditMaxRoomsHeader() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET MAX ROOMS PER STAFF PER SHIFT");
    System.out.println(
        " Current values shown in brackets. Leave blank or type 'C' to keep a value"
            + " unchanged.\n");
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
