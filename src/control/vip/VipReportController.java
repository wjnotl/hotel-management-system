package control.vip;

import adt.LinkedList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.VipSystemConfig;
import java.time.Duration;
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

    boolean generateSelected = handleFilterControlPanel(reportTitle, state, reportType);
    if (!generateSelected) {
      return;
    }

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
        GetMenuInputResult result;

        reportView.displayReportTitleHeader(
            (reportType == 1)
                ? "REPORT 1: WAIT TIME EFFICIENCY & SLA ATTAINMENT AUDIT"
                : (reportType == 2)
                    ? "REPORT 2: VIP PENALTY & EVICTION AUDIT REPORT"
                    : "REPORT 3: ROOM HOLDING BAY & GRACE WINDOW AUDIT");

        ConsoleUtil.clearBuffer();
        ConsoleUtil.startRecording();

        if (reportType == 1) {
          VipReportView.SlaReportDTO dto =
              buildSlaReportDTO(filteredList, state.recordLimit, state.endDate);
          reportView.renderSlaReportBody(dto, scopeStr, sortStr, state.recordLimit);
        } else if (reportType == 2) {
          VipReportView.PenaltyReportDTO dto =
              buildPenaltyReportDTO(filteredList, state.recordLimit);
          reportView.renderPenaltyReportBody(dto, scopeStr, sortStr, state.recordLimit);
        } else {
          VipReportView.HoldingReportDTO dto =
              buildHoldingReportDTO(filteredList, state.recordLimit, state.endDate);
          reportView.renderHoldingReportBody(dto, scopeStr, sortStr, state.recordLimit);
        }

        ConsoleUtil.stopRecording();
        String capturedReportText = ConsoleUtil.getCapturedString();

        result = reportView.promptReportActionMenu();

        if ("Q".equalsIgnoreCase(result.input)) {
          return;
        } else if ("B".equalsIgnoreCase(result.input)) {
          boolean generateSelected = handleFilterControlPanel(reportTitle, state, reportType);
          if (!generateSelected) {
            return;
          }
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Refresh
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
          return true;
        } else if (choice == 2) {
          handleEditFiltersSubmenu(state);
        } else if (choice == 3) {
          handleSortOptionsSubmenu(state);
        } else if (choice == 4) {
          state.recordLimit = handleRecordLimitSubmenu(state.recordLimit);
        } else if (choice == 5) {
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
          return false; // Back
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

    // Prompt Start Date
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

    // Prompt End Date
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

    // Prompt Start Date-Time
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

    // Prompt End Date-Time
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

    if (source == null || source.isEmpty()) return new LinkedList<>();

    ListInterface<Reservation> filtered =
        source.filter(
            reservation -> {
              if (reservation == null) return false;

              // Report 3 is Room Holding Bay & Grace Window Audit
              // only include those that have been entered into the holding bay
              if (reportType == 3) {
                boolean enteredHoldingBay =
                    reservation.getAllocatedTime() != null
                        || reservation.getStatus() == Reservation.Status.ALLOCATED
                        || reservation.getStatus() == Reservation.Status.NO_SHOW
                        || reservation.getStatus() == Reservation.Status.CHECKED_IN
                        || reservation.getStatus() == Reservation.Status.CHECKED_OUT;
                if (!enteredHoldingBay) {
                  return false;
                }
              }

              Guest g = guestRepo.findById(reservation.getGuestId());
              Member m =
                  (g != null && g.getMemberId() != null)
                      ? memberRepo.findById(g.getMemberId())
                      : null;

              boolean matchSearch = true;
              boolean matchTier = true;
              boolean matchRoom = true;
              boolean matchBoiling = true;
              boolean matchDate = true;

              if (startDate != null || endDate != null) {
                LocalDateTime resTime =
                    (reservation.getAllocatedTime() != null)
                        ? reservation.getAllocatedTime()
                        : reservation.getQueueArrivalTime();
                if (resTime != null) {
                  if (startDate != null && resTime.isBefore(startDate)) matchDate = false;
                  if (endDate != null && resTime.isAfter(endDate)) matchDate = false;
                }
              }

              if (search != null && !search.trim().isEmpty()) {
                String query = search.trim().toLowerCase();
                boolean mRes =
                    reservation.getReservationId() != null
                        && reservation.getReservationId().toLowerCase().contains(query);
                boolean mConf =
                    reservation.getConfirmationNumber() != null
                        && reservation.getConfirmationNumber().toLowerCase().contains(query);
                boolean mName =
                    g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
                boolean mPhone =
                    g != null
                        && g.getPhoneNumber() != null
                        && g.getPhoneNumber().toLowerCase().contains(query);
                matchSearch = mRes || mConf || mName || mPhone;
              }

              if (tier != null) {
                String actualTier =
                    (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER";
                matchTier = tier.equalsIgnoreCase(actualTier);
              }

              if (roomType != null) {
                String actualRoom =
                    (reservation.getRoomType() != null) ? reservation.getRoomType().name() : "";
                matchRoom = roomType.equalsIgnoreCase(actualRoom);
              }

              if (boiling != null) {
                if ("BOILING".equalsIgnoreCase(boiling)) matchBoiling = reservation.getIsBoiling();
                else if ("NORMAL".equalsIgnoreCase(boiling))
                  matchBoiling = !reservation.getIsBoiling();
              }

              return matchSearch && matchTier && matchRoom && matchBoiling && matchDate;
            });

    boolean isAsc = "ASCENDING".equalsIgnoreCase(sortDir);

    if ("PRIORITY SCORE".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            int cmp =
                isAsc
                    ? Integer.compare(r1.getPriorityScore(), r2.getPriorityScore())
                    : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
            if (cmp != 0) return cmp;
            String id1 = (r1.getReservationId() != null) ? r1.getReservationId() : "";
            String id2 = (r2.getReservationId() != null) ? r2.getReservationId() : "";
            return id1.compareToIgnoreCase(id2);
          });
    } else if ("STRIKE COUNT".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            Guest g1 = (r1.getGuestId() != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2.getGuestId() != null) ? guestRepo.findById(r2.getGuestId()) : null;
            int s1 =
                (r1.getStrikeCountSnapshot() != null)
                    ? r1.getStrikeCountSnapshot()
                    : ((g1 != null) ? g1.getStrikeCount() : 0);
            int s2 =
                (r2.getStrikeCountSnapshot() != null)
                    ? r2.getStrikeCountSnapshot()
                    : ((g2 != null) ? g2.getStrikeCount() : 0);

            int cmp = isAsc ? Integer.compare(s1, s2) : Integer.compare(s2, s1);
            if (cmp != 0) return cmp;
            String id1 = (r1.getReservationId() != null) ? r1.getReservationId() : "";
            String id2 = (r2.getReservationId() != null) ? r2.getReservationId() : "";
            return id1.compareToIgnoreCase(id2);
          });
    } else if ("GUEST NAME".equalsIgnoreCase(sortAttr)) {
      filtered.sort(
          (r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            Guest g1 = (r1.getGuestId() != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2.getGuestId() != null) ? guestRepo.findById(r2.getGuestId()) : null;
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";

            int cmp = isAsc ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
            if (cmp != 0) return cmp;
            String id1 = (r1.getReservationId() != null) ? r1.getReservationId() : "";
            String id2 = (r2.getReservationId() != null) ? r2.getReservationId() : "";
            return id1.compareToIgnoreCase(id2);
          });
    } else {
      filtered.sort(
          (r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            LocalDateTime t1 = r1.getQueueArrivalTime();
            LocalDateTime t2 = r2.getQueueArrivalTime();
            int cmp;
            if (t1 == null && t2 == null) cmp = 0;
            else if (t1 == null) cmp = 1;
            else if (t2 == null) cmp = -1;
            else cmp = isAsc ? t2.compareTo(t1) : t1.compareTo(t2);

            if (cmp != 0) return cmp;
            String id1 = (r1.getReservationId() != null) ? r1.getReservationId() : "";
            String id2 = (r2.getReservationId() != null) ? r2.getReservationId() : "";
            return id1.compareToIgnoreCase(id2);
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

  private VipReportView.SlaReportDTO buildSlaReportDTO(
      ListInterface<Reservation> filteredList, int recordLimit, LocalDateTime maxCutoff) {
    if (filteredList == null) {
      return new VipReportView.SlaReportDTO(
          new LinkedList<>(),
          new VipReportView.SlaReportSummaryDTO(
              0, 0, 0, 100.0, 100.0, 0, 0, 0, 100.0, 100.0, 0, 0, 0, 100.0, 100.0),
          0);
    }

    int totalMatches = filteredList.getNumberOfEntries();
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<Guest> guestList = guestRepo.getGuestList();
    ListInterface<Member> memberList = memberRepo.getMemberList();

    ListInterface<VipReportView.SlaReportRowDTO> rows = new LinkedList<>();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    ListInterface<Reservation> displayList =
        (totalMatches > 0 && displayCount > 0)
            ? filteredList.slice(1, displayCount)
            : new LinkedList<>();

    int diamondTotal = 0, goldTotal = 0, silverTotal = 0;
    int diamondSlaMet = 0, goldSlaMet = 0, silverSlaMet = 0;

    for (Reservation reservation : filteredList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;
      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

      long wait = calculateWaitMins(reservation, maxCutoff);
      int targetMins = config.getPatienceLimitMins(tier);

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

    int rank = 1;
    for (Reservation reservation : displayList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;

      long wait = calculateWaitMins(reservation, maxCutoff);
      String rankStr = rank + ".";
      String name = (guest != null) ? guest.getName() : "N/A";
      String tierStr =
          (member != null && member.getTier() != null) ? member.getTier().name() : "NON-MEMBER";
      String room = (reservation.getRoomType() != null) ? reservation.getRoomType().name() : "N/A";
      int strikes = (guest != null) ? guest.getStrikeCount() : 0;
      String status = (reservation.getStatus() != null) ? reservation.getStatus().name() : "N/A";

      String resId =
          (reservation.getReservationId() != null) ? reservation.getReservationId() : "N/A";

      rows.add(
          new VipReportView.SlaReportRowDTO(
              rankStr, resId, name, tierStr, room, wait, strikes, status));
      rank++;
    }

    double dPct = (diamondTotal == 0) ? 100.0 : ((double) diamondSlaMet / diamondTotal) * 100.0;
    double gPct = (goldTotal == 0) ? 100.0 : ((double) goldSlaMet / goldTotal) * 100.0;
    double sPct = (silverTotal == 0) ? 100.0 : ((double) silverSlaMet / silverTotal) * 100.0;

    VipReportView.SlaReportSummaryDTO summary =
        new VipReportView.SlaReportSummaryDTO(
            diamondTotal,
            diamondSlaMet,
            config.getDiamondPatienceLimitMins(),
            dPct,
            config.getDiamondSlaTargetPct(),
            goldTotal,
            goldSlaMet,
            config.getGoldPatienceLimitMins(),
            gPct,
            config.getGoldSlaTargetPct(),
            silverTotal,
            silverSlaMet,
            config.getSilverPatienceLimitMins(),
            sPct,
            config.getSilverSlaTargetPct());

    return new VipReportView.SlaReportDTO(rows, summary, totalMatches);
  }

  private VipReportView.PenaltyReportDTO buildPenaltyReportDTO(
      ListInterface<Reservation> filteredList, int recordLimit) {
    if (filteredList == null) {
      return new VipReportView.PenaltyReportDTO(
          new LinkedList<>(),
          new VipReportView.PenaltyReportSummaryDTO(
              0, 0, 0, 0.0, 0.0, 0, 0, 0.0, 0.0, 0, 0, 0.0, 0.0),
          0);
    }

    int totalMatches = filteredList.getNumberOfEntries();
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<Guest> guestList = guestRepo.getGuestList();
    ListInterface<Member> memberList = memberRepo.getMemberList();

    ListInterface<VipReportView.PenaltyReportRowDTO> rows = new LinkedList<>();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    ListInterface<Reservation> displayList =
        (totalMatches > 0 && displayCount > 0)
            ? filteredList.slice(1, displayCount)
            : new LinkedList<>();

    int totalStrikes =
        filteredList.reduce(
            0,
            (sum, res) -> {
              if (res == null) return sum;
              Guest g =
                  guestList.find(guest -> guest.getGuestId().equalsIgnoreCase(res.getGuestId()));
              int strikes =
                  (res.getStrikeCountSnapshot() != null)
                      ? res.getStrikeCountSnapshot()
                      : ((g != null) ? g.getStrikeCount() : 0);
              return sum + strikes;
            });

    int dTotal = 0, gTotal = 0, sTotal = 0;
    int dEvicted = 0, gEvicted = 0, sEvicted = 0;

    for (Reservation reservation : filteredList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;
      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

      int strikes =
          (reservation.getStrikeCountSnapshot() != null)
              ? reservation.getStrikeCountSnapshot()
              : ((guest != null) ? guest.getStrikeCount() : 0);
      boolean isEvicted = strikes > config.getMaxStrikes(tier);

      if (tier == Member.LoyaltyTier.DIAMOND) {
        dTotal++;
        if (isEvicted) dEvicted++;
      } else if (tier == Member.LoyaltyTier.GOLD) {
        gTotal++;
        if (isEvicted) gEvicted++;
      } else {
        sTotal++;
        if (isEvicted) sEvicted++;
      }
    }

    int rank = 1;
    for (Reservation reservation : displayList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;
      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

      int strikes =
          (reservation.getStrikeCountSnapshot() != null)
              ? reservation.getStrikeCountSnapshot()
              : ((guest != null) ? guest.getStrikeCount() : 0);
      boolean isEvicted = strikes > config.getMaxStrikes(tier);

      String rankStr = rank + ".";
      String name = (guest != null) ? guest.getName() : "N/A";
      String tierStr =
          (member != null && member.getTier() != null) ? member.getTier().name() : "NON-MEMBER";
      String statusStr = (reservation.getStatus() != null) ? reservation.getStatus().name() : "N/A";
      String evictedStr = isEvicted ? "YES" : "NO";

      String resId =
          (reservation.getReservationId() != null) ? reservation.getReservationId() : "N/A";

      rows.add(
          new VipReportView.PenaltyReportRowDTO(
              rankStr, resId, name, tierStr, strikes, statusStr, evictedStr));
      rank++;
    }

    double dRate = (dTotal == 0) ? 0.0 : ((double) dEvicted / dTotal) * 100.0;
    double gRate = (gTotal == 0) ? 0.0 : ((double) gEvicted / gTotal) * 100.0;
    double sRate = (sTotal == 0) ? 0.0 : ((double) sEvicted / sTotal) * 100.0;

    VipReportView.PenaltyReportSummaryDTO summary =
        new VipReportView.PenaltyReportSummaryDTO(
            totalStrikes,
            dTotal,
            dEvicted,
            dRate,
            config.getDiamondEvictionRateTargetPct(),
            gTotal,
            gEvicted,
            gRate,
            config.getGoldEvictionRateTargetPct(),
            sTotal,
            sEvicted,
            sRate,
            config.getSilverEvictionRateTargetPct());

    return new VipReportView.PenaltyReportDTO(rows, summary, totalMatches);
  }

  private VipReportView.HoldingReportDTO buildHoldingReportDTO(
      ListInterface<Reservation> filteredList, int recordLimit, LocalDateTime maxCutoff) {
    if (filteredList == null) {
      return new VipReportView.HoldingReportDTO(
          new LinkedList<>(),
          new VipReportView.HoldingReportSummaryDTO(
              0, 0, 0, 0.0, 0.0, 0, 0, 0.0, 0.0, 0, 0, 0.0, 0.0),
          0);
    }

    int totalMatches = filteredList.getNumberOfEntries();
    VipSystemConfig config = configRepo.getConfig();
    ListInterface<Guest> guestList = guestRepo.getGuestList();
    ListInterface<Member> memberList = memberRepo.getMemberList();

    ListInterface<VipReportView.HoldingReportRowDTO> rows = new LinkedList<>();
    int displayCount =
        (recordLimit == 0 || recordLimit >= totalMatches) ? totalMatches : recordLimit;

    ListInterface<Reservation> displayList =
        (totalMatches > 0 && displayCount > 0)
            ? filteredList.slice(1, displayCount)
            : new LinkedList<>();

    int dCount = 0, gCount = 0, sCount = 0;
    double dUtilSum = 0.0, gUtilSum = 0.0, sUtilSum = 0.0;

    for (Reservation reservation : filteredList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;
      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

      String pctStr = calculateGraceUsedPctStr(reservation, member, config, maxCutoff);
      double pct = 0.0;
      try {
        pct = Double.parseDouble(pctStr);
      } catch (Exception ignored) {
      }

      if (tier == Member.LoyaltyTier.DIAMOND) {
        dCount++;
        dUtilSum += pct;
      } else if (tier == Member.LoyaltyTier.GOLD) {
        gCount++;
        gUtilSum += pct;
      } else {
        sCount++;
        sUtilSum += pct;
      }
    }

    int rank = 1;
    for (Reservation reservation : displayList) {
      if (reservation == null) continue;

      Guest guest = guestList.find(g -> g.getGuestId().equalsIgnoreCase(reservation.getGuestId()));
      Member member =
          (guest != null && guest.getMemberId() != null)
              ? memberList.find(m -> m.getMemberId().equalsIgnoreCase(guest.getMemberId()))
              : null;
      Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

      String pctStr = calculateGraceUsedPctStr(reservation, member, config, maxCutoff);
      String rankStr = rank + ".";
      String name = (guest != null) ? guest.getName() : "N/A";
      String tierStr =
          (member != null && member.getTier() != null) ? member.getTier().name() : "NON-MEMBER";

      int allowedGrace =
          (reservation.getAllocatedGraceMins() != null && reservation.getAllocatedGraceMins() > 0)
              ? reservation.getAllocatedGraceMins()
              : config.getGraceWindowMins(tier);

      String timeUsedStr = calculateTimeUsedStr(reservation, member, config, maxCutoff);
      String status = (reservation.getStatus() != null) ? reservation.getStatus().name() : "N/A";

      String resId =
          (reservation.getReservationId() != null) ? reservation.getReservationId() : "N/A";

      rows.add(
          new VipReportView.HoldingReportRowDTO(
              rankStr, resId, name, tierStr, allowedGrace, timeUsedStr, status, pctStr));
      rank++;
    }

    double dAvgUtil = (dCount == 0) ? 0.0 : dUtilSum / dCount;
    double gAvgUtil = (gCount == 0) ? 0.0 : gUtilSum / gCount;
    double sAvgUtil = (sCount == 0) ? 0.0 : sUtilSum / sCount;

    VipReportView.HoldingReportSummaryDTO summary =
        new VipReportView.HoldingReportSummaryDTO(
            totalMatches,
            dCount,
            config.getDiamondGraceWindowMins(),
            dAvgUtil,
            config.getDiamondGraceUtilTargetPct(),
            gCount,
            config.getGoldGraceWindowMins(),
            gAvgUtil,
            config.getGoldGraceUtilTargetPct(),
            sCount,
            config.getSilverGraceWindowMins(),
            sAvgUtil,
            config.getSilverGraceUtilTargetPct());

    return new VipReportView.HoldingReportDTO(rows, summary, totalMatches);
  }

  private long calculateWaitMins(Reservation r, LocalDateTime maxCutoff) {
    if (r == null || r.getQueueArrivalTime() == null) return 0;
    LocalDateTime endTime = r.getAllocatedTime();
    if (endTime == null) {
      endTime =
          (maxCutoff != null && maxCutoff.isBefore(LocalDateTime.now()))
              ? maxCutoff
              : LocalDateTime.now();
    }
    long mins = Duration.between(r.getQueueArrivalTime(), endTime).toMinutes();
    return Math.max(0, mins);
  }

  private String calculateTimeUsedStr(
      Reservation r, Member m, VipSystemConfig config, LocalDateTime maxCutoff) {
    if (r == null) return "N/A";

    int allowedGraceMins =
        (r.getAllocatedGraceMins() != null && r.getAllocatedGraceMins() > 0)
            ? r.getAllocatedGraceMins()
            : config.getGraceWindowMins((m != null) ? m.getTier() : null);

    if (r.getStatus() == Reservation.Status.NO_SHOW) {
      return allowedGraceMins + " Mins";
    }

    LocalDateTime startTime = r.getAllocatedTime();
    if (startTime == null) return "N/A";

    LocalDateTime endTime = resolveHoldingEndTime(r, m, config, maxCutoff);
    long elapsedMins = Duration.between(startTime, endTime).toMinutes();
    if (elapsedMins < 0) elapsedMins = 0;

    long timeUsed = Math.min(elapsedMins, (long) allowedGraceMins);
    return timeUsed + " Mins";
  }

  private String calculateGraceUsedPctStr(
      Reservation r, Member m, VipSystemConfig config, LocalDateTime maxCutoff) {
    if (r == null) return "0.0";

    int allowedGraceMins =
        (r.getAllocatedGraceMins() != null && r.getAllocatedGraceMins() > 0)
            ? r.getAllocatedGraceMins()
            : config.getGraceWindowMins((m != null) ? m.getTier() : null);
    if (allowedGraceMins <= 0) return "0.0";

    if (r.getStatus() == Reservation.Status.NO_SHOW) {
      return "100.0";
    }

    LocalDateTime startTime = r.getAllocatedTime();
    if (startTime == null) return "0.0";

    LocalDateTime endTime = resolveHoldingEndTime(r, m, config, maxCutoff);
    long elapsedMins = Duration.between(startTime, endTime).toMinutes();
    if (elapsedMins < 0) elapsedMins = 0;

    long timeUsed = Math.min(elapsedMins, (long) allowedGraceMins);
    double pct = ((double) timeUsed / allowedGraceMins) * 100.0;
    pct = Math.min(100.0, Math.max(0.0, pct));
    return String.format("%.1f", pct);
  }

  private LocalDateTime resolveHoldingEndTime(
      Reservation r, Member m, VipSystemConfig config, LocalDateTime maxCutoff) {
    if (r == null) {
      return (maxCutoff != null && maxCutoff.isBefore(LocalDateTime.now()))
          ? maxCutoff
          : LocalDateTime.now();
    }
    if (r.getStatus() == Reservation.Status.CHECKED_IN
        || r.getStatus() == Reservation.Status.CHECKED_OUT) {
      if (r.getCheckInTime() != null) {
        return r.getCheckInTime();
      }
    }

    LocalDateTime startTime = r.getAllocatedTime();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoff = (maxCutoff != null && maxCutoff.isBefore(now)) ? maxCutoff : now;

    if (startTime != null && config != null) {
      int allowedGraceMins =
          (r.getAllocatedGraceMins() != null && r.getAllocatedGraceMins() > 0)
              ? r.getAllocatedGraceMins()
              : config.getGraceWindowMins((m != null) ? m.getTier() : null);
      if (allowedGraceMins > 0) {
        LocalDateTime expireTime = startTime.plusMinutes(allowedGraceMins);
        if (cutoff.isAfter(expireTime)) {
          return expireTime;
        }
      }
    }

    return cutoff;
  }
}
