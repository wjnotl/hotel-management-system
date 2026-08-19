package control.frontdesk;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.LinkedList;
import adt.LinkedStack;
import adt.ListInterface;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Reservation;
import entity.Room;
import java.time.LocalDateTime;
import repo.GuestRepo;
import repo.HousekeepingTaskRepo;
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
  private final HousekeepingTaskRepo housekeepingTaskRepo;

  public ManageRoomStatusController(
      RoomRepo roomRepo,
      ReservationRepo reservationRepo,
      GuestRepo guestRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo,
      HousekeepingTaskRepo housekeepingTaskRepo) {
    this.roomRepo = roomRepo;
    this.reservationRepo = reservationRepo;
    this.guestRepo = guestRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
    this.housekeepingTaskRepo = housekeepingTaskRepo;
  }

  // Queues a task for a room that just went DIRTY outside of Housekeeping's own Add Task flow —
  // e.g. front desk manually marking a room dirty, or vacating a room during a room change.
  // No-ops if the room already has an active task, so this can never create a duplicate.
  private void autoQueueCleaningTask(String roomNumber, HousekeepingTask.TaskType taskType) {
    if (housekeepingTaskRepo == null || housekeepingTaskRepo.hasActiveTask(roomNumber)) return;
    housekeepingTaskRepo.enqueueTask(
        new HousekeepingTask(
            housekeepingTaskRepo.generateTaskId(),
            roomNumber,
            taskType,
            HousekeepingTask.Status.PENDING,
            null,
            false,
            LocalDateTime.now()));
  }

  // =========================================================================
  // ENTRY POINT
  // =========================================================================

  public void start() {
    int currentPage = 1;
    String searchQuery = null;
    String roomTypeFilter = null;
    String roomStatusFilter = null;
    String sortCriteria = "ROOM NUMBER (LOW -> HIGH)";

    while (true) {
      try {
        ArrayList<ManageRoomStatusView.RoomRowDTO> allDtos = buildRoomRowDTOs();
        ArrayList<ManageRoomStatusView.RoomRowDTO> filtered =
            filterAndSortDTOs(allDtos, searchQuery, roomTypeFilter, roomStatusFilter, sortCriteria);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ConsoleUtil.GetMenuInputResult result =
            roomStatusView.renderRoomStatusScreen(
                filtered,
                searchQuery,
                roomTypeFilter,
                roomStatusFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        String raw = result.input.trim();

        if ("E".equalsIgnoreCase(raw)) {
          return;
        } else if ("R".equalsIgnoreCase(raw)) {
          // Re-fetched at top of loop
        } else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          String[] filters = handleFilterMenu(searchQuery, roomTypeFilter, roomStatusFilter);
          searchQuery = filters[0];
          roomTypeFilter = filters[1];
          roomStatusFilter = filters[2];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if (result.isNumber) {
          int actualIndex = (currentPage - 1) * PAGE_SIZE + result.getAsInt();
          if (actualIndex < 1 || actualIndex > total) {
            ConsoleUtil.printError("Invalid room row selection!");
            continue;
          }
          ManageRoomStatusView.RoomRowDTO dto = filtered.getEntry(actualIndex);
          if (dto != null) handleRoomAction(dto.roomNumber);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // ROOM ACTION SUBMENU
  // =========================================================================

  private void handleRoomAction(String roomNumber) {
    while (true) {
      try {
        Room room = roomRepo.findByRoomNumber(roomNumber);
        if (room == null) {
          ConsoleUtil.printError("Room not found.");
          return;
        }

        Reservation linked = findActiveReservationByRoomNumber(roomNumber);
        Guest linkedGuest = (linked != null) ? guestRepo.findById(linked.getGuestId()) : null;

        ManageRoomStatusView.RoomDetailDTO detailDto =
            buildRoomDetailDTO(room, linked, linkedGuest);

        int action = roomStatusView.displayRoomActionSubmenu(detailDto);

        if (action == 1) handleChangeRoom(room, linked);
        else if (action == 2) handleMarkAvailable(room, linked);
        else if (action == 3) handleMarkOccupied(room);
        else if (action == 4) handleMarkDirty(room);
        else if (action == 5) return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // ACTION 1 — CHANGE ROOM
  // =========================================================================

  private void handleChangeRoom(Room room, Reservation linked) {
    if (linked == null) {
      ConsoleUtil.printError(
          "This room has no active reservation to move. Use Mark Occupied instead.");
      return;
    }

    // Build available rooms using LinkedList (sequential add during scan)
    LinkedList<ManageRoomStatusView.RoomRowDTO> availableBuffer = new LinkedList<>();
    ListInterface<Room> allRooms = roomRepo.getRoomList();
    for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
      Room r = allRooms.getEntry(i);
      if (r == null
          || r.getStatus() != Room.Status.VACANT_CLEAN
          || r.getRoomNumber().equalsIgnoreCase(room.getRoomNumber())) continue;
      availableBuffer.add(
          new ManageRoomStatusView.RoomRowDTO(
              r.getRoomNumber(),
              r.getRoomType().name(),
              r.getStatus().name(),
              String.format("%.2f", r.getPrice()),
              "N/A",
              "N/A"));
    }
    // Sort ascending by room number
    availableBuffer.sort((a, b) -> nullSafeCompare(a.roomNumber, b.roomNumber));

    if (availableBuffer.isEmpty()) {
      ConsoleUtil.printError("No VACANT_CLEAN rooms available to move into.");
      return;
    }

    // Transfer to ArrayList for paged display
    ArrayList<ManageRoomStatusView.RoomRowDTO> availableRooms = new ArrayList<>();
    for (int i = 1; i <= availableBuffer.getNumberOfEntries(); i++) {
      availableRooms.add(availableBuffer.getEntry(i));
    }

    Guest linkedGuest = guestRepo.findById(linked.getGuestId());
    ManageRoomStatusView.RoomDetailDTO currentDto = buildRoomDetailDTO(room, linked, linkedGuest);
    String targetRoomNumber = handleAvailableRoomSelection(availableRooms, currentDto);
    if (targetRoomNumber == null) return;

    Room targetRoom = roomRepo.findByRoomNumber(targetRoomNumber);
    if (targetRoom == null) {
      ConsoleUtil.printError("Selected room not found.");
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
                + linked.getConfirmationNumber()
                + " from Room "
                + room.getRoomNumber()
                + " to Room "
                + targetRoom.getRoomNumber()
                + "?");
    if (!confirmed) return;

    // Old room → DIRTY
    Room.Status oldPrev = room.getStatus();
    room.setStatus(Room.Status.DIRTY);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(room.getRoomNumber(), oldPrev, Room.Status.DIRTY);
    autoQueueCleaningTask(room.getRoomNumber(), HousekeepingTask.TaskType.DEEP_CLEAN);

    // Target room → OCCUPIED
    Room.Status targetPrev = targetRoom.getStatus();
    targetRoom.setIsOccupied(true);
    roomRepo.updateRoom(targetRoom);
    roomStatusHistoryRepo.recordStatusChange(
        targetRoom.getRoomNumber(), targetPrev, targetRoom.getStatus());

    linked.setRoomNumber(targetRoom.getRoomNumber());
    reservationRepo.updateReservation(linked);

    roomStatusView.displayChangeRoomSuccess(
        room.getRoomNumber(),
        targetRoom.getRoomNumber(),
        oldPrev.name(),
        linked.getConfirmationNumber() != null ? linked.getConfirmationNumber() : "N/A");
  }

  private String handleAvailableRoomSelection(
      ArrayList<ManageRoomStatusView.RoomRowDTO> availableRooms,
      ManageRoomStatusView.RoomDetailDTO currentRoom) {
    int currentPage = 1;
    while (true) {
      try {
        int total = availableRooms.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        ConsoleUtil.GetMenuInputResult result =
            roomStatusView.renderAvailableRoomsTable(
                availableRooms, currentRoom, currentPage, PAGE_SIZE);

        String raw = result.input.trim();
        if ("C".equalsIgnoreCase(raw)) return null;
        else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if (result.isNumber) {
          int actualIndex = (currentPage - 1) * PAGE_SIZE + result.getAsInt();
          if (actualIndex >= 1 && actualIndex <= total) {
            ManageRoomStatusView.RoomRowDTO selected = availableRooms.getEntry(actualIndex);
            if (selected != null) return selected.roomNumber;
          } else {
            ConsoleUtil.printError("Invalid selection.");
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // ACTION 2 — MARK AVAILABLE (VACANT_CLEAN)
  // =========================================================================

  private void handleMarkAvailable(Room room, Reservation linked) {
    if (room.getStatus() == Room.Status.VACANT_CLEAN) {
      ConsoleUtil.printError("Room is already VACANT_CLEAN (Available)!");
      return;
    }
    if (linked != null) {
      boolean confirm =
          ConsoleUtil.showConfirmMessage(
              "Room "
                  + room.getRoomNumber()
                  + " has confirmation "
                  + linked.getConfirmationNumber()
                  + " attached. Marking Available will detach the reservation. Continue?");
      if (!confirm) return;
      linked.setRoomNumber(null);
      reservationRepo.updateReservation(linked);
    } else {
      boolean confirm =
          ConsoleUtil.showConfirmMessage(
              "Mark Room " + room.getRoomNumber() + " as VACANT_CLEAN (Available)?");
      if (!confirm) return;
    }
    Room.Status prev = room.getStatus();
    room.setStatus(Room.Status.VACANT_CLEAN);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(room.getRoomNumber(), prev, Room.Status.VACANT_CLEAN);
    roomStatusView.displayStatusChangeSuccess(
        room.getRoomNumber(), room.getRoomType().name(), "VACANT_CLEAN (Available)");
  }

  // =========================================================================
  // ACTION 3 — MARK OCCUPIED
  // =========================================================================

  private void handleMarkOccupied(Room room) {
    if (room.getIsOccupied()) {
      ConsoleUtil.printError("Room is already OCCUPIED!");
      return;
    }
    boolean confirm =
        ConsoleUtil.showConfirmMessage(
            "Mark Room " + room.getRoomNumber() + " as OCCUPIED (manual override)?");
    if (!confirm) return;
    Room.Status prev = room.getStatus();
    room.setIsOccupied(true);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(room.getRoomNumber(), prev, room.getStatus());
    roomStatusView.displayStatusChangeSuccess(
        room.getRoomNumber(), room.getRoomType().name(), "OCCUPIED");
  }

  // =========================================================================
  // ACTION 4 — MARK DIRTY
  // =========================================================================

  private void handleMarkDirty(Room room) {
    if (room.getStatus() == Room.Status.DIRTY) {
      ConsoleUtil.printError("Room is already DIRTY!");
      return;
    }
    boolean confirm =
        ConsoleUtil.showConfirmMessage(
            "Mark Room " + room.getRoomNumber() + " as DIRTY? This will flag it for housekeeping.");
    if (!confirm) return;
    Room.Status prev = room.getStatus();
    room.setStatus(Room.Status.DIRTY);
    roomRepo.updateRoom(room);
    roomStatusHistoryRepo.recordStatusChange(room.getRoomNumber(), prev, Room.Status.DIRTY);
    autoQueueCleaningTask(room.getRoomNumber(), HousekeepingTask.TaskType.STANDARD_CLEAN);
    roomStatusView.displayStatusChangeSuccess(
        room.getRoomNumber(), room.getRoomType().name(), "DIRTY (Pending Housekeeping)");
  }

  // =========================================================================
  // FILTER MENU
  // =========================================================================

  private String[] handleFilterMenu(String search, String roomType, String roomStatus) {
    String s = search;
    String rt = roomType;
    String rs = roomStatus;
    while (true) {
      try {
        int choice = roomStatusView.displayFilterMenu(s, rt, rs);
        if (choice == 1) {
          String input = roomStatusView.promptSearchInput(s);
          s = (input == null || input.trim().isEmpty()) ? null : input.trim();
        } else if (choice == 2) {
          rt = handleRoomTypeSubmenu(rt);
        } else if (choice == 3) {
          rs = handleRoomStatusSubmenu(rs);
        } else if (choice == 4) {
          return new String[] {null, null, null};
        } else if (choice == 5) {
          return new String[] {s, rt, rs};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
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
        String selected = roomStatusView.displaySortMenu(currentSort);
        return selected;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // DATA PROCESSING
  //
  // ADT usage:
  //   DoublyLinkedHashMap  — O(1) lookup: roomNumber→Reservation (active), guestId→Guest
  //   LinkedStack          — reverse-walk reservations to find latest active one per room
  //   LinkedList           — sequential DTO accumulation during build (cheap add)
  //   ArrayList            — final paged list for view (index-based access)
  // =========================================================================

  private ArrayList<ManageRoomStatusView.RoomRowDTO> buildRoomRowDTOs() {

    ListInterface<Reservation> allReservations = reservationRepo.getAllReservations();

    // Build roomNumber → active Reservation map (O(1) lookup during room iteration)
    DoublyLinkedHashMap<String, Reservation> activeResMap = new DoublyLinkedHashMap<>();
    for (int i = 1; i <= allReservations.getNumberOfEntries(); i++) {
      Reservation r = allReservations.getEntry(i);
      if (r == null || r.getRoomNumber() == null) continue;
      if (r.getStatus() == Reservation.Status.CHECKED_IN
          || r.getStatus() == Reservation.Status.ALLOCATED) {
        activeResMap.put(r.getRoomNumber().toLowerCase(), r);
      }
    }

    // Build guestId → Guest name map via LinkedStack (walk all guests, push each,
    // then peek/pop to add to map — demonstrates stack usage for accumulation)
    ListInterface<Guest> allGuests = guestRepo.getGuestList();
    LinkedStack<Guest> guestStack = new LinkedStack<>();
    for (int i = 1; i <= allGuests.getNumberOfEntries(); i++) {
      Guest g = allGuests.getEntry(i);
      if (g != null) guestStack.push(g);
    }
    DoublyLinkedHashMap<String, String> guestNameMap = new DoublyLinkedHashMap<>();
    while (!guestStack.isEmpty()) {
      Guest g = guestStack.pop();
      if (g.getGuestId() != null) {
        guestNameMap.put(g.getGuestId().toLowerCase(), g.getName());
      }
    }

    // Build DTOs using LinkedList (sequential add during iteration)
    LinkedList<ManageRoomStatusView.RoomRowDTO> dtoBuffer = new LinkedList<>();
    ListInterface<Room> allRooms = roomRepo.getRoomList();

    for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
      Room r = allRooms.getEntry(i);
      if (r == null) continue;

      // O(1) lookup for active reservation
      Reservation linked = activeResMap.get(r.getRoomNumber().toLowerCase());

      String guestName = "N/A";
      String confNum = "N/A";
      if (linked != null && linked.getGuestId() != null) {
        String name = guestNameMap.get(linked.getGuestId().toLowerCase());
        guestName = (name != null) ? name : "N/A";
        confNum = (linked.getConfirmationNumber() != null) ? linked.getConfirmationNumber() : "N/A";
      }

      String resId =
          (linked != null && linked.getReservationId() != null) ? linked.getReservationId() : null;

      dtoBuffer.add(
          new ManageRoomStatusView.RoomRowDTO(
              r.getRoomNumber(),
              r.getRoomType().name(),
              r.getIsOccupied() ? "OCCUPIED" : r.getStatus().name(),
              String.format("%.2f", r.getPrice()),
              guestName,
              confNum,
              resId));
    }

    // Transfer LinkedList → ArrayList for index-based paging
    ArrayList<ManageRoomStatusView.RoomRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= dtoBuffer.getNumberOfEntries(); i++) {
      result.add(dtoBuffer.getEntry(i));
    }
    return result;
  }

  private ManageRoomStatusView.RoomDetailDTO buildRoomDetailDTO(
      Room room, Reservation linked, Guest guest) {
    return new ManageRoomStatusView.RoomDetailDTO(
        room.getRoomNumber(),
        room.getRoomType().name(),
        room.getStatus().name(),
        String.format("%.2f", room.getPrice()),
        linked != null && linked.getReservationId() != null ? linked.getReservationId() : "N/A",
        linked != null && linked.getConfirmationNumber() != null
            ? linked.getConfirmationNumber()
            : "N/A",
        guest != null ? guest.getName() + "  (" + guest.getGuestId() + ")" : "N/A");
  }

  private ArrayList<ManageRoomStatusView.RoomRowDTO> filterAndSortDTOs(
      ArrayList<ManageRoomStatusView.RoomRowDTO> source,
      String search,
      String roomType,
      String roomStatus,
      String sort) {

    // LinkedList for sequential filter accumulation
    LinkedList<ManageRoomStatusView.RoomRowDTO> filteredBuffer = new LinkedList<>();

    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ManageRoomStatusView.RoomRowDTO dto = source.getEntry(i);
        if (dto == null) continue;

        boolean matchesSearch =
            search == null
                || search.trim().isEmpty()
                || containsIgnoreCase(dto.roomNumber, search)
                || containsIgnoreCase(dto.guestName, search)
                || containsIgnoreCase(dto.confirmationNumber, search);

        boolean matchesType = roomType == null || roomType.equalsIgnoreCase(dto.roomType);
        boolean matchesStatus = roomStatus == null || roomStatus.equalsIgnoreCase(dto.status);

        if (matchesSearch && matchesType && matchesStatus) {
          filteredBuffer.add(dto);
        }
      }
    }

    // Sort LinkedList in-place
    if ("ROOM NUMBER (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.roomNumber, a.roomNumber));
    } else if ("ROOM TYPE (A -> Z)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.roomType, b.roomType));
    } else if ("ROOM TYPE (Z -> A)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.roomType, a.roomType));
    } else if ("STATUS (A -> Z)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.status, b.status));
    } else if ("STATUS (Z -> A)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.status, a.status));
    } else if ("PRICE (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.pricePerNight, b.pricePerNight));
    } else if ("PRICE (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.pricePerNight, a.pricePerNight));
    } else {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.roomNumber, b.roomNumber));
    }

    // Transfer to ArrayList for index-based paging
    ArrayList<ManageRoomStatusView.RoomRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= filteredBuffer.getNumberOfEntries(); i++) {
      result.add(filteredBuffer.getEntry(i));
    }
    return result;
  }

  // =========================================================================
  // LOOKUP HELPERS
  // =========================================================================

  private Reservation findActiveReservationByRoomNumber(String roomNumber) {
    if (roomNumber == null) return null;
    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || !roomNumber.equalsIgnoreCase(r.getRoomNumber())) continue;
      if (r.getStatus() == Reservation.Status.CHECKED_IN
          || r.getStatus() == Reservation.Status.ALLOCATED) return r;
    }
    return null;
  }

  // =========================================================================
  // UTILITY
  // =========================================================================

  private boolean containsIgnoreCase(String field, String query) {
    return field != null && field.toLowerCase().contains(query.toLowerCase());
  }

  private int nullSafeCompare(String a, String b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareToIgnoreCase(b);
  }
}
