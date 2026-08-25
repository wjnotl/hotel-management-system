package control.housekeeping;

import adt.ListInterface;
import entity.HousekeepingSettings;
import entity.HousekeepingStaff;
import entity.HousekeepingTask;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import repo.HousekeepingSettingsRepo;
import repo.HousekeepingStaffRepo;
import repo.HousekeepingTaskRepo;
import repo.RoomRepo;
import util.ConsoleUtil;
import util.TxtExportUtil;
import view.housekeeping.HousekeepingReportView;
import view.housekeeping.HousekeepingReportView.ReportResult;
import view.housekeeping.HousekeepingReportView.StaffPerformanceRowDTO;

public class HousekeepingReportController {
  private final HousekeepingReportView reportView = new HousekeepingReportView();
  private final HousekeepingTaskRepo taskRepo;
  private final HousekeepingStaffRepo staffRepo;
  private final RoomRepo roomRepo;
  private final HousekeepingSettingsRepo settingsRepo;

  public HousekeepingReportController(
      HousekeepingTaskRepo taskRepo,
      HousekeepingStaffRepo staffRepo,
      RoomRepo roomRepo,
      HousekeepingSettingsRepo settingsRepo) {
    this.taskRepo = taskRepo;
    this.staffRepo = staffRepo;
    this.roomRepo = roomRepo;
    this.settingsRepo = settingsRepo;
  }

  // --- REPORT TYPE ROUTER ---
  public void start() {
    while (true) {
      int choice = reportView.displayReportTypeMenu();
      if (choice == 1) {
        startOperationsReport();
      } else if (choice == 2) {
        startStaffPerformanceReport();
      } else if (choice == 3) {
        return; // Back to Housekeeping Main Menu
      }
    }
  }

