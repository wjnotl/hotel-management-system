package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import adt.QueueInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.RoomRepo;
import repo.StandardReservationRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.booking.WalkInQueueView;

public class WalkInQueueController {
  private static final int PAGE_SIZE = 10;
  private static final String DEFAULT_SORT = "QUEUE POSITION (FIFO)";

  private final WalkInQueueView walkInQueueView = new WalkInQueueView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;

  public WalkInQueueController(
      StandardReservationRepo standardReservationRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      RoomRepo roomRepo) {
    this.standardReservationRepo = standardReservationRepo;
    this.vipReservationRepo = vipReservationRepo;
    this.guestRepo = guestRepo;
    this.memberRepo = memberRepo;
    this.roomRepo = roomRepo;
  }

  public void startQueueManagement() {
    while (true) {
      try {
        Room.RoomType selectedLine = walkInQueueView.displayQueueSelectionMenu();
        if (selectedLine == null) {
          return;
        }

        manageLine(selectedLine);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void manageLine(Room.RoomType roomType) {
    int currentPage = 1;
    String searchQuery = null;
    String tierFilter = null;
    String sortCriteria = DEFAULT_SORT;

    while (true) {
      try {
        int lapsed = standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);
        if (lapsed > 0) {
          walkInQueueView.displaySweepNotice(lapsed, StandardReservationRepo.GRACE_MINUTES);
        }

        QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
        ListInterface<Reservation> lineRows =
            filterAndSortLine(
                standardReservationRepo.snapshotQueue(roomType),
                queue,
                searchQuery,
                tierFilter,
                sortCriteria);
        ListInterface<Reservation> holds = standardReservationRepo.getHoldsByRoomType(roomType);

        ConsoleUtil.GetMenuInputResult result =
            walkInQueueView.renderQueueScreen(
                queue,
                lineRows,
                holds,
                guestRepo.getGuestList(),
                memberRepo.getMemberList(),
                roomRepo.getRoomList(),
                roomType,
                standardReservationRepo.getQueueCapacity(roomType),
                countVacantCleanRooms(roomType),
                countVipWaiting(roomType),
                StandardReservationRepo.GRACE_MINUTES,
                searchQuery,
                tierFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleAddWalkIn(roomType);
          currentPage = 1;
        } else if ("G".equalsIgnoreCase(result.input)) {
          handleAllocateNext(roomType);
        } else if ("V".equalsIgnoreCase(result.input)) {
          handleOverrideAllocate(roomType);
        } else if ("C".equalsIgnoreCase(result.input)) {
          handleCheckInHold(holds, roomType);
        } else if ("X".equalsIgnoreCase(result.input)) {
          handleCloseQueue(roomType);
          currentPage = 1;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(searchQuery, tierFilter);
          searchQuery = filters[0];
          tierFilter = filters[1];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = walkInQueueView.displaySortMenu();
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) lineRows.getNumberOfEntries() / PAGE_SIZE);
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
          handleRowAction(lineRows, result.getAsInt(), currentPage, roomType);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleAddWalkIn(Room.RoomType roomType) {
    while (true) {
      try {
        String input = walkInQueueView.promptAddWalkInInput();
        if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
          return;
        }

        String term = input.trim();
        Guest guest = guestRepo.findById(term);
        if (guest == null) {
          guest = guestRepo.findByName(term);
        }

        // A walk-in never booked, so a first-time arrival legitimately has no guest file yet.
        // Refusing them here would mean the module can only serve people the seeder created.
        if (guest == null) {
          int choice = walkInQueueView.displayGuestNotFoundScreen(term);
          if (choice == 2) {
            continue;
          } else if (choice != 1) {
            return;
          }

          guest =
              new GuestRegistrationController(guestRepo).registerNewGuest(nameSuggestionFrom(term));
          if (guest == null) {
            continue;
          }
        }

        QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);

        Reservation alreadyQueued = findQueuedReservationForGuest(roomType, guest.getGuestId());
        if (alreadyQueued != null) {
          walkInQueueView.displayAlreadyInLineScreen(
              guest, alreadyQueued, queue.getPosition(alreadyQueued));
          return;
        }

        Member member =
            (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;

        // A tier holder does not belong at the back of a FIFO line, so the decision is put to
        // the clerk before the line is offered at all.
        if (member != null && member.getTier() != null) {
          int decision = promptVipRouting(guest, member, roomType, queue.getNumberOfEntries());
          if (decision == 1) {
            if (assignRoomDirectly(guest, member, roomType)) {
              return;
            }
            continue;
          } else if (decision == 3) {
            return;
          }
        }

        int waiting = queue.getNumberOfEntries();
        boolean confirmed =
            walkInQueueView.displayAddWalkInConfirmationScreen(
                guest,
                member,
                roomType,
                waiting + 1,
                waiting,
                standardReservationRepo.getQueueCapacity(roomType));
        if (!confirmed) {
          continue;
        }

        LocalDateTime now = LocalDateTime.now();
        Reservation walkIn =
            new Reservation(
                standardReservationRepo.generateReservationId(),
                guest.getGuestId(),
                standardReservationRepo.generateConfirmationNumber(vipReservationRepo),
                roomType,
                Reservation.Status.WAITING,
                false,
                0,
                now,
                now);

        standardReservationRepo.addReservation(walkIn);

        walkInQueueView.displayAddWalkInSuccessScreen(
            walkIn,
            guest,
            queue.getPosition(walkIn),
            queue.getNumberOfEntries(),
            standardReservationRepo.getQueueCapacity(roomType));
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleAllocateNext(Room.RoomType roomType) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    if (queue.isEmpty()) {
      ConsoleUtil.printError("Nobody is standing in the " + roomType.name() + " line!");
      return;
    }

    int vacantRooms = countVacantCleanRooms(roomType);
    int vipWaiting = countVipWaiting(roomType);

    if (vacantRooms == 0) {
      walkInQueueView.displayNoVacantRoomScreen(roomType, vipWaiting);
      return;
    }

    // High tier members bypass this line, so a free room only reaches the standard queue once
    // every waiting VIP for that room type could already have been given one.
    if (vacantRooms <= vipWaiting) {
      walkInQueueView.displayAllocationBlockedScreen(roomType, vacantRooms, vipWaiting);
      return;
    }

    allocateFrontToRoom(roomType, vipWaiting, false);
  }

  private void handleOverrideAllocate(Room.RoomType roomType) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    if (queue.isEmpty()) {
      ConsoleUtil.printError("Nobody is standing in the " + roomType.name() + " line!");
      return;
    }

    int vacantRooms = countVacantCleanRooms(roomType);
    int vipWaiting = countVipWaiting(roomType);

    if (vacantRooms == 0) {
      walkInQueueView.displayNoVacantRoomScreen(roomType, vipWaiting);
      return;
    }

    if (vacantRooms > vipWaiting) {
      walkInQueueView.displayNothingToOverrideScreen(roomType, vacantRooms);
      return;
    }

    Reservation front = queue.peek();
    Guest guest = guestRepo.findById(front.getGuestId());

    boolean authorised =
        walkInQueueView.displayOverrideAuthorisationScreen(
            roomType, front, guest, vacantRooms, vipWaiting);
    if (!authorised) {
      return;
    }

    allocateFrontToRoom(roomType, vipWaiting, true);
  }

  private void allocateFrontToRoom(Room.RoomType roomType, int vipWaiting, boolean overridden) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);

    Room room = roomRepo.findVacantCleanRoom(roomType);
    if (room == null) {
      ConsoleUtil.printError("No VACANT & CLEAN " + roomType.name() + " room is available!");
      return;
    }

    Reservation front = queue.peek();
    if (front == null) {
      ConsoleUtil.printError("Nobody is standing in the " + roomType.name() + " line!");
      return;
    }

    Guest guest = guestRepo.findById(front.getGuestId());
    Member member =
        (guest != null && guest.getMemberId() != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;

    boolean confirmed =
        walkInQueueView.displayAllocateConfirmationScreen(
            front,
            guest,
            member,
            room,
            StandardReservationRepo.GRACE_MINUTES,
            vipWaiting,
            overridden);
    if (!confirmed) {
      return;
    }

    Reservation allocated = standardReservationRepo.allocateFront(roomType);
    if (allocated == null) {
      ConsoleUtil.printError("The line emptied before the room could be assigned!");
      return;
    }

    room.setStatus(Room.Status.OCCUPIED);
    room.setReservationConfirmationNumber(allocated.getConfirmationNumber());
    roomRepo.updateRoom(room);

    walkInQueueView.displayAllocateSuccessScreen(
        allocated, guest, room, queue.getNumberOfEntries(), overridden);
  }

  private void handleCheckInHold(ListInterface<Reservation> holds, Room.RoomType roomType) {
    if (holds == null || holds.isEmpty()) {
      ConsoleUtil.printError("No " + roomType.name() + " rooms are currently on hold!");
      return;
    }

    Integer selection = walkInQueueView.promptHoldSelection(holds.getNumberOfEntries());
    if (selection == null) {
      return;
    }

    Reservation hold = holds.getEntry(selection);
    if (hold == null) {
      ConsoleUtil.printError("Invalid hold selection!");
      return;
    }

    Guest guest = guestRepo.findById(hold.getGuestId());
    Room room = standardReservationRepo.findHeldRoom(hold, roomRepo);

    Integer stayDays = walkInQueueView.promptStayDays(hold, guest, room);
    if (stayDays == null) {
      return;
    }

    standardReservationRepo.checkIn(hold, stayDays);
    walkInQueueView.displayCheckInSuccessScreen(hold, guest, room, stayDays);
  }

  private void handleCloseQueue(Room.RoomType roomType) {
    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);
    if (queue.isEmpty()) {
      ConsoleUtil.printError("The " + roomType.name() + " line is already empty!");
      return;
    }

    boolean confirmed =
        walkInQueueView.displayCloseQueueConfirmationScreen(roomType, queue.getNumberOfEntries());
    if (!confirmed) {
      return;
    }

    int closed = standardReservationRepo.closeQueue(roomType);
    walkInQueueView.displayCloseQueueSuccessScreen(roomType, closed);
  }

  private void handleRowAction(
      ListInterface<Reservation> lineRows, int indexOnPage, int page, Room.RoomType roomType) {

    int actualIndex = (page - 1) * PAGE_SIZE + indexOnPage;
    if (actualIndex < 1 || actualIndex > lineRows.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = lineRows.getEntry(actualIndex);
    if (selected == null) return;

    QueueInterface<Reservation> queue = standardReservationRepo.getQueueByRoomType(roomType);

    while (true) {
      try {
        int action = walkInQueueView.displayRowActionSubmenu(selected);

        if (action == 1) {
          Guest guest = guestRepo.findById(selected.getGuestId());
          Member member =
              (guest != null && guest.getMemberId() != null)
                  ? memberRepo.findById(guest.getMemberId())
                  : null;
          walkInQueueView.displayReservationDetailScreen(
              selected, guest, member, queue.getPosition(selected), queue.getNumberOfEntries());
        } else if (action == 2) {
          Guest guest = guestRepo.findById(selected.getGuestId());
          boolean confirmed =
              walkInQueueView.displayCancelConfirmationScreen(
                  selected, guest, queue.getPosition(selected));
          if (confirmed) {
            standardReservationRepo.cancelReservation(selected);
            return;
          }
        } else if (action == 3) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String[] handleFilterMenu(String currentSearch, String currentTier) {
    String search = currentSearch;
    String tier = currentTier;

    while (true) {
      try {
        int choice = walkInQueueView.displayFilterMainMenu(search, tier);

        if (choice == 1) {
          search = handleSearchSubmenu(search);
        } else if (choice == 2) {
          tier = handleTierSubmenu(tier);
        } else if (choice == 3) {
          search = null;
          tier = null;
        } else if (choice == 4) {
          return new String[] {search, tier};
        } else if (choice == 5) {
          return new String[] {currentSearch, currentTier};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchSubmenu(String currentSearch) {
    while (true) {
      try {
        int option = walkInQueueView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          String input = walkInQueueView.promptSearchInput();
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

  private String handleTierSubmenu(String currentTier) {
    while (true) {
      try {
        int choice = walkInQueueView.displayTierSubmenu(currentTier);
        if (choice == 1) return "DIAMOND";
        if (choice == 2) return "GOLD";
        if (choice == 3) return "SILVER";
        if (choice == 4) return "NON-MEMBER";
        if (choice == 5) return null;
        if (choice == 6) return currentTier;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Reservation> filterAndSortLine(
      ListInterface<Reservation> source,
      QueueInterface<Reservation> queue,
      String search,
      String tier,
      String sort) {

    ListInterface<Reservation> filtered = new ArrayList<>();
    if (source == null || source.isEmpty()) {
      return filtered;
    }

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());
      Member m =
          (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

      if (!matchesSearch(r, g, search)) continue;
      if (!matchesTier(m, tier)) continue;

      filtered.add(r);
    }

    if ("WAIT TIME (LONGEST -> SHORTEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareArrival(a, b));
    } else if ("WAIT TIME (SHORTEST -> LONGEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareArrival(b, a));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(a).compareToIgnoreCase(guestNameOf(b)));
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("STRIKES (HIGHEST -> LOWEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> Integer.compare(strikesOf(b), strikesOf(a)));
    } else {
      // FIFO: the snapshot already arrives front to back, so the queue itself defines the order.
      filtered.sort((a, b) -> Integer.compare(queue.getPosition(a), queue.getPosition(b)));
    }

    return filtered;
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

  private boolean matchesTier(Member m, String tier) {
    if (tier == null) return true;

    String actual = (m != null && m.getTier() != null) ? m.getTier().name() : "NON-MEMBER";
    return tier.equalsIgnoreCase(actual);
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

  private Reservation findQueuedReservationForGuest(Room.RoomType roomType, String guestId) {
    ListInterface<Reservation> snapshot = standardReservationRepo.snapshotQueue(roomType);
    for (int i = 1; i <= snapshot.getNumberOfEntries(); i++) {
      Reservation r = snapshot.getEntry(i);
      if (r != null && guestId.equalsIgnoreCase(r.getGuestId())) {
        return r;
      }
    }
    return null;
  }

  // A guest id typed into the search box is not a name, so it must not be pre-filled as one.
  private String nameSuggestionFrom(String term) {
    if (term == null || term.toUpperCase().startsWith("G-")) {
      return null;
    }
    return term;
  }

  // A free room may only be handed straight to this member while one is left over after every
  // VIP already on the waitlist is covered. That is the same guard handleAllocateNext applies,
  // so arriving late cannot buy a better place than the VIPs who have been waiting.
  private int promptVipRouting(Guest guest, Member member, Room.RoomType roomType, int lineLength) {
    int vacantRooms = countVacantCleanRooms(roomType);
    int vipWaiting = countVipWaiting(roomType);

    return walkInQueueView.displayVipArrivalScreen(
        guest, member, roomType, vacantRooms, vipWaiting, lineLength, vacantRooms > vipWaiting);
  }

  private boolean assignRoomDirectly(Guest guest, Member member, Room.RoomType roomType) {
    Room room = roomRepo.findVacantCleanRoom(roomType);
    if (room == null) {
      ConsoleUtil.printError("No VACANT & CLEAN " + roomType.name() + " room is available!");
      return false;
    }

    int vacantRooms = countVacantCleanRooms(roomType);
    int vipWaiting = countVipWaiting(roomType);

    boolean confirmed =
        walkInQueueView.displayVipDirectAssignConfirmationScreen(
            guest, member, room, StandardReservationRepo.GRACE_MINUTES, vacantRooms, vipWaiting);
    if (!confirmed) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();

    // queueArrivalTime is stamped even though the guest never queues, so the wait-time report
    // still counts this arrival and records it as the zero-wait case it actually was.
    Reservation direct =
        new Reservation(
            standardReservationRepo.generateReservationId(),
            guest.getGuestId(),
            standardReservationRepo.generateConfirmationNumber(vipReservationRepo),
            roomType,
            Reservation.Status.ALLOCATED,
            false,
            0,
            now,
            now);

    if (!standardReservationRepo.allocateDirect(direct)) {
      ConsoleUtil.printError("The reservation could not be recorded!");
      return false;
    }

    room.setStatus(Room.Status.OCCUPIED);
    room.setReservationConfirmationNumber(direct.getConfirmationNumber());
    roomRepo.updateRoom(room);

    walkInQueueView.displayVipDirectAssignSuccessScreen(
        direct, guest, member, room, StandardReservationRepo.GRACE_MINUTES);
    return true;
  }

  private int countVacantCleanRooms(Room.RoomType roomType) {
    ListInterface<Room> rooms = roomRepo.getRoomList();
    int count = 0;
    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room != null
          && room.getRoomType() == roomType
          && room.getStatus() == Room.Status.VACANT_CLEAN) {
        count++;
      }
    }
    return count;
  }

  private int countVipWaiting(Room.RoomType roomType) {
    ListInterface<Reservation> vipLine = vipReservationRepo.getListByRoomType(roomType);
    return (vipLine == null) ? 0 : vipLine.getNumberOfEntries();
  }
}
