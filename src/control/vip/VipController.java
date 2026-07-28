package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
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
      try {
        ListInterface<Reservation> rawList = vipReservationRepo.getReservationList();

        // filter & sort
        ListInterface<Reservation> filteredList =
            filterAndSortList(rawList, searchQuery, tierFilter, statusFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
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

        if ("E".equalsIgnoreCase(result.input)) {
          break; // Return to VIP Main Menu
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleAddGuest();
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(searchQuery, tierFilter, statusFilter);
          searchQuery = filters[0];
          tierFilter = filters[1];
          statusFilter = filters[2];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("Q".equalsIgnoreCase(result.input)) {
          handleQuickAssignTop();
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalMatches = filteredList.getNumberOfEntries();
          int totalPages = (int) Math.ceil((double) totalMatches / pageSize);
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(result.input)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if (result.isNumber) {
          handleGuestAction(filteredList, result.getAsInt(), currentPage, pageSize);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String[] handleFilterMenu(
      String currentSearch, String currentTier, String currentStatus) {

    String search = currentSearch;
    String tier = currentTier;
    String status = currentStatus;

    while (true) {
      try {
        int choice = vipView.displayFilterMainMenu(search, tier, status);

        if (choice == 1) {
          search = handleSearchQuerySubmenu(search);
        } else if (choice == 2) {
          tier = handleTierFilterSubmenu(tier);
        } else if (choice == 3) {
          status = handleStatusFilterSubmenu(status);
        } else if (choice == 4) { // clear all filters
          search = null;
          tier = null;
          status = null;
        } else if (choice == 5) { // apply and return
          return new String[] {search, tier, status};
        } else if (choice == 6) { // back without changing
          return new String[] {currentSearch, currentTier, currentStatus};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchQuerySubmenu(String currentSearch) {
    while (true) {
      try {
        int option = vipView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          String input = vipView.promptSearchInput();
          if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
            return currentSearch; // cancelled
          }
          return input.trim();
        } else if (option == 2) {
          return null; // clear search query
        } else if (option == 3) {
          return currentSearch; // back
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleTierFilterSubmenu(String currentTier) {
    while (true) {
      try {
        int choice = vipView.displayTierSubmenu(currentTier);
        if (choice == 1) return "DIAMOND";
        if (choice == 2) return "GOLD";
        if (choice == 3) return "SILVER";
        if (choice == 4) return null; // clear tier filter
        if (choice == 5) return currentTier; // back
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleStatusFilterSubmenu(String currentStatus) {
    while (true) {
      try {
        int choice = vipView.displayStatusSubmenu(currentStatus);
        if (choice == 1) return "BOILING";
        if (choice == 2) return "NORMAL";
        if (choice == 3) return null; // clear status filter
        if (choice == 4) return currentStatus; // back
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        String selectedSort = vipView.displaySortMenu();
        if (selectedSort == null) {
          return currentSort; // back
        }
        return selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleAddGuest() {
    while (true) {
      try {
        String id = vipView.promptAddGuestInput();
        if (id == null || id.trim().isEmpty() || "C".equalsIgnoreCase(id.trim())) {
          return; // back
        }

        // TODO: Process guest reservation addition
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleQuickAssignTop() {
    try {
      Reservation top = vipReservationRepo.dequeueNextVip();
      if (top != null) {
        ConsoleUtil.clearScreen();
        System.out.println(" >> STATUS: SUCCESS");
        System.out.println(
            " Successfully processed top VIP: " + top.getConfirmationNumber() + "\n");
        ConsoleUtil.printContinueMessage();
      } else {
        ConsoleUtil.printError("Waitlist is currently empty!");
      }
    } catch (Exception e) {
      ConsoleUtil.printError(e.getMessage());
    }
  }

  private void handleGuestAction(
      ListInterface<Reservation> list, int indexOnPage, int page, int pageSize) {
    while (true) {
      try {
        int actualIndex = (page - 1) * pageSize + indexOnPage;
        if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
          ConsoleUtil.printError("Invalid row selection index!");
          return;
        }

        Reservation selected = list.getEntry(actualIndex);
        if (selected == null) return;

        int action = vipView.displayGuestActionSubmenu(selected);

        if (action == 1 || action == 2) {
          vipReservationRepo.cancelReservation(selected);
          ConsoleUtil.clearScreen();
          System.out.println(" >> STATUS: SUCCESS");
          System.out.println(
              " Guest reservation " + selected.getConfirmationNumber() + " updated.\n");
          ConsoleUtil.printContinueMessage();
        }
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Reservation> filterAndSortList(
      ListInterface<Reservation> source, String search, String tier, String status, String sort) {

    if (source == null || source.isEmpty()) {
      return new ArrayList<>();
    }

    ListInterface<Reservation> filtered = new ArrayList<>();
    ListInterface<Guest> guestList = guestRepo.getGuestList();
    ListInterface<Member> memberList = memberRepo.getMemberList();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = findGuest(guestList, r.getGuestId());
      Member m = (g != null) ? findMember(memberList, g.getMemberId()) : null;

      boolean matchesSearch = true;
      boolean matchesTier = true;
      boolean matchesStatus = true;

      // text search matching
      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchConf =
            r.getConfirmationNumber() != null
                && r.getConfirmationNumber().toLowerCase().contains(query);
        boolean matchGuestId =
            r.getGuestId() != null && r.getGuestId().toLowerCase().contains(query);
        boolean matchName =
            g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
        boolean matchPhone =
            g != null
                && g.getPhoneNumber() != null
                && g.getPhoneNumber().toLowerCase().contains(query);

        matchesSearch = matchConf || matchGuestId || matchName || matchPhone;
      }

      // tier matching
      if (tier != null) {
        String actualTier = (m != null && m.getTier() != null) ? m.getTier().name() : "";
        matchesTier = tier.equalsIgnoreCase(actualTier);
      }

      // boiling state matching
      if (status != null) {
        if ("BOILING".equalsIgnoreCase(status)) {
          matchesStatus = r.getIsBoiling();
        } else if ("NORMAL".equalsIgnoreCase(status)) {
          matchesStatus = !r.getIsBoiling();
        }
      }

      if (matchesSearch && matchesTier && matchesStatus) {
        filtered.add(r);
      }
    }

    // sort logic
    if ("WAIT TIME (LONGEST -> SHORTEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            // Earliest arrival first = Longest wait
            int cmp = r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
            return (cmp != 0) ? cmp : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
          });

    } else if ("WAIT TIME (SHORTEST -> LONGEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            // Latest arrival first = Shortest wait
            int cmp = r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime());
            return (cmp != 0) ? cmp : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
          });

    } else if ("TIER RANK (DIAMOND -> SILVER)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            Member m1 = (g1 != null) ? findMember(memberList, g1.getMemberId()) : null;
            Member m2 = (g2 != null) ? findMember(memberList, g2.getMemberId()) : null;

            int cmp = Integer.compare(getTierWeight(m2), getTierWeight(m1));
            if (cmp != 0) return cmp;

            cmp = Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
            return (cmp != 0) ? cmp : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
          });

    } else if ("TIER RANK (SILVER -> DIAMOND)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            Member m1 = (g1 != null) ? findMember(memberList, g1.getMemberId()) : null;
            Member m2 = (g2 != null) ? findMember(memberList, g2.getMemberId()) : null;

            int cmp = Integer.compare(getTierWeight(m1), getTierWeight(m2));
            if (cmp != 0) return cmp;

            cmp = Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
            return (cmp != 0) ? cmp : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
          });

    } else if ("STRIKE COUNT (LOWEST -> HIGHEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;

            int cmp = Integer.compare(s1, s2);
            return (cmp != 0) ? cmp : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
          });

    } else if ("STRIKE COUNT (HIGHEST -> LOWEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;

            int cmp = Integer.compare(s2, s1);
            return (cmp != 0) ? cmp : Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
          });

    } else if ("PRIORITY SCORE (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            int cmp = Integer.compare(r1.getPriorityScore(), r2.getPriorityScore());
            return (cmp != 0) ? cmp : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
          });

    } else {
      // DEFAULT: PRIORITY SCORE (HIGH -> LOW)
      filtered.sort(
          (r1, r2) -> {
            int cmp = Integer.compare(r2.getPriorityScore(), r1.getPriorityScore());
            return (cmp != 0) ? cmp : r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime());
          });
    }

    return filtered;
  }

  private int getTierWeight(Member m) {
    if (m == null) return 0;
    switch (m.getTier()) {
      case DIAMOND:
        return 3;
      case GOLD:
        return 2;
      case SILVER:
        return 1;
      default:
        return 0;
    }
  }

  private Guest findGuest(ListInterface<Guest> guestList, String guestId) {
    if (guestList == null || guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equals(g.getGuestId())) return g;
    }
    return null;
  }

  private Member findMember(ListInterface<Member> memberList, String memberId) {
    if (memberList == null || memberId == null) return null;
    for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
      Member m = memberList.getEntry(i);
      if (m != null && memberId.equals(m.getMemberId())) return m;
    }
    return null;
  }
}
