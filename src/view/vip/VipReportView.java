package view.vip;

import adt.ListInterface;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TableUtil;

public class VipReportView {

  public GetMenuInputResult displayReportHubMenu() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("VIP ANALYTICS REPORT HUB");
    System.out.println("Select an operational performance report to generate:\n");
    System.out.println("1. Wait Time Efficiency & SLA Attainment Report");
    System.out.println("   Analyzes physical wait durations vs. tier SLA targets.\n");
    System.out.println("2. VIP Penalty & Eviction Audit Report");
    System.out.println("   Analyzes no-show strike counts & max-strike eviction lockouts.\n");
    System.out.println("3. Room Holding Bay & Grace Window Report");
    System.out.println("   Analyzes room hold times & grace countdown utilization.\n");
    System.out.println("4. Back to VIP Menu\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4);
  }

  public GetMenuInputResult displayFilterControlPanel(
      String reportTitle,
      String search,
      String tier,
      String roomType,
      String boiling,
      String datePreset,
      String sortAttr,
      String sortDir,
      int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT GENERATION CONFIGURATION", 88);
    System.out.println("CURRENT ACTIVE PARAMETERS:");
    System.out.println(
        "  - Guest Search Query : [ "
            + (search == null ? "NONE (All)" : "\"" + search + "\"")
            + " ]");
    System.out.println("  - Membership Tier    : [ " + (tier == null ? "ALL TIERS" : tier) + " ]");
    System.out.println(
        "  - Room Queue Type    : [ " + (roomType == null ? "ALL ROOM TYPES" : roomType) + " ]");
    System.out.println(
        "  - Boiling Status     : [ " + (boiling == null ? "ALL STATES" : boiling) + " ]");
    System.out.println(
        "  - Date Range Filter  : [ " + (datePreset == null ? "ALL TIME" : datePreset) + " ]");
    System.out.println("  - Sort Attribute     : [ " + sortAttr + " ]");
    System.out.println("  - Sort Direction     : [ " + sortDir + " ]");
    System.out.println(
        "  - Max Display Records: [ "
            + (recordLimit == 0 ? "SHOW ALL (Unlimited)" : "Top " + recordLimit + " Records")
            + " ]\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("1. Generate Report");
    System.out.println("2. Edit Filters");
    System.out.println("3. Sort Options");
    System.out.println("4. Max Display Records");
    System.out.println("5. Reset All Options");
    System.out.println("6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6);
  }

  public GetMenuInputResult displayEditFiltersSubmenu(
      String search, String tier, String roomType, String boiling, String datePreset) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("EDIT FILTERS", 88);
    System.out.println(
        "  - Guest Search Query : [ "
            + (search == null ? "NONE (All)" : "\"" + search + "\"")
            + " ]");
    System.out.println("  - Membership Tier    : [ " + (tier == null ? "ALL TIERS" : tier) + " ]");
    System.out.println(
        "  - Room Queue Type    : [ " + (roomType == null ? "ALL ROOM TYPES" : roomType) + " ]");
    System.out.println(
        "  - Boiling Status     : [ " + (boiling == null ? "ALL STATES" : boiling) + " ]");
    System.out.println(
        "  - Date Range Filter  : [ " + (datePreset == null ? "ALL TIME" : datePreset) + " ]\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("1. Membership Tier");
    System.out.println("2. Boiling Status");
    System.out.println("3. Room Queue Type");
    System.out.println("4. Date Range Filter");
    System.out.println("5. Search Query");
    System.out.println("6. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6);
  }

  public GetMenuInputResult displayDateFilterSubmenu(String currentPreset) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("DATE RANGE FILTER OPTIONS", 88);
    System.out.println(
        "Active Preset: [ " + (currentPreset == null ? "ALL TIME" : currentPreset) + " ]\n");
    System.out.println("1. Today (00:00 - 23:59)");
    System.out.println("2. Yesterday");
    System.out.println("3. Last 7 Days");
    System.out.println("4. Last 30 Days");
    System.out.println("5. Custom Date Range (YYYY-MM-DD to YYYY-MM-DD)");
    System.out.println("6. Custom Date & Time Range (YYYY-MM-DD HH:mm to YYYY-MM-DD HH:mm)");
    System.out.println("7. All Time (No Date Filter)");
    System.out.println("8. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 8);
  }

  public GetMenuInputResult displaySortOptionsSubmenu(String sortAttr, String sortDir) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SORT OPTIONS", 88);
    System.out.println("  - Sort Attribute     : [ " + sortAttr + " ]");
    System.out.println("  - Sort Direction     : [ " + sortDir + " ]\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("1. Sort Attribute");
    System.out.println("2. Sort Direction");
    System.out.println("3. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3);
  }

  public GetMenuInputResult displayRecordLimitSubmenu(int currentLimit) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT MAX DISPLAY RECORDS");
    System.out.println(
        "Current Selected: [ " + (currentLimit == 0 ? "SHOW ALL" : "Top " + currentLimit) + " ]\n");
    System.out.println("1. Top 10 Records");
    System.out.println("2. Top 20 Records");
    System.out.println("3. Top 50 Records");
    System.out.println("4. Enter Custom Max Limit");
    System.out.println("5. Show All Records (Unlimited)");
    System.out.println("6. Keep Current Limit & Return\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 6);
  }

  public Integer promptCustomRecordLimit() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("ENTER CUSTOM DISPLAY LIMIT");
    System.out.println("Enter the exact number of top records to display on screen [1 - 500].");
    System.out.println("Press ENTER or 'C' to keep current limit.\n");
    return ConsoleUtil.getIntegerInput("[ Custom Limit ]: ", 1, 500);
  }

  public String promptSearchInput() {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SEARCH QUERY FILTER");
    System.out.println("Enter Guest Name, Reservation ID, or Phone Number match.");
    System.out.println("Press ENTER or type 'C' to clear search filter.\n");
    return ConsoleUtil.getStringInput("[ Search Query ]: ");
  }

  public GetMenuInputResult displayTierFilterSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT TIER FILTER");
    System.out.println("Current Selected: [ " + (current == null ? "ALL TIERS" : current) + " ]\n");
    System.out.println("1. DIAMOND ONLY");
    System.out.println("2. GOLD ONLY");
    System.out.println("3. SILVER ONLY");
    System.out.println("4. ALL TIERS");
    System.out.println("5. Keep Current Tier & Return\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5);
  }

  public GetMenuInputResult displayRoomTypeFilterSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT ROOM QUEUE FILTER");
    System.out.println(
        "Current Selected: [ " + (current == null ? "ALL ROOM TYPES" : current) + " ]\n");
    System.out.println("1. LUXURY ROOMS");
    System.out.println("2. SUITE ROOMS");
    System.out.println("3. STANDARD ROOMS");
    System.out.println("4. ALL ROOM TYPES");
    System.out.println("5. Keep Current Queue & Return\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5);
  }

  public GetMenuInputResult displayBoilingFilterSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT BOILING STATUS FILTER");
    System.out.println(
        "Current Selected: [ " + (current == null ? "ALL STATES" : current) + " ]\n");
    System.out.println("1. BOILING ONLY [!]");
    System.out.println("2. NORMAL ONLY [ ]");
    System.out.println("3. ALL STATES");
    System.out.println("4. Keep Current Status & Return\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 4);
  }

  public GetMenuInputResult displaySortAttrSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT PRIMARY SORT ATTRIBUTE");
    System.out.println("Current Selected: [ " + current + " ]\n");
    System.out.println("1. PHYSICAL WAIT TIME");
    System.out.println("2. PRIORITY SCORE");
    System.out.println("3. STRIKE COUNT");
    System.out.println("4. GUEST NAME");
    System.out.println("5. Keep Current Sort & Return\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 5);
  }

  public GetMenuInputResult displaySortDirSubmenu(String current) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("SELECT VECTOR DIRECTION");
    System.out.println("Current Selected: [ " + current + " ]\n");
    System.out.println("1. DESCENDING (High -> Low)");
    System.out.println("2. ASCENDING  (Low -> High)");
    System.out.println("3. Keep Current Direction & Return\n");
    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3);
  }

  // ==========================================
  // VIEW MODEL DTO CLASSES (STATIC INNER)
  // ==========================================

  public static class SlaReportRowDTO {
    private final String rank;
    private final String reservationId;
    private final String guestName;
    private final String tier;
    private final String roomType;
    private final long waitMins;
    private final int strikes;
    private final String status;

    public SlaReportRowDTO(
        String rank,
        String reservationId,
        String guestName,
        String tier,
        String roomType,
        long waitMins,
        int strikes,
        String status) {
      this.rank = rank;
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.tier = tier;
      this.roomType = roomType;
      this.waitMins = waitMins;
      this.strikes = strikes;
      this.status = status;
    }

    public String getRank() {
      return rank;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getTier() {
      return tier;
    }

    public String getRoomType() {
      return roomType;
    }

    public long getWaitMins() {
      return waitMins;
    }

    public int getStrikes() {
      return strikes;
    }

    public String getStatus() {
      return status;
    }
  }

  public static class SlaReportSummaryDTO {
    private final int diamondTotal, diamondSlaMet, diamondLimitMins;
    private final double diamondSlaPct, diamondSlaTargetPct;
    private final int goldTotal, goldSlaMet, goldLimitMins;
    private final double goldSlaPct, goldSlaTargetPct;
    private final int silverTotal, silverSlaMet, silverLimitMins;
    private final double silverSlaPct, silverSlaTargetPct;

    public SlaReportSummaryDTO(
        int diamondTotal,
        int diamondSlaMet,
        int diamondLimitMins,
        double diamondSlaPct,
        double diamondSlaTargetPct,
        int goldTotal,
        int goldSlaMet,
        int goldLimitMins,
        double goldSlaPct,
        double goldSlaTargetPct,
        int silverTotal,
        int silverSlaMet,
        int silverLimitMins,
        double silverSlaPct,
        double silverSlaTargetPct) {
      this.diamondTotal = diamondTotal;
      this.diamondSlaMet = diamondSlaMet;
      this.diamondLimitMins = diamondLimitMins;
      this.diamondSlaPct = diamondSlaPct;
      this.diamondSlaTargetPct = diamondSlaTargetPct;
      this.goldTotal = goldTotal;
      this.goldSlaMet = goldSlaMet;
      this.goldLimitMins = goldLimitMins;
      this.goldSlaPct = goldSlaPct;
      this.goldSlaTargetPct = goldSlaTargetPct;
      this.silverTotal = silverTotal;
      this.silverSlaMet = silverSlaMet;
      this.silverLimitMins = silverLimitMins;
      this.silverSlaPct = silverSlaPct;
      this.silverSlaTargetPct = silverSlaTargetPct;
    }

    public int getDiamondTotal() {
      return diamondTotal;
    }

    public int getDiamondSlaMet() {
      return diamondSlaMet;
    }

    public int getDiamondLimitMins() {
      return diamondLimitMins;
    }

    public double getDiamondSlaPct() {
      return diamondSlaPct;
    }

    public double getDiamondSlaTargetPct() {
      return diamondSlaTargetPct;
    }

    public int getGoldTotal() {
      return goldTotal;
    }

    public int getGoldSlaMet() {
      return goldSlaMet;
    }

    public int getGoldLimitMins() {
      return goldLimitMins;
    }

    public double getGoldSlaPct() {
      return goldSlaPct;
    }

    public double getGoldSlaTargetPct() {
      return goldSlaTargetPct;
    }

    public int getSilverTotal() {
      return silverTotal;
    }

    public int getSilverSlaMet() {
      return silverSlaMet;
    }

    public int getSilverLimitMins() {
      return silverLimitMins;
    }

    public double getSilverSlaPct() {
      return silverSlaPct;
    }

    public double getSilverSlaTargetPct() {
      return silverSlaTargetPct;
    }
  }

  public static class SlaReportDTO {
    private final ListInterface<SlaReportRowDTO> rows;
    private final SlaReportSummaryDTO summary;
    private final int totalMatches;

    public SlaReportDTO(
        ListInterface<SlaReportRowDTO> rows, SlaReportSummaryDTO summary, int totalMatches) {
      this.rows = rows;
      this.summary = summary;
      this.totalMatches = totalMatches;
    }

    public ListInterface<SlaReportRowDTO> getRows() {
      return rows;
    }

    public SlaReportSummaryDTO getSummary() {
      return summary;
    }

    public int getTotalMatches() {
      return totalMatches;
    }
  }

  public static class PenaltyReportRowDTO {
    private final String rank;
    private final String reservationId;
    private final String guestName;
    private final String tier;
    private final int strikes;
    private final String boiling;
    private final String status;
    private final String evicted;

    public PenaltyReportRowDTO(
        String rank,
        String reservationId,
        String guestName,
        String tier,
        int strikes,
        String boiling,
        String status,
        String evicted) {
      this.rank = rank;
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.tier = tier;
      this.strikes = strikes;
      this.boiling = boiling;
      this.status = status;
      this.evicted = evicted;
    }

    public String getRank() {
      return rank;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getTier() {
      return tier;
    }

    public int getStrikes() {
      return strikes;
    }

    public String getBoiling() {
      return boiling;
    }

    public String getStatus() {
      return status;
    }

    public String getEvicted() {
      return evicted;
    }
  }

  public static class PenaltyReportSummaryDTO {
    private final int totalStrikes;
    private final int diamondTotal, diamondEvicted;
    private final double diamondRate, diamondMaxTarget;
    private final int goldTotal, goldEvicted;
    private final double goldRate, goldMaxTarget;
    private final int silverTotal, silverEvicted;
    private final double silverRate, silverMaxTarget;

    public PenaltyReportSummaryDTO(
        int totalStrikes,
        int diamondTotal,
        int diamondEvicted,
        double diamondRate,
        double diamondMaxTarget,
        int goldTotal,
        int goldEvicted,
        double goldRate,
        double goldMaxTarget,
        int silverTotal,
        int silverEvicted,
        double silverRate,
        double silverMaxTarget) {
      this.totalStrikes = totalStrikes;
      this.diamondTotal = diamondTotal;
      this.diamondEvicted = diamondEvicted;
      this.diamondRate = diamondRate;
      this.diamondMaxTarget = diamondMaxTarget;
      this.goldTotal = goldTotal;
      this.goldEvicted = goldEvicted;
      this.goldRate = goldRate;
      this.goldMaxTarget = goldMaxTarget;
      this.silverTotal = silverTotal;
      this.silverEvicted = silverEvicted;
      this.silverRate = silverRate;
      this.silverMaxTarget = silverMaxTarget;
    }

    public int getTotalStrikes() {
      return totalStrikes;
    }

    public int getDiamondTotal() {
      return diamondTotal;
    }

    public int getDiamondEvicted() {
      return diamondEvicted;
    }

    public double getDiamondRate() {
      return diamondRate;
    }

    public double getDiamondMaxTarget() {
      return diamondMaxTarget;
    }

    public int getGoldTotal() {
      return goldTotal;
    }

    public int getGoldEvicted() {
      return goldEvicted;
    }

    public double getGoldRate() {
      return goldRate;
    }

    public double getGoldMaxTarget() {
      return goldMaxTarget;
    }

    public int getSilverTotal() {
      return silverTotal;
    }

    public int getSilverEvicted() {
      return silverEvicted;
    }

    public double getSilverRate() {
      return silverRate;
    }

    public double getSilverMaxTarget() {
      return silverMaxTarget;
    }
  }

  public static class PenaltyReportDTO {
    private final ListInterface<PenaltyReportRowDTO> rows;
    private final PenaltyReportSummaryDTO summary;
    private final int totalMatches;

    public PenaltyReportDTO(
        ListInterface<PenaltyReportRowDTO> rows,
        PenaltyReportSummaryDTO summary,
        int totalMatches) {
      this.rows = rows;
      this.summary = summary;
      this.totalMatches = totalMatches;
    }

    public ListInterface<PenaltyReportRowDTO> getRows() {
      return rows;
    }

    public PenaltyReportSummaryDTO getSummary() {
      return summary;
    }

    public int getTotalMatches() {
      return totalMatches;
    }
  }

  public static class HoldingReportRowDTO {
    private final String rank;
    private final String reservationId;
    private final String guestName;
    private final String tier;
    private final int allowedGraceMins;
    private final String timeUsedStr;
    private final String holdStatus;
    private final String graceUsedPctStr;

    public HoldingReportRowDTO(
        String rank,
        String reservationId,
        String guestName,
        String tier,
        int allowedGraceMins,
        String timeUsedStr,
        String holdStatus,
        String graceUsedPctStr) {
      this.rank = rank;
      this.reservationId = reservationId;
      this.guestName = guestName;
      this.tier = tier;
      this.allowedGraceMins = allowedGraceMins;
      this.timeUsedStr = timeUsedStr;
      this.holdStatus = holdStatus;
      this.graceUsedPctStr = graceUsedPctStr;
    }

    public String getRank() {
      return rank;
    }

    public String getReservationId() {
      return reservationId;
    }

    public String getGuestName() {
      return guestName;
    }

    public String getTier() {
      return tier;
    }

    public int getAllowedGraceMins() {
      return allowedGraceMins;
    }

    public String getTimeUsedStr() {
      return timeUsedStr;
    }

    public String getHoldStatus() {
      return holdStatus;
    }

    public String getGraceUsedPctStr() {
      return graceUsedPctStr;
    }
  }

  public static class HoldingReportSummaryDTO {
    private final int totalHeld;
    private final double diamondGraceUtilTarget;
    private final double goldGraceUtilTarget;
    private final double silverGraceUtilTarget;

    public HoldingReportSummaryDTO(
        int totalHeld,
        double diamondGraceUtilTarget,
        double goldGraceUtilTarget,
        double silverGraceUtilTarget) {
      this.totalHeld = totalHeld;
      this.diamondGraceUtilTarget = diamondGraceUtilTarget;
      this.goldGraceUtilTarget = goldGraceUtilTarget;
      this.silverGraceUtilTarget = silverGraceUtilTarget;
    }

    public int getTotalHeld() {
      return totalHeld;
    }

    public double getDiamondGraceUtilTarget() {
      return diamondGraceUtilTarget;
    }

    public double getGoldGraceUtilTarget() {
      return goldGraceUtilTarget;
    }

    public double getSilverGraceUtilTarget() {
      return silverGraceUtilTarget;
    }
  }

  public static class HoldingReportDTO {
    private final ListInterface<HoldingReportRowDTO> rows;
    private final HoldingReportSummaryDTO summary;
    private final int totalMatches;

    public HoldingReportDTO(
        ListInterface<HoldingReportRowDTO> rows,
        HoldingReportSummaryDTO summary,
        int totalMatches) {
      this.rows = rows;
      this.summary = summary;
      this.totalMatches = totalMatches;
    }

    public ListInterface<HoldingReportRowDTO> getRows() {
      return rows;
    }

    public HoldingReportSummaryDTO getSummary() {
      return summary;
    }

    public int getTotalMatches() {
      return totalMatches;
    }
  }

  // ==========================================
  // RENDER REPORT SCREENS
  // ==========================================

  public GetMenuInputResult renderSlaReportScreen(
      SlaReportDTO viewModel, String scopeStr, String sortStr, int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 1: WAIT TIME EFFICIENCY & SLA ATTAINMENT AUDIT", 88);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope:");
    System.out.println(scopeStr);
    System.out.println("Sort Order  : " + sortStr + "\n");

    int totalMatches = (viewModel == null) ? 0 : viewModel.getTotalMatches();
    ListInterface<SlaReportRowDTO> rows = (viewModel == null) ? null : viewModel.getRows();
    int rowCount = (rows == null) ? 0 : rows.getNumberOfEntries();
    int displayCount = (recordLimit == 0 || recordLimit >= rowCount) ? rowCount : recordLimit;

    int[] columnWidths = {4, 11, 20, 12, 14, 12, 9, 15};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"RANK", "RES ID", "GUEST NAME", "TIER", "ROOM TYPE", "WAIT TIME", "STRIKES", "STATUS"},
        headerSettings);

    if (rows == null || rowCount == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {118}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        SlaReportRowDTO row = rows.getEntry(i);
        TableUtil.printTableRow(
            new String[] {
              row.getRank(),
              row.getReservationId(),
              row.getGuestName(),
              row.getTier(),
              row.getRoomType(),
              row.getWaitMins() + " Mins",
              String.valueOf(row.getStrikes()),
              row.getStatus()
            },
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    if (viewModel != null && viewModel.getSummary() != null) {
      printSlaSummaryBlock(viewModel.getSummary());
    }

    ConsoleUtil.stopRecording();

    while (true) {
      System.out.println(
          "\n"
              + "[S] Modify Filter Matrix    [R] Refresh Report      [E] Export Report       [Q]"
              + " Quit to Analytics Hub\n");
      try {
        return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E', 'Q'});
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        return null;
      }
    }
  }

  public GetMenuInputResult renderPenaltyReportScreen(
      PenaltyReportDTO viewModel, String scopeStr, String sortStr, int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 2: VIP PENALTY & EVICTION AUDIT REPORT", 88);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope:");
    System.out.println(scopeStr);
    System.out.println("Sort Order  : " + sortStr + "\n");

    int totalMatches = (viewModel == null) ? 0 : viewModel.getTotalMatches();
    ListInterface<PenaltyReportRowDTO> rows = (viewModel == null) ? null : viewModel.getRows();
    int rowCount = (rows == null) ? 0 : rows.getNumberOfEntries();
    int displayCount = (recordLimit == 0 || recordLimit >= rowCount) ? rowCount : recordLimit;

    int[] columnWidths = {4, 11, 20, 12, 9, 9, 12, 9};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"RANK", "RES ID", "GUEST NAME", "TIER", "STRIKES", "BOILING", "STATUS", "EVICTED"},
        headerSettings);

