package control.vip;

import adt.ArrayList;
import adt.ListInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
import java.time.LocalDateTime;
import repo.AllocationRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.VipReservationRepo;
import repo.VipSystemConfigRepo;
import util.ConsoleUtil;
import util.NumberUtil;
import view.vip.VipManageWaitlistView;

public class VipManageWaitlistController {

  private final VipManageWaitlistView waitlistView = new VipManageWaitlistView();
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final AllocationRepo allocationRepo;
  private final VipSystemConfigRepo vipSystemConfigRepo;

  public VipManageWaitlistController(
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      AllocationRepo allocationRepo,
      VipSystemConfigRepo vipSystemConfigRepo) {
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.allocationRepo = allocationRepo;
    this.vipSystemConfigRepo = vipSystemConfigRepo;
  }

  public void startWaitlistManagement() {
    while (true) {
      try {
        Room.RoomType selectedRoomQueue = waitlistView.displayWaitlistQueueSelectionMenu();
        if (selectedRoomQueue == null) {
          return;
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
    String boilingFilter = null;
    String sortCriteria = "SCORE (HIGH -> LOW)";

    while (true) {
      try {
        ListInterface<Reservation> rawList = vipReservationRepo.getListByRoomType(roomType);

        ListInterface<Reservation> filteredList =
            filterAndSortList(rawList, searchQuery, tierFilter, boilingFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            waitlistView.renderWaitlistScreen(
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
          handleAddGuest(roomType);
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

  private void handleAddGuest(Room.RoomType roomType) {
    VipSystemConfig config = vipSystemConfigRepo.getConfig();

    while (true) {
      try {
        String inputId = waitlistView.promptAddGuestInput();
        if (inputId == null || inputId.trim().isEmpty() || "C".equalsIgnoreCase(inputId.trim())) {
          return;
        }

        String searchId = inputId.trim();

        Guest guest = guestRepo.findById(searchId);
        if (guest == null) {
          guest = findGuestByName(guestRepo.getGuestList(), searchId);
        }

        if (guest == null) {
          waitlistView.displayGuestNotFoundErrorScreen(searchId);
          continue;
        }

        Member member =
            (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;
        int maxStrikes =
            (member != null && member.getTier() == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondMaxStrikes()
                : (member != null && member.getTier() == Member.LoyaltyTier.GOLD)
                    ? config.getGoldMaxStrikes()
                    : config.getSilverMaxStrikes();

        if (guest.getStrikeCount() >= maxStrikes) {
          int choice = waitlistView.displayMaxStrikeWarningScreen(guest, member, maxStrikes);

          if (choice == 1) {
            guest.setStrikeCount(0);
            guestRepo.updateGuest(guest);
            waitlistView.displayStrikeResetOverrideScreen(guest);
          } else if (choice == 2) {
            ConsoleUtil.printError("Eviction lockout enforced. Guest entry denied.");
            return;
          } else {
            return;
          }
        }

        if (member == null || member.getTier() == null) {
          waitlistView.displayNonMemberDeniedScreen(guest);
          return;
        }

        int baseScore =
            vipReservationRepo.calculatePriorityScore(
                null, guest, member, vipSystemConfigRepo.getConfig());

        boolean confirmed =
            waitlistView.displayAddGuestConfirmationScreen(guest, member, roomType, baseScore);
        if (!confirmed) {
          continue;
        }

        String resId = generateUniqueReservationId();
        String confNum = generateUniqueConfirmationNumber();

        LocalDateTime now = LocalDateTime.now();

        Reservation newRes =
            new Reservation(
                resId,
                guest.getGuestId(),
                confNum,
                roomType,
                Reservation.Status.WAITING,
                false,
                baseScore,
                now,
                now);

        vipReservationRepo.addReservation(
            newRes, baseScore, guestRepo, memberRepo, vipSystemConfigRepo);

        waitlistView.displayAddGuestSuccessScreen(resId, confNum, guest, roomType);
        break;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleQuickAssignTop(Room.RoomType roomType) {
    try {
      ListInterface<Reservation> queueList = vipReservationRepo.getListByRoomType(roomType);
      if (queueList == null || queueList.isEmpty()) {
        ConsoleUtil.printError("This room queue is currently empty!");
        return;
      }

      Reservation top = queueList.getEntry(1);
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
        int action = waitlistView.displayGuestActionSubmenu(selected);

        if (action == 1) {
          boolean completed = processGuestDequeue(selected, roomType);
          if (completed) {
            break;
          }
        } else if (action == 2) {
          Guest g = guestRepo.findById(selected.getGuestId());
          Member m =
              (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

          boolean confirmed = waitlistView.displayCancelConfirmationScreen(selected, g, m);

          if (confirmed) {
            vipReservationRepo.cancelReservation(
                selected, guestRepo, memberRepo, vipSystemConfigRepo);
            waitlistView.displayCancelSuccessScreen(selected.getReservationId());
            break;
          }
        } else if (action == 3) {
          break;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean processGuestDequeue(Reservation reservation, Room.RoomType roomType) {
    Room vacantRoom = roomRepo.findVacantCleanRoom(roomType);
    if (vacantRoom == null) {
      ConsoleUtil.printError(
          "No VACANT & CLEAN " + roomType.name() + " rooms available for allocation!");
      return false;
    }

    Guest g = guestRepo.findById(reservation.getGuestId());
    Member m = (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

    VipSystemConfig config = vipSystemConfigRepo.getConfig();
    Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;
    int graceMins =
        (tier == Member.LoyaltyTier.DIAMOND)
            ? config.getDiamondGraceWindowMins()
            : (tier == Member.LoyaltyTier.GOLD)
                ? config.getGoldGraceWindowMins()
                : config.getSilverGraceWindowMins();

    boolean confirmed =
        waitlistView.displayDequeueConfirmationScreen(reservation, g, m, vacantRoom, graceMins);
    if (!confirmed) {
      return false;
    }

    long holdDurationMs = graceMins * 60 * 1000L;

    AllocationEntry entry =
        new AllocationEntry(
            reservation.getReservationId(),
            vacantRoom.getRoomNumber(),
            System.currentTimeMillis() + holdDurationMs);

    allocationRepo.addAllocationEntry(
        entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);

    vacantRoom.setStatus(Room.Status.OCCUPIED);
    vacantRoom.setReservationConfirmationNumber(reservation.getConfirmationNumber());
    roomRepo.updateRoom(vacantRoom);

    vipReservationRepo.allocateReservation(reservation, guestRepo, memberRepo, vipSystemConfigRepo);

    int remainingCount = vipReservationRepo.getListByRoomType(roomType).getNumberOfEntries();

    waitlistView.displayDequeueSuccessScreen(
        reservation, g, m, vacantRoom.getRoomNumber(), remainingCount, entry);
    return true;
  }

  private String generateUniqueConfirmationNumber() {
    while (true) {
      String code = NumberUtil.generateDigitPin(8);

      boolean exists = false;
      ListInterface<Reservation> allReservations = vipReservationRepo.getAllReservations();
      if (allReservations != null) {
        for (int i = 1; i <= allReservations.getNumberOfEntries(); i++) {
          Reservation r = allReservations.getEntry(i);
          if (r != null && code.equalsIgnoreCase(r.getConfirmationNumber())) {
            exists = true;
            break;
          }
        }
      }

      if (!exists) {
        return code;
      }
    }
  }

  private String generateUniqueReservationId() {
    int maxIdNum = 10000;
    ListInterface<Reservation> allReservations = vipReservationRepo.getAllReservations();

    if (allReservations != null) {
      for (int i = 1; i <= allReservations.getNumberOfEntries(); i++) {
        Reservation r = allReservations.getEntry(i);
        if (r != null && r.getReservationId() != null && r.getReservationId().startsWith("RES-")) {
          try {
            int num = Integer.parseInt(r.getReservationId().substring(4));
            if (num > maxIdNum) {
              maxIdNum = num;
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    return "RES-" + (maxIdNum + 1);
  }

  private Guest findGuestByName(ListInterface<Guest> guestList, String name) {
    if (guestList == null || name == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && name.equalsIgnoreCase(g.getName())) {
        return g;
      }
    }
    return null;
  }

  private String[] handleFilterMenu(
      String currentSearch, String currentTier, String currentBoiling) {
    String search = currentSearch;
    String tier = currentTier;
    String boiling = currentBoiling;

    while (true) {
      try {
        int choice = waitlistView.displayFilterMainMenu(search, tier, boiling);

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
        int option = waitlistView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          String input = waitlistView.promptSearchInput();
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
        int choice = waitlistView.displayTierSubmenu(currentTier);
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
        int choice = waitlistView.displayBoilingSubmenu(currentBoiling);
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
        String selectedSort = waitlistView.displaySortMenu();
        return (selectedSort == null) ? currentSort : selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Reservation> filterAndSortList(
      ListInterface<Reservation> source, String search, String tier, String boiling, String sort) {

    if (source == null || source.isEmpty()) {
      return new ArrayList<>();
    }

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchesSearch = true;
      boolean matchesTier = true;
      boolean matchesBoiling = true;

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

      if (tier != null) {
        String actualTier = (m != null && m.getTier() != null) ? m.getTier().name() : "";
        matchesTier = tier.equalsIgnoreCase(actualTier);
      }

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

    if ("SCORE (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> Integer.compare(r1.getPriorityScore(), r2.getPriorityScore()));
    } else if ("STRIKES (LOWEST -> HIGHEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;
            return Integer.compare(s1, s2);
          });
    } else if ("STRIKES (HIGHEST -> LOWEST)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            int s1 = (g1 != null) ? g1.getStrikeCount() : 0;
            int s2 = (g2 != null) ? g2.getStrikeCount() : 0;
            return Integer.compare(s2, s1);
          });
    } else if ("TIER RANK (DIAMOND -> SILVER)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            Member m1 =
                (g1 != null && g1.getMemberId() != null)
                    ? memberRepo.findById(g1.getMemberId())
                    : null;
            Member m2 =
                (g2 != null && g2.getMemberId() != null)
                    ? memberRepo.findById(g2.getMemberId())
                    : null;
            return Integer.compare(getTierWeight(m2), getTierWeight(m1));
          });
    } else if ("TIER RANK (SILVER -> DIAMOND)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> {
            Guest g1 = guestRepo.findById(r1.getGuestId());
            Guest g2 = guestRepo.findById(r2.getGuestId());
            Member m1 =
                (g1 != null && g1.getMemberId() != null)
                    ? memberRepo.findById(g1.getMemberId())
                    : null;
            Member m2 =
                (g2 != null && g2.getMemberId() != null)
                    ? memberRepo.findById(g2.getMemberId())
                    : null;
            return Integer.compare(getTierWeight(m1), getTierWeight(m2));
          });
    } else if ("WAIT TIME (LONGEST -> SHORTEST)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> r1.getQueueArrivalTime().compareTo(r2.getQueueArrivalTime()));
    } else if ("WAIT TIME (SHORTEST -> LONGEST)".equalsIgnoreCase(sort)) {
      filtered.sort((r1, r2) -> r2.getQueueArrivalTime().compareTo(r1.getQueueArrivalTime()));
    } else {
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
}
