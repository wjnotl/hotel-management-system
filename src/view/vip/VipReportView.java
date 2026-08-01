package view.vip;

import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
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
      String sortAttr,
      String sortDir,
      int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("FILTER & SORT CONFIGURATION MATRIX");
    System.out.println("ACTIVE REPORT: [ " + reportTitle + " ]\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println(
        "- Guest Search String   : [ "
            + (search == null ? "NONE (All)" : "\"" + search + "\"")
            + " ]");
    System.out.println("- Membership Tier       : [ " + (tier == null ? "ALL TIERS" : tier) + " ]");
    System.out.println(
        "- Room Queue Type       : [ " + (roomType == null ? "ALL ROOM TYPES" : roomType) + " ]");
    System.out.println(
        "- Boiling Status        : [ " + (boiling == null ? "ALL STATES" : boiling) + " ]");
    System.out.println("- Sort Attribute        : [ " + sortAttr + " ]");
    System.out.println("- Sort Vector Direction : [ " + sortDir + " ]");
    System.out.println(
        "- Max Display Records   : [ "
            + (recordLimit == 0 ? "SHOW ALL (Unlimited)" : "Top " + recordLimit + " Records")
            + " ]\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("1. Edit Guest Search String");
    System.out.println("2. Edit Membership Tier Filter");
    System.out.println("3. Edit Room Queue Type Filter");
    System.out.println("4. Edit Boiling Status Filter");
    System.out.println("5. Edit Sort Attribute & Direction");
    System.out.println("6. Edit Max Display Records Limit");
    System.out.println("7. Reset All Filters");
    System.out.println("8. Generate Report Now");
    System.out.println("9. Back\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 9);
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

  public int displayExecutionConfirmationScreen(
      String reportTitle, String scopeStr, String sortStr, int totalMatches, int limit) {
    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("CONFIRM REPORT EXECUTION");
    System.out.println("ACTIVE REPORT PARAMETERS:\n");
    System.out.println("  - Report Type  : " + reportTitle);
    System.out.println("  - Scope Match  : " + scopeStr);
    System.out.println("  - Sort Order   : " + sortStr);
    System.out.println(
        "  - Display Limit: "
            + (limit == 0
                ? "Show All (" + totalMatches + " Records)"
                : "Top " + Math.min(limit, totalMatches) + " of " + totalMatches + " Records")
            + "\n");
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("Proceed with memory array parsing and generate analytics display?\n");
    System.out.println("1. Yes, Run Pipeline & Render Report");
    System.out.println("2. Back to Filter Matrix");
    System.out.println("3. Cancel & Exit to Analytics Hub\n");

    return ConsoleUtil.getMenuInput("Choose an option: ", 1, 3).getAsInt();
  }

  public GetMenuInputResult renderSlaReportScreen(
      ListInterface<Reservation> matchedList,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config,
      String scopeStr,
      String sortStr,
      int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 1: WAIT TIME EFFICIENCY & SLA ATTAINMENT AUDIT", 88);

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope : " + scopeStr);
    System.out.println("Sort Order   : " + sortStr + "\n");

    int totalMatches = (matchedList == null) ? 0 : matchedList.getNumberOfEntries();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    int[] columnWidths = {6, 20, 12, 14, 12, 9, 15};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER)
            .setHAlign(6, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"RANK", "GUEST NAME", "TIER", "ROOM TYPE", "WAIT TIME", "STRIKES", "STATUS"},
        settings);

    if (matchedList == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {88}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        Reservation r = matchedList.getEntry(i);
        Guest g = (r != null) ? findGuest(guestList, r.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;

        String rank = i + ".";
        String name = (g != null) ? g.getName() : "N/A";
        String tier = (m != null) ? m.getTier().name() : "NON-MEMBER";
        String room = (r != null) ? r.getRoomType().name() : "N/A";
        long waitMins = calculateWaitMins(r);
        int strikes = (g != null) ? g.getStrikeCount() : 0;
        String status = (r != null) ? r.getStatus().name() : "N/A";

        TableUtil.printTableRow(
            new String[] {
              rank, name, tier, room, waitMins + " Mins", String.valueOf(strikes), status
            },
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    printSlaSummaryBlock(matchedList, guestList, memberList, config);

    System.out.println(
        "\n[S] Modify Filter Matrix    [R] Refresh Report      [E] Exit to Analytics Hub\n");
    return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E'});
  }

  public GetMenuInputResult renderPenaltyReportScreen(
      ListInterface<Reservation> matchedList,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config,
      String scopeStr,
      String sortStr,
      int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 2: VIP PENALTY & EVICTION AUDIT REPORT", 88);

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope : " + scopeStr);
    System.out.println("Sort Order   : " + sortStr + "\n");

    int totalMatches = (matchedList == null) ? 0 : matchedList.getNumberOfEntries();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    int[] columnWidths = {6, 22, 12, 10, 10, 28};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.LEFT);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {"RANK", "GUEST NAME", "TIER", "STRIKES", "BOILING", "RESOLUTION STATUS"},
        settings);

    if (matchedList == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {88}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        Reservation r = matchedList.getEntry(i);
        Guest g = (r != null) ? findGuest(guestList, r.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;

        String rank = i + ".";
        String name = (g != null) ? g.getName() : "N/A";
        String tier = (m != null) ? m.getTier().name() : "NON-MEMBER";
        int strikes = (g != null) ? g.getStrikeCount() : 0;
        String boiling = (r != null && r.getIsBoiling()) ? "[!]" : "[ ]";
        String resolution = (r != null) ? r.getStatus().name() : "N/A";

        if (r != null && r.getStatus() == Reservation.Status.NO_SHOW) {
          resolution = "Evicted (Max Strikes Exceeded)";
        }

        TableUtil.printTableRow(
            new String[] {rank, name, tier, String.valueOf(strikes), boiling, resolution},
            settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    printPenaltySummaryBlock(matchedList, guestList, memberList, config);

    System.out.println(
        "\n[S] Modify Filter Matrix    [R] Refresh Report      [E] Exit to Analytics Hub\n");
    return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E'});
  }

  public GetMenuInputResult renderHoldingReportScreen(
      ListInterface<Reservation> matchedList,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config,
      String scopeStr,
      String sortStr,
      int recordLimit) {

    ConsoleUtil.clearScreen();
    ConsoleUtil.printTitleBox("REPORT 3: ROOM HOLDING BAY & GRACE WINDOW AUDIT", 88);

    System.out.println(
        "Generated At: "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    System.out.println("Active Scope : " + scopeStr);
    System.out.println("Sort Order   : " + sortStr + "\n");

    int totalMatches = (matchedList == null) ? 0 : matchedList.getNumberOfEntries();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    int[] columnWidths = {6, 22, 12, 14, 16, 18};
    TableUtil.TableSettings settings =
        new TableUtil.TableSettings(columnWidths)
            .setHAlign(0, TableUtil.Align.CENTER)
            .setHAlign(1, TableUtil.Align.LEFT)
            .setHAlign(2, TableUtil.Align.CENTER)
            .setHAlign(3, TableUtil.Align.CENTER)
            .setHAlign(4, TableUtil.Align.CENTER)
            .setHAlign(5, TableUtil.Align.CENTER);

    TableUtil.printTableBorder(settings, TableUtil.BorderPosition.TOP);
    TableUtil.printTableRow(
        new String[] {
          "RANK", "GUEST NAME", "TIER", "ALLOWED GRACE", "HOLD STATUS", "UTILIZATION %"
        },
        settings);

    if (matchedList == null || totalMatches == 0) {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.HEADER_CLOSE);
      TableUtil.TableSettings emptySettings =
          new TableUtil.TableSettings(new int[] {88}).setHAlign(0, TableUtil.Align.CENTER);
      TableUtil.printTableRow(
          new String[] {"*** NO MATCHING RECORDS FOUND FOR REPORT ***"}, emptySettings);
      TableUtil.printTableBorder(emptySettings, TableUtil.BorderPosition.PLAIN_BOTTOM);
    } else {
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.MIDDLE);

      for (int i = 1; i <= displayCount; i++) {
        Reservation r = matchedList.getEntry(i);
        Guest g = (r != null) ? findGuest(guestList, r.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;

        String rank = i + ".";
        String name = (g != null) ? g.getName() : "N/A";
        String tier = (m != null) ? m.getTier().name() : "NON-MEMBER";

        int allowedGrace = getGraceMinsForTier(m, config);
        String status = (r != null) ? r.getStatus().name() : "N/A";
        String utilPctStr =
            (r != null && r.getStatus() == Reservation.Status.CHECKED_IN)
                ? "100.0%"
                : "Active Hold";

        TableUtil.printTableRow(
            new String[] {rank, name, tier, allowedGrace + " Mins", status, utilPctStr}, settings);
      }
      TableUtil.printTableBorder(settings, TableUtil.BorderPosition.BOTTOM);
    }

    System.out.printf("\nDisplaying %d of %d Matched Records\n", displayCount, totalMatches);

    printHoldingSummaryBlock(matchedList, guestList, memberList, config);

    System.out.println(
        "\n[S] Modify Filter Matrix    [R] Refresh Report      [E] Exit to Analytics Hub\n");
    return ConsoleUtil.getMenuInput("Select a command: ", new char[] {'S', 'R', 'E'});
  }

  private void printSlaSummaryBlock(
      ListInterface<Reservation> list,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config) {

    int diamondTotal = 0, goldTotal = 0, silverTotal = 0;
    int diamondSlaMet = 0, goldSlaMet = 0, silverSlaMet = 0;

    if (list != null) {
      for (int i = 1; i <= list.getNumberOfEntries(); i++) {
        Reservation r = list.getEntry(i);
        if (r == null) continue;

        Guest g = findGuest(guestList, r.getGuestId());
        Member m =
            (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;
        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

        long wait = calculateWaitMins(r);
        int targetMins =
            (tier == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondPatienceLimitMins()
                : (tier == Member.LoyaltyTier.GOLD)
                    ? config.getGoldPatienceLimitMins()
                    : config.getSilverPatienceLimitMins();

        if (tier == Member.LoyaltyTier.DIAMOND) {
          diamondTotal++;
          if (wait <= targetMins) diamondSlaMet++;
        } else if (tier == Member.LoyaltyTier.GOLD) {
          goldTotal++;
          if (wait <= targetMins) goldSlaMet++;
        } else {
          silverTotal++;
          if (wait <= targetMins) silverSlaMet++;
        }
      }
    }

    double dPct = (diamondTotal == 0) ? 100.0 : ((double) diamondSlaMet / diamondTotal) * 100.0;
    double gPct = (goldTotal == 0) ? 100.0 : ((double) goldSlaMet / goldTotal) * 100.0;
    double sPct = (silverTotal == 0) ? 100.0 : ((double) silverSlaMet / silverTotal) * 100.0;

    int dLimit = config.getDiamondPatienceLimitMins();
    int gLimit = config.getGoldPatienceLimitMins();
    int sLimit = config.getSilverPatienceLimitMins();

    double dTarget = config.getDiamondSlaTargetPct();
    double gTarget = config.getGoldSlaTargetPct();
    double sTarget = config.getSilverSlaTargetPct();

    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (SLA PERFORMANCE):\n");
    System.out.printf(
        " DIAMOND TIER (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n",
        dLimit,
        diamondSlaMet,
        diamondTotal,
        dPct,
        dTarget,
        (dPct >= dTarget ? "[OK]" : "[!] SLA BREACH"));
    System.out.printf(
        " GOLD TIER    (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n",
        gLimit,
        goldSlaMet,
        goldTotal,
        gPct,
        gTarget,
        (gPct >= gTarget ? "[OK]" : "[!] SLA BREACH"));
    System.out.printf(
        " SILVER TIER  (Limit: <= %d Mins) : %d / %d Met SLA (Actual: %.1f%%  |  Target: %.1f%%) ->"
            + " %s\n\n",
        sLimit,
        silverSlaMet,
        silverTotal,
        sPct,
        sTarget,
        (sPct >= sTarget ? "[OK]" : "[!] SLA BREACH"));
    System.out.println(
        "--------------------------------------------------------------------------\n");

    if (dPct < dTarget || gPct < gTarget || sPct < sTarget) {
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

  private void printPenaltySummaryBlock(
      ListInterface<Reservation> list,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config) {

    int dTotal = 0, gTotal = 0, sTotal = 0;
    int dEvicted = 0, gEvicted = 0, sEvicted = 0;
    int totalStrikes = 0;

    if (list != null) {
      for (int i = 1; i <= list.getNumberOfEntries(); i++) {
        Reservation r = list.getEntry(i);
        if (r == null) continue;

        Guest g = findGuest(guestList, r.getGuestId());
        Member m =
            (g != null && g.getMemberId() != null) ? findMember(memberList, g.getMemberId()) : null;
        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

        if (g != null) totalStrikes += g.getStrikeCount();

        if (tier == Member.LoyaltyTier.DIAMOND) {
          dTotal++;
          if (r.getStatus() == Reservation.Status.NO_SHOW) dEvicted++;
        } else if (tier == Member.LoyaltyTier.GOLD) {
          gTotal++;
          if (r.getStatus() == Reservation.Status.NO_SHOW) gEvicted++;
        } else {
          sTotal++;
          if (r.getStatus() == Reservation.Status.NO_SHOW) sEvicted++;
        }
      }
    }

    double dRate = (dTotal == 0) ? 0.0 : ((double) dEvicted / dTotal) * 100.0;
    double gRate = (gTotal == 0) ? 0.0 : ((double) gEvicted / gTotal) * 100.0;
    double sRate = (sTotal == 0) ? 0.0 : ((double) sEvicted / sTotal) * 100.0;

    double dMax = config.getDiamondEvictionRateTargetPct();
    double gMax = config.getGoldEvictionRateTargetPct();
    double sMax = config.getSilverEvictionRateTargetPct();

    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (EVICTION & CHURN AUDIT):\n");
    System.out.printf(" - Total Strikes Logged: %d Penalty Strikes\n\n", totalStrikes);
    System.out.printf(
        " DIAMOND EVICTIONS : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n",
        dEvicted, dTotal, dRate, dMax, (dRate <= dMax ? "[OK]" : "[!] HIGH CHURN"));
    System.out.printf(
        " GOLD EVICTIONS    : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n",
        gEvicted, gTotal, gRate, gMax, (gRate <= gMax ? "[OK]" : "[!] HIGH CHURN"));
    System.out.printf(
        " SILVER EVICTIONS  : %d / %d (Eviction Rate: %.1f%%  |  Max Limit: %.1f%%) -> %s\n\n",
        sEvicted, sTotal, sRate, sMax, (sRate <= sMax ? "[OK]" : "[!] HIGH CHURN"));
    System.out.println(
        "--------------------------------------------------------------------------\n");

    if (dRate > dMax || gRate > gMax || sRate > sMax) {
      System.out.println("EXECUTIVE DECISION REMEDIATION ALERT:");
      System.out.println(" [!] VIP CHURN ALERT: Eviction rate exceeds tier target threshold!");
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

  private void printHoldingSummaryBlock(
      ListInterface<Reservation> list,
      ListInterface<Guest> guestList,
      ListInterface<Member> memberList,
      VipSystemConfig config) {

    int totalHeld = (list == null) ? 0 : list.getNumberOfEntries();
    System.out.println(
        "\n--------------------------------------------------------------------------\n");
    System.out.println("ALGORITHM SUMMARY METRICS (HOLDING BAY AUDIT):\n");
    System.out.printf(" - Total Holding Bay Entries : %d Rooms Held\n\n", totalHeld);
    System.out.printf(
        " - DIAMOND MAX GRACE TARGET  : %.1f%%\n", config.getDiamondGraceUtilTargetPct());
    System.out.printf(
        " - GOLD MAX GRACE TARGET     : %.1f%%\n", config.getGoldGraceUtilTargetPct());
    System.out.printf(
        " - SILVER MAX GRACE TARGET   : %.1f%%\n\n", config.getSilverGraceUtilTargetPct());
    System.out.println(
        "--------------------------------------------------------------------------\n");
    System.out.println("EXECUTIVE DECISION REMEDIATION ALERT:");
    System.out.println(" If rooms sit idle in holding bay, navigate to Settings -> Grace Windows");
    System.out.println(" to reduce the hold period and free unclaimed rooms faster.\n");
    System.out.println(
        "--------------------------------------------------------------------------");
  }

  private long calculateWaitMins(Reservation r) {
    if (r == null || r.getQueueArrivalTime() == null) return 0;
    return java.time.Duration.between(r.getQueueArrivalTime(), LocalDateTime.now()).toMinutes();
  }

  private int getGraceMinsForTier(Member m, VipSystemConfig config) {
    if (m == null || m.getTier() == null) return config.getSilverGraceWindowMins();
    switch (m.getTier()) {
      case DIAMOND:
        return config.getDiamondGraceWindowMins();
      case GOLD:
        return config.getGoldGraceWindowMins();
      default:
        return config.getSilverGraceWindowMins();
    }
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) return g;
    }
    return null;
  }

  private Member findMember(ListInterface<Member> memberList, String memberId) {
    if (memberList == null || memberId == null) return null;
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equalsIgnoreCase(m.getMemberId())) return m;
    }
    return null;
  }
}
