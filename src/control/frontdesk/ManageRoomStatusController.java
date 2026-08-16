package control.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import entity.Room;
import repo.GuestRepo;
import repo.ReservationRepo;
import repo.RoomRepo;
import repo.RoomStatusHistoryRepo;
import util.ConsoleUtil;
import view.frontdesk.ManageRoomStatusView;

public class ManageRoomStatusController {
  private static final int PAGE_SIZE = 10;

  private final ManageRoomStatusView roomStatusView = new ManageRoomStatusView();
  private final RoomRepo roomRepo;
  private final RoomStatusHistoryRepo roomStatusHistoryRepo;
  private final ReservationRepo reservationRepo;
  private final GuestRepo guestRepo;

  public ManageRoomStatusController(
      RoomRepo roomRepo,
      ReservationRepo reservationRepo,
      GuestRepo guestRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo) {
    this.roomRepo = roomRepo;
    this.reservationRepo = reservationRepo;
    this.guestRepo = guestRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
  }

  public void start() {
    int currentPage = 1;

    String roomNumberFilter = null;
    String roomTypeFilter = null;
    String roomStatusFilter = null;
    String sortCriteria = "ROOM NUMBER (LOW -> HIGH)";

    while (true) {
      try {
        ListInterface<Room> allRooms = roomRepo.getRoomList();
        ListInterface<Room> filteredList =
            filterAndSortRooms(
                allRooms, roomNumberFilter, roomTypeFilter, roomStatusFilter, sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            roomStatusView.renderRoomStatusScreen(
                filteredList,
                roomNumberFilter,
                roomTypeFilter,
                roomStatusFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters = handleFilterMenu(roomNumberFilter, roomTypeFilter, roomStatusFilter);
          roomNumberFilter = filters[0];
          roomTypeFilter = filters[1];
          roomStatusFilter = filters[2];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Table refreshes on loop - rooms are re-fetched from RoomRepo each pass.
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalMatches = filteredList.getNumberOfEntries();
          int totalPages = (int) Math.ceil((double) totalMatches / PAGE_SIZE);
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
          handleRoomAction(filteredList, result.getAsInt(), currentPage, PAGE_SIZE);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- ROOM ACTION DISPATCH ---
  private void handleRoomAction(ListInterface<Room> list, int indexOnPage, int page, int pageSize) {
    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid room row selection!");
      return;
    }

    Room room = list.getEntry(actualIndex);
    if (room == null) return;

    while (true) {
      try {
        // Room may have been mutated by a previous loop iteration - re-fetch the latest copy.
        Room current = roomRepo.findByRoomNumber(room.getRoomNumber());
        if (current == null) return;
        room = current;

        Reservation linkedReservation = findReservationByConfirmationNumber(room);
        Guest linkedGuest =
            (linkedReservation != null) ? guestRepo.findById(linkedReservation.getGuestId()) : null;

        int action = roomStatusView.displayRoomActionSubmenu(room, linkedReservation, linkedGuest);

        if (action == 1) {
          handleChangeRoom(room);
        } else if (action == 2) {
          handleMarkAvailable(room);
        } else if (action == 3) {
          handleMarkOccupied(room);
        } else if (action == 4) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- ACTION 1: CHANGE ROOM ---
  private void handleChangeRoom(Room room) {
    if (room.getReservationConfirmationNumber() == null) {
      ConsoleUtil.printError(
          "This room has no active reservation to move - use Assign Room instead!");
      return;
    }

    String targetRoomNumber = roomStatusView.promptTargetRoomNumberInput();
    if (targetRoomNumber == null
        || targetRoomNumber.trim().isEmpty()
        || "C".equalsIgnoreCase(targetRoomNumber.trim())) {
      return; // Cancelled
    }
    targetRoomNumber = targetRoomNumber.trim();

    if (targetRoomNumber.equalsIgnoreCase(room.getRoomNumber())) {
      ConsoleUtil.printError("Target room must be different from the current room!");
      return;
    }

    Room targetRoom = roomRepo.findByRoomNumber(targetRoomNumber);
    if (targetRoom == null) {
      ConsoleUtil.printError("Room \"" + targetRoomNumber + "\" does not exist!");
      return;
    }

    if (targetRoom.getStatus() != Room.Status.VACANT_CLEAN) {
      ConsoleUtil.printError(
          "Room "
              + targetRoom.getRoomNumber()
              + " is "
              + targetRoom.getStatus().name()
              + " - not available!");
      return;
    }

    if (targetRoom.getRoomType() != room.getRoomType()) {
      boolean confirmMismatch =
          ConsoleUtil.showConfirmMessage(
              "Target room is "
                  + targetRoom.getRoomType().name()
                  + " but current room is "
                  + room.getRoomType().name()
                  + ". Move anyway?");
      if (!confirmMismatch) return;
    }

    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            "Move reservation "
                + room.getReservationConfirmationNumber()
                + " from Room "
                + room.getRoomNumber()
                + " to Room "
                + targetRoom.getRoomNumber()
                + "?");
    if (!confirmed) return;

    String confNum = room.getReservationConfirmationNumber();

    Room.Status oldRoomPreviousStatus = room.getStatus();
    room.setStatus(Room.Status.VACANT_CLEAN);
    room.setReservationConfirmationNumber(null);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(
        room.getRoomNumber(), oldRoomPreviousStatus, Room.Status.VACANT_CLEAN);

    Room.Status targetPreviousStatus = targetRoom.getStatus();
    targetRoom.setStatus(Room.Status.OCCUPIED);
    targetRoom.setReservationConfirmationNumber(confNum);
    roomRepo.updateRoom(targetRoom);
    roomStatusHistoryRepo.recordStatusChange(
        targetRoom.getRoomNumber(), targetPreviousStatus, Room.Status.OCCUPIED);

    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: ROOM CHANGED");
    System.out.println(
        "Confirmation "
            + confNum
            + " moved from Room "
            + room.getRoomNumber()
            + " to Room "
            + targetRoom.getRoomNumber()
            + ".\n");
    ConsoleUtil.printContinueMessage();
  }

  // --- ACTION 2: MARK ROOM AS AVAILABLE ---
  private void handleMarkAvailable(Room room) {
    if (room.getStatus() == Room.Status.VACANT_CLEAN) {
      ConsoleUtil.printError("Room is already marked as Available!");
      return;
    }

    if (room.getReservationConfirmationNumber() != null) {
      boolean confirmOrphan =
          ConsoleUtil.showConfirmMessage(
              "Room "
                  + room.getRoomNumber()
                  + " still has confirmation "
                  + room.getReservationConfirmationNumber()
                  + " attached. Marking it Available will detach the reservation. Continue?");
      if (!confirmOrphan) return;
    } else {
      boolean confirmed =
          ConsoleUtil.showConfirmMessage("Mark Room " + room.getRoomNumber() + " as Available?");
      if (!confirmed) return;
    }

    Room.Status previousStatus = room.getStatus();
    room.setStatus(Room.Status.VACANT_CLEAN);
    room.setReservationConfirmationNumber(null);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(
        room.getRoomNumber(), previousStatus, Room.Status.VACANT_CLEAN);

    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: ROOM MARKED AS AVAILABLE");
    System.out.println("Room " + room.getRoomNumber() + " is now Available (Vacant & Clean).\n");
    ConsoleUtil.printContinueMessage();
  }

  // --- ACTION 3: MARK ROOM AS OCCUPIED ---
  private void handleMarkOccupied(Room room) {
    if (room.getStatus() == Room.Status.OCCUPIED) {
      ConsoleUtil.printError("Room is already marked as Occupied!");
      return;
    }

    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            "Mark Room "
                + room.getRoomNumber()
                + " as Occupied (manual override, no reservation)?");
    if (!confirmed) return;

    Room.Status previousStatus = room.getStatus();
    room.setStatus(Room.Status.OCCUPIED);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(
        room.getRoomNumber(), previousStatus, Room.Status.OCCUPIED);

    ConsoleUtil.clearScreen();
    System.out.println(">> STATUS: ROOM MARKED AS OCCUPIED");
    System.out.println("Room " + room.getRoomNumber() + " is now Occupied.\n");
    ConsoleUtil.printContinueMessage();
  }

  // --- RESERVATION LOOKUP HELPERS ---
  // Room links to Reservation via confirmationNumber (not reservationId), so this is a manual scan.
  private Reservation findReservationByConfirmationNumber(Room room) {
    if (room == null || room.getReservationConfirmationNumber() == null) return null;
    return findReservationByConfirmationNumberValue(room.getReservationConfirmationNumber());
  }

  private Reservation findReservationByConfirmationNumberValue(String confirmationNumber) {
    if (confirmationNumber == null) return null;
    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && confirmationNumber.equalsIgnoreCase(r.getConfirmationNumber())) {
        return r;
      }
    }
    return null;
  }

  // --- FILTER & SORT MENUS ---
  private String[] handleFilterMenu(String roomNumber, String roomType, String roomStatus) {
    String rNo = roomNumber;
    String rType = roomType;
    String rStatus = roomStatus;

    while (true) {
      try {
        int choice = roomStatusView.displayFilterMainMenu(rNo, rType, rStatus);

        if (choice == 1) {
          rNo = normalizeFilter(roomStatusView.promptRoomNumberFilterInput());
        } else if (choice == 2) {
          rType = handleRoomTypeSubmenu(rType);
        } else if (choice == 3) {
          rStatus = handleRoomStatusSubmenu(rStatus);
        } else if (choice == 4) {
          return new String[] {null, null, null};
        } else if (choice == 5) {
          return new String[] {rNo, rType, rStatus};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String normalizeFilter(String input) {
    if (input == null || input.trim().isEmpty() || "C".equalsIgnoreCase(input.trim())) {
      return null;
    }
    return input.trim();
  }

  private String handleRoomTypeSubmenu(String current) {
    while (true) {
      try {
        int choice = roomStatusView.displayRoomTypeSubmenu(current);
        if (choice == 1) return "LUXURY";
        if (choice == 2) return "SUITE";
        if (choice == 3) return "STANDARD";
        if (choice == 4) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleRoomStatusSubmenu(String current) {
    while (true) {
      try {
        int choice = roomStatusView.displayRoomStatusSubmenu(current);
        if (choice == 1) return "DIRTY";
        if (choice == 2) return "CLEANING";
        if (choice == 3) return "INSPECTED";
        if (choice == 4) return "VACANT_CLEAN";
        if (choice == 5) return "OCCUPIED";
        if (choice == 6) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        String selectedSort = roomStatusView.displaySortMenu();
        return (selectedSort == null) ? currentSort : selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Room> filterAndSortRooms(
      ListInterface<Room> source,
      String roomNumber,
      String roomType,
      String roomStatus,
      String sort) {
    ListInterface<Room> filtered = new ArrayList<>();
    if (source == null || source.isEmpty()) return filtered;

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Room r = source.getEntry(i);
      if (r == null) continue;

      boolean matchesRoomNumber =
          roomNumber == null
              || (r.getRoomNumber() != null
                  && r.getRoomNumber().toLowerCase().contains(roomNumber.toLowerCase()));
      boolean matchesRoomType =
          roomType == null || roomType.equalsIgnoreCase(r.getRoomType().name());
      boolean matchesRoomStatus =
          roomStatus == null || roomStatus.equalsIgnoreCase(r.getStatus().name());

      if (matchesRoomNumber && matchesRoomType && matchesRoomStatus) {
        filtered.add(r);
      }
    }

    if ("ROOM TYPE (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort(
          (r1, r2) -> r1.getRoomType().name().compareToIgnoreCase(r2.getRoomType().name()));
    } else {
      // Default: ROOM NUMBER (LOW -> HIGH)
      filtered.sort((r1, r2) -> r1.getRoomNumber().compareToIgnoreCase(r2.getRoomNumber()));
    }

    return filtered;
  }
}