    if (rows == null || rowCount == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {107}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        PenaltyReportRowDTO row = rows.getEntry(i);
        TableUtil.printTableRow(
            new String[] {
              row.getRank(),
              row.getReservationId(),
              row.getGuestName(),
              row.getTier(),
              String.valueOf(row.getStrikes()),
              row.getBoiling(),
              row.getStatus(),
              row.getEvicted()
            },
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    if (viewModel != null && viewModel.getSummary() != null) {
      printPenaltySummaryBlock(viewModel.getSummary());
    }

    ConsoleUtil.stopRecording();

    while (true) {
      System.out.println(
          "\n"
              + "[S] Modify Filter Matrix    [R] Refresh Report      [E] Export Report       [Q]"
              + " Quit to Analytics Hub\n");
      try {
        return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E', 'Q'});
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        return null;
      }
    }
  }

  public GetMenuInputResult renderHoldingReportScreen(
      HoldingReportDTO viewModel, String scopeStr, String sortStr, int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 3: ROOM HOLDING BAY & GRACE WINDOW AUDIT", 88);

    ConsoleUtil.clearBuffer();
    ConsoleUtil.startRecording();

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope:");
    System.out.println(scopeStr);
    System.out.println("Sort Order  : " + sortStr + "\n");

    int totalMatches = (viewModel == null) ? 0 : viewModel.getTotalMatches();
    ListInterface<HoldingReportRowDTO> rows = (viewModel == null) ? null : viewModel.getRows();
    int rowCount = (rows == null) ? 0 : rows.getNumberOfEntries();
    int displayCount = (recordLimit == 0 || recordLimit >= rowCount) ? rowCount : recordLimit;

    int[] columnWidths = {4, 11, 18, 12, 15, 12, 13, 13};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.LEFT)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.TableSettings headerSettings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.CENTER)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER)
            .setHAlign(7, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "RANK", "RES ID", "GUEST NAME", "TIER", "ALLOWED GRACE", "TIME USED", "HOLD STATUS", "GRACE USED %"
        },
        headerSettings);

    if (rows == null || rowCount == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {119}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        HoldingReportRowDTO row = rows.getEntry(i);
        TableUtil.printTableRow(
            new String[] {
              row.getRank(),
              row.getReservationId(),
              row.getGuestName(),
              row.getTier(),
              row.getAllowedGraceMins() + " Mins",
              row.getTimeUsedStr(),
              row.getHoldStatus(),
              row.getGraceUsedPctStr()
            },
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    if (viewModel != null && viewModel.getSummary() != null) {
      printHoldingSummaryBlock(viewModel.getSummary());
    }

    ConsoleUtil.stopRecording();

    while (true) {
      System.out.println(
          "\n"
              + "[S] Modify Filter Matrix    [R] Refresh Report      [E] Export Report       [Q]"
              + " Quit to Analytics Hub\n");
      try {
        return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E', 'Q'});
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
        return null;
      }
    }
  }

  private void printSlaSummaryBlock(SlaReportSummaryDTO summary) {
    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (SLA PERFORMANCE):\n");
    System.out.printf(
        " DIAMOND TIER (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n",
        summary.getDiamondLimitMins(),
        summary.getDiamondSlaMet(),
        summary.getDiamondTotal(),
        summary.getDiamondSlaPct(),
        summary.getDiamondSlaTargetPct(),
        (summary.getDiamondSlaPct() >= summary.getDiamondSlaTargetPct()
            ? "[OK]"
            : "[!] SLA BREACH"));
    System.out.printf(
        " GOLD TIER    (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n",
        summary.getGoldLimitMins(),
        summary.getGoldSlaMet(),
        summary.getGoldTotal(),
        summary.getGoldSlaPct(),
        summary.getGoldSlaTargetPct(),
        (summary.getGoldSlaPct() >= summary.getGoldSlaTargetPct() ? "[OK]" : "[!] SLA BREACH"));
    System.out.printf(
        " SILVER TIER  (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n\n",
        summary.getSilverLimitMins(),
        summary.getSilverSlaMet(),
        summary.getSilverTotal(),
        summary.getSilverSlaPct(),
        summary.getSilverSlaTargetPct(),
        (summary.getSilverSlaPct() >= summary.getSilverSlaTargetPct() ? "[OK]" : "[!] SLA BREACH"));
    System.out.println(
        "--------------------------------------------------------------------------\n");

    if (summary.getDiamondSlaPct() < summary.getDiamondSlaTargetPct()
        || summary.getGoldSlaPct() < summary.getGoldSlaTargetPct()
        || summary.getSilverSlaPct() < summary.getSilverSlaTargetPct()) {
      System.out.println("EXECUTIVE DECISION REMEDIATION ALERT:");
      System.out.println(" [!] Critical SLA breach detected in one or more member tiers!");
      System.out.println(" REMEDIATION: Open Settings -> Tweak Operational Rules -> Lower Boiling");
      System.out.println(" Point Limit (Mins) or increase Tier Base Values to accelerate queue.\n");
    } else {
      System.out.println(
          "EXECUTIVE STATUS: All VIP tiers are operating within SLA attainment targets.\n");
    }
    System.out.println(
        "--------------------------------------------------------------------------");
  }

  private void printPenaltySummaryBlock(PenaltyReportSummaryDTO summary) {
    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (EVICTION AUDIT):\n");
    System.out.printf(" - Total Strikes Logged: %d Penalty Strikes\n\n", summary.getTotalStrikes());
    System.out.printf(
        " DIAMOND EVICTIONS : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n",
        summary.getDiamondEvicted(),
        summary.getDiamondTotal(),
        summary.getDiamondRate(),
        summary.getDiamondMaxTarget(),
        (summary.getDiamondRate() <= summary.getDiamondMaxTarget() ? "[OK]" : "[!] HIGH EVICTION"));
    System.out.printf(
        " GOLD EVICTIONS    : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n",
        summary.getGoldEvicted(),
        summary.getGoldTotal(),
        summary.getGoldRate(),
        summary.getGoldMaxTarget(),
        (summary.getGoldRate() <= summary.getGoldMaxTarget() ? "[OK]" : "[!] HIGH EVICTION"));
    System.out.printf(
        " SILVER EVICTIONS  : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n\n",
        summary.getSilverEvicted(),
        summary.getSilverTotal(),
        summary.getSilverRate(),
        summary.getSilverMaxTarget(),
        (summary.getSilverRate() <= summary.getSilverMaxTarget() ? "[OK]" : "[!] HIGH EVICTION"));
    System.out.println(
        "--------------------------------------------------------------------------\n");

    if (summary.getDiamondRate() > summary.getDiamondMaxTarget()
        || summary.getGoldRate() > summary.getGoldMaxTarget()
        || summary.getSilverRate() > summary.getSilverMaxTarget()) {
      System.out.println("EXECUTIVE DECISION REMEDIATION ALERT:");
      System.out.println(" [!] VIP EVICTION ALERT: Eviction rate exceeds tier target threshold!");
      System.out.println(
          " REMEDIATION: Open Settings -> Tweak Operational Rules -> Increase Max Strike");
      System.out.println(" Limit to grant high-value members more callout opportunities.\n");
    } else {
      System.out.println(
          "EXECUTIVE STATUS: Penalty eviction rates are within acceptable limits.\n");
    }
    System.out.println(
        "--------------------------------------------------------------------------");
  }

  private void printHoldingSummaryBlock(HoldingReportSummaryDTO summary) {
    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (HOLDING BAY AUDIT):\n");
    System.out.printf(" - Total Holding Bay Entries : %d Rooms Held\n\n", summary.getTotalHeld());
    System.out.printf(
        " - DIAMOND MAX GRACE TARGET  : %.1f%%\n", summary.getDiamondGraceUtilTarget());
    System.out.printf(" - GOLD MAX GRACE TARGET     : %.1f%%\n", summary.getGoldGraceUtilTarget());
    System.out.printf(
        " - SILVER MAX GRACE TARGET   : %.1f%%\n\n", summary.getSilverGraceUtilTarget());
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("EXECUTIVE DECISION REMEDIATION ALERT:");
    System.out.println(" If rooms sit idle in holding bay, navigate to Settings -> Grace Windows");
    System.out.println(" to reduce the hold period and free unclaimed rooms faster.\n");
    System.out.println(
        "--------------------------------------------------------------------------");
  }

  public void displayExportSuccessScreen(String filePath) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT EXPORT COMPLETE");
    System.out.println(" >> SUCCESS: Report successfully exported to disk!");
    System.out.println(" >> File Location: " + filePath + "\n");
    ConsoleUtil.printContinueMessage();
  }

  public String promptCustomDateStep(
      String stepTitle, String formatInfo, String promptLabel, String currentContext) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CUSTOM DATE RANGE FILTER", 88);
    System.out.println("Step: [ " + stepTitle + " ]");
    if (currentContext != null && !currentContext.isEmpty()) {
      System.out.println("Context: " + currentContext);
    }
    System.out.println("Expected Format: " + formatInfo);
    System.out.println("Type 'C' to cancel.\n");
    String input = ConsoleUtil.getStringInput(promptLabel);
    if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
      return null;
    }
    return input.trim();
  }

  public String promptCustomDateTimeStep(
      String stepTitle, String formatInfo, String promptLabel, String currentContext) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CUSTOM DATE & TIME RANGE FILTER", 88);
    System.out.println("Step: [ " + stepTitle + " ]");
    if (currentContext != null && !currentContext.isEmpty()) {
      System.out.println("Context: " + currentContext);
    }
    System.out.println("Expected Format: " + formatInfo);
    System.out.println("Type 'C' to cancel.\n");
    String input = ConsoleUtil.getStringInput(promptLabel);
    if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
      return null;
    }
    return input.trim();
  }
}
