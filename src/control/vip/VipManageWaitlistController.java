package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.VipView;

public class VipManageWaitlistController {
  private final VipView vipView;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;

  public VipManageWaitlistController(
      VipView vipView,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo) {
    this.vipView = vipView;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
  }

  public void startWaitlistManagement() {
    while (true) {
      try {
        Room.RoomType selectedRoomQueue = vipView.displayWaitlistQueueSelectionMenu();
        if (selectedRoomQueue == null) {
          return; // Return to VIP Main Menu
        }

        manageSpecificQueue(selectedRoomQueue);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageSpecificQueue(Room.RoomType roomType) {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String tierFilter = null;
    String boilingFilter = null; // "BOILING", "NORMAL", or null (ALL)
    String sortCriteria = "SCORE (HIGH -> LOW)";

    while (true) {
      try {
        ListInterface<Reservation> rawList = vipReservationRepo.getListByRoomType(roomType);

        ListInterface<Reservation> filteredList =
            filterAndSortList(rawList, searchQuery, tierFilter, boilingFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            vipView.renderWaitlistScreen(
                filteredList,
                guestRepo.getGuestList(),
                memberRepo.getMemberList(),
                roomType,
                searchQuery,
                tierFilter,
                boilingFilter,
                sortCriteria,
                currentPage,
                pageSize);

        if ("E".equalsIgnoreCase(result.input)) {
          break;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleAddGuest();
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(searchQuery, tierFilter, boilingFilter);
          searchQuery = filters[0];
          tierFilter = filters[1];
          boilingFilter = filters[2];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("Q".equalsIgnoreCase(result.input)) {
          handleQuickAssignTop(roomType);
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
          handleGuestAction(filteredList, result.getAsInt(), currentPage, pageSize, roomType);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String[] handleFilterMenu(
      String currentSearch, String currentTier, String currentBoiling) {
    String search = currentSearch;
    String tier = currentTier;
    String boiling = currentBoiling;

    while (true) {
      try {
        int choice = vipView.displayFilterMainMenu(search, tier, boiling);

        if (choice == 1) {
          search = handleSearchQuerySubmenu(search);
        } else if (choice == 2) {
          tier = handleTierFilterSubmenu(tier);
        } else if (choice == 3) {
          boiling = handleBoilingFilterSubmenu(boiling);
        } else if (choice == 4) {
          search = null;
          tier = null;
          boiling = null;
        } else if (choice == 5) {
          return new String[] {search, tier, boiling};
        } else if (choice == 6) {
          return new String[] {currentSearch, currentTier, currentBoiling};
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
            return currentSearch;
          }
          return input.trim();
        } else if (option == 2) {
          return null;
        } else if (option == 3) {
          return currentSearch;
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
        if (choice == 4) return null;
        if (choice == 5) return currentTier;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleBoilingFilterSubmenu(String currentBoiling) {
    while (true) {
      try {
        int choice = vipView.displayBoilingSubmenu(currentBoiling);
        if (choice == 1) return "BOILING";
        if (choice == 2) return "NORMAL";
        if (choice == 3) return null;
        if (choice == 4) return currentBoiling;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        String selectedSort = vipView.displaySortMenu();
        return (selectedSort == null) ? currentSort : selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleAddGuest() {
    try {
      String id = vipView.promptAddGuestInput();
      if (id == null || id.trim().isEmpty() || "C".equalsIgnoreCase(id.trim())) {
        return;
      }
    } catch (Exception e) {
      ConsoleUtil.printError(e.getMessage());
    }
  }

  private void handleQuickAssignTop(Room.RoomType roomType) {
    try {
      Reservation top = vipReservationRepo.dequeueNextVip(roomType);
      if (top == null) {
        ConsoleUtil.printError("This room queue is currently empty!");
        return;
      }
      processGuestDequeue(top, roomType);
    } catch (Exception e) {
      ConsoleUtil.printError(e.getMessage());
    }
  }

  private void handleGuestAction(
      ListInterface<Reservation> list,
      int indexOnPage,
      int page,
      int pageSize,
      Room.RoomType roomType) {

    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = list.getEntry(actualIndex);
    if (selected == null) return;

    while (true) {
      try {
        int action = vipView.displayGuestActionSubmenu(selected);

        if (action == 1) {
          // Only break to main table if dequeue actually succeeded!
          boolean completed = processGuestDequeue(selected, roomType);
          if (completed) {
            break;
          }
          // If cancelled (returned false), loop continues and re-renders submenu!
        } else if (action == 2) {
          if (ConsoleUtil.showConfirmMessage("Remove guest from waitlist?")) {
            vipReservationRepo.cancelReservation(selected);
            ConsoleUtil.clearScreen();
            System.out.println(" >> STATUS: SUCCESS");
            System.out.println(" Reservation " + selected.getReservationId() + " removed.\n");
            ConsoleUtil.printContinueMessage();
            break; // Removed successfully, return to table
          }
          // Chose 'N' on cancel confirmation -> loop continues back to submenu!
        } else if (action == 3) {
          break; // Back explicitly chosen, exit to table
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean processGuestDequeue(Reservation reservation, Room.RoomType roomType) {
    Guest g = findGuest(guestRepo.getGuestList(), reservation.getGuestId());
    Member m = (g != null) ? findMember(memberRepo.getMemberList(), g.getMemberId()) : null;

    String assignedRoom = "Room 801"; // Or lookup dynamically via RoomRepo

    boolean confirmed = vipView.displayDequeueConfirmationScreen(reservation, g, m, assignedRoom);
    if (!confirmed) {
      return false; // User selected 'N' -> Abort and stay in submenu!
    }

    vipReservationRepo.cancelReservation(reservation);
    int remainingCount = vipReservationRepo.getListByRoomType(roomType).getNumberOfEntries();

    vipView.displayDequeueSuccessScreen(reservation, g, m, assignedRoom, remainingCount);
    return true; // Successfully assigned -> Return true to break out to table
  }

  private ListInterface<Reservation> filterAndSortList(
      ListInterface<Reservation> source, String search, String tier, String boiling, String sort) {

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
      boolean matchesBoiling = true;

      // Search Query Matching
      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchResId =
            r.getReservationId() != null && r.getReservationId().toLowerCase().contains(query);
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

        matchesSearch = matchResId || matchConf || matchGuestId || matchName || matchPhone;
      }

      // Tier Filter Matching
      if (tier != null) {
        String actualTier = (m != null && m.getTier() != null) ? m.getTier().name() : "";
        matchesTier = tier.equalsIgnoreCase(actualTier);
      }

      // Boiling Status Filter Matching
      if (boiling != null) {
        if ("BOILING".equalsIgnoreCase(boiling)) {
          matchesBoiling = r.getIsBoiling();
        } else if ("NORMAL".equalsIgnoreCase(boiling)) {
          matchesBoiling = !r.getIsBoiling();
        }
      }

      if (matchesSearch && matchesTier && matchesBoiling) {
        filtered.add(r);
      }
    }

    // Bidirectional Sorting Algorithms
    if ("SCORE (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> Integer.compare(r1.getPriorityScore(), r2.getPriorityScore()));
    } else if ("STRIKES (LOWEST -> HIGHEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;
            return Integer.compare(s1, s2);
          });
    } else if ("STRIKES (HIGHEST -> LOWEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;
            return Integer.compare(s2, s1);
          });
    } else if ("TIER RANK (DIAMOND -> SILVER)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            Member m1 = (g1 != null) ? findMember(memberList, g1.getMemberId()) : null;
            Member m2 = (g2 != null) ? findMember(memberList, g2.getMemberId()) : null;
            return Integer.compare(getTierWeight(m2), getTierWeight(m1));
          });
    } else if ("TIER RANK (SILVER -> DIAMOND)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = findGuest(guestList, r1.getGuestId());
            Guest g2 = findGuest(guestList, r2.getGuestId());
            Member m1 = (g1 != null) ? findMember(memberList, g1.getMemberId()) : null;
            Member m2 = (g2 != null) ? findMember(memberList, g2.getMemberId()) : null;
            return Integer.compare(getTierWeight(m1), getTierWeight(m2));
          });
    } else if ("WAIT TIME (LONGEST -> SHORTEST)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime()));
    } else if ("WAIT TIME (SHORTEST -> LONGEST)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime()));
    } else {
      // Default: Priority Score (High -> Low)
      filtered.sort((r1, r2) -> Integer.compare(r2.getPriorityScore(), r1.getPriorityScore()));
    }

    return filtered;
  }

  private int getTierWeight(Member m) {
    if (m == null || m.getTier() == null) return 0;
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
