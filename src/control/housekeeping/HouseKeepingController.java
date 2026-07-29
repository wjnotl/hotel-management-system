package control.housekeeping;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import java.time.LocalDateTime;
import java.util.Iterator;
import repo.HousekeepingStaffRepo;
import repo.HousekeepingTaskRepo;
import util.ConsoleUtil;
import util.NumberUtil;
import view.HousekeepingView;

public class HouseKeepingController {
  private final HousekeepingView houseKeepingView;
  private final HousekeepingTaskRepo taskRepo;
  private final HousekeepingStaffRepo staffRepo;

  public HouseKeepingController() {
    this.houseKeepingView = new HousekeepingView();
    this.taskRepo = new HousekeepingTaskRepo();
    this.staffRepo = new HousekeepingStaffRepo();
  }

  // --- HOUSEKEEPING MAIN MENU LOOP ---
  public void start() {
    while (true) {
      try {
        String choice = houseKeepingView.displayMenu();

        if ("1".equals(choice)) {
          manageCleaningTaskBoard();
        } else if ("6".equals(choice)) {
          return; // Go back to Resort Main Menu
        }
        // Options 2-5 (Staff Assignments, Room Status Sync, Reports, Settings) are still WIP.
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

        // 3. Render Task Board Screen
        ConsoleUtil.GetMenuInputResult result =
            houseKeepingView.renderTaskBoardScreen(
                filteredList,
                staffRepo.getStaffList(),
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
        String roomNumber = houseKeepingView.promptRoomNumberInput();
        if (roomNumber == null
            || roomNumber.trim().isEmpty()
            || "C".equalsIgnoreCase(roomNumber.trim())) {
          return; // Cancel action
        }

        int typeChoice = houseKeepingView.promptTaskTypeInput();
        HousekeepingTask.TaskType taskType = mapTaskType(typeChoice);

        HousekeepingTask newTask =
            new HousekeepingTask(
                NumberUtil.generateFormattedId("T-", 1000, 9999, 4),
                roomNumber.trim(),
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

  // --- DEQUEUE NEXT TASK (NEXT STAFF PULLS FROM FRONT) ---
  private void handleDequeueNext() {
    try {
      HousekeepingTask top = taskRepo.dequeueNextTask();
      if (top != null) {
        taskRepo.updateTaskStatus(top, HousekeepingTask.Status.IN_PROGRESS);

        ConsoleUtil.clearScreen();
        System.out.println(" >> STATUS: [\u2713] SUCCESS");
        System.out.println(
            " Next task dequeued: " + top.getTaskId() + " (Room " + top.getRoomNumber() + ")\n");
        ConsoleUtil.printContinueMessage();
      } else {
        ConsoleUtil.printError("Task board is currently empty!");
      }
    } catch (Exception e) {
      ConsoleUtil.printError(e.getMessage());
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
        if (choice == 2) return "IN_PROGRESS";
        if (choice == 3) return "COMPLETED";
        if (choice == 4) return "SKIPPED";
        if (choice == 5) return null;
        if (choice == 6) return currentStatus;
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
            taskRepo.assignStaff(selected, staff.getStaffId());
          }
        } else if (action == 2) {
          taskRepo.updateTaskStatus(selected, HousekeepingTask.Status.IN_PROGRESS);
        } else if (action == 3) {
          taskRepo.updateTaskStatus(selected, HousekeepingTask.Status.COMPLETED);
        } else if (action == 4) {
          taskRepo.updateTaskStatus(selected, HousekeepingTask.Status.SKIPPED);
        } else if (action == 5) {
          taskRepo.escalateToFront(selected);
        }
        return; // Return back to task board
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
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
}
