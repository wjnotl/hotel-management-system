package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.BookingReportView;

public class BookingReportController {
  private static final int ARRIVAL_REGISTER = 1;

  private final BookingReportView reportView = new BookingReportView();
  private final StandardReservationRepo standardReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;

  public BookingReportController(
      StandardReservationRepo standardReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
  }

  public void startReportManagement() {
    while (true) {
      try {
        int choice = reportView.displayReportHubMenu();
        if (choice == 3) {
          return;
        }

        runReportPipeline(choice);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void runReportPipeline(int reportType) {
    boolean isRegister = (reportType == ARRIVAL_REGISTER);

    String search = null;
    String roomTypeFilter = null;
    String statusFilter = null;
    String tierFilter = null;
    String periodFilter = isRegister ? "TODAY" : "ALL TIME";
    Integer minWaitMinutes = null;
    String sortAttribute = isRegister ? "ARRIVAL TIME" : "WAIT DURATION";
    String sortDirection = "DESCENDING";
    int recordLimit = 10;

    String title = isRegister ? "DAILY ARRIVAL REGISTER - FILTERS" : "QUEUE PERFORMANCE - FILTERS";

    while (true) {
      try {
        standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);

        ListInterface<Reservation> matched =
            filterReservations(
                search, roomTypeFilter, statusFilter, tierFilter, periodFilter, minWaitMinutes);

        int choice =
            reportView.displayFilterControlPanel(
                title,
                search,
                roomTypeFilter,
                statusFilter,
                tierFilter,
                periodFilter,
                minWaitMinutes,
                sortAttribute,
                sortDirection,
                recordLimit,
                matched.getNumberOfEntries(),
                isRegister,
                !isRegister);

        if (choice == 1) {
          String input = reportView.promptSearchInput();
          search = (input == null || input.trim().isEmpty()) ? null : input.trim();
        } else if (choice == 2) {
          roomTypeFilter = handleRoomTypeSubmenu(roomTypeFilter);
        } else if (choice == 3) {
          if (isRegister) {
            statusFilter = handleStatusSubmenu(statusFilter);
          } else {
            Integer entered = reportView.promptMinimumWait(minWaitMinutes);
            if (entered != null) {
              minWaitMinutes = (entered == 0) ? null : entered;
            }
          }
        } else if (choice == 4) {
          tierFilter = handleTierSubmenu(tierFilter);
        } else if (choice == 5) {
          periodFilter = handlePeriodSubmenu(periodFilter);
        } else if (choice == 6) {
          sortAttribute = handleSortAttributeSubmenu(sortAttribute, isRegister);
          sortDirection = handleSortDirectionSubmenu(sortDirection);
        } else if (choice == 7) {
          recordLimit = handleRecordLimitSubmenu(recordLimit);
        } else if (choice == 8) {
          search = null;
          roomTypeFilter = null;
          statusFilter = null;
          tierFilter = null;
          periodFilter = isRegister ? "TODAY" : "ALL TIME";
          minWaitMinutes = null;
          sortAttribute = isRegister ? "ARRIVAL TIME" : "WAIT DURATION";
          sortDirection = "DESCENDING";
          recordLimit = 10;
        } else if (choice == 9) {
          boolean exitToHub =
              renderReport(
                  reportType,
                  matched,
                  buildScopeLabel(
                      search,
                      roomTypeFilter,
                      statusFilter,
                      tierFilter,
                      periodFilter,
                      minWaitMinutes),
                  sortAttribute,
                  sortDirection,
                  recordLimit);
          if (exitToHub) {
            return;
          }
        } else if (choice == 10) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean renderReport(
      int reportType,
      ListInterface<Reservation> matched,
      String scope,
      String sortAttribute,
      String sortDirection,
      int recordLimit) {

    while (true) {
      try {
        ListInterface<Reservation> sorted = sortReservations(matched, sortAttribute, sortDirection);
        String sortLabel = sortAttribute + " (" + sortDirection + ")";

        ConsoleUtil.GetMenuInputResult result;
        if (reportType == ARRIVAL_REGISTER) {
          result =
              reportView.renderArrivalRegister(
                  sorted,
                  guestRepo.getGuestList(),
                  scope,
                  sortLabel,
                  recordLimit,
                  buildRoomTypeSubtotals(sorted),
                  "RESERVATION ID".equalsIgnoreCase(sortAttribute)
                      && "ASCENDING".equalsIgnoreCase(sortDirection));
        } else {
          result =
              reportView.renderPerformanceReport(
                  sorted,
                  guestRepo.getGuestList(),
                  scope,
                  sortLabel,
                  recordLimit,
                  buildPerformanceSummary(sorted),
                  buildOverallSummary(sorted));
        }

        if ("E".equalsIgnoreCase(result.input)) {
          return true;
        } else if ("S".equalsIgnoreCase(result.input)) {
          return false;
        } else if ("F".equalsIgnoreCase(result.input)) {
          handleBinarySearch(sorted, sortAttribute, sortDirection);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Binary search is only valid on the key the list is actually ordered by, so the register
  // has to be sorted ascending by reservation ID before this is offered.
  private void handleBinarySearch(
      ListInterface<Reservation> sorted, String sortAttribute, String sortDirection) {

    if (!"RESERVATION ID".equalsIgnoreCase(sortAttribute)
        || !"ASCENDING".equalsIgnoreCase(sortDirection)) {
      ConsoleUtil.printError(
          "Binary search needs the register sorted by RESERVATION ID (ASCENDING). Change the"
              + " sort order first.");
      return;
    }

    String target = reportView.promptReservationIdSearch();
    if (target == null || target.trim().isEmpty()) {
      return;
    }

    String needle = target.trim();
    int low = 1;
    int high = sorted.getNumberOfEntries();
    int comparisons = 0;
    int foundAt = -1;

    while (low <= high) {
      int mid = low + (high - low) / 2;
      comparisons++;

      Reservation candidate = sorted.getEntry(mid);
      // Every id is "STD-" plus a five digit number, so alphabetical order and numeric order
      // are the same and a plain string comparison is safe here.
      int comparison = candidate.getReservationId().compareToIgnoreCase(needle);

      if (comparison == 0) {
        foundAt = mid;
        break;
      } else if (comparison < 0) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    Reservation found = (foundAt == -1) ? null : sorted.getEntry(foundAt);
    Guest guest = (found != null) ? guestRepo.findById(found.getGuestId()) : null;

    reportView.displayBinarySearchResult(
        needle, found, guest, foundAt, comparisons, sorted.getNumberOfEntries());
  }

  private ListInterface<Reservation> filterReservations(
      String search,
      String roomTypeFilter,
      String statusFilter,
      String tierFilter,
      String periodFilter,
      Integer minWaitMinutes) {

    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    ListInterface<Reservation> matched = new ArrayList<>();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || r.getRoomType() == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      if (!matchesSearch(r, g, search)) continue;
      if (roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase(r.getRoomType().name())) {
        continue;
      }
      if (statusFilter != null && !statusFilter.equalsIgnoreCase(r.getStatus().name())) continue;
      if (!matchesTier(m, tierFilter)) continue;
      if (!matchesPeriod(r, periodFilter)) continue;
      if (minWaitMinutes != null && waitMinutesOf(r) < minWaitMinutes) continue;

      matched.add(r);
    }

    return matched;
  }

  private ListInterface<Reservation> sortReservations(
      ListInterface<Reservation> source, String sortAttribute, String sortDirection) {

    ListInterface<Reservation> sorted = new ArrayList<>();
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      sorted.add(source.getEntry(i));
    }

    boolean ascending = "ASCENDING".equalsIgnoreCase(sortDirection);

    if ("RESERVATION ID".equalsIgnoreCase(sortAttribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? a.getReservationId().compareToIgnoreCase(b.getReservationId())
                  : b.getReservationId().compareToIgnoreCase(a.getReservationId()));
    } else if ("ARRIVAL TIME".equalsIgnoreCase(sortAttribute)) {
      sorted.sort((a, b) -> ascending ? compareArrival(a, b) : compareArrival(b, a));
    } else if ("GUEST NAME".equalsIgnoreCase(sortAttribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? guestNameOf(a).compareToIgnoreCase(guestNameOf(b))
                  : guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("ROOM TYPE".equalsIgnoreCase(sortAttribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? a.getRoomType().name().compareTo(b.getRoomType().name())
                  : b.getRoomType().name().compareTo(a.getRoomType().name()));
    } else if ("STRIKE COUNT".equalsIgnoreCase(sortAttribute)) {
      sorted.sort(
          (a, b) ->
              ascending
                  ? Integer.compare(strikesOf(a), strikesOf(b))
                  : Integer.compare(strikesOf(b), strikesOf(a)));
    } else {
      sorted.sort(
          (a, b) ->
              ascending
                  ? Long.compare(waitMinutesOf(a), waitMinutesOf(b))
                  : Long.compare(waitMinutesOf(b), waitMinutesOf(a)));
    }

    return sorted;
  }

  private String[][] buildRoomTypeSubtotals(ListInterface<Reservation> rows) {
    Room.RoomType[] types = Room.RoomType.values();
    String[][] table = new String[types.length][6];

    for (int t = 0; t < types.length; t++) {
      int inScope = 0;
      int inLine = 0;
      int onHold = 0;
      int checkedIn = 0;
      int closed = 0;

      for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
        Reservation r = rows.getEntry(i);
        if (r == null || r.getRoomType() != types[t]) continue;

        inScope++;
        if (r.getStatus() == Reservation.Status.WAITING) inLine++;
        else if (r.getStatus() == Reservation.Status.ALLOCATED) onHold++;
        else if (r.getStatus() == Reservation.Status.CHECKED_IN) checkedIn++;
        else if (r.getStatus() == Reservation.Status.NO_SHOW
            || r.getStatus() == Reservation.Status.CANCELLED) closed++;
      }

      table[t] =
          new String[] {
            types[t].name(),
            String.valueOf(inScope),
            String.valueOf(inLine),
            String.valueOf(onHold),
            String.valueOf(checkedIn),
            String.valueOf(closed)
          };
    }

    return table;
  }

  private String[][] buildPerformanceSummary(ListInterface<Reservation> rows) {
    Room.RoomType[] types = Room.RoomType.values();
    String[][] table = new String[types.length][7];

    for (int t = 0; t < types.length; t++) {
      table[t] = summariseRows(rows, types[t], types[t].name());
    }

    return table;
  }

  private String[] buildOverallSummary(ListInterface<Reservation> rows) {
    return summariseRows(rows, null, "ALL TYPES");
  }

  // A booking counts as an arrival once it has actually stood in a line, so RESERVED bookings
  // that never turned up are left out of the wait and no-show maths entirely.
  private String[] summariseRows(
      ListInterface<Reservation> rows, Room.RoomType roomType, String label) {

    int arrivals = 0;
    int served = 0;
    int noShows = 0;
    long totalWait = 0;
    long longestWait = 0;

    for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
      Reservation r = rows.getEntry(i);
      if (r == null) continue;
      if (roomType != null && r.getRoomType() != roomType) continue;
      if (r.getQueueArrivalTime() == null) continue;

      arrivals++;

      long wait = waitMinutesOf(r);
      totalWait += wait;
      if (wait > longestWait) {
        longestWait = wait;
      }

      if (r.getStatus() == Reservation.Status.CHECKED_IN) served++;
      if (r.getStatus() == Reservation.Status.NO_SHOW) noShows++;
    }

    long averageWait = (arrivals == 0) ? 0 : totalWait / arrivals;
    String noShowRate =
        (arrivals == 0) ? "-" : String.format("%.1f%%", (noShows * 100.0) / arrivals);

    return new String[] {
      label,
      String.valueOf(arrivals),
      formatMinutes(averageWait),
      formatMinutes(longestWait),
      String.valueOf(served),
      String.valueOf(noShows),
      noShowRate
    };
  }

  private String buildScopeLabel(
      String search,
      String roomTypeFilter,
      String statusFilter,
      String tierFilter,
      String periodFilter,
      Integer minWaitMinutes) {

    StringBuilder label = new StringBuilder();
    label.append(periodFilter);
    label.append("  |  ").append(roomTypeFilter == null ? "All Room Types" : roomTypeFilter);
    label.append("  |  ").append(tierFilter == null ? "All Tiers" : tierFilter);

    if (statusFilter != null) {
      label.append("  |  Status ").append(statusFilter);
    }
    if (minWaitMinutes != null) {
      label.append("  |  Waited >= ").append(minWaitMinutes).append("m");
    }
    if (search != null) {
      label.append("  |  Search \"").append(search).append("\"");
    }

    return label.toString();
  }

  private boolean matchesSearch(Reservation r, Guest g, String search) {
    if (search == null || search.trim().isEmpty()) return true;

    String query = search.trim().toLowerCase();
    boolean matchesId =
        r.getReservationId() != null && r.getReservationId().toLowerCase().contains(query);
    boolean matchesConfirmation =
        r.getConfirmationNumber() != null
            && r.getConfirmationNumber().toLowerCase().contains(query);
    boolean matchesName =
        g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
    boolean matchesPhone =
        g != null && g.getPhoneNumber() != null && g.getPhoneNumber().toLowerCase().contains(query);

    return matchesId || matchesConfirmation || matchesName || matchesPhone;
  }

  private boolean matchesTier(Member m, String tierFilter) {
    if (tierFilter == null) return true;

    String actual = (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER";
    return tierFilter.equalsIgnoreCase(actual);
  }

  private boolean matchesPeriod(Reservation r, String periodFilter) {
    if ("ALL TIME".equalsIgnoreCase(periodFilter)) return true;

    LocalDateTime stamp =
        (r.getQueueArrivalTime() != null) ? r.getQueueArrivalTime() : r.getReservationTime();
    if (stamp == null) return false;

    LocalDate today = LocalDate.now();
    if ("TODAY".equalsIgnoreCase(periodFilter)) {
      return stamp.toLocalDate().isEqual(today);
    }
    return !stamp.toLocalDate().isBefore(today.minusDays(6));
  }

  private long waitMinutesOf(Reservation r) {
    if (r.getQueueArrivalTime() == null) return -1;

    LocalDateTime end = (r.getAllocatedTime() != null) ? r.getAllocatedTime() : LocalDateTime.now();
    return Duration.between(r.getQueueArrivalTime(), end).toMinutes();
  }

  private int compareArrival(Reservation a, Reservation b) {
    LocalDateTime first = a.getQueueArrivalTime();
    LocalDateTime second = b.getQueueArrivalTime();
    if (first == null && second == null) return 0;
    if (first == null) return -1;
    if (second == null) return 1;
    return first.compareTo(second);
  }

  private String guestNameOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null && g.getName() != null) ? g.getName() : "";
  }

  private int strikesOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null) ? g.getStrikeCount() : 0;
  }

  private String formatMinutes(long minutes) {
    if (minutes < 0) return "-";
    if (minutes < 60) return minutes + "m";
    return (minutes / 60) + "h " + String.format("%02dm", minutes % 60);
  }

  private String handleRoomTypeSubmenu(String current) {
    while (true) {
      int choice = reportView.displayRoomTypeSubmenu(current);
      if (choice == 1) return "LUXURY";
      if (choice == 2) return "SUITE";
      if (choice == 3) return "STANDARD";
      if (choice == 4) return null;
      if (choice == 5) return current;
    }
  }

  private String handleStatusSubmenu(String current) {
    while (true) {
      int choice = reportView.displayStatusSubmenu(current);
      if (choice == 1) return "RESERVED";
      if (choice == 2) return "WAITING";
      if (choice == 3) return "ALLOCATED";
      if (choice == 4) return "CHECKED_IN";
      if (choice == 5) return "NO_SHOW";
      if (choice == 6) return "CANCELLED";
      if (choice == 7) return null;
      if (choice == 8) return current;
    }
  }

  private String handleTierSubmenu(String current) {
    while (true) {
      int choice = reportView.displayTierSubmenu(current);
      if (choice == 1) return "DIAMOND";
      if (choice == 2) return "GOLD";
      if (choice == 3) return "SILVER";
      if (choice == 4) return "NON-MEMBER";
      if (choice == 5) return null;
      if (choice == 6) return current;
    }
  }

  private String handlePeriodSubmenu(String current) {
    while (true) {
      int choice = reportView.displayPeriodSubmenu(current);
      if (choice == 1) return "TODAY";
      if (choice == 2) return "LAST 7 DAYS";
      if (choice == 3) return "ALL TIME";
      if (choice == 4) return current;
    }
  }

  private String handleSortAttributeSubmenu(String current, boolean isRegister) {
    while (true) {
      int choice = reportView.displaySortAttributeSubmenu(current, isRegister);
      if (choice == 1) return "WAIT DURATION";
      if (choice == 2) return "ARRIVAL TIME";
      if (choice == 3) return "GUEST NAME";
      if (choice == 4) return "ROOM TYPE";
      if (choice == 5) return isRegister ? "RESERVATION ID" : "STRIKE COUNT";
      if (choice == 6) return current;
    }
  }

  private String handleSortDirectionSubmenu(String current) {
    while (true) {
      int choice = reportView.displaySortDirectionSubmenu(current);
      if (choice == 1) return "DESCENDING";
      if (choice == 2) return "ASCENDING";
      if (choice == 3) return current;
    }
  }

  private int handleRecordLimitSubmenu(int current) {
    while (true) {
      int choice = reportView.displayRecordLimitSubmenu(current);
      if (choice == 1) return 5;
      if (choice == 2) return 10;
      if (choice == 3) return 25;
      if (choice == 4) return 0;
      if (choice == 5) return current;
    }
  }
}
