package view.housekeeping;

import adt.ListInterface;
import util.ConsoleUtil;
import util.TableUtil;

public class HousekeepingReportView {

  // --- REPORT TYPE SELECTOR ---

  public int displayReportTypeMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("HOUSEKEEPING REPORTS");
    System.out.println(" 1. Operations Report (cleaning time, productivity, overdue/skipped,");
    System.out.println("    maintenance flags, queue wait)");
    System.out.println(" 2. Staff Performance Report (per-staff completed/skipped tasks &");
    System.out.println("    avg cleaning time)");
    System.out.println(" 3. Back to Housekeeping Main Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  // --- FILTER HUB ---

  public int displayFilterHub(
      String dateRangeLabel, String staffLabel, String roomTypeLabel, String taskTypeLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GENERATE OPERATIONS REPORT");
    System.out.println(" Configure filters below, then generate the report. Nothing is");
    System.out.println(" calculated until you choose 'Generate Report'.\n");
    System.out.println(" Date Range : [ " + dateRangeLabel + " ]");
    System.out.println(" Staff      : [ " + staffLabel + " ]");
    System.out.println(" Room Type  : [ " + roomTypeLabel + " ]");
    System.out.println(" Task Type  : [ " + taskTypeLabel + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Set Date Range");
    System.out.println(" 2. Set Staff Filter");
    System.out.println(" 3. Set Room Type Filter");
    System.out.println(" 4. Set Task Type Filter");
    System.out.println(" 5. Reset / Clear All Filters");
    System.out.println(" 6. Generate Report");
    System.out.println(" 7. Cancel and Return to Report Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
  }

  // --- DATE RANGE SUBMENU ---

  public int displayDateRangeSubmenu(String startLabel, String endLabel, String presetLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET DATE RANGE");
    System.out.println(" Preset     : [ " + presetLabel + " ]");
    System.out.println(" Start Date : [ " + startLabel + " ]");
    System.out.println(" End Date   : [ " + endLabel + " ]\n");
    System.out.println(" 1. Today");
    System.out.println(" 2. Yesterday");
    System.out.println(" 3. Last 7 Days");
    System.out.println(" 4. Last 30 Days");
    System.out.println(" 5. Custom Date Range");
    System.out.println(" 6. Clear Date Range (All Time)");
    System.out.println(" 7. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 7).getAsInt();
  }

  public int displayCustomDateRangeSubmenu(String startLabel, String endLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET CUSTOM DATE RANGE");
    System.out.println(" Start Date : [ " + startLabel + " ]");
    System.out.println(" End Date   : [ " + endLabel + " ]\n");
    System.out.println(" 1. Set Start Date");
    System.out.println(" 2. Set End Date");
    System.out.println(" 3. Back to Date Range Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public String promptDateInput(String label) {
    System.out.println("\n [Format: YYYY-MM-DD. Leave blank or type 'C' to Cancel]");
    return ConsoleUtil.getStringInput("Enter " + label + ": ");
  }

  // --- STAFF FILTER SUBMENU ---

  public String promptStaffFilterInput(String currentLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET STAFF FILTER");
    System.out.println(" Current Staff Filter: [ " + currentLabel + " ]");
    System.out.println("\n [Leave blank or type 'C' to Clear/Cancel]");
    return ConsoleUtil.getStringInput("Enter Staff ID: ");
  }

  // --- ROOM TYPE FILTER SUBMENU ---

  public int displayRoomTypeFilterSubmenu(String currentLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET ROOM TYPE FILTER");
    System.out.println(" Current Room Type Filter: [ " + currentLabel + " ]\n");
    System.out.println(" 1. LUXURY");
    System.out.println(" 2. SUITE");
    System.out.println(" 3. STANDARD");
    System.out.println(" 4. Clear Filter (Show All)");
    System.out.println(" 5. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // --- TASK TYPE FILTER SUBMENU ---

  public int displayTaskTypeFilterSubmenu(String currentLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET TASK TYPE FILTER");
    System.out.println(" Current Task Type Filter: [ " + currentLabel + " ]\n");
    System.out.println(" 1. STANDARD_CLEAN");
    System.out.println(" 2. DEEP_CLEAN");
    System.out.println(" 3. MAINTENANCE_CHECK");
    System.out.println(" 4. Clear Filter (Show All)");
    System.out.println(" 5. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // ================= STAFF PERFORMANCE REPORT =================

  // --- FILTER HUB ---

  public int displayStaffFilterHub(String dateRangeLabel, String shiftLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("GENERATE STAFF PERFORMANCE REPORT");
    System.out.println(" Configure filters below, then generate the report. Nothing is");
    System.out.println(" calculated until you choose 'Generate Report'.\n");
    System.out.println(" Date Range : [ " + dateRangeLabel + " ]");
    System.out.println(" Shift      : [ " + shiftLabel + " ]");
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Set Date Range");
    System.out.println(" 2. Set Shift Filter");
    System.out.println(" 3. Reset / Clear All Filters");
    System.out.println(" 4. Generate Report");
    System.out.println(" 5. Cancel and Return to Report Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // --- SHIFT FILTER SUBMENU ---

  public int displayShiftFilterSubmenu(String currentLabel) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SET SHIFT FILTER");
    System.out.println(" Current Shift Filter: [ " + currentLabel + " ]\n");
    System.out.println(" 1. MORNING");
    System.out.println(" 2. AFTERNOON");
    System.out.println(" 3. NIGHT");
    System.out.println(" 4. Clear Filter (Show All)");
    System.out.println(" 5. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5).getAsInt();
  }

  // --- REPORT OUTPUT ---

  // Fully computed by the controller — the view only formats/prints it, never resolves or
  // looks anything up on its own (matches the "no data processing in view" rule).
  public static class StaffPerformanceRowDTO {
    public final String staffId;
    public final String name;
    public final String shiftLabel;
    public final String availabilityLabel;
    public final int completedCount;
    public final int skippedCount;
    public final String avgCleanTimeLabel;

    public StaffPerformanceRowDTO(
        String staffId,
        String name,
        String shiftLabel,
        String availabilityLabel,
        int completedCount,
        int skippedCount,
        String avgCleanTimeLabel) {
      this.staffId = staffId;
      this.name = name;
      this.shiftLabel = shiftLabel;
      this.availabilityLabel = availabilityLabel;
      this.completedCount = completedCount;
      this.skippedCount = skippedCount;
      this.avgCleanTimeLabel = avgCleanTimeLabel;
    }
  }

  public int displayStaffPerformanceReport(
      String dateRangeLabel, String shiftLabel, ListInterface<StaffPerformanceRowDTO> rows) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("STAFF PERFORMANCE REPORT");
    System.out.println(" Filters Applied:");
    System.out.println("   Date Range : " + dateRangeLabel);
    System.out.println("   Shift      : " + shiftLabel);
    System.out.println("   Matched Staff: " + rows.getNumberOfEntries());
    System.out.println("------------------------------------------------------");

    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(new int[] {5, 9, 17, 11, 12, 11, 9, 18})
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "NO.", "STAFF ID", "NAME", "SHIFT", "AVAILABILITY", "COMPLETED", "SKIPPED", "AVG TIME"
        },
        settings);

    if (rows.isEmpty()) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {113}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(new String[] {"No staff match the selected filters."}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);
      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        StaffPerformanceRowDTO row = rows.getEntry(i);
        TableUtil.printTableRow(
            new String[] {
              String.valueOf(i),
              row.staffId,
              row.name,
              row.shiftLabel,
              row.availabilityLabel,
              String.valueOf(row.completedCount),
              String.valueOf(row.skippedCount),
              row.avgCleanTimeLabel
            },
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.println();
    System.out.println(" 1. Export Report to Text File");
    System.out.println(" 2. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt();
  }

  public static class ReportResult {
    public final int totalMatched;
    public final int cleanTimeCountLuxury;
    public final double cleanTimeAvgLuxury;
    public final int cleanTimeCountSuite;
    public final double cleanTimeAvgSuite;
    public final int cleanTimeCountStandard;
    public final double cleanTimeAvgStandard;
    public final int productivityMorning;
    public final int productivityAfternoon;
    public final int productivityNight;
    public final int skippedCount;
    public final double skippedRatePercent;
    public final int overdueCount;
    public final double overdueRatePercent;
    public final int maintenanceCount;
    public final double maintenanceFrequencyPercent;
    public final int queueWaitCount;
    public final double avgQueueWaitMinutes;

    public ReportResult(
        int totalMatched,
        int cleanTimeCountLuxury,
        double cleanTimeAvgLuxury,
        int cleanTimeCountSuite,
        double cleanTimeAvgSuite,
        int cleanTimeCountStandard,
        double cleanTimeAvgStandard,
        int productivityMorning,
        int productivityAfternoon,
        int productivityNight,
        int skippedCount,
        double skippedRatePercent,
        int overdueCount,
        double overdueRatePercent,
        int maintenanceCount,
        double maintenanceFrequencyPercent,
        int queueWaitCount,
        double avgQueueWaitMinutes) {
      this.totalMatched = totalMatched;
      this.cleanTimeCountLuxury = cleanTimeCountLuxury;
      this.cleanTimeAvgLuxury = cleanTimeAvgLuxury;
      this.cleanTimeCountSuite = cleanTimeCountSuite;
      this.cleanTimeAvgSuite = cleanTimeAvgSuite;
      this.cleanTimeCountStandard = cleanTimeCountStandard;
      this.cleanTimeAvgStandard = cleanTimeAvgStandard;
      this.productivityMorning = productivityMorning;
      this.productivityAfternoon = productivityAfternoon;
      this.productivityNight = productivityNight;
      this.skippedCount = skippedCount;
      this.skippedRatePercent = skippedRatePercent;
      this.overdueCount = overdueCount;
      this.overdueRatePercent = overdueRatePercent;
      this.maintenanceCount = maintenanceCount;
      this.maintenanceFrequencyPercent = maintenanceFrequencyPercent;
      this.queueWaitCount = queueWaitCount;
      this.avgQueueWaitMinutes = avgQueueWaitMinutes;
    }
  }

  public int displayReportOutput(
      String dateRangeLabel,
      String staffLabel,
      String roomTypeLabel,
      String taskTypeLabel,
      ReportResult r) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("HOUSEKEEPING REPORT");
    System.out.println(" Filters Applied:");
    System.out.println("   Date Range : " + dateRangeLabel);
    System.out.println("   Staff      : " + staffLabel);
    System.out.println("   Room Type  : " + roomTypeLabel);
    System.out.println("   Task Type  : " + taskTypeLabel);
    System.out.println("   Matched Tasks: " + r.totalMatched);
    System.out.println("------------------------------------------------------");

    System.out.println(" 1. Average Cleaning Time per Room Type");
    System.out.println("    LUXURY   : " + formatAvg(r.cleanTimeAvgLuxury, r.cleanTimeCountLuxury));
    System.out.println("    SUITE    : " + formatAvg(r.cleanTimeAvgSuite, r.cleanTimeCountSuite));
    System.out.println(
        "    STANDARD : " + formatAvg(r.cleanTimeAvgStandard, r.cleanTimeCountStandard));
    System.out.println();

    System.out.println(" 2. Staff Productivity (Tasks Completed per Shift)");
    System.out.println("    MORNING   : " + r.productivityMorning + " task(s)");
    System.out.println("    AFTERNOON : " + r.productivityAfternoon + " task(s)");
    System.out.println("    NIGHT     : " + r.productivityNight + " task(s)");
    System.out.println();

    System.out.println(" 3. Overdue / Skipped Task Rate");
    System.out.println(
        "    Skipped : "
            + r.skippedCount
            + " / "
            + r.totalMatched
            + " ("
            + formatPercent(r.skippedRatePercent)
            + ")");
    System.out.println(
        "    Overdue : "
            + r.overdueCount
            + " / "
            + r.totalMatched
            + " ("
            + formatPercent(r.overdueRatePercent)
            + ")");
    System.out.println();

    System.out.println(" 4. Maintenance Flag Frequency");
    System.out.println(
        "    "
            + r.maintenanceCount
            + " / "
            + r.totalMatched
            + " ("
            + formatPercent(r.maintenanceFrequencyPercent)
            + ")");
    System.out.println();

    System.out.println(" 5. Average Queue Wait Time Before Dequeued");
    System.out.println("    " + formatAvg(r.avgQueueWaitMinutes, r.queueWaitCount));
    System.out.println("------------------------------------------------------");
    System.out.println(" 1. Export Report to Text File");
    System.out.println(" 2. Back to Filter Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 2).getAsInt();
  }

  private String formatAvg(double avgMinutes, int sampleCount) {
    if (sampleCount == 0) return "N/A (no data)";
    return String.format("%.1f min (n=%d)", avgMinutes, sampleCount);
  }

  private String formatPercent(double percent) {
    return String.format("%.1f%%", percent);
  }

  public void printExportSuccess(String filePath) {
    ConsoleUtil.clearScreen();
    System.out.println(" >> STATUS: [\u2713] SUCCESS");
    System.out.println(" Report exported to: " + filePath + "\n");
    ConsoleUtil.printContinueMessage();
  }

  public void printExportFailure(String reason) {
    ConsoleUtil.printError("Failed to export report: " + reason);
  }
}
