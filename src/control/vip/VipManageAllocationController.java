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
        ListInterface<AllocationEntry> filteredList =
            filterAndSortAllocations(rawAllocations, searchQuery, tierFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            allocationView.renderAllocationScreen(
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
          // Table refreshes on loop
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
    if (entry == null) return;

    Reservation reservation = vipReservationRepo.findById(entry.getReservationId());
    Guest guest = (reservation != null) ? guestRepo.findById(reservation.getGuestId()) : null;

    while (true) {
      try {
        int action = allocationView.displaySettleAllocationSubmenu(guest);

        if (action == 1) {
          // CONFIRM ALLOCATE -> COMPLETE CHECK-IN
          completeCheckIn(entry, reservation, guest);
          break;
        } else if (action == 2) {
          // CANCEL ALLOCATION / NO-SHOW RESOLUTION
          boolean resolved = handleCancelAllocationResolution(entry, reservation, guest);
          if (resolved) {
            break;
          }
        } else if (action == 3) {
          break; // Return to list
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void completeCheckIn(AllocationEntry entry, Reservation reservation, Guest guest) {
    if (reservation == null || entry == null) {
      ConsoleUtil.printError("Missing booking records for check-in completion!");
      return;
    }

    // 1. Update Room state
    Room room = roomRepo.findByRoomNumber(entry.getAssignedRoomNumber());
    if (room != null) {
      room.setStatus(Room.Status.OCCUPIED);
      room.setReservationConfirmationNumber(reservation.getConfirmationNumber());
      roomRepo.updateRoom(room);
    }

    // 2. Update Reservation state to CHECKED_IN & log timestamp
    reservation.setStatus(Reservation.Status.CHECKED_IN);
    reservation.setAllocatedTime(LocalDateTime.now());
    vipReservationRepo.updateReservation(reservation);

    // 3. Remove hold entry from AllocationRepo
    allocationRepo.removeAllocationEntry(
        entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);
    vipReservationRepo.getAllReservations(); // Persists state updates

    allocationView.displayCheckInSuccessScreen(reservation, guest, room);
  }

  private boolean handleCancelAllocationResolution(
      AllocationEntry entry, Reservation reservation, Guest guest) {
    VipSystemConfig config = vipSystemConfigRepo.getConfig();
    Member member =
        (guest != null && guest.getMemberId() != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;
    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
    int maxStrikes =
        (tier == Member.LoyaltyTier.DIAMOND)
            ? config.getDiamondMaxStrikes()
            : (tier == Member.LoyaltyTier.GOLD)
                ? config.getGoldMaxStrikes()
                : config.getSilverMaxStrikes();

    while (true) {
      try {
        int choice = allocationView.displayCancelResolutionMenu(guest, member, maxStrikes);

        if (choice == 1) {
          // OPTION 1: ISSUE STRIKE & RE-ENTER WAITLIST QUEUE
          if (guest != null) {
            guest.setStrikeCount(guest.getStrikeCount() + 1);
            guestRepo.updateGuest(guest);
          }

          // CHECK STRIKE THRESHOLD:
          if (guest != null && guest.getStrikeCount() >= maxStrikes) {
            // Max strike limit reached! Forced eviction lockout triggers.
            if (reservation != null) {
              reservation.setStatus(Reservation.Status.NO_SHOW);
              vipReservationRepo.updateReservation(reservation);
            }

            freeHeldRoom(entry);
            allocationRepo.removeAllocationEntry(
                entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);

            ConsoleUtil.clearScreen();
            System.out.println(">> STATUS: EVICTION LOCKOUT ENFORCED");
            System.out.println(
                "Guest "
                    + guest.getName()
                    + " has accumulated "
                    + guest.getStrikeCount()
                    + " strikes (MAX LIMIT REACHED).");
            System.out.println("Cannot re-enter queue. Reservation evicted and room freed.\n");
            ConsoleUtil.printContinueMessage();
            return true;
          }

          // Guest has < 3 strikes, safe to re-queue:
          if (reservation != null) {
            int newScore = calculateDynamicPriorityScore(guest, member);
            reservation.setStatus(Reservation.Status.WAITING);
            reservation.setPriorityScore(newScore);
            reservation.setQueueArrivalTime(LocalDateTime.now());

            // Re-enqueue into heap/queue
            vipReservationRepo.addReservation(reservation, newScore);
          }

          // Free hold room
          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);

          ConsoleUtil.clearScreen();
          System.out.println(">> STATUS: STRIKE ISSUED & RE-QUEUED");
          System.out.println(
              "Guest "
                  + (guest != null ? guest.getName() : "N/A")
                  + " strike count is now "
                  + (guest != null ? guest.getStrikeCount() : 0)
                  + ".");
          System.out.println("Reservation re-entered waitlist queue.\n");
          ConsoleUtil.printContinueMessage();
          return true;
        } else if (choice == 2) {
          // OPTION 2: EVICT & REMOVE GUEST ENTIRELY
          if (reservation != null) {
            reservation.setStatus(Reservation.Status.NO_SHOW);
            vipReservationRepo.cancelReservation(reservation);
          }

          freeHeldRoom(entry);
          allocationRepo.removeAllocationEntry(
              entry, roomRepo, vipReservationRepo, guestRepo, memberRepo, vipSystemConfigRepo);

          ConsoleUtil.clearScreen();
          System.out.println(">> STATUS: EVICTION COMPLETED");
          System.out.println("Booking record marked as NO_SHOW and removed from active system.\n");
          ConsoleUtil.printContinueMessage();
          return true;

        } else if (choice == 3) {
          return false; // Back to Settle Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void freeHeldRoom(AllocationEntry entry) {
    if (entry == null) return;
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
          String input = allocationView.promptSearchInput();
          if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
            search = null;
          } else {
            search = input.trim();
          }
        } else if (choice == 2) {
          tier = handleTierSubmenu(tier);
        } else if (choice == 3) {
          return new String[] {null, null};
        } else if (choice == 4) {
          return new String[] {search, tier};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleTierSubmenu(String currentTier) {
    while (true) {
      try {
        int choice = allocationView.displayTierSubmenu(currentTier);
        if (choice == 1) return "DIAMOND";
        if (choice == 2) return "GOLD";
        if (choice == 3) return "SILVER";
        if (choice == 4) return null;
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
      if (entry == null) continue;

      Reservation r = vipReservationRepo.findById(entry.getReservationId());
      Guest g = (r != null) ? guestRepo.findById(r.getGuestId()) : null;
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      boolean matchesSearch = true;
      boolean matchesTier = true;

      if (search != null && !search.trim().isEmpty()) {
        String query = search.trim().toLowerCase();
        boolean matchName =
            g != null && g.getName() != null && g.getName().toLowerCase().contains(query);
        boolean matchRoom =
            entry.getAssignedRoomNumber() != null
                && entry.getAssignedRoomNumber().toLowerCase().contains(query);
        boolean matchConf =
            entry.getReservationId() != null
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

    if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
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
    } else if ("ROOM NUMBER (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (e1, e2) -> e1.getAssignedRoomNumber().compareToIgnoreCase(e2.getAssignedRoomNumber()));
    } else {
      // Default: TIME REMAINING (LOW -> HIGH)
      filtered.sort(
          (e1, e2) -> Long.compare(e1.getExpirationTimestamp(), e2.getExpirationTimestamp()));
    }

    return filtered;
  }

  private int calculateDynamicPriorityScore(Guest guest, Member member) {
    if (member == null || member.getTier() == null) return 1000;
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
