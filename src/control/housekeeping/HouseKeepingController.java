package control.housekeeping;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingSettings;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatusLogEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import repo.HousekeepingSettingsRepo;
import repo.HousekeepingStaffRepo;
import repo.HousekeepingTaskRepo;
import repo.RoomRepo;
import repo.RoomStatusHistoryRepo;
import util.ConsoleUtil;
import view.housekeeping.HousekeepingView;

public class HouseKeepingController {
  private final HousekeepingView houseKeepingView = new HousekeepingView();
  private final HousekeepingTaskRepo taskRepo;
  private final HousekeepingStaffRepo staffRepo;
  private final RoomRepo roomRepo;
  private final RoomStatusHistoryRepo roomStatusHistoryRepo;
  private final HousekeepingSettingsRepo settingsRepo;

  public HouseKeepingController(
      HousekeepingTaskRepo taskRepo,
      HousekeepingStaffRepo staffRepo,
      RoomRepo roomRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo,
      HousekeepingSettingsRepo settingsRepo) {
    this.taskRepo = taskRepo;
    this.staffRepo = staffRepo;
    this.roomRepo = roomRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
    this.settingsRepo = settingsRepo;
  }

  // --- HOUSEKEEPING MAIN MENU LOOP ---
  public void start() {
    while (true) {
      try {
        String choice = houseKeepingView.displayMenu();

        if ("1".equals(choice)) {
          manageCleaningTaskBoard();
        } else if ("2".equals(choice)) {
          manageStaffAssignments();
        } else if ("3".equals(choice)) {
          manageRoomStatusSync();
        } else if ("4".equals(choice)) {
          new HousekeepingReportController(taskRepo, staffRepo, roomRepo, settingsRepo).start();
        } else if ("5".equals(choice)) {
          manageSettings();
        } else if ("6".equals(choice)) {
          return; // Go back to Resort Main Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- TASK BOARD SCREEN LOOP ---
  public void manageCleaningTaskBoard() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String taskTypeFilter = null;
    String statusFilter = null;
    String displayOrder = "QUEUE ORDER (FRONT -> BACK)";

    while (true) {
      try {
        // 1. Fetch live data (deque order first, then terminal-status tasks)
        ListInterface<HousekeepingTask> orderedList = buildDisplayList();

        // 2. Filter & Sort
        ListInterface<HousekeepingTask> filteredList =
            filterAndSortList(orderedList, searchQuery, taskTypeFilter, statusFilter, displayOrder);

        // Clamp the page index in case a prior action (complete/skip/filter change) shrank the
        // result set out from under the page we were sitting on.
        currentPage = clampPage(currentPage, filteredList.getNumberOfEntries(), pageSize);

        // 3. Resolve each task's staff name up front so the view only prints (no lookups)
        ListInterface<HousekeepingView.TaskBoardRowDTO> rows =
            filteredList.map(
                t -> {
                  HousekeepingStaff staff =
                      (t == null || t.getAssignedStaffId() == null)
                          ? null
                          : staffRepo.findById(t.getAssignedStaffId());
                  return new HousekeepingView.TaskBoardRowDTO(
                      t, staff != null ? staff.getName() : null);
                });

        // 4. Render Task Board Screen
        ConsoleUtil.GetMenuInputResult result =
            houseKeepingView.renderTaskBoardScreen(
                rows,
                searchQuery,
                taskTypeFilter,
                statusFilter,
                displayOrder,
                currentPage,
                pageSize);

        if (result == null || result.input == null || result.input.trim().isEmpty()) {
          continue;
        }

        String command = result.input.trim();

        if ("E".equalsIgnoreCase(command)) {
          break; // Return to Housekeeping Main Menu
        } else if ("A".equalsIgnoreCase(command)) {
          handleAddTask(false);
        } else if ("U".equalsIgnoreCase(command)) {
          handleAddTask(true);
        } else if ("D".equalsIgnoreCase(command)) {
          handleDequeueNext();
        } else if ("S".equalsIgnoreCase(command)) {
          String[] filters = handleFilterMenu(searchQuery, taskTypeFilter, statusFilter);
          searchQuery = filters[0];
          taskTypeFilter = filters[1];
          statusFilter = filters[2];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(command)) {
          String newOrder = handleDisplayOrderMenu(displayOrder);
          if (newOrder != null) {
            displayOrder = newOrder;
            currentPage = 1;
          }
        } else if ("N".equalsIgnoreCase(command)) {
          int totalMatches = filteredList.getNumberOfEntries();
          int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(command)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if (result.isNumber) {
          int selectedIndex = result.getAsInt();
          handleTaskAction(filteredList, selectedIndex, currentPage, pageSize);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- ADD TASK (BACK) / ADD URGENT TASK (FRONT) ---
  private void handleAddTask(boolean urgent) {
    while (true) {
      try {
        String roomNumberInput = houseKeepingView.promptRoomNumberInput();
        if (roomNumberInput == null
            || roomNumberInput.trim().isEmpty()
            || "C".equalsIgnoreCase(roomNumberInput.trim())) {
          return; // Cancel action
        }

        Room room = roomRepo.findByRoomNumber(roomNumberInput.trim());
        if (room == null) {
          ConsoleUtil.printError("No room found with number: " + roomNumberInput.trim());
          continue; // Re-prompt instead of creating a task for a room that doesn't exist
        }

        int typeChoice = houseKeepingView.promptTaskTypeInput();
        HousekeepingTask.TaskType taskType = mapTaskType(typeChoice);

        // Reference info only — pulled straight from Settings, doesn't block or alter anything.
        boolean jumpAllowed = isQueueJumpAllowed(taskType);
        System.out.println();
        if (isCleaningTaskType(taskType)) {
          int estMinutes = getEstimatedCleanTimeMinutes(room.getRoomType());
          System.out.println(
              " [Reference] Est. cleaning time: "
                  + estMinutes
                  + " min ("
                  + room.getRoomType().name()
                  + " room) | Queue-jump allowed for "
                  + taskType.name()
                  + ": "
                  + (jumpAllowed ? "YES" : "NO"));
        } else {
          System.out.println(
              " [Reference] Queue-jump allowed for "
                  + taskType.name()
                  + ": "
                  + (jumpAllowed ? "YES" : "NO"));
        }
        ConsoleUtil.printContinueMessage();

        // Warn (don't hard-block) on a likely duplicate: same room, same task type, already
        // active. Different types (e.g. Maintenance Check alongside a queued Standard Clean)
        // or an ad-hoc urgent add on top of an existing queued task are legitimate, so this
        // only fires — and only asks — when it's the exact same type of work twice.
        if (hasActiveTaskOfSameType(room.getRoomNumber(), taskType)) {
          boolean proceedAnyway =
              ConsoleUtil.showConfirmMessage(
                  "Room "
                      + room.getRoomNumber()
                      + " already has an active "
                      + taskType.name()
                      + " task. Add another one anyway?");
          if (!proceedAnyway) {
            continue; // Back to the top of Add Task, not a hard cancel
          }
        }

        HousekeepingTask newTask =
            new HousekeepingTask(
                taskRepo.generateTaskId(),
                room.getRoomNumber(),
                taskType,
                HousekeepingTask.Status.PENDING,
                null,
                urgent,
                LocalDateTime.now());

        if (urgent) {
          taskRepo.enqueueUrgentTask(newTask);
        } else {
          taskRepo.enqueueTask(newTask);
        }

        // Sync to Room Status Sync: a cleaning-type task means the room just got messed up.
        // Maintenance checks don't imply dirtiness (room could already be clean), so those
        // only get logged as a note, not a forced status change.
        if (isCleaningTaskType(taskType)) {
          Room.Status oldStatus = room.getStatus();
          if (oldStatus != Room.Status.DIRTY) {
            room.setStatus(Room.Status.DIRTY);
            roomRepo.updateRoom(room);
            roomStatusHistoryRepo.recordStatusChange(
                room.getRoomNumber(), oldStatus, Room.Status.DIRTY);
          }
        } else {
          roomStatusHistoryRepo.recordNote(
              room.getRoomNumber(), "Maintenance check task added (" + newTask.getTaskId() + ")");
        }

        ConsoleUtil.clearScreen();
        System.out.println(" >> STATUS: [\u2713] SUCCESS");
        System.out.println(
            " Task "
                + newTask.getTaskId()
                + " added to "
                + (urgent ? "the FRONT of the queue." : "the queue.")
                + "\n");
        ConsoleUtil.printContinueMessage();
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Reference-only lookups pulled from Settings — display purposes only, no enforcement.
  private int getEstimatedCleanTimeMinutes(Room.RoomType roomType) {
    HousekeepingSettings settings = settingsRepo.getSettings();
    switch (roomType) {
      case LUXURY:
        return settings.getCleanTimeLuxuryMinutes();
      case SUITE:
        return settings.getCleanTimeSuiteMinutes();
      default:
        return settings.getCleanTimeStandardMinutes();
    }
  }

  private boolean isQueueJumpAllowed(HousekeepingTask.TaskType taskType) {
    HousekeepingSettings settings = settingsRepo.getSettings();
    switch (taskType) {
      case STANDARD_CLEAN:
        return settings.isQueueJumpStandardClean();
      case DEEP_CLEAN:
        return settings.isQueueJumpDeepClean();
      case TURNOVER:
        return settings.isQueueJumpTurnover();
      default:
        return settings.isQueueJumpMaintenanceCheck();
    }
  }

  // True if this room already has a task of the same type sitting PENDING/ASSIGNED/
  // IN_PROGRESS — used to warn against (not block) an accidental duplicate task creation.
  private boolean hasActiveTaskOfSameType(String roomNumber, HousekeepingTask.TaskType taskType) {
    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();
    for (int i = 1; i <= fullList.getNumberOfEntries(); i++) {
      HousekeepingTask t = fullList.getEntry(i);
      if (t == null || !roomNumber.equalsIgnoreCase(t.getRoomNumber())) continue;
      if (t.getTaskType() != taskType) continue;

      boolean isActive =
          t.getStatus() == HousekeepingTask.Status.PENDING
              || t.getStatus() == HousekeepingTask.Status.ASSIGNED
              || t.getStatus() == HousekeepingTask.Status.IN_PROGRESS;
      if (isActive) return true;
    }
    return false;
  }

  private boolean isCleaningTaskType(HousekeepingTask.TaskType taskType) {
    return taskType == HousekeepingTask.TaskType.STANDARD_CLEAN
        || taskType == HousekeepingTask.TaskType.DEEP_CLEAN
        || taskType == HousekeepingTask.TaskType.TURNOVER;
  }

  // --- DEQUEUE NEXT TASK (NEXT STAFF PULLS FROM FRONT) ---
  private void handleDequeueNext() {
    try {
      HousekeepingTask top = taskRepo.dequeueNextTask();
      if (top == null) {
        ConsoleUtil.printError("Task board is currently empty!");
        return;
      }

      String staffId = houseKeepingView.promptStaffIdInput();
      if (staffId == null || staffId.trim().isEmpty() || "C".equalsIgnoreCase(staffId.trim())) {
        // No staff picked — put it back at the front (it's still in the master list; only
        // re-insert into the deque, don't re-add it, or it'd be duplicated).
        taskRepo.getTaskDeque().addFirst(top);
        return;
      }

      HousekeepingStaff staff = staffRepo.findById(staffId.trim());
      if (staff == null) {
        ConsoleUtil.printError("No staff found with ID: " + staffId.trim());
        taskRepo.getTaskDeque().addFirst(top); // put it back, don't drop it
        return;
      }

      assignTaskToStaff(top, staff);

      ConsoleUtil.clearScreen();
      System.out.println(" >> STATUS: [\u2713] SUCCESS");
      System.out.println(
          " Task "
              + top.getTaskId()
              + " (Room "
              + top.getRoomNumber()
              + ") assigned to "
              + staff.getName()
              + ". Staff must Start Cleaning to begin work.\n");
      ConsoleUtil.printContinueMessage();
    } catch (Exception e) {
      ConsoleUtil.printError(e.getMessage());
    }
  }

  // Staff has actually started work on the room: DIRTY -> CLEANING. Only fires from DIRTY —
  // a room already further along (or OCCUPIED) is left alone so this can't regress real state.
  private void advanceRoomToCleaning(String roomNumber) {
    Room room = roomRepo.findByRoomNumber(roomNumber);
    if (room == null || room.getStatus() != Room.Status.DIRTY) return;

    room.setStatus(Room.Status.CLEANING);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(roomNumber, Room.Status.DIRTY, Room.Status.CLEANING);
  }

  // Checks for tasks still active on this room before a done-status change is allowed through.
  // Returns true if it's safe to proceed (nothing stale, or the user chose to resolve it now —
  // stale tasks get force-completed here, before the caller applies the status change). Returns
  // false if the user declined, meaning the caller must NOT change the room's status.
  private boolean resolveStaleTasksBeforeStatusChange(String roomNumber) {
    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();
    ListInterface<HousekeepingTask> staleTasks = new ArrayList<>();

    for (int i = 1; i <= fullList.getNumberOfEntries(); i++) {
      HousekeepingTask t = fullList.getEntry(i);
      if (t == null || !roomNumber.equalsIgnoreCase(t.getRoomNumber())) continue;

      boolean stillActive =
          t.getStatus() == HousekeepingTask.Status.PENDING
              || t.getStatus() == HousekeepingTask.Status.ASSIGNED
              || t.getStatus() == HousekeepingTask.Status.IN_PROGRESS;
      if (stillActive) {
        staleTasks.add(t);
      }
    }

    if (staleTasks.isEmpty()) return true; // nothing in the way, proceed as normal

    boolean shouldComplete =
        ConsoleUtil.showConfirmMessage(
            "Room "
                + roomNumber
                + " has "
                + staleTasks.getNumberOfEntries()
                + " task(s) still PENDING/ASSIGNED/IN_PROGRESS. Mark them Completed and proceed"
                + " with the status change?");

    if (!shouldComplete) return false; // caller must not change the room's status

    for (int i = 1; i <= staleTasks.getNumberOfEntries(); i++) {
      finishTask(staleTasks.getEntry(i), HousekeepingTask.Status.COMPLETED);
    }
    return true;
  }

  // Assigns a task to a staff member and keeps the roster in sync: the room lands on the
  // staff's assigned-rooms list and their availability flips to ON_TASK.
  private void assignTaskToStaff(HousekeepingTask task, HousekeepingStaff staff) {
    taskRepo.assignStaff(task, staff.getStaffId());
    staffRepo.assignRoomToStaff(staff, task.getRoomNumber());
  }

  // Reassigns an active task to a different staff member. Uses reassignTask() rather than
  // assignStaff() so a task that was already IN_PROGRESS under the old staff resets to
  // ASSIGNED — the new staff member hasn't started cleaning yet and still needs to hit
  // "Start Cleaning" themselves.
  private void reassignTaskToStaff(HousekeepingTask task, HousekeepingStaff staff) {
    taskRepo.reassignTask(task, staff.getStaffId());
    staffRepo.assignRoomToStaff(staff, task.getRoomNumber());
  }

  // Moves a task to a terminal status (Completed/Skipped) and releases the assigned staff
  // member's hold on the room, freeing them back to AVAILABLE if they've got nothing else on.
  private void finishTask(HousekeepingTask task, HousekeepingTask.Status newStatus) {
    taskRepo.updateTaskStatus(task, newStatus);

    if (task.getAssignedStaffId() != null) {
      HousekeepingStaff staff = staffRepo.findById(task.getAssignedStaffId());
      if (staff != null) {
        staffRepo.releaseRoomFromStaff(staff, task.getRoomNumber());
      }
    }
  }

  // --- STAFF ASSIGNMENTS SCREEN LOOP ---
  public void manageStaffAssignments() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String shiftFilter = null;
    String availabilityFilter = null;

    while (true) {
      try {
        ListInterface<HousekeepingStaff> filteredList =
            filterStaffList(searchQuery, shiftFilter, availabilityFilter);

        currentPage = clampPage(currentPage, filteredList.getNumberOfEntries(), pageSize);

        ConsoleUtil.GetMenuInputResult result =
            houseKeepingView.renderStaffRosterScreen(
                filteredList, searchQuery, shiftFilter, availabilityFilter, currentPage, pageSize);

        if (result == null || result.input == null || result.input.trim().isEmpty()) {
          continue;
        }

        String command = result.input.trim();

        if ("E".equalsIgnoreCase(command)) {
          break; // Return to Housekeeping Main Menu
        } else if ("S".equalsIgnoreCase(command)) {
          String[] filters = handleStaffFilterMenu(searchQuery, shiftFilter, availabilityFilter);
          searchQuery = filters[0];
          shiftFilter = filters[1];
          availabilityFilter = filters[2];
          currentPage = 1;
        } else if ("N".equalsIgnoreCase(command)) {
          int totalMatches = filteredList.getNumberOfEntries();
          int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(command)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if ("A".equalsIgnoreCase(command)) {
          handleAddStaff();
        } else if (result.isNumber) {
          int selectedIndex = result.getAsInt();
          handleStaffAction(filteredList, selectedIndex, currentPage, pageSize);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- MAIN STAFF FILTER MENU & NESTED SUBMENUS ---
  private String[] handleStaffFilterMenu(
      String currentSearch, String currentShift, String currentAvailability) {

    String search = currentSearch;
    String shift = currentShift;
    String availability = currentAvailability;

    while (true) {
      try {
        int choice = houseKeepingView.displayStaffFilterMainMenu(search, shift, availability);

        if (choice == 1) {
          search = handleStaffSearchSubmenu(search);
        } else if (choice == 2) {
          shift = handleShiftSubmenu(shift);
        } else if (choice == 3) {
          availability = handleAvailabilitySubmenu(availability);
        } else if (choice == 4) {
          search = null;
          shift = null;
          availability = null;
        } else if (choice == 5) {
          return new String[] {search, shift, availability};
        } else if (choice == 6) {
          return new String[] {currentSearch, currentShift, currentAvailability};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleStaffSearchSubmenu(String currentSearch) {
    while (true) {
      try {
        int option = houseKeepingView.displayStaffSearchSubmenu(currentSearch);
        if (option == 1) {
          String input = houseKeepingView.promptStaffSearchInput();
          if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
            return currentSearch;
          }
          return input.trim();
        } else if (option == 2) {
          return null;
        } else if (option == 3) {
          return currentSearch;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleShiftSubmenu(String currentShift) {
    while (true) {
      try {
        int choice = houseKeepingView.displayShiftFilterSubmenu(currentShift);
        if (choice == 1) return "MORNING";
        if (choice == 2) return "AFTERNOON";
        if (choice == 3) return "NIGHT";
        if (choice == 4) return null;
        if (choice == 5) return currentShift;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleAvailabilitySubmenu(String currentAvailability) {
    while (true) {
      try {
        int choice = houseKeepingView.displayAvailabilityFilterSubmenu(currentAvailability);
        if (choice == 1) return "AVAILABLE";
        if (choice == 2) return "ON_TASK";
        if (choice == 3) return "OFF_DUTY";
        if (choice == 4) return null;
        if (choice == 5) return currentAvailability;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<HousekeepingStaff> filterStaffList(
      String search, String shiftFilter, String availabilityFilter) {

    ListInterface<HousekeepingStaff> source = staffRepo.getStaffList();
    ListInterface<HousekeepingStaff> filtered = new ArrayList<>();

    if (source == null || source.isEmpty()) return filtered;

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      HousekeepingStaff s = source.getEntry(i);
      if (s == null) continue;

      boolean matchesSearch =
          search == null
              || search.trim().isEmpty()
              || (s.getName() != null
                  && s.getName().toLowerCase().contains(search.trim().toLowerCase()));

      boolean matchesShift =
          shiftFilter == null
              || shiftFilter.trim().isEmpty()
              || shiftFilter.equalsIgnoreCase(s.getShift().name());

      boolean matchesAvailability =
          availabilityFilter == null
              || availabilityFilter.trim().isEmpty()
              || availabilityFilter.equalsIgnoreCase(s.getAvailability().name());

      if (matchesSearch && matchesShift && matchesAvailability) {
        filtered.add(s);
      }
    }

    return filtered;
  }

  // Creates a new staff member: name, then shift, then an auto-generated unique ID. New staff
  // always start AVAILABLE with no assigned rooms.
  private void handleAddStaff() {
    String nameInput = houseKeepingView.promptNewStaffName();
    if (nameInput == null || nameInput.trim().isEmpty() || "C".equalsIgnoreCase(nameInput.trim())) {
      return;
    }
    String name = nameInput.trim();

    HousekeepingStaff.Shift shift = null;
    while (shift == null) {
      int shiftChoice = houseKeepingView.displayShiftSelectionMenu();
      if (shiftChoice == 1) {
        shift = HousekeepingStaff.Shift.MORNING;
      } else if (shiftChoice == 2) {
        shift = HousekeepingStaff.Shift.AFTERNOON;
      } else if (shiftChoice == 3) {
        shift = HousekeepingStaff.Shift.NIGHT;
      } else if (shiftChoice == 4) {
        return; // Cancelled
      }
    }

    String staffId = generateUniqueStaffId();
    HousekeepingStaff newStaff =
        new HousekeepingStaff(staffId, name, shift, HousekeepingStaff.Availability.AVAILABLE);
    staffRepo.addStaff(newStaff);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(
        " Staff " + staffId + " (" + name + ", " + shift.name() + ") added to the roster.\n");
    ConsoleUtil.printContinueMessage();
  }

  private String generateUniqueStaffId() {
    return staffRepo.generateStaffId();
  }

  // --- ISOLATED STAFF ACTION SUBMENU LOOP ---
  private void handleStaffAction(
      ListInterface<HousekeepingStaff> list, int indexOnPage, int page, int pageSize) {
    while (true) {
      try {
        int actualIndex = (page - 1) * pageSize + indexOnPage;
        if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
          ConsoleUtil.printError("Invalid row selection index!");
          return;
        }

        HousekeepingStaff selected = list.getEntry(actualIndex);
        if (selected == null) return;

        int action = houseKeepingView.displayStaffActionSubmenu(selected);

        if (action == 1) {
          handleAutoAssignNextTask(selected);
        } else if (action == 2) {
          handleReassignRoom(selected);
        } else if (action == 3) {
          handleToggleAvailability(selected);
        } else if (action == 4) {
          showStaffTaskHistory(selected);
        } else if (action == 5) {
          handleEditShift(selected);
        }
        return; // Return back to staff roster
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleEditShift(HousekeepingStaff staff) {
    HousekeepingStaff.Shift oldShift = staff.getShift();

    HousekeepingStaff.Shift newShift = null;
    while (newShift == null) {
      int shiftChoice = houseKeepingView.displayShiftSelectionMenu();
      if (shiftChoice == 1) {
        newShift = HousekeepingStaff.Shift.MORNING;
      } else if (shiftChoice == 2) {
        newShift = HousekeepingStaff.Shift.AFTERNOON;
      } else if (shiftChoice == 3) {
        newShift = HousekeepingStaff.Shift.NIGHT;
      } else if (shiftChoice == 4) {
        return; // Cancelled
      }
    }

    if (newShift == oldShift) return; // No actual change

    staffRepo.setShift(staff, newShift);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(
        " "
            + staff.getName()
            + "'s shift changed from "
            + oldShift.name()
            + " to "
            + newShift.name()
            + ".\n");
    ConsoleUtil.printContinueMessage();
  }

  // Requires AVAILABLE first so we never dequeue a task and then discover it can't be placed —
  // the check happens before anything leaves the queue.
  private void handleAutoAssignNextTask(HousekeepingStaff staff) {
    if (staff.getAvailability() != HousekeepingStaff.Availability.AVAILABLE) {
      ConsoleUtil.printError(
          "Staff "
              + staff.getName()
              + " is currently "
              + staff.getAvailability().name()
              + " and can't be auto-assigned a new task.");
      return;
    }

    HousekeepingTask next = taskRepo.dequeueNextTask();
    if (next == null) {
      ConsoleUtil.printError("Task queue is currently empty — nothing to assign!");
      return;
    }

    assignTaskToStaff(next, staff);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(
        " Task "
            + next.getTaskId()
            + " (Room "
            + next.getRoomNumber()
            + ") auto-assigned to "
            + staff.getName()
            + ". Staff must Start Cleaning to begin work.\n");
    ConsoleUtil.printContinueMessage();
  }

  // Moves an active task on a given room from this staff member to another one.
  private void handleReassignRoom(HousekeepingStaff fromStaff) {
    String roomNumberInput = houseKeepingView.promptRoomNumberForReassign();
    if (roomNumberInput == null
        || roomNumberInput.trim().isEmpty()
        || "C".equalsIgnoreCase(roomNumberInput.trim())) {
      return;
    }
    String roomNumber = roomNumberInput.trim();

    HousekeepingTask activeTask = findActiveTaskForRoom(fromStaff.getStaffId(), roomNumber);
    if (activeTask == null) {
      ConsoleUtil.printError(
          fromStaff.getName() + " has no active task on room " + roomNumber + ".");
      return;
    }

    String targetStaffIdInput = houseKeepingView.promptTargetStaffIdInput();
    if (targetStaffIdInput == null
        || targetStaffIdInput.trim().isEmpty()
        || "C".equalsIgnoreCase(targetStaffIdInput.trim())) {
      return;
    }

    HousekeepingStaff toStaff = staffRepo.findById(targetStaffIdInput.trim());
    if (toStaff == null) {
      ConsoleUtil.printError("No staff found with ID: " + targetStaffIdInput.trim());
      return;
    }
    if (toStaff.getStaffId().equals(fromStaff.getStaffId())) {
      ConsoleUtil.printError("Pick a different staff member to reassign to!");
      return;
    }

    reassignTaskToStaff(activeTask, toStaff);
    staffRepo.releaseRoomFromStaff(fromStaff, roomNumber);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(
        " Room "
            + roomNumber
            + " reassigned from "
            + fromStaff.getName()
            + " to "
            + toStaff.getName()
            + ".\n");
    ConsoleUtil.printContinueMessage();
  }

  private HousekeepingTask findActiveTaskForRoom(String staffId, String roomNumber) {
    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();
    for (int i = 1; i <= fullList.getNumberOfEntries(); i++) {
      HousekeepingTask t = fullList.getEntry(i);
      if (t == null) continue;
      if (!roomNumber.equalsIgnoreCase(t.getRoomNumber())) continue;
      if (!staffId.equals(t.getAssignedStaffId())) continue;

      boolean isActive =
          t.getStatus() == HousekeepingTask.Status.PENDING
              || t.getStatus() == HousekeepingTask.Status.ASSIGNED
              || t.getStatus() == HousekeepingTask.Status.IN_PROGRESS;
      if (isActive) return t;
    }
    return null;
  }

  // Manually toggles between AVAILABLE and OFF_DUTY. Blocked while ON_TASK — that state is
  // system-managed (set/cleared by task assign/finish) so a manual flip here could leave a
  // task pointing at a staff member the roster claims is free, or vice versa.
  private void handleToggleAvailability(HousekeepingStaff staff) {
    if (staff.getAvailability() == HousekeepingStaff.Availability.ON_TASK) {
      ConsoleUtil.printError(
          staff.getName()
              + " is currently ON_TASK. Reassign or finish their active task before changing"
              + " availability.");
      return;
    }

    HousekeepingStaff.Availability newAvailability =
        staff.getAvailability() == HousekeepingStaff.Availability.OFF_DUTY
            ? HousekeepingStaff.Availability.AVAILABLE
            : HousekeepingStaff.Availability.OFF_DUTY;

    staffRepo.setAvailability(staff, newAvailability);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" " + staff.getName() + " is now " + newAvailability.name() + ".\n");
    ConsoleUtil.printContinueMessage();
  }

  // Tasks assigned to this staff member, created today — a lightweight "for the day" view
  // built directly off the task list rather than a separate log.
  private void showStaffTaskHistory(HousekeepingStaff staff) {
    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();
    ListInterface<HousekeepingTask> todayTasks = new ArrayList<>();

    LocalDate today = LocalDate.now();
    for (int i = 1; i <= fullList.getNumberOfEntries(); i++) {
      HousekeepingTask t = fullList.getEntry(i);
      if (t == null) continue;
      if (!staff.getStaffId().equals(t.getAssignedStaffId())) continue;
      if (t.getCreatedAt() == null || !t.getCreatedAt().toLocalDate().equals(today)) continue;

      todayTasks.add(t);
    }

    houseKeepingView.renderStaffTaskHistoryScreen(staff, todayTasks);
  }

  // --- ROOM STATUS SYNC SCREEN LOOP ---
  public void manageRoomStatusSync() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String statusFilter = null;

    while (true) {
      try {
        ListInterface<Room> filteredList = filterRoomList(searchQuery, statusFilter);

        // Same clamp as the task board: an action can shrink the filtered set (e.g. a status
        // change bumps a room out of the active status filter) out from under the current page.
        currentPage = clampPage(currentPage, filteredList.getNumberOfEntries(), pageSize);

        ConsoleUtil.GetMenuInputResult result =
            houseKeepingView.renderRoomStatusScreen(
                filteredList, searchQuery, statusFilter, currentPage, pageSize);

        if (result == null || result.input == null || result.input.trim().isEmpty()) {
          continue;
        }

        String command = result.input.trim();

        if ("E".equalsIgnoreCase(command)) {
          break; // Return to Housekeeping Main Menu
        } else if ("S".equalsIgnoreCase(command)) {
          String[] filters = handleRoomFilterMenu(searchQuery, statusFilter);
          searchQuery = filters[0];
          statusFilter = filters[1];
          currentPage = 1;
        } else if ("N".equalsIgnoreCase(command)) {
          int totalMatches = filteredList.getNumberOfEntries();
          int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(command)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if (result.isNumber) {
          int selectedIndex = result.getAsInt();
          handleRoomAction(filteredList, selectedIndex, currentPage, pageSize);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String[] handleRoomFilterMenu(String currentSearch, String currentStatus) {
    String search = currentSearch;
    String status = currentStatus;

    while (true) {
      try {
        int choice = houseKeepingView.displayRoomFilterMainMenu(search, status);

        if (choice == 1) {
          String input = houseKeepingView.promptRoomSearchInput();
          if (input != null && !input.trim().isEmpty() && !"C".equalsIgnoreCase(input.trim())) {
            search = input.trim();
          } else if (input != null && input.trim().isEmpty()) {
            search = null;
          }
        } else if (choice == 2) {
          int statusChoice = houseKeepingView.displayRoomStatusFilterSubmenu(status);
          if (statusChoice == 1) status = "DIRTY";
          else if (statusChoice == 2) status = "CLEANING";
          else if (statusChoice == 3) status = "INSPECTED";
          else if (statusChoice == 4) status = "VACANT_CLEAN";
          else if (statusChoice == 5) status = "OCCUPIED";
          else if (statusChoice == 6) status = null;
        } else if (choice == 3) {
          search = null;
          status = null;
        } else if (choice == 4) {
          return new String[] {search, status};
        } else if (choice == 5) {
          return new String[] {currentSearch, currentStatus};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleRoomAction(ListInterface<Room> list, int indexOnPage, int page, int pageSize) {
    while (true) {
      try {
        int actualIndex = (page - 1) * pageSize + indexOnPage;
        if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
          ConsoleUtil.printError("Invalid row selection index!");
          return;
        }

        Room selected = list.getEntry(actualIndex);
        if (selected == null) return;

        boolean hasUndoHistory =
            !roomStatusHistoryRepo.getRecentHistory(selected.getRoomNumber(), 1).isEmpty();

        int action = houseKeepingView.displayRoomActionSubmenu(selected, hasUndoHistory);

        if (action == 1) {
          int statusChoice = houseKeepingView.promptNewStatusInput();
          Room.Status newStatus = mapRoomStatus(statusChoice);
          Room.Status oldStatus = selected.getStatus();

          if (newStatus != oldStatus) {
            boolean okToProceed = true;

            if (newStatus == Room.Status.VACANT_CLEAN || newStatus == Room.Status.INSPECTED) {
              okToProceed = resolveStaleTasksBeforeStatusChange(selected.getRoomNumber());
            }

            if (okToProceed) {
              selected.setStatus(newStatus);
              roomRepo.updateRoom(selected);
              roomStatusHistoryRepo.recordStatusChange(
                  selected.getRoomNumber(), oldStatus, newStatus);
            } else {
              ConsoleUtil.printError(
                  "Status change cancelled — resolve the pending task(s) via Task Board first.");
            }
          }
        } else if (action == 2) {
          Room.Status revertTo = roomStatusHistoryRepo.undoLastChange(selected.getRoomNumber());
          if (revertTo != null) {
            selected.setStatus(revertTo);
            roomRepo.updateRoom(selected);
          } else {
            ConsoleUtil.printError("No prior status change on record for this room!");
          }
        } else if (action == 3) {
          ListInterface<RoomStatusLogEntry> history =
              roomStatusHistoryRepo.getRecentHistory(selected.getRoomNumber(), 10);
          houseKeepingView.renderRoomHistoryScreen(selected.getRoomNumber(), history);
        } else if (action == 4) {
          if (hasActiveTaskOfSameType(
              selected.getRoomNumber(), HousekeepingTask.TaskType.MAINTENANCE_CHECK)) {
            boolean proceedAnyway =
                ConsoleUtil.showConfirmMessage(
                    "Room "
                        + selected.getRoomNumber()
                        + " already has an active Maintenance Check task. Flag another one"
                        + " anyway?");
            if (!proceedAnyway) {
              return; // Cancelled, back to Room Status Sync screen
            }
          }

          roomStatusHistoryRepo.recordNote(selected.getRoomNumber(), "Flagged for maintenance");

          HousekeepingTask maintenanceTask =
              new HousekeepingTask(
                  taskRepo.generateTaskId(),
                  selected.getRoomNumber(),
                  HousekeepingTask.TaskType.MAINTENANCE_CHECK,
                  HousekeepingTask.Status.PENDING,
                  null,
                  false,
                  LocalDateTime.now());
          taskRepo.enqueueTask(maintenanceTask);
        } else if (action == 5) {
          roomStatusHistoryRepo.recordNote(
              selected.getRoomNumber(), "Supervisor inspection requested");
        }

        if (action != 3) return; // Return to Room Status Sync screen (history has its own pause)
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Room> filterRoomList(String search, String statusFilter) {
    ListInterface<Room> source = roomRepo.getRoomList();
    ListInterface<Room> filtered = new ArrayList<>();

    if (source == null || source.isEmpty()) return filtered;

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Room r = source.getEntry(i);
      if (r == null) continue;

      boolean matchesSearch =
          search == null
              || search.trim().isEmpty()
              || (r.getRoomNumber() != null
                  && r.getRoomNumber().toLowerCase().contains(search.trim().toLowerCase()));

      boolean matchesStatus =
          statusFilter == null
              || statusFilter.trim().isEmpty()
              || statusFilter.equalsIgnoreCase(r.getStatus().name());

      if (matchesSearch && matchesStatus) {
        filtered.add(r);
      }
    }

    return filtered;
  }

  private Room.Status mapRoomStatus(int choice) {
    switch (choice) {
      case 2:
        return Room.Status.CLEANING;
      case 3:
        return Room.Status.INSPECTED;
      case 4:
        return Room.Status.VACANT_CLEAN;
      case 5:
        return Room.Status.OCCUPIED;
      default:
        return Room.Status.DIRTY;
    }
  }

  // --- MAIN FILTER MENU & NESTED SUBMENUS ---
  private String[] handleFilterMenu(
      String currentSearch, String currentTaskType, String currentStatus) {

    String search = currentSearch;
    String taskType = currentTaskType;
    String status = currentStatus;

    while (true) {
      try {
        int choice = houseKeepingView.displayFilterMainMenu(search, taskType, status);

        if (choice == 1) {
          search = handleSearchQuerySubmenu(search);
        } else if (choice == 2) {
          taskType = handleTaskTypeSubmenu(taskType);
        } else if (choice == 3) {
          status = handleStatusSubmenu(status);
        } else if (choice == 4) {
          search = null;
          taskType = null;
          status = null;
        } else if (choice == 5) {
          return new String[] {search, taskType, status};
        } else if (choice == 6) {
          return new String[] {currentSearch, currentTaskType, currentStatus};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchQuerySubmenu(String currentSearch) {
    while (true) {
      try {
        int option = houseKeepingView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          String input = houseKeepingView.promptSearchInput();
          if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
            return currentSearch;
          }
          return input.trim();
        } else if (option == 2) {
          return null;
        } else if (option == 3) {
          return currentSearch;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleTaskTypeSubmenu(String currentType) {
    while (true) {
      try {
        int choice = houseKeepingView.displayTaskTypeSubmenu(currentType);
        if (choice == 1) return "STANDARD_CLEAN";
        if (choice == 2) return "DEEP_CLEAN";
        if (choice == 3) return "TURNOVER";
        if (choice == 4) return "MAINTENANCE_CHECK";
        if (choice == 5) return null;
        if (choice == 6) return currentType;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleStatusSubmenu(String currentStatus) {
    while (true) {
      try {
        int choice = houseKeepingView.displayStatusSubmenu(currentStatus);
        if (choice == 1) return "PENDING";
        if (choice == 2) return "ASSIGNED";
        if (choice == 3) return "IN_PROGRESS";
        if (choice == 4) return "COMPLETED";
        if (choice == 5) return "SKIPPED";
        if (choice == 6) return null;
        if (choice == 7) return currentStatus;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- ISOLATED DISPLAY ORDER SUBMENU LOOP ---
  private String handleDisplayOrderMenu(String currentOrder) {
    while (true) {
      try {
        String selected = houseKeepingView.displayDisplayOrderMenu();
        if (selected == null) {
          return currentOrder; // Keep current order
        }
        return selected;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- ISOLATED TASK ACTION SUBMENU LOOP ---
  private void handleTaskAction(
      ListInterface<HousekeepingTask> list, int indexOnPage, int page, int pageSize) {
    while (true) {
      try {
        int actualIndex = (page - 1) * pageSize + indexOnPage;
        if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
          ConsoleUtil.printError("Invalid row selection index!");
          return;
        }

        HousekeepingTask selected = list.getEntry(actualIndex);
        if (selected == null) return;

        int action = houseKeepingView.displayTaskActionSubmenu(selected);

        // Assign / Start Cleaning / Escalate don't make sense on a task that's already
        // Completed or Skipped — block those here so the terminal status can't be regressed
        // (e.g. Start Cleaning silently flipping a COMPLETED task back to IN_PROGRESS).
        boolean isTerminal =
            selected.getStatus() == HousekeepingTask.Status.COMPLETED
                || selected.getStatus() == HousekeepingTask.Status.SKIPPED;
        if (isTerminal && (action == 1 || action == 2 || action == 5)) {
          ConsoleUtil.printError(
              "Task "
                  + selected.getTaskId()
                  + " is already "
                  + selected.getStatus().name()
                  + " — this action is no longer available.");
          continue;
        }

        if (action == 1) {
          String staffId = houseKeepingView.promptStaffIdInput();
          if (staffId != null
              && !staffId.trim().isEmpty()
              && !"C".equalsIgnoreCase(staffId.trim())) {
            HousekeepingStaff staff = staffRepo.findById(staffId.trim());
            if (staff == null) {
              ConsoleUtil.printError("No staff found with ID: " + staffId.trim());
              continue;
            }
            assignTaskToStaff(selected, staff);
          }
        } else if (action == 2) {
          boolean started = taskRepo.startCleaning(selected);
          if (started) {
            advanceRoomToCleaning(selected.getRoomNumber());
          } else {
            ConsoleUtil.printError("Assign a staff member to this task before starting cleaning!");
          }
        } else if (action == 3) {
          finishTask(selected, HousekeepingTask.Status.COMPLETED);
        } else if (action == 4) {
          finishTask(selected, HousekeepingTask.Status.SKIPPED);
        } else if (action == 5) {
          boolean escalated = taskRepo.escalateToFront(selected);
          if (escalated) {
            ConsoleUtil.clearScreen();
            System.out.println(" >> STATUS: [\u2713] SUCCESS");
            System.out.println(
                " Task " + selected.getTaskId() + " escalated to the front of the queue.\n");
            ConsoleUtil.printContinueMessage();
          } else {
            ConsoleUtil.printError(
                "Task "
                    + selected.getTaskId()
                    + " is not currently waiting in the queue (status: "
                    + selected.getStatus().name()
                    + ") and cannot be escalated.");
          }
        }
        return; // Return back to task board
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Keeps a page index inside [1, totalPages] for a given result-set size. An empty result set
  // still reports page 1 (matches what the view treats as its empty state).
  private int clampPage(int currentPage, int totalMatches, int pageSize) {
    int totalPages = (totalMatches == 0) ? 1 : (int) Math.ceil((double) totalMatches / pageSize);
    if (currentPage > totalPages) return totalPages;
    if (currentPage < 1) return 1;
    return currentPage;
  }

  // --- BUILD DISPLAY LIST: TRUE DEQUE ORDER (FRONT -> BACK) FIRST, THEN EVERYTHING ELSE ---
  // Every task appears exactly once. Still-queued (PENDING) tasks come first, in the exact
  // order the deque would hand them out. Tasks that have already left the queue (in progress,
  // completed, skipped) follow afterwards in their original insertion order.
  private ListInterface<HousekeepingTask> buildDisplayList() {
    ListInterface<HousekeepingTask> ordered = new ArrayList<>();

    Iterator<HousekeepingTask> it = taskRepo.getTaskDeque().getIterator();
    while (it.hasNext()) {
      ordered.add(it.next());
    }

    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();
    for (int i = 1; i <= fullList.getNumberOfEntries(); i++) {
      HousekeepingTask t = fullList.getEntry(i);
      if (t != null && !ordered.contains(t)) {
        ordered.add(t);
      }
    }

    return ordered;
  }

  // --- FILTER & SORT HELPER ---
  private ListInterface<HousekeepingTask> filterAndSortList(
      ListInterface<HousekeepingTask> source,
      String search,
      String taskType,
      String status,
      String displayOrder) {

    if (source == null || source.isEmpty()) {
      return new ArrayList<>();
    }

    ListInterface<HousekeepingTask> filtered = new ArrayList<>();
    ListInterface<HousekeepingStaff> staffList = staffRepo.getStaffList();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      HousekeepingTask t = source.getEntry(i);
      if (t == null) continue;

      HousekeepingStaff staff = findStaff(staffList, t.getAssignedStaffId());

      boolean matchesSearch = true;
      boolean matchesType = true;
      boolean matchesStatus = true;

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchRoom =
            t.getRoomNumber() != null && t.getRoomNumber().toLowerCase().contains(query);
        boolean matchStaffName =
            staff != null
                && staff.getName() != null
                && staff.getName().toLowerCase().contains(query);
        matchesSearch = matchRoom || matchStaffName;
      }

      if (taskType != null && !taskType.trim().isEmpty()) {
        matchesType = taskType.equalsIgnoreCase(t.getTaskType().name());
      }

      if (status != null && !status.trim().isEmpty()) {
        matchesStatus = status.equalsIgnoreCase(t.getStatus().name());
      } else {
        // No explicit status filter selected: default the board to active work only. Nothing
        // is deleted — Completed/Skipped tasks stay fully queryable via the status filter and
        // remain in taskRepo for Reports (Task 4) — they just don't clutter the default view.
        matchesStatus =
            t.getStatus() != HousekeepingTask.Status.COMPLETED
                && t.getStatus() != HousekeepingTask.Status.SKIPPED;
      }

      if (matchesSearch && matchesType && matchesStatus) {
        filtered.add(t);
      }
    }

    // "QUEUE ORDER" needs no re-sort: `source` already arrives in front-to-back deque order.
    if ("ROOM NUMBER (ASCENDING)".equalsIgnoreCase(displayOrder)) {
      filtered.sort((t1, t2) -> t1.getRoomNumber().compareTo(t2.getRoomNumber()));
    } else if ("ASSIGNED TIME (OLDEST -> NEWEST)".equalsIgnoreCase(displayOrder)) {
      filtered.sort((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()));
    } else if ("ASSIGNED TIME (NEWEST -> OLDEST)".equalsIgnoreCase(displayOrder)) {
      filtered.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
    }

    return filtered;
  }

  private HousekeepingTask.TaskType mapTaskType(int choice) {
    switch (choice) {
      case 2:
        return HousekeepingTask.TaskType.DEEP_CLEAN;
      case 3:
        return HousekeepingTask.TaskType.TURNOVER;
      case 4:
        return HousekeepingTask.TaskType.MAINTENANCE_CHECK;
      default:
        return HousekeepingTask.TaskType.STANDARD_CLEAN;
    }
  }

  private HousekeepingStaff findStaff(ListInterface<HousekeepingStaff> staffList, String staffId) {
    if (staffList == null || staffId == null) return null;
    for (int i = 1; i <= staffList.getNumberOfEntries(); i++) {
      HousekeepingStaff s = staffList.getEntry(i);
      if (s != null && staffId.equals(s.getStaffId())) return s;
    }
    return null;
  }

  // --- SETTINGS & CONFIGURATION SCREEN LOOP ---
  public void manageSettings() {
    while (true) {
      try {
        int choice = houseKeepingView.displaySettingsMainMenu();

        if (choice == 1) {
          manageTaskRules();
        } else if (choice == 2) {
          manageStaffConfiguration();
        } else if (choice == 3) {
          return; // Back to Housekeeping Main Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageTaskRules() {
    while (true) {
      try {
        HousekeepingSettings settings = settingsRepo.getSettings();
        int choice = houseKeepingView.displayTaskRulesMenu(settings);

        if (choice == 1) {
          handleEditCleaningTimes(settings);
        } else if (choice == 2) {
          handleEditOverdueThreshold(settings);
        } else if (choice == 3) {
          handleEditQueueJumpTypes(settings);
        } else if (choice == 4) {
          return; // Back to Settings Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleEditCleaningTimes(HousekeepingSettings settings) {
    houseKeepingView.printEditCleaningTimesHeader();

    Integer standard =
        ConsoleUtil.getIntegerInput(
            "STANDARD room clean time in minutes ["
                + settings.getCleanTimeStandardMinutes()
                + "]: ",
            1,
            600);
    Integer suite =
        ConsoleUtil.getIntegerInput(
            "SUITE room clean time in minutes [" + settings.getCleanTimeSuiteMinutes() + "]: ",
            1,
            600);
    Integer luxury =
        ConsoleUtil.getIntegerInput(
            "LUXURY room clean time in minutes [" + settings.getCleanTimeLuxuryMinutes() + "]: ",
            1,
            600);

    settingsRepo.updateCleaningTimes(
        standard != null ? standard : settings.getCleanTimeStandardMinutes(),
        suite != null ? suite : settings.getCleanTimeSuiteMinutes(),
        luxury != null ? luxury : settings.getCleanTimeLuxuryMinutes());

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Cleaning time estimates updated.\n");
    ConsoleUtil.printContinueMessage();
  }

  private void handleEditOverdueThreshold(HousekeepingSettings settings) {
    houseKeepingView.printEditOverdueThresholdHeader(settings);

    Integer minutes =
        ConsoleUtil.getIntegerInput(
            "New overdue threshold in minutes [" + settings.getOverdueThresholdMinutes() + "]: ",
            1,
            1440);

    if (minutes == null) return; // Cancelled / kept unchanged

    settingsRepo.updateOverdueThreshold(minutes);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Overdue threshold updated to " + minutes + " minutes.\n");
    ConsoleUtil.printContinueMessage();
  }

  private void handleEditQueueJumpTypes(HousekeepingSettings settings) {
    boolean[] result = houseKeepingView.promptQueueJumpTypes(settings);
    settingsRepo.updateQueueJumpTypes(result[0], result[1], result[2], result[3]);

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Queue-jump eligible task types updated.\n");
    ConsoleUtil.printContinueMessage();
  }

  private void manageStaffConfiguration() {
    while (true) {
      try {
        HousekeepingSettings settings = settingsRepo.getSettings();
        int choice = houseKeepingView.displayStaffConfigMenu(settings);

        if (choice == 1) {
          handleEditShiftSchedules(settings);
        } else if (choice == 2) {
          handleEditMaxRooms(settings);
        } else if (choice == 3) {
          return; // Back to Settings Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleEditShiftSchedules(HousekeepingSettings settings) {
    houseKeepingView.printEditShiftSchedulesHeader();

    String morning =
        promptOptionalText("MORNING shift schedule [" + settings.getMorningShiftSchedule() + "]: ");
    String afternoon =
        promptOptionalText(
            "AFTERNOON shift schedule [" + settings.getAfternoonShiftSchedule() + "]: ");
    String night =
        promptOptionalText("NIGHT shift schedule [" + settings.getNightShiftSchedule() + "]: ");

    settingsRepo.updateShiftSchedules(
        morning != null ? morning : settings.getMorningShiftSchedule(),
        afternoon != null ? afternoon : settings.getAfternoonShiftSchedule(),
        night != null ? night : settings.getNightShiftSchedule());

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Shift schedules updated.\n");
    ConsoleUtil.printContinueMessage();
  }

  // Blank or "C" means "keep current" — mirrors getIntegerInput's built-in cancel semantics,
  // for the free-text fields where there's no equivalent ConsoleUtil helper.
  private String promptOptionalText(String prompt) {
    String input = ConsoleUtil.getStringInput(prompt);
    if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
      return null;
    }
    return input.trim();
  }

  private void handleEditMaxRooms(HousekeepingSettings settings) {
    houseKeepingView.printEditMaxRoomsHeader();

    Integer morning =
        ConsoleUtil.getIntegerInput(
            "Max rooms for MORNING shift [" + settings.getMaxRoomsMorning() + "]: ", 1, 50);
    Integer afternoon =
        ConsoleUtil.getIntegerInput(
            "Max rooms for AFTERNOON shift [" + settings.getMaxRoomsAfternoon() + "]: ", 1, 50);
    Integer night =
        ConsoleUtil.getIntegerInput(
            "Max rooms for NIGHT shift [" + settings.getMaxRoomsNight() + "]: ", 1, 50);

    settingsRepo.updateMaxRooms(
        morning != null ? morning : settings.getMaxRoomsMorning(),
        afternoon != null ? afternoon : settings.getMaxRoomsAfternoon(),
        night != null ? night : settings.getMaxRoomsNight());

    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Max rooms per shift updated.\n");
    ConsoleUtil.printContinueMessage();
  }
}
