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

  private void manageReportPipeline(int reportType) {
    String searchQuery = null;
    String tierFilter = null;
    String roomTypeFilter = null;
    String boilingFilter = null;
    String sortAttribute = (reportType == 2) ? "STRIKE COUNT" : "PHYSICAL WAIT TIME";
    String sortDirection = "DESCENDING";
    int recordLimit = 10; // Default: Top 10 Records

    String reportTitle =
        (reportType == 1)
            ? "Wait Time Efficiency & SLA Attainment Report"
            : (reportType == 2)
                ? "VIP Penalty & Eviction Audit Report"
                : "Room Holding Bay & Grace Window Report";

    while (true) {
      try {
        ListInterface<Reservation> allReservations = vipReservationRepo.getAllReservations();
        ListInterface<Reservation> filteredList =
            filterAndSortList(
                allReservations,
                searchQuery,
                tierFilter,
                roomTypeFilter,
                boilingFilter,
                sortAttribute,
                sortDirection);

        GetMenuInputResult action =
            reportView.displayFilterControlPanel(
                reportTitle,
                searchQuery,
                tierFilter,
                roomTypeFilter,
                boilingFilter,
                sortAttribute,
                sortDirection,
                recordLimit);

        int choice = action.getAsInt();

        if (choice == 1) {
          searchQuery = handleSearchSubmenu(searchQuery);
        } else if (choice == 2) {
          tierFilter = handleTierSubmenu(tierFilter);
        } else if (choice == 3) {
          roomTypeFilter = handleRoomTypeSubmenu(roomTypeFilter);
        } else if (choice == 4) {
          boilingFilter = handleBoilingSubmenu(boilingFilter);
        } else if (choice == 5) {
          sortAttribute = handleSortAttrSubmenu(sortAttribute);
          sortDirection = handleSortDirSubmenu(sortDirection);
        } else if (choice == 6) {
          recordLimit = handleRecordLimitSubmenu(recordLimit);
        } else if (choice == 7) {
          // Reset to defaults
          searchQuery = null;
          tierFilter = null;
          roomTypeFilter = null;
          boilingFilter = null;
          sortAttribute = (reportType == 2) ? "STRIKE COUNT" : "PHYSICAL WAIT TIME";
          sortDirection = "DESCENDING";
          recordLimit = 10;
        } else if (choice == 8) {
          // Generate Report Now!
          String scopeStr =
              buildScopeString(searchQuery, tierFilter, roomTypeFilter, boilingFilter);
          String sortStr = sortAttribute + " (" + sortDirection + ")";

          int confirmChoice =
              promptExecutionConfirmation(
                  reportTitle, scopeStr, sortStr, filteredList.getNumberOfEntries(), recordLimit);

          if (confirmChoice == 1) {
            boolean exitToHub =
                renderGeneratedReportLoop(reportType, filteredList, scopeStr, sortStr, recordLimit);
            if (exitToHub) {
              return; // Cleanly exit back to Analytics Hub!
            }
          } else if (confirmChoice == 3) {
            return; // Exit to Hub
          }
        } else if (choice == 9) {
          return; // Back to Hub
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptExecutionConfirmation(
      String reportTitle, String scopeStr, String sortStr, int totalMatches, int limit) {
    while (true) {
      try {
        return reportView.displayExecutionConfirmationScreen(
            reportTitle, scopeStr, sortStr, totalMatches, limit);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean renderGeneratedReportLoop(
      int reportType,
      ListInterface<Reservation> matchedList,
      String scopeStr,
      String sortStr,
      int recordLimit) {

    while (true) {
      try {
        VipSystemConfig config = configRepo.getConfig();
        GetMenuInputResult result;

        ConsoleUtil.clearBuffer();
        ConsoleUtil.startRecording();

        if (reportType == 1) {
          result =
              reportView.renderSlaReportScreen(
                  matchedList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  recordLimit);
        } else if (reportType == 2) {
          result =
              reportView.renderPenaltyReportScreen(
                  matchedList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  recordLimit);
        } else {
          result =
              reportView.renderHoldingReportScreen(
                  matchedList,
                  guestRepo.getGuestList(),
                  memberRepo.getMemberList(),
                  config,
                  scopeStr,
                  sortStr,
                  recordLimit);
        }

        String capturedReportText = ConsoleUtil.getCapturedString();

        if ("Q".equalsIgnoreCase(result.input)) {
          return true; // Return true to signal Quit to Analytics Hub!
        } else if ("S".equalsIgnoreCase(result.input)) {
          return false; // Return false to re-open Filter Control Panel!
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Refresh live view
        } else if ("E".equalsIgnoreCase(result.input)) {

          String filePrefix = "unknown_report";
          String reportTitle = "UNKNOWN REPORT";

          switch (reportType) {
            case 1:
              filePrefix = "vip/sla_report";
              reportTitle = "WAIT TIME EFFICIENCY & SLA ATTAINMENT AUDIT REPORT";
              break;
            case 2:
              filePrefix = "vip/penalty_report";
              reportTitle = "VIP PENALTY & EVICTION AUDIT REPORT";
              break;
            case 3:
              filePrefix = "vip/holding_report";
              reportTitle = "ROOM HOLDING BAY & GRACE WINDOW AUDIT REPORT";
              break;
            default:
              throw new IllegalArgumentException("Invalid report type: " + reportType);
          }

          String exportedPath =
              TxtExportUtil.export(filePrefix, reportTitle + "\n" + capturedReportText);
          reportView.displayExportSuccessScreen(exportedPath);
        }
      } catch (Exception e) {
        ConsoleUtil.stopRecording();
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
      String sortAttr,
      String sortDir) {

    if (source == null || source.isEmpty()) return new ArrayList<>();

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchSearch = true;
      boolean matchTier = true;
      boolean matchRoom = true;
      boolean matchBoiling = true;

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

      if (matchSearch && matchTier && matchRoom && matchBoiling) {
        filtered.add(r);
      }
    }

    boolean isAsc = "ASCENDING".equalsIgnoreCase(sortDir);

    if ("PRIORITY SCORE".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) ->
              isAsc
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
          (r1, r2) ->
              isAsc
                  ? r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime())
                  : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime()));
    }

    return filtered;
  }

  private String buildScopeString(String search, String tier, String room, String boiling) {
    StringBuilder sb = new StringBuilder();
    sb.append(tier == null ? "All Tiers" : tier).append(" | ");
    sb.append(room == null ? "All Room Types" : room).append(" | ");
    sb.append(boiling == null ? "All Boiling States" : boiling);
    if (search != null) {
      sb.append(" | Search: \"").append(search).append("\"");
    }
    return sb.toString();
  }
}
