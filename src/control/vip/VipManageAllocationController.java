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
import view.vip.VipManageAllocationView;

public class VipManageAllocationController {

  private final VipManageAllocationView allocationView = new VipManageAllocationView();
  private final AllocationRepo allocationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;
  private final VipSystemConfigRepo vipSystemConfigRepo;

  public VipManageAllocationController(
      AllocationRepo allocationRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo,
      VipSystemConfigRepo vipSystemConfigRepo) {
    this.allocationRepo = allocationRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
    this.vipSystemConfigRepo = vipSystemConfigRepo;
  }

  public void startAllocationManagement() {
    int currentPage = 1;
    int pageSize = 10;

    String searchQuery = null;
    String tierFilter = null;
    String sortCriteria = "TIME REMAINING (LOW -> HIGH)";

    while (true) {
      try {
        ListInterface<AllocationEntry> rawAllocations = allocationRepo.getAllocationList();
        ListInterface<AllocationEntry> filteredList = filterAndSortAllocations(rawAllocations, searchQuery, tierFilter,
            sortCriteria);

        ConsoleUtil.GetMenuInputResult result = allocationView.renderAllocationScreen(
            filteredList,
            vipReservationRepo.getAllReservations(),
            guestRepo.getGuestList(),
            memberRepo.getMemberList(),
            searchQuery,
            tierFilter,
            sortCriteria,
            currentPage,
            pageSize);

        if ("E".equalsIgnoreCase(result.input)) {
          break;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(searchQuery, tierFilter);
          searchQuery = filters[0];
          tierFilter = filters[1];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Refresh
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
          handleSettleAllocation(filteredList, result.getAsInt(), currentPage, pageSize);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleSettleAllocation(
      ListInterface<AllocationEntry> list, int indexOnPage, int page, int pageSize) {
    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid allocation row selection!");
      return;
    }

    AllocationEntry entry = list.getEntry(actualIndex);
    if (entry == null)
      return;

    Reservation reservation = vipReservationRepo.findById(entry.getReservationId());
    Guest guest = (reservation != null) ? guestRepo.findById(reservation.getGuestId()) : null;
    Member member = (guest != null && guest.getMemberId() != null)
        ? memberRepo.findById(guest.getMemberId())
        : null;

    while (true) {
      try {
        // Show Detailed Summary Screen Submenu
        int action = allocationView.displayAllocationDetailScreen(entry, reservation, guest, member);

        if (action == 1) {
          // ROUTE TO METHOD 1: CHECK-IN PROCESS
          boolean completed = handleConfirmCheckInSubmenu(entry, reservation, guest);
          if (completed) {
            break; // Finished check-in -> return to allocation table
          }
        } else if (action == 2) {
          // ROUTE TO METHOD 2: CANCEL & NO-SHOW RESOLUTION
          boolean resolved = handleCancelAllocationSubmenu(entry, reservation, guest, member);
          if (resolved) {
            break; // Resolved cancellation -> return to allocation table
          }
        } else if (action == 3) {
          break; // Return to list
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- SUBMENU METHOD 1: CONFIRM ALLOCATION & CHECK-IN ---
  private boolean handleConfirmCheckInSubmenu(
      AllocationEntry entry, Reservation reservation, Guest guest) {
    if (reservation == null || entry == null) {
      ConsoleUtil.printError("Missing booking records for check-in completion!");
      return false;
    }

    Integer stayDays = promptStayDuration(entry, guest);
    if (stayDays == null) {
      return false; // User pressed 'C' to cancel back to Detail Screen
    }

    // Confirmation step
    boolean confirmed = promptConfirmCheckIn(guest, entry.getAssignedRoomNumber(), stayDays);

    if (!confirmed) {
      return false; // Back to Detail Screen
    }

    LocalDateTime now = LocalDateTime.now();

    // 1. Update Room state
    Room room = roomRepo.findByRoomNumber(entry.getAssignedRoomNumber());
    if (room != null) {
      room.setStatus(Room.Status.OCCUPIED);
      room.setReservationConfirmationNumber(reservation.getConfirmationNumber());
      roomRepo.updateRoom(room);
    }

    // 2. Update Reservation state & store stay duration
    reservation.setStatus(Reservation.Status.CHECKED_IN);
    reservation.setAllocatedTime(now);
    reservation.setStayDays(stayDays);
    vipReservationRepo.updateReservation(reservation);

    // 3. Remove hold entry from AllocationRepo
    allocationRepo.removeAllocationEntry(
        entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);

    allocationView.displayCheckInSuccessScreen(reservation, guest, room);
    return true; // Successfully checked in!
  }

  private boolean promptConfirmCheckIn(Guest guest, String roomNumber, int stayDays) {
    while (true) {
      try {
        return ConsoleUtil.showConfirmMessage(
            "Complete check-in for "
                + (guest != null ? guest.getName() : "Guest")
                + " in Room "
                + roomNumber
                + " for "
                + stayDays
                + " day(s)?");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- HELPER METHOD: STAY DURATION INPUT ---
  private Integer promptStayDuration(AllocationEntry entry, Guest guest) {
    while (true) {
      try {
        Integer days = allocationView.promptStayDuration(entry, guest);

        return days;
      } catch (Exception e) {
        ConsoleUtil.printError("Invalid input format! Please enter a valid number of days (1-30).");
      }
    }
  }

  // --- SUBMENU METHOD 2: CANCEL ALLOCATION & NO-SHOW RESOLUTION ---
  private boolean handleCancelAllocationSubmenu(
      AllocationEntry entry, Reservation reservation, Guest guest, Member member) {

    VipSystemConfig config = vipSystemConfigRepo.getConfig();
    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
    int maxStrikes = (tier == Member.LoyaltyTier.DIAMOND)
        ? config.getDiamondMaxStrikes()
        : (tier == Member.LoyaltyTier.GOLD)
            ? config.getGoldMaxStrikes()
            : config.getSilverMaxStrikes();

    while (true) {
      try {
        // Displays NO-SHOW EVICTION PROCESSING Submenu screen first
        int choice = allocationView.displayCancelResolutionMenu(guest, member, maxStrikes);

        if (choice == 1) {
          // OPTION 1: ISSUE STRIKE & RE-QUEUE
          boolean confirmStrike = promptConfirmStrikeAndRequeue(guest);
          if (!confirmStrike) {
            continue;
          }

          if (guest != null && guest.getStrikeCount() >= maxStrikes) {
            if (reservation != null) {
              reservation.setStatus(Reservation.Status.NO_SHOW);
              vipReservationRepo.updateReservation(reservation);
            }
            freeHeldRoom(entry);
            allocationRepo.removeAllocationEntry(
                entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
            allocationView.displayEvictionLockoutScreen(guest);
            return true;
          }

          if (guest != null) {
            guest.setStrikeCount(guest.getStrikeCount() + 1);
            guestRepo.updateGuest(guest);
          }

          if (reservation != null) {
            int newScore = calculateDynamicPriorityScore(guest, member);
            reservation.setStatus(Reservation.Status.WAITING);
            reservation.setPriorityScore(newScore);
            reservation.setQueueArrivalTime(LocalDateTime.now());
            vipReservationRepo.addReservation(
                reservation, newScore, guestRepo, memberRepo, vipSystemConfigRepo);
          }

          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
          allocationView.displayStrikeIssuedScreen(guest);
          return true;

        } else if (choice == 2) {
          // OPTION 2: ISSUE STRIKE ONLY (DO NOT RE-QUEUE)
          boolean confirmStrike = promptConfirmStrikeWithoutRequeue(guest);
          if (!confirmStrike) {
            continue;
          }

          if (guest != null) {
            guest.setStrikeCount(guest.getStrikeCount() + 1);
            guestRepo.updateGuest(guest);
          }

          if (reservation != null) {
            reservation.setStatus(Reservation.Status.NO_SHOW);
            vipReservationRepo.cancelReservation(
                reservation, guestRepo, memberRepo, vipSystemConfigRepo);
          }

          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
          allocationView.displayStrikeIssuedWithoutRequeueScreen(guest);
          return true;

        } else if (choice == 3) {
          // OPTION 3: RE-QUEUE ONLY (NO STRIKE ISSUED)
          boolean confirmRequeue = ConsoleUtil.showConfirmMessage(
              "Re-queue "
                  + (guest != null ? guest.getName() : "Guest")
                  + " in waitlist queue without issuing a strike?");
          if (!confirmRequeue) {
            continue;
          }

          if (reservation != null) {
            int newScore = calculateDynamicPriorityScore(guest, member);
            reservation.setStatus(Reservation.Status.WAITING);
            reservation.setPriorityScore(newScore);
            reservation.setQueueArrivalTime(LocalDateTime.now());
            vipReservationRepo.addReservation(
                reservation, newScore, guestRepo, memberRepo, vipSystemConfigRepo);
          }

          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
          allocationView.displayRequeuedWithoutStrikeScreen(guest);
          return true;

        } else if (choice == 4) {
          // OPTION 4: EVICT & REMOVE GUEST ENTIRELY
          boolean confirmEvict = promptConfirmEvict(guest, entry.getAssignedRoomNumber());
          if (!confirmEvict) {
            continue;
          }

          if (reservation != null) {
            reservation.setStatus(Reservation.Status.NO_SHOW);
            vipReservationRepo.cancelReservation(
                reservation, guestRepo, memberRepo, vipSystemConfigRepo);
          }

          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
          allocationView.displayEvictionCompletedScreen();
          return true;

        } else if (choice == 5) {
          return false; // Back to Detail Screen
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptConfirmStrikeAndRequeue(Guest guest) {
    while (true) {
      try {
        return ConsoleUtil.showConfirmMessage(
            "Issue 1 strike to "
                + (guest != null ? guest.getName() : "Guest")
                + " and send back to waitlist queue?");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptConfirmStrikeWithoutRequeue(Guest guest) {
    while (true) {
      try {
        return ConsoleUtil.showConfirmMessage(
            "Issue 1 strike to "
                + (guest != null ? guest.getName() : "Guest")
                + " and cancel reservation WITHOUT re-queueing?");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean promptConfirmEvict(Guest guest, String roomNumber) {
    while (true) {
      try {
        return ConsoleUtil.showConfirmMessage(
            "Are you sure you want to PERMANENTLY EVICT "
                + (guest != null ? guest.getName() : "Guest")
                + " and free room "
                + roomNumber
                + "?");
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void freeHeldRoom(AllocationEntry entry) {
    if (entry == null)
      return;
    Room room = roomRepo.findByRoomNumber(entry.getAssignedRoomNumber());
    if (room != null) {
      room.setStatus(Room.Status.VACANT_CLEAN);
      room.setReservationConfirmationNumber(null);
      roomRepo.updateRoom(room);
    }
  }

  private String[] handleFilterMenu(String currentSearch, String currentTier) {
    String search = currentSearch;
    String tier = currentTier;

    while (true) {
      try {
        int choice = allocationView.displayFilterMainMenu(search, tier);

        if (choice == 1) {
          search = handleSearchSubmenu(search);
        } else if (choice == 2) {
          tier = handleTierSubmenu(tier);
        } else if (choice == 3) {
          search = null;
          tier = null;
        } else if (choice == 4) {
          return new String[] { search, tier };
        } else if (choice == 5) {
          return new String[] { currentSearch, currentTier };
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchSubmenu(String currentSearch) {
    while (true) {
      try {
        int option = allocationView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          return promptSearchInputWithRetry(currentSearch);
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

  private String promptSearchInputWithRetry(String defaultValue) {
    while (true) {
      try {
        String input = allocationView.promptSearchInput(defaultValue);
        if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
          return defaultValue;
        }
        return input.trim();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleTierSubmenu(String currentTier) {
    while (true) {
      try {
        int choice = allocationView.displayTierSubmenu(currentTier);
        if (choice == 1)
          return "DIAMOND";
        if (choice == 2)
          return "GOLD";
        if (choice == 3)
          return "SILVER";
        if (choice == 4)
          return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        String selectedSort = allocationView.displaySortMenu();
        return (selectedSort == null) ? currentSort : selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<AllocationEntry> filterAndSortAllocations(
      ListInterface<AllocationEntry> source, String search, String tier, String sort) {
    if (source == null || source.isEmpty()) {
      return new ArrayList<>();
    }

    ListInterface<AllocationEntry> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      AllocationEntry entry = source.getEntry(i);
      if (entry == null)
        continue;

      Reservation r = vipReservationRepo.findById(entry.getReservationId());
      Guest g = (r != null) ? guestRepo.findById(r.getGuestId()) : null;
      Member m = (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchesSearch = true;
      boolean matchesTier = true;

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchName = g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
        boolean matchRoom = entry.getAssignedRoomNumber() != null
            && entry.getAssignedRoomNumber().toLowerCase().contains(query);
        boolean matchConf = entry.getReservationId() != null
            && entry.getReservationId().toLowerCase().contains(query);

        matchesSearch = matchName || matchRoom || matchConf;
      }

      if (tier != null) {
        String actualTier = (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER";
        matchesTier = tier.equalsIgnoreCase(actualTier);
      }

      if (matchesSearch && matchesTier) {
        filtered.add(entry);
      }
    }

    if ("TIME REMAINING (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> Long.compare(e2.getExpirationTimestamp(), e1.getExpirationTimestamp()));
    } else if ("TIER RANK (DIAMOND -> SILVER)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> {
            Reservation r1 = vipReservationRepo.findById(e1.getReservationId());
            Reservation r2 = vipReservationRepo.findById(e2.getReservationId());
            Guest g1 = (r1 != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2 != null) ? guestRepo.findById(r2.getGuestId()) : null;
            Member m1 = (g1 != null && g1.getMemberId() != null) ? memberRepo.findById(g1.getMemberId()) : null;
            Member m2 = (g2 != null && g2.getMemberId() != null) ? memberRepo.findById(g2.getMemberId()) : null;
            int rank1 = (m1 != null && m1.getTier() != null) ? m1.getTier().ordinal() : -1;
            int rank2 = (m2 != null && m2.getTier() != null) ? m2.getTier().ordinal() : -1;
            return Integer.compare(rank2, rank1);
          });
    } else if ("TIER RANK (SILVER -> DIAMOND)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> {
            Reservation r1 = vipReservationRepo.findById(e1.getReservationId());
            Reservation r2 = vipReservationRepo.findById(e2.getReservationId());
            Guest g1 = (r1 != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2 != null) ? guestRepo.findById(r2.getGuestId()) : null;
            Member m1 = (g1 != null && g1.getMemberId() != null) ? memberRepo.findById(g1.getMemberId()) : null;
            Member m2 = (g2 != null && g2.getMemberId() != null) ? memberRepo.findById(g2.getMemberId()) : null;
            int rank1 = (m1 != null && m1.getTier() != null) ? m1.getTier().ordinal() : -1;
            int rank2 = (m2 != null && m2.getTier() != null) ? m2.getTier().ordinal() : -1;
            return Integer.compare(rank1, rank2);
          });
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> {
            Reservation r1 = vipReservationRepo.findById(e1.getReservationId());
            Reservation r2 = vipReservationRepo.findById(e2.getReservationId());
            Guest g1 = (r1 != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2 != null) ? guestRepo.findById(r2.getGuestId()) : null;
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return n1.compareToIgnoreCase(n2);
          });
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> {
            Reservation r1 = vipReservationRepo.findById(e1.getReservationId());
            Reservation r2 = vipReservationRepo.findById(e2.getReservationId());
            Guest g1 = (r1 != null) ? guestRepo.findById(r1.getGuestId()) : null;
            Guest g2 = (r2 != null) ? guestRepo.findById(r2.getGuestId()) : null;
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return n2.compareToIgnoreCase(n1);
          });
    } else if ("ROOM NUMBER (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> e1.getAssignedRoomNumber().compareToIgnoreCase(e2.getAssignedRoomNumber()));
    } else if ("ROOM NUMBER (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> e2.getAssignedRoomNumber().compareToIgnoreCase(e1.getAssignedRoomNumber()));
    } else if ("RESERVATION ID (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> e1.getReservationId().compareToIgnoreCase(e2.getReservationId()));
    } else if ("RESERVATION ID (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> e2.getReservationId().compareToIgnoreCase(e1.getReservationId()));
    } else {
      // Default: TIME REMAINING (LOW -> HIGH)
      filtered.sort(
          (e1, e2) -> Long.compare(e1.getExpirationTimestamp(), e2.getExpirationTimestamp()));
    }

    return filtered;
  }

  private int calculateDynamicPriorityScore(Guest guest, Member member) {
    if (member == null || member.getTier() == null)
      return 1000;
    int tierBase;
    switch (member.getTier()) {
      case DIAMOND:
        tierBase = 9000;
        break;
      case GOLD:
        tierBase = 7000;
        break;
      case SILVER:
        tierBase = 5000;
        break;
      default:
        tierBase = 1000;
        break;
    }
    int pointsBonus = Math.min(member.getPoints() / 10, 800);
    int strikePenalty = (guest != null) ? (guest.getStrikeCount() * 500) : 0;
    return Math.max(1000, tierBase + pointsBonus - strikePenalty);
  }
}
