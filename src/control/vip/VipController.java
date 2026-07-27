package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.Reservation;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.VipView;

public class VipController {
  private final VipView vipView;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;

  public VipController() {
    this.vipView = new VipView();
    this.vipReservationRepo = new VipReservationRepo();
    this.guestRepo = new GuestRepo();
    this.memberRepo = new MemberRepo();
  }

  public void start() {
    while (true) {
      try {
        String choice = vipView.displayMenu();

        if ("1".equals(choice)) {
          manageWaitlist();
        } else if ("5".equals(choice)) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  public void manageWaitlist() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String tierFilter = null;
    String statusFilter = null;
    String sortCriteria = "PRIORITY SCORE (HIGH -> LOW)";

    while (true) {
      // 1. Fetch current live list from Repo
      ListInterface<Reservation> rawList = vipReservationRepo.getReservationList();

      // 2. Filter & Sort using ListInterface's built-in sort()
      ListInterface<Reservation> filteredList =
          filterAndSortList(rawList, searchQuery, tierFilter, statusFilter, sortCriteria);

      // 3. Render Table View via VipView
      String input =
          vipView.renderWaitlistScreen(
              filteredList,
              guestRepo.getGuestList(),
              memberRepo.getMemberList(),
              searchQuery,
              tierFilter,
              statusFilter,
              sortCriteria,
              currentPage,
              pageSize);

      // 4. Handle Input Commands
      if (input == null || input.trim().isEmpty()) continue;
      String command = input.trim().toUpperCase();

      if ("E".equals(command)) {
        break; // Return to VIP Main Menu
      } else if ("A".equals(command)) {
        handleAddGuest();
      } else if ("S".equals(command)) {
        // Show Search / Filter Submenu
        String[] filters = vipView.displayFilterMenu(searchQuery, tierFilter, statusFilter);
        searchQuery = filters[0];
        tierFilter = filters[1];
        statusFilter = filters[2];
        currentPage = 1;
      } else if ("O".equals(command)) {
        // Show Change Sort Order Submenu
        sortCriteria = vipView.displaySortMenu();
        currentPage = 1;
      } else if ("Q".equals(command)) {
        handleQuickAssignTop();
      } else if ("N".equals(command)) {
        int totalMatches = filteredList.getNumberOfEntries();
        int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
        if (currentPage < totalPages) {
          currentPage++;
        }
      } else if ("P".equals(command) && currentPage > 1) {
        currentPage--;
      } else if (command.matches("\\d+")) {
        int selectedIndex = Integer.parseInt(command);
        handleGuestAction(filteredList, selectedIndex, currentPage, pageSize);
      }
    }
  }

  private void handleAddGuest() {
    String id = vipView.promptAddGuestInput();
    if (id == null || id.trim().isEmpty() || id.equalsIgnoreCase("C")) return;

    // TODO: Fetch profile/booking details using guest ID from domain services
  }

  private void handleQuickAssignTop() {
    Reservation top = vipReservationRepo.dequeueNextVip();
    if (top != null) {
      ConsoleUtil.clearScreen();
      System.out.println(" >> STATUS: [✓] SUCCESS");
      System.out.println(" Successfully processed top VIP: " + top.getConfirmationNumber() + "\n");
      ConsoleUtil.printContinueMessage();
    } else {
      ConsoleUtil.printError("Waitlist is currently empty!");
    }
  }

  private void handleGuestAction(
      ListInterface<Reservation> list, int indexOnPage, int page, int pageSize) {
    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = list.getEntry(actualIndex);
    int action = vipView.displayGuestActionSubmenu(selected);

    if (action == 1 || action == 2) {
      vipReservationRepo.cancelReservation(selected);
      ConsoleUtil.clearScreen();
      System.out.println(" >> STATUS: [✓] SUCCESS");
      System.out.println(" Guest reservation " + selected.getConfirmationNumber() + " updated.\n");
      ConsoleUtil.printContinueMessage();
    }
  }

  private ListInterface<Reservation> filterAndSortList(
      ListInterface<Reservation> source, String search, String tier, String status, String sort) {

    if (source == null || source.isEmpty()) {
      return new ArrayList<>();
    }

    ListInterface<Reservation> filtered = new ArrayList<>();

    // --- FILTER LOOP ---
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      boolean matchesSearch = true;
      boolean matchesStatus = true;

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchConf =
            r.getConfirmationNumber() != null
                && r.getConfirmationNumber().toLowerCase().contains(query);
        boolean matchGuest = r.getGuestId() != null && r.getGuestId().toLowerCase().contains(query);
        matchesSearch = matchConf || matchGuest;
      }

      if (status != null) {
        if ("BOILING".equals(status)) {
          matchesStatus = r.getIsBoiling();
        } else if ("NORMAL".equals(status)) {
          matchesStatus = !r.getIsBoiling();
        }
      }

      if (matchesSearch && matchesStatus) {
        filtered.add(r);
      }
    }

    // --- USE BUILT-IN ADT SORT METHOD ---
    if ("WAIT TIME (LONGEST -> SHORTEST)".equals(sort)) {
      filtered.sort((r1, r2) -> r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime()));
    } else {
      // Default: PRIORITY SCORE (HIGH -> LOW)
      filtered.sort((r1, r2) -> Integer.compare(r2.getPriorityScore(), r1.getPriorityScore()));
    }

    return filtered;
  }
}