  // --- OPERATIONS REPORT: FILTER HUB LOOP ---
  private void startOperationsReport() {
    LocalDate startDate = null;
    LocalDate endDate = null;
    String datePresetLabel = "ALL TIME";
    String staffId = null;
    Room.RoomType roomTypeFilter = null;
    HousekeepingTask.TaskType taskTypeFilter = null;

    while (true) {
      try {
        int choice =
            reportView.displayFilterHub(
                dateRangeLabel(startDate, endDate),
                staffLabel(staffId),
                roomTypeFilter == null ? "ALL" : roomTypeFilter.name(),
                taskTypeFilter == null ? "ALL" : taskTypeFilter.name());

        if (choice == 1) {
          Object[] range = handleDateRangeSubmenu(startDate, endDate, datePresetLabel);
          startDate = (LocalDate) range[0];
          endDate = (LocalDate) range[1];
          datePresetLabel = (String) range[2];
        } else if (choice == 2) {
          staffId = handleStaffFilterSubmenu(staffId);
        } else if (choice == 3) {
          roomTypeFilter = handleRoomTypeFilterSubmenu(roomTypeFilter);
        } else if (choice == 4) {
          taskTypeFilter = handleTaskTypeFilterSubmenu(taskTypeFilter);
        } else if (choice == 5) {
          startDate = null;
          endDate = null;
          datePresetLabel = "ALL TIME";
          staffId = null;
          roomTypeFilter = null;
          taskTypeFilter = null;
        } else if (choice == 6) {
          generateAndShowReport(startDate, endDate, staffId, roomTypeFilter, taskTypeFilter);
        } else if (choice == 7) {
          return; // Cancel, back to Report Type Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String dateRangeLabel(LocalDate start, LocalDate end) {
    if (start == null && end == null) return "ALL";
    return (start == null ? "..." : start.toString())
        + " to "
        + (end == null ? "..." : end.toString());
  }

  private String staffLabel(String staffId) {
    if (staffId == null) return "ALL";
    HousekeepingStaff staff = staffRepo.findById(staffId);
    return staff == null ? staffId : staffId + " (" + staff.getName() + ")";
  }

  // --- DATE RANGE SUBMENU ---
  // Returns {LocalDate start, LocalDate end, String datePresetLabel}.
  private Object[] handleDateRangeSubmenu(
      LocalDate currentStart, LocalDate currentEnd, String currentPresetLabel) {
    LocalDate start = currentStart;
    LocalDate end = currentEnd;
    String presetLabel = currentPresetLabel;

    while (true) {
      try {
        int choice =
            reportView.displayDateRangeSubmenu(
                start == null ? "Not Set" : start.toString(),
                end == null ? "Not Set" : end.toString(),
                presetLabel);

        if (choice == 1) {
          start = LocalDate.now();
          end = LocalDate.now();
          presetLabel = "TODAY";
        } else if (choice == 2) {
          LocalDate yesterday = LocalDate.now().minusDays(1);
          start = yesterday;
          end = yesterday;
          presetLabel = "YESTERDAY";
        } else if (choice == 3) {
          start = LocalDate.now().minusDays(6);
          end = LocalDate.now();
          presetLabel = "LAST 7 DAYS";
        } else if (choice == 4) {
          start = LocalDate.now().minusDays(29);
          end = LocalDate.now();
          presetLabel = "LAST 30 DAYS";
        } else if (choice == 5) {
          LocalDate[] custom = handleCustomDateRangeSubmenu(start, end);
          start = custom[0];
          end = custom[1];
          presetLabel = "CUSTOM";
        } else if (choice == 6) {
          start = null;
          end = null;
          presetLabel = "ALL TIME";
        } else if (choice == 7) {
          return new Object[] {start, end, presetLabel};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- CUSTOM DATE RANGE SUBMENU ---
  private LocalDate[] handleCustomDateRangeSubmenu(LocalDate currentStart, LocalDate currentEnd) {
    LocalDate start = currentStart;
    LocalDate end = currentEnd;

    while (true) {
      try {
        int choice =
            reportView.displayCustomDateRangeSubmenu(
                start == null ? "Not Set" : start.toString(),
                end == null ? "Not Set" : end.toString());

        if (choice == 1) {
          start = promptForDate("Start Date", start);
        } else if (choice == 2) {
          end = promptForDate("End Date", end);
        } else if (choice == 3) {
          return new LocalDate[] {start, end};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private LocalDate promptForDate(String label, LocalDate current) {
    while (true) {
      String input = reportView.promptDateInput(label);
      if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
        return current; // Cancel this single edit, keep whatever it was
      }
      try {
        return LocalDate.parse(input.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (DateTimeParseException e) {
        ConsoleUtil.printError("Invalid date format. Use YYYY-MM-DD (e.g. 2026-07-01).");
      }
    }
  }

  // --- STAFF FILTER SUBMENU ---
  private String handleStaffFilterSubmenu(String currentStaffId) {
    while (true) {
      String input = reportView.promptStaffFilterInput(staffLabel(currentStaffId));
      if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
        return currentStaffId; // Leave unchanged (blank does NOT clear here — matches Task Board
        // convention where blank/C cancels the edit)
      }

      HousekeepingStaff staff = staffRepo.findById(input.trim());
      if (staff == null) {
        ConsoleUtil.printError("No staff found with ID: " + input.trim());
        continue;
      }
      return staff.getStaffId();
    }
  }

  // --- ROOM TYPE FILTER SUBMENU ---
  private Room.RoomType handleRoomTypeFilterSubmenu(Room.RoomType current) {
    while (true) {
      try {
        int choice =
            reportView.displayRoomTypeFilterSubmenu(current == null ? "ALL" : current.name());
        if (choice == 1) return Room.RoomType.LUXURY;
        if (choice == 2) return Room.RoomType.SUITE;
        if (choice == 3) return Room.RoomType.STANDARD;
        if (choice == 4) return null;
        if (choice == 5) return current;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- TASK TYPE FILTER SUBMENU ---
  private HousekeepingTask.TaskType handleTaskTypeFilterSubmenu(HousekeepingTask.TaskType current) {
    while (true) {
      try {
        int choice =
            reportView.displayTaskTypeFilterSubmenu(current == null ? "ALL" : current.name());
        if (choice == 1) return HousekeepingTask.TaskType.STANDARD_CLEAN;
        if (choice == 2) return HousekeepingTask.TaskType.DEEP_CLEAN;
        if (choice == 3) return HousekeepingTask.TaskType.MAINTENANCE_CHECK;
        if (choice == 4) return null;
        if (choice == 5) return current;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- CALCULATION ENGINE + REPORT OUTPUT ---
  private void generateAndShowReport(
      LocalDate startDate,
      LocalDate endDate,
      String staffId,
      Room.RoomType roomTypeFilter,
      HousekeepingTask.TaskType taskTypeFilter) {

    ListInterface<HousekeepingTask> matched =
        filterTasks(startDate, endDate, staffId, roomTypeFilter, taskTypeFilter);

    ReportResult result = calculateReport(matched);

    int choice =
        reportView.displayReportOutput(
            dateRangeLabel(startDate, endDate),
            staffLabel(staffId),
            roomTypeFilter == null ? "ALL" : roomTypeFilter.name(),
            taskTypeFilter == null ? "ALL" : taskTypeFilter.name(),
            result);

    if (choice == 1) {
      exportReportToFile(
          dateRangeLabel(startDate, endDate),
          staffLabel(staffId),
          roomTypeFilter == null ? "ALL" : roomTypeFilter.name(),
          taskTypeFilter == null ? "ALL" : taskTypeFilter.name(),
          result);
    }
    // choice == 2 (or export done): falls through, back to filter hub
  }

  private ListInterface<HousekeepingTask> filterTasks(
      LocalDate startDate,
      LocalDate endDate,
      String staffId,
      Room.RoomType roomTypeFilter,
      HousekeepingTask.TaskType taskTypeFilter) {

    ListInterface<HousekeepingTask> fullList = taskRepo.getTaskList();

    return fullList.filter(
        t -> {
          if (t == null || t.getCreatedAt() == null) return false;

          LocalDate createdDate = t.getCreatedAt().toLocalDate();
          if (startDate != null && createdDate.isBefore(startDate)) return false;
          if (endDate != null && createdDate.isAfter(endDate)) return false;

          if (staffId != null && !staffId.equals(t.getAssignedStaffId())) return false;

          if (taskTypeFilter != null && t.getTaskType() != taskTypeFilter) return false;

          if (roomTypeFilter != null) {
            Room room = roomRepo.findByRoomNumber(t.getRoomNumber());
            if (room == null || room.getRoomType() != roomTypeFilter) return false;
          }

          return true;
        });
  }

  private ReportResult calculateReport(ListInterface<HousekeepingTask> matched) {
    int total = matched.getNumberOfEntries();

    // 1. Average cleaning time per room type (cleaning-type tasks only, needs both timestamps)
    int luxCount = 0, suiteCount = 0, stdCount = 0;
    double luxTotalMin = 0, suiteTotalMin = 0, stdTotalMin = 0;

    // 2. Staff productivity (COMPLETED tasks per shift)
    int morningCount = 0, afternoonCount = 0, nightCount = 0;

    // 3. Overdue / skipped
    int skippedCount = 0;
    int overdueCount = 0;

    // 4. Maintenance flag frequency
    int maintenanceCount = 0;

    // 5. Average queue wait time before dequeued
    int queueWaitSampleCount = 0;
    double queueWaitTotalMin = 0;

    HousekeepingSettings settings = settingsRepo.getSettings();
    int overdueThreshold = settings.getOverdueThresholdMinutes();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 1; i <= total; i++) {
      HousekeepingTask t = matched.getEntry(i);
      if (t == null) continue;

      // --- Cleaning time (only cleaning-type tasks with a full start->complete pair) ---
      boolean isCleaningType =
          t.getTaskType() == HousekeepingTask.TaskType.STANDARD_CLEAN
              || t.getTaskType() == HousekeepingTask.TaskType.DEEP_CLEAN;

      if (isCleaningType && t.getStartedAt() != null && t.getCompletedAt() != null) {
        double minutes = Duration.between(t.getStartedAt(), t.getCompletedAt()).toMinutes();
        Room room = roomRepo.findByRoomNumber(t.getRoomNumber());
        if (room != null) {
          switch (room.getRoomType()) {
            case LUXURY:
              luxCount++;
              luxTotalMin += minutes;
              break;
            case SUITE:
              suiteCount++;
              suiteTotalMin += minutes;
              break;
            default:
              stdCount++;
              stdTotalMin += minutes;
              break;
          }
        }
      }

      // --- Staff productivity: COMPLETED tasks grouped by the assigned staff's shift ---
      if (t.getStatus() == HousekeepingTask.Status.COMPLETED && t.getAssignedStaffId() != null) {
        HousekeepingStaff staff = staffRepo.findById(t.getAssignedStaffId());
        if (staff != null) {
          switch (staff.getShift()) {
            case MORNING:
              morningCount++;
              break;
            case AFTERNOON:
              afternoonCount++;
              break;
            case NIGHT:
              nightCount++;
              break;
          }
        }
      }

      // --- Skipped rate ---
      if (t.getStatus() == HousekeepingTask.Status.SKIPPED) {
        skippedCount++;
      }

      // --- Overdue rate: age (createdAt -> completedAt, or now if still open) exceeds threshold
      // ---
      LocalDateTime effectiveEnd = t.getCompletedAt() != null ? t.getCompletedAt() : now;
      long ageMinutes = Duration.between(t.getCreatedAt(), effectiveEnd).toMinutes();
      if (ageMinutes > overdueThreshold) {
        overdueCount++;
      }

      // --- Maintenance flag frequency ---
      if (t.getTaskType() == HousekeepingTask.TaskType.MAINTENANCE_CHECK) {
        maintenanceCount++;
      }

      // --- Queue wait time before dequeued (assignedAt - createdAt) ---
      if (t.getAssignedAt() != null) {
        double waitMinutes = Duration.between(t.getCreatedAt(), t.getAssignedAt()).toMinutes();
        queueWaitSampleCount++;
        queueWaitTotalMin += waitMinutes;
      }
    }

    double skippedRate = total == 0 ? 0.0 : (skippedCount * 100.0) / total;
    double overdueRate = total == 0 ? 0.0 : (overdueCount * 100.0) / total;
    double maintenanceFrequency = total == 0 ? 0.0 : (maintenanceCount * 100.0) / total;

    return new ReportResult(
        total,
        luxCount,
        luxCount == 0 ? 0.0 : luxTotalMin / luxCount,
        suiteCount,
        suiteCount == 0 ? 0.0 : suiteTotalMin / suiteCount,
        stdCount,
        stdCount == 0 ? 0.0 : stdTotalMin / stdCount,
        morningCount,
        afternoonCount,
        nightCount,
        skippedCount,
        skippedRate,
        overdueCount,
        overdueRate,
        maintenanceCount,
        maintenanceFrequency,
        queueWaitSampleCount,
        queueWaitSampleCount == 0 ? 0.0 : queueWaitTotalMin / queueWaitSampleCount);
  }

  // ================= TXT EXPORT (delegates to the shared TxtExportUtil, same as frontdesk)
  // =================
  // TxtExportUtil handles timestamping, folder placement (exports/housekeeping/...), and the
  // actual file write. This method's only job is to build the report content as a String.
  private void exportReportToFile(
      String dateRangeLabel,
      String staffLabel,
      String roomTypeLabel,
      String taskTypeLabel,
      ReportResult r) {

    StringBuilder sb = new StringBuilder();
    sb.append("HOUSEKEEPING REPORT\n");
    sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
    sb.append("------------------------------------------------------\n");
    sb.append("Filters Applied:\n");
    sb.append("  Date Range : ").append(dateRangeLabel).append("\n");
    sb.append("  Staff      : ").append(staffLabel).append("\n");
    sb.append("  Room Type  : ").append(roomTypeLabel).append("\n");
    sb.append("  Task Type  : ").append(taskTypeLabel).append("\n");
    sb.append("  Matched Tasks: ").append(r.totalMatched).append("\n");
    sb.append("------------------------------------------------------\n\n");

    sb.append("1. Average Cleaning Time per Room Type\n");
    sb.append("   LUXURY   : ")
        .append(formatAvgForFile(r.cleanTimeAvgLuxury, r.cleanTimeCountLuxury))
        .append("\n");
    sb.append("   SUITE    : ")
        .append(formatAvgForFile(r.cleanTimeAvgSuite, r.cleanTimeCountSuite))
        .append("\n");
    sb.append("   STANDARD : ")
        .append(formatAvgForFile(r.cleanTimeAvgStandard, r.cleanTimeCountStandard))
        .append("\n\n");

    sb.append("2. Staff Productivity (Tasks Completed per Shift)\n");
    sb.append("   MORNING   : ").append(r.productivityMorning).append(" task(s)\n");
    sb.append("   AFTERNOON : ").append(r.productivityAfternoon).append(" task(s)\n");
    sb.append("   NIGHT     : ").append(r.productivityNight).append(" task(s)\n\n");

    sb.append("3. Overdue / Skipped Task Rate\n");
    sb.append("   Skipped : ")
        .append(r.skippedCount)
        .append(" / ")
        .append(r.totalMatched)
        .append(" (")
        .append(String.format("%.1f%%", r.skippedRatePercent))
        .append(")\n");
    sb.append("   Overdue : ")
        .append(r.overdueCount)
        .append(" / ")
        .append(r.totalMatched)
        .append(" (")
        .append(String.format("%.1f%%", r.overdueRatePercent))
        .append(")\n\n");

    sb.append("4. Maintenance Flag Frequency\n");
    sb.append("   ")
        .append(r.maintenanceCount)
        .append(" / ")
        .append(r.totalMatched)
        .append(" (")
        .append(String.format("%.1f%%", r.maintenanceFrequencyPercent))
        .append(")\n\n");

    sb.append("5. Average Queue Wait Time Before Dequeued\n");
    sb.append("   ").append(formatAvgForFile(r.avgQueueWaitMinutes, r.queueWaitCount)).append("\n");

    try {
      String path = TxtExportUtil.export("housekeeping/operations_report", sb.toString());
      reportView.printExportSuccess(path);
    } catch (RuntimeException e) {
      reportView.printExportFailure(e.getMessage());
    }
  }

  private String formatAvgForFile(double avgMinutes, int sampleCount) {
    if (sampleCount == 0) return "N/A (no data)";
    return String.format("%.1f min (n=%d)", avgMinutes, sampleCount);
  }

  // ================= STAFF PERFORMANCE REPORT =================

  // --- FILTER HUB LOOP ---
  private void startStaffPerformanceReport() {
    LocalDate startDate = null;
    LocalDate endDate = null;
    String datePresetLabel = "ALL TIME";
    HousekeepingStaff.Shift shiftFilter = null;

    while (true) {
      try {
        int choice =
            reportView.displayStaffFilterHub(
                dateRangeLabel(startDate, endDate),
                shiftFilter == null ? "ALL" : shiftFilter.name());

        if (choice == 1) {
          Object[] range = handleDateRangeSubmenu(startDate, endDate, datePresetLabel);
          startDate = (LocalDate) range[0];
          endDate = (LocalDate) range[1];
          datePresetLabel = (String) range[2];
        } else if (choice == 2) {
          shiftFilter = handleShiftFilterSubmenu(shiftFilter);
        } else if (choice == 3) {
          startDate = null;
          endDate = null;
          datePresetLabel = "ALL TIME";
          shiftFilter = null;
        } else if (choice == 4) {
          generateAndShowStaffPerformanceReport(startDate, endDate, shiftFilter);
        } else if (choice == 5) {
          return; // Cancel, back to Report Type Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- SHIFT FILTER SUBMENU ---
  private HousekeepingStaff.Shift handleShiftFilterSubmenu(HousekeepingStaff.Shift current) {
    while (true) {
      try {
        int choice = reportView.displayShiftFilterSubmenu(current == null ? "ALL" : current.name());
        if (choice == 1) return HousekeepingStaff.Shift.MORNING;
        if (choice == 2) return HousekeepingStaff.Shift.AFTERNOON;
        if (choice == 3) return HousekeepingStaff.Shift.NIGHT;
        if (choice == 4) return null;
        if (choice == 5) return current;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- CALCULATION ENGINE + REPORT OUTPUT ---
  private void generateAndShowStaffPerformanceReport(
      LocalDate startDate, LocalDate endDate, HousekeepingStaff.Shift shiftFilter) {

    ListInterface<StaffPerformanceRowDTO> rows =
        calculateStaffPerformance(startDate, endDate, shiftFilter);

    int choice =
        reportView.displayStaffPerformanceReport(
            dateRangeLabel(startDate, endDate),
            shiftFilter == null ? "ALL" : shiftFilter.name(),
            rows);

    if (choice == 1) {
      exportStaffPerformanceReportToFile(
          dateRangeLabel(startDate, endDate),
          shiftFilter == null ? "ALL" : shiftFilter.name(),
          rows);
    }
    // choice == 2 (or export done): falls through, back to filter hub
  }

  // Builds one fully-computed row per matching staff member — no lookups left for the view to
  // do. Uses filter()/map() instead of manual index loops, per team convention.
  private ListInterface<StaffPerformanceRowDTO> calculateStaffPerformance(
      LocalDate startDate, LocalDate endDate, HousekeepingStaff.Shift shiftFilter) {

    ListInterface<HousekeepingTask> allTasks = taskRepo.getTaskList();

    ListInterface<HousekeepingStaff> staffToInclude =
        staffRepo.getStaffList().filter(s -> shiftFilter == null || s.getShift() == shiftFilter);

    return staffToInclude.map(
        staff -> {
          ListInterface<HousekeepingTask> staffTasks =
              allTasks.filter(
                  t ->
                      t != null
                          && t.getCreatedAt() != null
                          && staff.getStaffId().equals(t.getAssignedStaffId())
                          && (startDate == null
                              || !t.getCreatedAt().toLocalDate().isBefore(startDate))
                          && (endDate == null || !t.getCreatedAt().toLocalDate().isAfter(endDate)));

          int completedCount =
              staffTasks
                  .filter(t -> t.getStatus() == HousekeepingTask.Status.COMPLETED)
                  .getNumberOfEntries();
          int skippedCount =
              staffTasks
                  .filter(t -> t.getStatus() == HousekeepingTask.Status.SKIPPED)
                  .getNumberOfEntries();

          ListInterface<HousekeepingTask> timedCleaningTasks =
              staffTasks.filter(
                  t ->
                      (t.getTaskType() == HousekeepingTask.TaskType.STANDARD_CLEAN
                              || t.getTaskType() == HousekeepingTask.TaskType.DEEP_CLEAN)
                          && t.getStartedAt() != null
                          && t.getCompletedAt() != null);

          int timedCount = timedCleaningTasks.getNumberOfEntries();
          double totalMinutes =
              timedCleaningTasks.reduce(
                  0.0,
                  (sum, t) ->
                      sum + Duration.between(t.getStartedAt(), t.getCompletedAt()).toMinutes());

          return new StaffPerformanceRowDTO(
              staff.getStaffId(),
              staff.getName(),
              staff.getShift().name(),
              staff.getAvailability().name(),
              completedCount,
              skippedCount,
              formatAvgForFile(timedCount == 0 ? 0.0 : totalMinutes / timedCount, timedCount));
        });
  }

  // ================= TXT EXPORT (delegates to the shared TxtExportUtil, same as frontdesk)
  // =================
  private void exportStaffPerformanceReportToFile(
      String dateRangeLabel, String shiftLabel, ListInterface<StaffPerformanceRowDTO> rows) {

    StringBuilder sb = new StringBuilder();
    sb.append("STAFF PERFORMANCE REPORT\n");
    sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
    sb.append("------------------------------------------------------\n");
    sb.append("Filters Applied:\n");
    sb.append("  Date Range   : ").append(dateRangeLabel).append("\n");
    sb.append("  Shift        : ").append(shiftLabel).append("\n");
    sb.append("  Matched Staff: ").append(rows.getNumberOfEntries()).append("\n");
    sb.append("------------------------------------------------------\n\n");

    if (rows.isEmpty()) {
      sb.append("No staff match the selected filters.\n");
    } else {
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        StaffPerformanceRowDTO row = rows.getEntry(i);
        sb.append(i)
            .append(". ")
            .append(row.staffId)
            .append(" - ")
            .append(row.name)
            .append(" (")
            .append(row.shiftLabel)
            .append(", ")
            .append(row.availabilityLabel)
            .append(")\n");
        sb.append("   Completed: ")
            .append(row.completedCount)
            .append(" | Skipped: ")
            .append(row.skippedCount)
            .append(" | Avg Cleaning Time: ")
            .append(row.avgCleanTimeLabel)
            .append("\n\n");
      }
    }

    try {
      String path = TxtExportUtil.export("housekeeping/staff_performance_report", sb.toString());
      reportView.printExportSuccess(path);
    } catch (RuntimeException e) {
      reportView.printExportFailure(e.getMessage());
    }
  }
}
