package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;
import util.ConsoleUtil;
import util.ConsoleUtil.GetMenuInputResult;
import util.TxtExportUtil;
import view.vip.VipReportView;

public class VipReportController {

  private final VipReportView reportView = new VipReportView();
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final VipSystemConfigRepo configRepo;

  public VipReportController(
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.configRepo = configRepo;
  }

  public void startReportManagement() {
    while (true) {
      try {
        GetMenuInputResult hubResult = reportView.displayReportHubMenu();
        int reportChoice = hubResult.getAsInt();
        if (reportChoice == 4) {
          return;
        }

        manageReportPipeline(reportChoice);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private static class ReportFilterState {
    String searchQuery = null;
    String tierFilter = null;
    String roomTypeFilter = null;
    String boilingFilter = null;
    String datePresetLabel = "TODAY";
    LocalDateTime startDate = LocalDate.now().atStartOfDay();
    LocalDateTime endDate = LocalDateTime.now();
    boolean customEndIsToday = false;
    String sortAttribute;
    String sortDirection = "DESCENDING";
    int recordLimit = 10;

    ReportFilterState(int reportType) {
      this.sortAttribute = (reportType == 2) ? "STRIKE COUNT" : "PHYSICAL WAIT TIME";
    }

    void refreshPresetTimestamps() {
      if ("TODAY".equals(datePresetLabel)) {
        this.startDate = LocalDate.now().atStartOfDay();
        this.endDate = LocalDateTime.now();
      } else if ("YESTERDAY".equals(datePresetLabel)) {
        LocalDate yest = LocalDate.now().minusDays(1);
        this.startDate = yest.atStartOfDay();
        this.endDate = yest.atTime(23, 59);
      } else if ("LAST 7 DAYS".equals(datePresetLabel)) {
        this.startDate = LocalDate.now().minusDays(6).atStartOfDay();
        this.endDate = LocalDateTime.now();
      } else if ("LAST 30 DAYS".equals(datePresetLabel)) {
        this.startDate = LocalDate.now().minusDays(29).atStartOfDay();
        this.endDate = LocalDateTime.now();
      } else if (customEndIsToday) {
        this.endDate = LocalDateTime.now();
      }
    }
  }

  private void manageReportPipeline(int reportType) {
    ReportFilterState state = new ReportFilterState(reportType);

    String reportTitle =
        (reportType == 1)
            ? "Wait Time Efficiency & SLA Attainment Report"
            : (reportType == 2)
                ? "VIP Penalty & Eviction Audit Report"
                : "Room Holding Bay & Grace Window Report";

    // Step 1: Open Filter & Sort Options screen FIRST!
    boolean generateSelected = handleFilterControlPanel(reportTitle, state, reportType);
    if (!generateSelected) {
      return; // User selected Back (Option 6) -> Return to Analytics Hub
    }

    // Step 2: User selected Option 1 (Generate Report) -> Enter Report Screen Loop!
    renderGeneratedReportLoop(reportType, reportTitle, state);
  }

  private void renderGeneratedReportLoop(
      int reportType, String reportTitle, ReportFilterState state) {

    while (true) {
      try {
        state.refreshPresetTimestamps();
        ListInterface<Reservation> allReservations = vipReservationRepo.getAllReservations();
        ListInterface<Reservation> filteredList =
            filterAndSortList(
                allReservations,
                state.searchQuery,
                state.tierFilter,
                state.roomTypeFilter,
                state.boilingFilter,
                state.startDate,
                state.endDate,
                state.sortAttribute,
                state.sortDirection,
                reportType);

        String scopeStr =
            buildScopeString(
                state.searchQuery,
                state.tierFilter,
                state.roomTypeFilter,
                state.boilingFilter,
                state.startDate,
                state.endDate,
                state.datePresetLabel);
        String sortStr = state.sortAttribute + " (" + state.sortDirection + ")";
        VipSystemConfig config = configRepo.getConfig();
        GetMenuInputResult result;

        ConsoleUtil.clearBuffer();
        ConsoleUtil.startRecording();

        if (reportType == 1) {
          result =
              reportView.renderSlaReportScreen(
                  filteredList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  state.recordLimit);
        } else if (reportType == 2) {
          result =
              reportView.renderPenaltyReportScreen(
                  filteredList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  state.recordLimit);
        } else {
          result =
              reportView.renderHoldingReportScreen(
                  filteredList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  state.recordLimit);
        }

        if (result == null) {
          continue; // Re-render whole report cleanly on invalid command input
        }

        String capturedReportText = ConsoleUtil.getCapturedString();

        if ("Q".equalsIgnoreCase(result.input)) {
          return; // Quit to Analytics Hub
        } else if ("S".equalsIgnoreCase(result.input)) {
          boolean generateSelected = handleFilterControlPanel(reportTitle, state, reportType);
          if (!generateSelected) {
            return; // Back from filter menu -> return to Analytics Hub
          }
          // generateSelected is true -> loop continues & re-renders report with new
          // filters!
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Refresh -> loop continues & re-fetches live data & re-renders report screen
          // directly!
        } else if ("E".equalsIgnoreCase(result.input)) {
          String filePrefix = "unknown_report";
          switch (reportType) {
            case 1:
              filePrefix = "vip/sla_report";
              break;
            case 2:
              filePrefix = "vip/penalty_report";
              break;
            case 3:
              filePrefix = "vip/holding_report";
              break;
          }

          String exportedPath =
              TxtExportUtil.export(
                  filePrefix, reportTitle.toUpperCase() + "\n" + capturedReportText);
          reportView.displayExportSuccessScreen(exportedPath);
        }
      } catch (Exception e) {
        ConsoleUtil.stopRecording();
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean handleFilterControlPanel(
      String reportTitle, ReportFilterState state, int reportType) {
    while (true) {
      try {
        GetMenuInputResult action =
            reportView.displayFilterControlPanel(
                reportTitle,
                state.searchQuery,
                state.tierFilter,
                state.roomTypeFilter,
                state.boilingFilter,
                state.datePresetLabel,
                state.sortAttribute,
                state.sortDirection,
                state.recordLimit);

        int choice = action.getAsInt();

        if (choice == 1) {
          return true; // 1. Generate Report
        } else if (choice == 2) {
          handleEditFiltersSubmenu(state); // 2. Edit Filters
        } else if (choice == 3) {
          handleSortOptionsSubmenu(state); // 3. Sort Options
        } else if (choice == 4) {
          state.recordLimit = handleRecordLimitSubmenu(state.recordLimit); // 4. Max Display Records
        } else if (choice == 5) {
          // 5. Reset All Options
          state.searchQuery = null;
          state.tierFilter = null;
          state.roomTypeFilter = null;
          state.boilingFilter = null;
          state.datePresetLabel = "TODAY";
          state.startDate = LocalDate.now().atStartOfDay();
          state.endDate = LocalDate.now().atTime(LocalTime.MAX);
          state.sortAttribute = (reportType == 2) ? "STRIKE COUNT" : "PHYSICAL WAIT TIME";
          state.sortDirection = "DESCENDING";
          state.recordLimit = 10;
        } else if (choice == 6) {
          return false; // 6. Back to Analytics Hub
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleEditFiltersSubmenu(ReportFilterState state) {
    while (true) {
      try {
        GetMenuInputResult action =
            reportView.displayEditFiltersSubmenu(
                state.searchQuery,
                state.tierFilter,
                state.roomTypeFilter,
                state.boilingFilter,
                state.datePresetLabel);
        int choice = action.getAsInt();
        if (choice == 1) {
          state.tierFilter = handleTierSubmenu(state.tierFilter);
        } else if (choice == 2) {
          state.boilingFilter = handleBoilingSubmenu(state.boilingFilter);
        } else if (choice == 3) {
          state.roomTypeFilter = handleRoomTypeSubmenu(state.roomTypeFilter);
        } else if (choice == 4) {
          handleDateFilterSubmenu(state);
        } else if (choice == 5) {
          state.searchQuery = handleSearchSubmenu(state.searchQuery);
        } else if (choice == 6) {
          return; // Back to Report Generation Configuration Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleDateFilterSubmenu(ReportFilterState state) {
    while (true) {
      try {
        GetMenuInputResult res = reportView.displayDateFilterSubmenu(state.datePresetLabel);
        int choice = res.getAsInt();
        if (choice == 1) {
          state.startDate = LocalDate.now().atStartOfDay();
          state.endDate = LocalDateTime.now();
          state.datePresetLabel = "TODAY";
          return;
        } else if (choice == 2) {
          LocalDate yest = LocalDate.now().minusDays(1);
          state.startDate = yest.atStartOfDay();
          state.endDate = yest.atTime(23, 59);
          state.datePresetLabel = "YESTERDAY";
          return;
        } else if (choice == 3) {
          state.startDate = LocalDate.now().minusDays(6).atStartOfDay();
          state.endDate = LocalDateTime.now();
          state.datePresetLabel = "LAST 7 DAYS";
          return;
        } else if (choice == 4) {
          state.startDate = LocalDate.now().minusDays(29).atStartOfDay();
          state.endDate = LocalDateTime.now();
          state.datePresetLabel = "LAST 30 DAYS";
          return;
        } else if (choice == 5) {
          if (handleCustomDateRangeSubmenu(state)) {
            return;
          }
        } else if (choice == 6) {
          if (handleCustomDateTimeRangeSubmenu(state)) {
            return;
          }
        } else if (choice == 7) {
          state.startDate = null;
          state.endDate = null;
          state.datePresetLabel = "ALL TIME";
          return;
        } else if (choice == 8) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean handleCustomDateRangeSubmenu(ReportFilterState state) {
    DateTimeFormatter parseFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    LocalDate start = null;
    String startStr = "N/A";

    // Step 1: Prompt Start Date (Immediate validation loop)
    while (true) {
      String input =
          reportView.promptCustomDateStep(
              "1 of 2: Start Date",
              "YYYY-MM-DD (e.g. 2026-08-01)",
              "Enter Start Date [or 'C' to Cancel]: ",
              null);
      if (input == null) return false;
      try {
        start = LocalDate.parse(input, parseFmt);
        if (start.isAfter(LocalDate.now())) {
          ConsoleUtil.printError(
              "Invalid Date! Start Date ("
                  + input
                  + ") cannot be in the future (Today is "
                  + LocalDate.now()
                  + ").");
          continue;
        }
        startStr = input;
        break;
      } catch (DateTimeParseException e) {
        ConsoleUtil.printError(
            "Invalid Start Date Format! Please use YYYY-MM-DD format (e.g. 2026-08-01).");
      }
    }

    // Step 2: Prompt End Date (Immediate validation loop)
    while (true) {
      String input =
          reportView.promptCustomDateStep(
              "2 of 2: End Date",
              "YYYY-MM-DD (e.g. 2026-08-16)",
              "Enter End Date [or 'C' to Cancel]: ",
              "Start Date set to " + startStr);
      if (input == null) return false;
      try {
        LocalDate end = LocalDate.parse(input, parseFmt);
        if (end.isAfter(LocalDate.now())) {
          ConsoleUtil.printError(
              "Invalid Date! End Date ("
                  + input
                  + ") cannot be in the future (Today is "
                  + LocalDate.now()
                  + ").");
          continue;
        }
        if (start == null || end.isBefore(start)) {
          ConsoleUtil.printError(
              "Invalid Range! End Date ("
                  + input
                  + ") cannot be before Start Date ("
                  + startStr
                  + ").");
          continue;
        }
        state.startDate = start.atStartOfDay();
        if (end.equals(LocalDate.now())) {
          state.customEndIsToday = true;
          state.endDate = LocalDateTime.now();
        } else {
          state.customEndIsToday = false;
          state.endDate = end.atTime(23, 59);
        }
        state.datePresetLabel = startStr.equals(input) ? startStr : (startStr + " to " + input);
        return true;
      } catch (DateTimeParseException e) {
        ConsoleUtil.printError(
            "Invalid End Date Format! Please use YYYY-MM-DD format (e.g. 2026-08-16).");
      }
    }
  }

  private boolean handleCustomDateTimeRangeSubmenu(ReportFilterState state) {
    DateTimeFormatter parseFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    LocalDateTime start = null;
    String startStr = null;

    // Step 1: Prompt Start Date-Time (Immediate validation loop)
    while (true) {
      String input =
          reportView.promptCustomDateTimeStep(
              "1 of 2: Start Date-Time",
              "YYYY-MM-DD HH:mm (e.g. 2026-08-16 08:00)",
              "Enter Start Date-Time [or 'C' to Cancel]: ",
              null);
      if (input == null) return false;
      try {
        start = LocalDateTime.parse(input, parseFmt);
        if (start.isAfter(LocalDateTime.now())) {
          ConsoleUtil.printError(
              "Invalid Date-Time! Start Date-Time (" + input + ") cannot be in the future.");
          continue;
        }
        startStr = input;
        break;
      } catch (DateTimeParseException e) {
        ConsoleUtil.printError(
            "Invalid Start Date-Time Format! Please use YYYY-MM-DD HH:mm format (e.g. 2026-08-16"
                + " 08:00).");
      }
    }

    // Step 2: Prompt End Date-Time (Immediate validation loop)
    while (true) {
      String input =
          reportView.promptCustomDateTimeStep(
              "2 of 2: End Date-Time",
              "YYYY-MM-DD HH:mm (e.g. 2026-08-16 18:00)",
              "Enter End Date-Time [or 'C' to Cancel]: ",
              "Start Date-Time set to " + startStr);
      if (input == null) return false;
      try {
        LocalDateTime end = LocalDateTime.parse(input, parseFmt);
        if (end.isAfter(LocalDateTime.now())) {
          ConsoleUtil.printError(
              "Invalid Date-Time! End Date-Time (" + input + ") cannot be in the future.");
          continue;
        }
        if (start == null || !end.isAfter(start)) {
          ConsoleUtil.printError(
              "Invalid Range! End Date-Time ("
                  + input
                  + ") must be AFTER Start Date-Time ("
                  + startStr
                  + ").");
          continue;
        }
        state.startDate = start;
        state.endDate = end;
        state.datePresetLabel =
            state.startDate.format(displayFmt) + " to " + state.endDate.format(displayFmt);
        return true;
      } catch (DateTimeParseException e) {
        ConsoleUtil.printError(
            "Invalid End Date-Time Format! Please use YYYY-MM-DD HH:mm format (e.g. 2026-08-16"
                + " 18:00).");
      }
    }
  }

  private void handleSortOptionsSubmenu(ReportFilterState state) {
    while (true) {
      try {
        GetMenuInputResult action =
            reportView.displaySortOptionsSubmenu(state.sortAttribute, state.sortDirection);
        int choice = action.getAsInt();
        if (choice == 1) {
          state.sortAttribute = handleSortAttrSubmenu(state.sortAttribute);
        } else if (choice == 2) {
          state.sortDirection = handleSortDirSubmenu(state.sortDirection);
        } else if (choice == 3) {
          return; // Back to Report Generation Configuration Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchSubmenu(String currentSearch) {
    while (true) {
      try {
        String input = reportView.promptSearchInput();
        if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
          return null;
        }
        return input.trim();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleTierSubmenu(String currentTier) {
    while (true) {
      GetMenuInputResult res = reportView.displayTierFilterSubmenu(currentTier);
      int choice = res.getAsInt();
      if (choice == 1) return "DIAMOND";
      if (choice == 2) return "GOLD";
      if (choice == 3) return "SILVER";
      if (choice == 4) return null;
      if (choice == 5) return currentTier;
    }
  }

  private String handleRoomTypeSubmenu(String currentRoom) {
    while (true) {
      GetMenuInputResult res = reportView.displayRoomTypeFilterSubmenu(currentRoom);
      int choice = res.getAsInt();
      if (choice == 1) return "LUXURY";
      if (choice == 2) return "SUITE";
      if (choice == 3) return "STANDARD";
      if (choice == 4) return null;
      if (choice == 5) return currentRoom;
    }
  }

  private String handleBoilingSubmenu(String currentBoiling) {
    while (true) {
      GetMenuInputResult res = reportView.displayBoilingFilterSubmenu(currentBoiling);
      int choice = res.getAsInt();
      if (choice == 1) return "BOILING";
      if (choice == 2) return "NORMAL";
      if (choice == 3) return null;
      if (choice == 4) return currentBoiling;
    }
  }

  private String handleSortAttrSubmenu(String currentAttr) {
    while (true) {
      GetMenuInputResult res = reportView.displaySortAttrSubmenu(currentAttr);
      int choice = res.getAsInt();
      if (choice == 1) return "PHYSICAL WAIT TIME";
      if (choice == 2) return "PRIORITY SCORE";
      if (choice == 3) return "STRIKE COUNT";
      if (choice == 4) return "GUEST NAME";
      if (choice == 5) return currentAttr;
    }
  }

  private String handleSortDirSubmenu(String currentDir) {
    while (true) {
      GetMenuInputResult res = reportView.displaySortDirSubmenu(currentDir);
      int choice = res.getAsInt();
      if (choice == 1) return "DESCENDING";
      if (choice == 2) return "ASCENDING";
      if (choice == 3) return currentDir;
    }
  }

  private int handleRecordLimitSubmenu(int currentLimit) {
    while (true) {
      GetMenuInputResult res = reportView.displayRecordLimitSubmenu(currentLimit);
      int choice = res.getAsInt();
      if (choice == 1) return 10;
      if (choice == 2) return 20;
      if (choice == 3) return 50;
      if (choice == 4) {
        Integer custom = promptCustomRecordLimit();
        return (custom == null) ? currentLimit : custom;
      }
      if (choice == 5) return 0; // 0 = Show All (Unlimited)
      if (choice == 6) return currentLimit;
    }
  }

  private Integer promptCustomRecordLimit() {
    while (true) {
      try {
        return reportView.promptCustomRecordLimit();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Reservation> filterAndSortList(
      ListInterface<Reservation> source,
      String search,
      String tier,
      String roomType,
      String boiling,
      LocalDateTime startDate,
      LocalDateTime endDate,
      String sortAttr,
      String sortDir,
      int reportType) {

    if (source == null || source.isEmpty()) return new ArrayList<>();

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      // Report 3 is Room Holding Bay & Grace Window Audit: strictly include holding
      // bay records
      if (reportType == 3) {
        boolean enteredHoldingBay =
            r.getAllocatedTime() != null
                || r.getStatus() == Reservation.Status.ALLOCATED
                || r.getStatus() == Reservation.Status.NO_SHOW
                || r.getStatus() == Reservation.Status.CHECKED_IN;
        if (!enteredHoldingBay) {
          continue;
        }
      }

      Guest g = guestRepo.findById(r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchSearch = true;
      boolean matchTier = true;
      boolean matchRoom = true;
      boolean matchBoiling = true;
      boolean matchDate = true;

      if (startDate != null || endDate != null) {
        LocalDateTime resTime =
            (r.getAllocatedTime() != null) ? r.getAllocatedTime() : r.getQueueArrivalTime();
        if (resTime != null) {
          if (startDate != null && resTime.isBefore(startDate)) matchDate = false;
          if (endDate != null && resTime.isAfter(endDate)) matchDate = false;
        }
      }

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean mRes =
            r.getReservationId() != null && r.getReservationId().toLowerCase().contains(query);
        boolean mConf =
            r.getConfirmationNumber() != null
                && r.getConfirmationNumber().toLowerCase().contains(query);
        boolean mName =
            g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
        boolean mPhone =
            g != null
                && g.getPhoneNumber() != null
                && g.getPhoneNumber().toLowerCase().contains(query);
        matchSearch = mRes || mConf || mName || mPhone;
      }

      if (tier != null) {
        String actualTier = (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER";
        matchTier = tier.equalsIgnoreCase(actualTier);
      }

      if (roomType != null) {
        String actualRoom = (r.getRoomType() != null) ? r.getRoomType().name() : "";
        matchRoom = roomType.equalsIgnoreCase(actualRoom);
      }

      if (boiling != null) {
        if ("BOILING".equalsIgnoreCase(boiling)) matchBoiling = r.getIsBoiling();
        else if ("NORMAL".equalsIgnoreCase(boiling)) matchBoiling = !r.getIsBoiling();
      }

      if (matchSearch && matchTier && matchRoom && matchBoiling && matchDate) {
        filtered.add(r);
      }
    }

    boolean isAsc = "ASCENDING".equalsIgnoreCase(sortDir);

    if ("PRIORITY SCORE".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            int cmp =
                isAsc
                    ? Integer.compare(r1.getPriorityScore(), r2.getPriorityScore())
                    : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
            if (cmp != 0) return cmp;
            return r1.getReservationId().compareTo(r2.getReservationId());
          });
    } else if ("STRIKE COUNT".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;

            int cmp = isAsc ? Integer.compare(s1, s2) : Integer.compare(s2, s1);
            if (cmp != 0) return cmp;
            return r1.getReservationId().compareTo(r2.getReservationId());
          });
    } else if ("GUEST NAME".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";

            int cmp = isAsc ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
            if (cmp != 0) return cmp;
            return r1.getReservationId().compareTo(r2.getReservationId());
          });
    } else {
      // Default: PHYSICAL WAIT TIME
      filtered.sort(
          (r1, r2) -> {
            int cmp =
                isAsc
                    ? r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime())
                    : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
            if (cmp != 0) return cmp;
            return r1.getReservationId().compareTo(r2.getReservationId());
          });
    }

    return filtered;
  }

  private String buildScopeString(
      String search,
      String tier,
      String room,
      String boiling,
      LocalDateTime startDate,
      LocalDateTime endDate,
      String datePresetLabel) {
    StringBuilder sb = new StringBuilder();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    String dateRangeStr;
    if (startDate == null && endDate == null) {
      dateRangeStr = "ALL TIME";
    } else if (startDate == null) {
      dateRangeStr = "Up to " + endDate.format(fmt);
    } else if (endDate == null) {
      dateRangeStr = "From " + startDate.format(fmt);
    } else {
      dateRangeStr = startDate.format(fmt) + " to " + endDate.format(fmt);
    }

    sb.append("  - Date Range      : [ ").append(dateRangeStr).append(" ]\n");
    sb.append("  - Membership Tier : ").append(tier == null ? "All Tiers" : tier).append("\n");
    sb.append("  - Room Queue Type : ").append(room == null ? "All Room Types" : room).append("\n");
    sb.append("  - Boiling Status  : ").append(boiling == null ? "All Boiling States" : boiling);

    if (search != null && !search.trim().isEmpty()) {
      sb.append("\n  - Search Query    : \"").append(search).append("\"");
    }

    return sb.toString();
  }
}
