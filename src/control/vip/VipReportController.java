package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
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
    String sortAttribute;
    String sortDirection = "DESCENDING";
    int recordLimit = 10;

    ReportFilterState(int reportType) {
      this.sortAttribute = (reportType == 2) ? "STRIKE COUNT" : "PHYSICAL WAIT TIME";
    }
  }

  private void manageReportPipeline(int reportType) {
    ReportFilterState state = new ReportFilterState(reportType);

    String reportTitle = (reportType == 1)
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
      int reportType,
      String reportTitle,
      ReportFilterState state) {

    while (true) {
      try {
        ListInterface<Reservation> allReservations = vipReservationRepo.getAllReservations();
        ListInterface<Reservation> filteredList = filterAndSortList(
            allReservations,
            state.searchQuery,
            state.tierFilter,
            state.roomTypeFilter,
            state.boilingFilter,
            state.sortAttribute,
            state.sortDirection);

        String scopeStr = buildScopeString(state.searchQuery, state.tierFilter, state.roomTypeFilter,
            state.boilingFilter);
        String sortStr = state.sortAttribute + " (" + state.sortDirection + ")";
        VipSystemConfig config = configRepo.getConfig();
        GetMenuInputResult result;

        ConsoleUtil.clearBuffer();
        ConsoleUtil.startRecording();

        if (reportType == 1) {
          result = reportView.renderSlaReportScreen(
              filteredList,
              guestRepo.getGuestList(),
              memberRepo.getMemberList(),
              config,
              scopeStr,
              sortStr,
              state.recordLimit);
        } else if (reportType == 2) {
          result = reportView.renderPenaltyReportScreen(
              filteredList,
              guestRepo.getGuestList(),
              memberRepo.getMemberList(),
              config,
              scopeStr,
              sortStr,
              state.recordLimit);
        } else {
          result = reportView.renderHoldingReportScreen(
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
          // generateSelected is true -> loop continues & re-renders report with new filters!
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Refresh -> loop continues & re-fetches live data & re-renders report screen directly!
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

          String exportedPath = TxtExportUtil.export(filePrefix, reportTitle.toUpperCase() + "\n" + capturedReportText);
          reportView.displayExportSuccessScreen(exportedPath);
        }
      } catch (Exception e) {
        ConsoleUtil.stopRecording();
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean handleFilterControlPanel(String reportTitle, ReportFilterState state, int reportType) {
    while (true) {
      try {
        GetMenuInputResult action = reportView.displayFilterControlPanel(
            reportTitle,
            state.searchQuery,
            state.tierFilter,
            state.roomTypeFilter,
            state.boilingFilter,
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
        GetMenuInputResult action = reportView.displayEditFiltersSubmenu(
            state.searchQuery, state.tierFilter, state.roomTypeFilter, state.boilingFilter);
        int choice = action.getAsInt();
        if (choice == 1) {
          state.tierFilter = handleTierSubmenu(state.tierFilter);
        } else if (choice == 2) {
          state.boilingFilter = handleBoilingSubmenu(state.boilingFilter);
        } else if (choice == 3) {
          state.roomTypeFilter = handleRoomTypeSubmenu(state.roomTypeFilter);
        } else if (choice == 4) {
          state.searchQuery = handleSearchSubmenu(state.searchQuery);
        } else if (choice == 5) {
          return; // Back to Report Generation Configuration Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleSortOptionsSubmenu(ReportFilterState state) {
    while (true) {
      try {
        GetMenuInputResult action = reportView.displaySortOptionsSubmenu(
            state.sortAttribute, state.sortDirection);
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
      if (choice == 1)
        return "DIAMOND";
      if (choice == 2)
        return "GOLD";
      if (choice == 3)
        return "SILVER";
      if (choice == 4)
        return null;
      if (choice == 5)
        return currentTier;
    }
  }

  private String handleRoomTypeSubmenu(String currentRoom) {
    while (true) {
      GetMenuInputResult res = reportView.displayRoomTypeFilterSubmenu(currentRoom);
      int choice = res.getAsInt();
      if (choice == 1)
        return "LUXURY";
      if (choice == 2)
        return "SUITE";
      if (choice == 3)
        return "STANDARD";
      if (choice == 4)
        return null;
      if (choice == 5)
        return currentRoom;
    }
  }

  private String handleBoilingSubmenu(String currentBoiling) {
    while (true) {
      GetMenuInputResult res = reportView.displayBoilingFilterSubmenu(currentBoiling);
      int choice = res.getAsInt();
      if (choice == 1)
        return "BOILING";
      if (choice == 2)
        return "NORMAL";
      if (choice == 3)
        return null;
      if (choice == 4)
        return currentBoiling;
    }
  }

  private String handleSortAttrSubmenu(String currentAttr) {
    while (true) {
      GetMenuInputResult res = reportView.displaySortAttrSubmenu(currentAttr);
      int choice = res.getAsInt();
      if (choice == 1)
        return "PHYSICAL WAIT TIME";
      if (choice == 2)
        return "PRIORITY SCORE";
      if (choice == 3)
        return "STRIKE COUNT";
      if (choice == 4)
        return "GUEST NAME";
      if (choice == 5)
        return currentAttr;
    }
  }

  private String handleSortDirSubmenu(String currentDir) {
    while (true) {
      GetMenuInputResult res = reportView.displaySortDirSubmenu(currentDir);
      int choice = res.getAsInt();
      if (choice == 1)
        return "DESCENDING";
      if (choice == 2)
        return "ASCENDING";
      if (choice == 3)
        return currentDir;
    }
  }

  private int handleRecordLimitSubmenu(int currentLimit) {
    while (true) {
      GetMenuInputResult res = reportView.displayRecordLimitSubmenu(currentLimit);
      int choice = res.getAsInt();
      if (choice == 1)
        return 10;
      if (choice == 2)
        return 20;
      if (choice == 3)
        return 50;
      if (choice == 4) {
        Integer custom = promptCustomRecordLimit();
        return (custom == null) ? currentLimit : custom;
      }
      if (choice == 5)
        return 0; // 0 = Show All (Unlimited)
      if (choice == 6)
        return currentLimit;
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
      String sortAttr,
      String sortDir) {

    if (source == null || source.isEmpty())
      return new ArrayList<>();

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null)
        continue;

      Guest g = guestRepo.findById(r.getGuestId());
      Member m = (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchSearch = true;
      boolean matchTier = true;
      boolean matchRoom = true;
      boolean matchBoiling = true;

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean mRes = r.getReservationId() != null && r.getReservationId().toLowerCase().contains(query);
        boolean mConf = r.getConfirmationNumber() != null
            && r.getConfirmationNumber().toLowerCase().contains(query);
        boolean mName = g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
        boolean mPhone = g != null
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
        if ("BOILING".equalsIgnoreCase(boiling))
          matchBoiling = r.getIsBoiling();
        else if ("NORMAL".equalsIgnoreCase(boiling))
          matchBoiling = !r.getIsBoiling();
      }

      if (matchSearch && matchTier && matchRoom && matchBoiling) {
        filtered.add(r);
      }
    }

    boolean isAsc = "ASCENDING".equalsIgnoreCase(sortDir);

    if ("PRIORITY SCORE".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> isAsc
              ? Integer.compare(r1.getPriorityScore(), r2.getPriorityScore())
              : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore()));
    } else if ("STRIKE COUNT".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;
            return isAsc ? Integer.compare(s1, s2) : Integer.compare(s2, s1);
          });
    } else if ("GUEST NAME".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return isAsc ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
          });
    } else {
      // Default: PHYSICAL WAIT TIME
      filtered.sort(
          (r1, r2) -> isAsc
              ? r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime())
              : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime()));
    }

    return filtered;
  }

  private String buildScopeString(String search, String tier, String room, String boiling) {
    StringBuilder sb = new StringBuilder("All-Time System Audit | ");
    sb.append(tier == null ? "All Tiers" : tier).append(" | ");
    sb.append(room == null ? "All Room Types" : room).append(" | ");
    sb.append(boiling == null ? "All Boiling States" : boiling);
    if (search != null) {
      sb.append(" | Search: \"").append(search).append("\"");
    }
    return sb.toString();
  }
}
