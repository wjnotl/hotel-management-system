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
import view.booking.AdvanceBookingView;

public class AdvanceBookingController {
  private static final int PAGE_SIZE = 10;
  private static final String DEFAULT_SORT = "BOOKED AT (NEWEST -> OLDEST)";

  private final AdvanceBookingView advanceBookingView = new AdvanceBookingView();
  private final StandardReservationRepo standardReservationRepo;
  private final VipReservationRepo vipReservationRepo;
  private final GuestRepo guestRepo;
  private final MemberRepo memberRepo;
  private final RoomRepo roomRepo;

  public AdvanceBookingController(
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

  public void startAdvanceBookingManagement() {
    int currentPage = 1;
    String searchQuery = null;
    String roomTypeFilter = null;
    String sortCriteria = DEFAULT_SORT;

    while (true) {
      try {
        standardReservationRepo.sweepLapsedHolds(roomRepo, guestRepo);

        ListInterface<Reservation> reserved = collectReserved();
        ListInterface<Reservation> rows =
            filterAndSort(reserved, searchQuery, roomTypeFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            advanceBookingView.renderAdvanceScreen(
                rows,
                guestRepo.getGuestList(),
                countByRoomType(reserved, Room.RoomType.LUXURY),
                countByRoomType(reserved, Room.RoomType.SUITE),
                countByRoomType(reserved, Room.RoomType.STANDARD),
                searchQuery,
                roomTypeFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("A".equalsIgnoreCase(result.input)) {
          handleNewBooking();
          currentPage = 1;
        } else if ("M".equalsIgnoreCase(result.input)) {
          handleMarkArrivalByPrompt(rows);
          currentPage = 1;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(searchQuery, roomTypeFilter);
          searchQuery = filters[0];
          roomTypeFilter = filters[1];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = advanceBookingView.displaySortMenu();
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) rows.getNumberOfEntries() / PAGE_SIZE);
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
          handleRowAction(rows, result.getAsInt(), currentPage);
          currentPage = 1;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleNewBooking() {
    while (true) {
      try {
        String input = advanceBookingView.promptGuestInput();
        if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
          return;
        }

        String term = input.trim();
        Guest guest = guestRepo.findById(term);
        if (guest == null) {
          guest = guestRepo.findByName(term);
        }

        // Someone booking for the first time has no guest file yet, so the desk opens
        // one
        // rather than turning away a booking it is perfectly able to take.
        if (guest == null) {
          int choice = advanceBookingView.displayGuestNotFoundScreen(term);
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

        Member member =
            (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;

        if (member != null
            && member.getTier() != null
            && !advanceBookingView.displayVipNoticeScreen(guest, member)) {
          continue;
        }

        Room.RoomType roomType = advanceBookingView.promptRoomType(guest);
        if (roomType == null) {
          return;
        }

        if (!advanceBookingView.displayNewBookingConfirmationScreen(guest, member, roomType)) {
          continue;
        }

        // RESERVED carries no queueArrivalTime: the guest has bought a room type, not a
        // place
        // in the line, so nothing is enqueued until they physically turn up.
        Reservation booking =
            new Reservation(
                standardReservationRepo.generateReservationId(),
                guest.getGuestId(),
                standardReservationRepo.generateConfirmationNumber(),
                roomType,
                Reservation.Status.RESERVED,
                false,
                0,
                LocalDateTime.now(),
                null,
                false);

        standardReservationRepo.addReservation(booking);
        advanceBookingView.displayNewBookingSuccessScreen(booking, guest);
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleMarkArrivalByPrompt(ListInterface<Reservation> rows) {
    if (rows == null || rows.isEmpty()) {
      ConsoleUtil.printError("There are no advance bookings awaiting arrival!");
      return;
    }

    Integer selection =
        ConsoleUtil.getIntegerInput(
            "\nEnter the row number of the guest who arrived (blank or 'C' to cancel): ",
            1,
            rows.getNumberOfEntries());
    if (selection == null) {
      return;
    }

    markArrival(rows.getEntry(selection));
  }

  private void markArrival(Reservation booking) {
    if (booking == null) {
      ConsoleUtil.printError("Invalid booking selection!");
      return;
    }

    Guest guest = guestRepo.findById(booking.getGuestId());

    Reservation alreadyQueued =
        findQueuedReservationForGuest(booking.getRoomType(), booking.getGuestId());
    if (alreadyQueued != null) {
      advanceBookingView.displayAlreadyInLineScreen(guest, alreadyQueued);
      return;
    }

    QueueInterface<Reservation> queue =
        standardReservationRepo.getQueueByRoomType(booking.getRoomType());

    Member member =
        (guest != null && guest.getMemberId() != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;

    // Marking arrival is the moment this booking becomes a person at the counter,
    // which is
    // exactly when a loyalty tier stops being decoration and starts deciding who
    // waits.
    if (member != null && member.getTier() != null) {
      int vacantRooms = countVacantCleanRooms(booking.getRoomType());
      int vipWaiting = countVipWaiting(booking.getRoomType());

      int decision =
          advanceBookingView.displayVipArrivalScreen(
              guest,
              member,
              booking.getRoomType(),
              vacantRooms,
              vipWaiting,
              queue.getNumberOfEntries(),
              vacantRooms > vipWaiting);

      if (decision == 1) {
        assignRoomDirectly(booking, guest, member);
        return;
      } else if (decision == 3) {
        return;
      }
    }

    boolean confirmed =
        advanceBookingView.displayMarkArrivalConfirmationScreen(
            booking,
            guest,
            queue.getNumberOfEntries(),
            standardReservationRepo.getQueueCapacity(booking.getRoomType()));
    if (!confirmed) {
      return;
    }

    if (!standardReservationRepo.joinQueue(booking)) {
      ConsoleUtil.printError("This booking could not be moved into the line!");
      return;
    }

    advanceBookingView.displayMarkArrivalSuccessScreen(
        booking, guest, queue.getPosition(booking), queue.getNumberOfEntries());
  }

  private void handleRowAction(ListInterface<Reservation> rows, int indexOnPage, int page) {
    int actualIndex = (page - 1) * PAGE_SIZE + indexOnPage;
    if (actualIndex < 1 || actualIndex > rows.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid row selection index!");
      return;
    }

    Reservation selected = rows.getEntry(actualIndex);
    if (selected == null) return;

    while (true) {
      try {
        int action = advanceBookingView.displayRowActionSubmenu(selected);

        if (action == 1) {
          Guest guest = guestRepo.findById(selected.getGuestId());
          Member member =
              (guest != null && guest.getMemberId() != null)
                  ? memberRepo.findById(guest.getMemberId())
                  : null;
          advanceBookingView.displayDetailScreen(selected, guest, member);
        } else if (action == 2) {
          markArrival(selected);
          return;
        } else if (action == 3) {
          Guest guest = guestRepo.findById(selected.getGuestId());
          if (advanceBookingView.displayCancelConfirmationScreen(selected, guest)) {
            standardReservationRepo.cancelReservation(selected);
            return;
          }
        } else if (action == 4) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String[] handleFilterMenu(String currentSearch, String currentRoomType) {
    String search = currentSearch;
    String roomType = currentRoomType;

    while (true) {
      try {
        int choice = advanceBookingView.displayFilterMainMenu(search, roomType);

        if (choice == 1) {
          search = handleSearchSubmenu(search);
        } else if (choice == 2) {
          roomType = handleRoomTypeSubmenu(roomType);
        } else if (choice == 3) {
          search = null;
          roomType = null;
        } else if (choice == 4) {
          return new String[] {search, roomType};
        } else if (choice == 5) {
          return new String[] {currentSearch, currentRoomType};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSearchSubmenu(String currentSearch) {
    while (true) {
      try {
        int option = advanceBookingView.displaySearchSubmenu(currentSearch);
        if (option == 1) {
          String input = advanceBookingView.promptSearchInput();
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

  private String handleRoomTypeSubmenu(String currentRoomType) {
    while (true) {
      try {
        int choice = advanceBookingView.displayRoomTypeSubmenu(currentRoomType);
        if (choice == 1) return "LUXURY";
        if (choice == 2) return "SUITE";
        if (choice == 3) return "STANDARD";
        if (choice == 4) return null;
        if (choice == 5) return currentRoomType;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Reservation> collectReserved() {
    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    ListInterface<Reservation> reserved = new ArrayList<>();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && r.getStatus() == Reservation.Status.RESERVED) {
        reserved.add(r);
      }
    }
    return reserved;
  }

  private ListInterface<Reservation> filterAndSort(
      ListInterface<Reservation> source, String search, String roomTypeFilter, String sort) {

    ListInterface<Reservation> filtered = new ArrayList<>();

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r == null) continue;

      Guest g = guestRepo.findById(r.getGuestId());

      if (!matchesSearch(r, g, search)) continue;
      if (roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase(r.getRoomType().name())) {
        continue;
      }

      filtered.add(r);
    }

    if ("BOOKED AT (OLDEST -> NEWEST)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> compareBookedAt(a, b));
    } else if ("GUEST NAME (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(a).compareToIgnoreCase(guestNameOf(b)));
    } else if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> guestNameOf(b).compareToIgnoreCase(guestNameOf(a)));
    } else if ("ROOM TYPE (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((a, b) -> a.getRoomType().name().compareTo(b.getRoomType().name()));
    } else {
      filtered.sort((a, b) -> compareBookedAt(b, a));
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

  private int compareBookedAt(Reservation a, Reservation b) {
    LocalDateTime first = a.getReservationTime();
    LocalDateTime second = b.getReservationTime();
    if (first == null && second == null) return 0;
    if (first == null) return -1;
    if (second == null) return 1;
    return first.compareTo(second);
  }

  private int countByRoomType(ListInterface<Reservation> source, Room.RoomType roomType) {
    int count = 0;
    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Reservation r = source.getEntry(i);
      if (r != null && r.getRoomType() == roomType) {
        count++;
      }
    }
    return count;
  }

  private String guestNameOf(Reservation r) {
    Guest g = guestRepo.findById(r.getGuestId());
    return (g != null && g.getName() != null) ? g.getName() : "";
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

  // A guest id typed into the search box is not a name, so it must not be
  // pre-filled as one.
  private String nameSuggestionFrom(String term) {
    if (term == null || term.toUpperCase().startsWith("G-")) {
      return null;
    }
    return term;
  }

  private void assignRoomDirectly(Reservation booking, Guest guest, Member member) {
    Room room = roomRepo.findVacantCleanRoom(booking.getRoomType());
    if (room == null) {
      ConsoleUtil.printError(
          "No VACANT & CLEAN " + booking.getRoomType().name() + " room is available!");
      return;
    }

    boolean confirmed =
        advanceBookingView.displayVipDirectAssignConfirmationScreen(
            booking, guest, member, room, StandardReservationRepo.GRACE_MINUTES);
    if (!confirmed) {
      return;
    }

    if (!standardReservationRepo.allocateDirect(booking)) {
      ConsoleUtil.printError("This booking could not be moved onto a room!");
      return;
    }

    booking.setRoomNumber(room.getRoomNumber());
    standardReservationRepo.updateReservation(booking);
    room.setStatus(Room.Status.OCCUPIED);
    roomRepo.updateRoom(room);

    advanceBookingView.displayVipDirectAssignSuccessScreen(
        booking, guest, room, StandardReservationRepo.GRACE_MINUTES);
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
