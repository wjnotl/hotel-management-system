package control.frontdesk;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.LinkedList;
import adt.LinkedStack;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.HousekeepingTaskRepo;
import repo.ReservationRepo;
import repo.RoomRepo;
import repo.RoomStatusHistoryRepo;
import util.ConsoleUtil;
import view.frontdesk.ManageReservationView;

public class ManageReservationController {

  private static final int PAGE_SIZE = 10;

  private final ManageReservationView view = new ManageReservationView();
  private final GuestRepo guestRepo;
  private final BillingRepo billingRepo;
  private final ReservationRepo reservationRepo;
  private final RoomRepo roomRepo;
  private final RoomStatusHistoryRepo roomStatusHistoryRepo;
  private final HousekeepingTaskRepo housekeepingTaskRepo;

  public ManageReservationController(
      GuestRepo guestRepo,
      BillingRepo billingRepo,
      ReservationRepo reservationRepo,
      RoomRepo roomRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo,
      HousekeepingTaskRepo housekeepingTaskRepo) {
    this.guestRepo = guestRepo;
    this.billingRepo = billingRepo;
    this.reservationRepo = reservationRepo;
    this.roomRepo = roomRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
    this.housekeepingTaskRepo = housekeepingTaskRepo;
  }

  // =========================================================================
  // ENTRY POINT
  // =========================================================================

  public void start() {
    int currentPage = 1;
    String searchQuery = null;
    String roomTypeFilter = null;
    String paymentStatusFilter = null;
    String sortCriteria = "GUEST NAME (A -> Z)";

    while (true) {
      try {
        ArrayList<ManageReservationView.ReservationRowDTO> allDtos = buildActiveStayDTOs();
        ArrayList<ManageReservationView.ReservationRowDTO> filtered =
            filterAndSortDTOs(
                allDtos, searchQuery, roomTypeFilter, paymentStatusFilter, sortCriteria);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ConsoleUtil.GetMenuInputResult result =
            view.renderReservationScreen(
                filtered,
                searchQuery,
                roomTypeFilter,
                paymentStatusFilter,
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
          String[] filters = handleFilterMenu(searchQuery, roomTypeFilter, paymentStatusFilter);
          searchQuery = filters[0];
          roomTypeFilter = filters[1];
          paymentStatusFilter = filters[2];
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
            ConsoleUtil.printError("Invalid selection.");
            continue;
          }
          ManageReservationView.ReservationRowDTO dto = filtered.getEntry(actualIndex);
          if (dto != null) handleGuestActions(dto.billingId);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // GUEST ACTIONS SUBMENU
  // =========================================================================

  private void handleGuestActions(String billingId) {
    while (true) {
      try {
        // Re-fetch fresh each loop iteration
        Billing billing = findBillingById(billingId);
        if (billing == null) {
          ConsoleUtil.printError("Billing record not found.");
          return;
        }

        Guest guest = guestRepo.findById(billing.getGuestId());
        int action = view.displayGuestActionsSubmenu(guest, billing);

        if (action == 1) {
          handleCalculateRoomCharges(billing);
        } else if (action == 2) {
          handleRecordPayment(billing);
        } else if (action == 3) {
          if (guest == null) ConsoleUtil.printError("Guest record not found.");
          else view.displayReceipt(guest, billing);
        } else if (action == 4) {
          handleStayExtension(billing, guest);
        } else if (action == 5) {
          boolean done = handleCompleteCheckOut(billing, guest);
          if (done) return;
        } else if (action == 6) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // ACTION 1 — CALCULATE ROOM CHARGES
  // =========================================================================

  private void handleCalculateRoomCharges(Billing billing) {
    LocalDate today = LocalDate.now();
    if (!today.equals(billing.getCheckOutDate())) {
      billing.setCheckOutDate(today);
      billingRepo.updateBilling(billing);
    }
    Guest guest = guestRepo.findById(billing.getGuestId());
    view.displayRoomCharges(guest, billing);
  }

  // =========================================================================
  // ACTION 2 — RECORD PAYMENT
  // =========================================================================

  private void handleRecordPayment(Billing billing) {
    if (billing.getStatus() == Billing.Status.PAID) {
      ConsoleUtil.printError("Payment has already been recorded for this stay!");
      return;
    }
    billing.setStatus(Billing.Status.PAID);
    billingRepo.updateBilling(billing);
    view.displayPaymentRecorded(billing);
  }

  // =========================================================================
  // ACTION 4 — STAY EXTENSION
  // =========================================================================

  private void handleStayExtension(Billing billing, Guest guest) {
    Integer extraDays = view.promptStayExtension(guest, billing);
    if (extraDays == null) return;

    billing.setCheckOutDate(billing.getCheckOutDate().plusDays(extraDays));
    billingRepo.updateBilling(billing);
    view.displayStayExtensionSuccess(guest, billing, extraDays);
  }

  // =========================================================================
  // ACTION 5 — COMPLETE CHECK-OUT
  // =========================================================================

  private boolean handleCompleteCheckOut(Billing billing, Guest guest) {
    if (billing.getStatus() != Billing.Status.PAID) {
      ConsoleUtil.printError("Guest must settle payment before check-out can be completed!");
      return false;
    }

    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            "Confirm check-out for "
                + (guest != null ? guest.getName() : "guest")
                + " in room "
                + billing.getRoomNumber()
                + "?");
    if (!confirmed) return false;

    // 1. Free the room → DIRTY
    Room room = roomRepo.findByRoomNumber(billing.getRoomNumber());
    if (room != null) {
      Room.Status previous = room.getStatus();
      room.setStatus(Room.Status.DIRTY);
      roomRepo.updateRoom(room);
      roomStatusHistoryRepo.recordStatusChange(room.getRoomNumber(), previous, Room.Status.DIRTY);
    }

    // 2. Queue housekeeping turnover task
    housekeepingTaskRepo.enqueueTask(
        new HousekeepingTask(
            housekeepingTaskRepo.generateTaskId(),
            billing.getRoomNumber(),
            HousekeepingTask.TaskType.TURNOVER,
            HousekeepingTask.Status.PENDING,
            null,
            false,
            LocalDateTime.now()));

    // 3. Update reservation — CHECKED_OUT, clear confirmation number
    Reservation reservation = findReservationById(billing.getReservationId());
    if (reservation != null) {
      reservation.setStatus(Reservation.Status.CHECKED_OUT);
      reservation.setConfirmationNumber(null);
      reservation.setCheckOutTime(LocalDateTime.now());
      reservationRepo.updateReservation(reservation);
    }

    view.displayCheckOutSuccess(guest, billing, room);
    return true;
  }

  // =========================================================================
  // FILTER MENU
  // =========================================================================

  private String[] handleFilterMenu(String search, String roomType, String paymentStatus) {
    String s = search;
    String rt = roomType;
    String ps = paymentStatus;

    while (true) {
      try {
        int choice = view.displayFilterMenu(s, rt, ps);
        if (choice == 1) {
          String input = view.promptSearchInput(s);
          s = (input == null || input.trim().isEmpty()) ? null : input.trim();
        } else if (choice == 2) {
          rt = handleRoomTypeSubmenu(rt);
        } else if (choice == 3) {
          ps = handlePaymentStatusSubmenu(ps);
        } else if (choice == 4) {
          return new String[] {null, null, null};
        } else if (choice == 5) {
          return new String[] {s, rt, ps};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleRoomTypeSubmenu(String current) {
    while (true) {
      try {
        int choice = view.displayRoomTypeSubmenu(current);
        if (choice == 1) return "LUXURY";
        if (choice == 2) return "SUITE";
        if (choice == 3) return "STANDARD";
        if (choice == 4) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handlePaymentStatusSubmenu(String current) {
    while (true) {
      try {
        int choice = view.displayPaymentStatusSubmenu(current);
        if (choice == 1) return "PAID";
        if (choice == 2) return "UNPAID";
        if (choice == 3) return null;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        String selected = view.displaySortMenu(currentSort);
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
  //   DoublyLinkedHashMap  — O(1) billing/reservation lookup by ID
  //   LinkedStack          — collect billings oldest→push, pop = newest-first order
  //   LinkedList           — cheap sequential build of DTO list before final sort
  //   ArrayList            — final paged DTO list passed to the view (index access)
  // =========================================================================

  private ArrayList<ManageReservationView.ReservationRowDTO> buildActiveStayDTOs() {

    // --- Build O(1) lookup maps from repos ---
    // billingId  → Billing
    DoublyLinkedHashMap<String, Billing> billingMap = new DoublyLinkedHashMap<>();
    ListInterface<Billing> allBillings = billingRepo.getBillingList();
    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b != null && b.getBillingId() != null) {
        billingMap.put(b.getBillingId().toLowerCase(), b);
      }
    }

    // reservationId → Reservation
    DoublyLinkedHashMap<String, Reservation> reservationMap = new DoublyLinkedHashMap<>();
    ListInterface<Reservation> allReservations = reservationRepo.getAllReservations();
    for (int i = 1; i <= allReservations.getNumberOfEntries(); i++) {
      Reservation r = allReservations.getEntry(i);
      if (r != null && r.getReservationId() != null) {
        reservationMap.put(r.getReservationId().toLowerCase(), r);
      }
    }

    // LinkedList to accumulate DTOs during iteration (no index access needed yet)
    LinkedList<ManageReservationView.ReservationRowDTO> dtoBuffer = new LinkedList<>();

    ListInterface<Room> allRooms = roomRepo.getRoomList();
    for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
      Room room = allRooms.getEntry(i);
      if (room == null || room.getStatus() != Room.Status.OCCUPIED) continue;

      // Collect only active (not-yet-checked-out) billings for this room,
      // then use a LinkedStack to surface the latest one.
      java.time.LocalDate today = java.time.LocalDate.now();
      LinkedList<Billing> candidates = new LinkedList<>();
      for (int j = 1; j <= allBillings.getNumberOfEntries(); j++) {
        Billing b = allBillings.getEntry(j);
        if (b == null || !room.getRoomNumber().equalsIgnoreCase(b.getRoomNumber())) continue;
        // Only consider billings whose stay is current (checkOut >= today)
        if (b.getCheckOutDate() == null || b.getCheckOutDate().isBefore(today)) continue;
        candidates.add(b);
      }
      // Sort candidates oldest → newest so the latest ends up on top of the stack
      candidates.sort(
          (a, b) -> {
            if (a.getCreatedAt() == null) return -1;
            if (b.getCreatedAt() == null) return 1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
          });

      LinkedStack<Billing> billingStack = new LinkedStack<>();
      for (int j = 1; j <= candidates.getNumberOfEntries(); j++) {
        billingStack.push(candidates.getEntry(j));
      }

      if (billingStack.isEmpty()) continue;
      Billing latestBilling = billingStack.peek(); // top = newest

      Guest guest = guestRepo.findById(latestBilling.getGuestId());

      // O(1) reservation lookup via map
      Reservation res =
          (latestBilling.getReservationId() != null)
              ? reservationMap.get(latestBilling.getReservationId().toLowerCase())
              : null;

      String guestName = (guest != null) ? guest.getName() : "N/A";
      String roomType =
          (latestBilling.getRoomType() != null) ? latestBilling.getRoomType().name() : "N/A";
      // resId: use reservation ID if found, else fall back to billing ID so the row
      // is still identifiable (happens when .dat files pre-date the reservation link)
      String resId =
          (res != null)
              ? res.getReservationId()
              : (latestBilling.getReservationId() != null
                  ? latestBilling.getReservationId()
                  : latestBilling.getBillingId());
      String checkIn =
          (latestBilling.getCheckInDate() != null)
              ? latestBilling.getCheckInDate().toString()
              : "N/A";
      String checkOut =
          (latestBilling.getCheckOutDate() != null)
              ? latestBilling.getCheckOutDate().toString()
              : "N/A";
      String payment = latestBilling.getStatus().name();

      dtoBuffer.add(
          new ManageReservationView.ReservationRowDTO(
              latestBilling.getBillingId(),
              latestBilling.getGuestId(),
              guestName,
              latestBilling.getRoomNumber(),
              roomType,
              resId,
              checkIn,
              checkOut,
              payment));
    }

    // Transfer LinkedList → ArrayList for index-based paging in view
    ArrayList<ManageReservationView.ReservationRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= dtoBuffer.getNumberOfEntries(); i++) {
      result.add(dtoBuffer.getEntry(i));
    }
    return result;
  }

  private ArrayList<ManageReservationView.ReservationRowDTO> filterAndSortDTOs(
      ArrayList<ManageReservationView.ReservationRowDTO> source,
      String search,
      String roomType,
      String paymentStatus,
      String sort) {

    // LinkedList for filter build (sequential add, no random access needed)
    LinkedList<ManageReservationView.ReservationRowDTO> filteredBuffer = new LinkedList<>();

    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ManageReservationView.ReservationRowDTO dto = source.getEntry(i);
        if (dto == null) continue;

        boolean matchesSearch =
            search == null
                || search.trim().isEmpty()
                || containsIgnoreCase(dto.guestId, search)
                || containsIgnoreCase(dto.guestName, search)
                || containsIgnoreCase(dto.roomNumber, search)
                || containsIgnoreCase(dto.reservationId, search);

        boolean matchesRoomType = roomType == null || roomType.equalsIgnoreCase(dto.roomType);
        boolean matchesPayment =
            paymentStatus == null || paymentStatus.equalsIgnoreCase(dto.paymentStatus);

        if (matchesSearch && matchesRoomType && matchesPayment) {
          filteredBuffer.add(dto);
        }
      }
    }

    // Sort the LinkedList in-place (it implements ListInterface which has sort)
    if ("GUEST NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.guestName, a.guestName));
    } else if ("ROOM NUMBER (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.roomNumber, b.roomNumber));
    } else if ("ROOM NUMBER (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.roomNumber, a.roomNumber));
    } else if ("CHECK-IN (EARLIEST FIRST)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.checkInDate, b.checkInDate));
    } else if ("CHECK-IN (LATEST FIRST)".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(b.checkInDate, a.checkInDate));
    } else if ("PAYMENT STATUS".equalsIgnoreCase(sort)) {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.paymentStatus, b.paymentStatus));
    } else {
      filteredBuffer.sort((a, b) -> nullSafeCompare(a.guestName, b.guestName));
    }

    // Transfer to ArrayList for index-based paging
    ArrayList<ManageReservationView.ReservationRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= filteredBuffer.getNumberOfEntries(); i++) {
      result.add(filteredBuffer.getEntry(i));
    }
    return result;
  }

  // =========================================================================
  // LOOKUP HELPERS
  // =========================================================================

  private Billing findBillingById(String billingId) {
    if (billingId == null) return null;
    ListInterface<Billing> all = billingRepo.getBillingList();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b != null && billingId.equalsIgnoreCase(b.getBillingId())) return b;
    }
    return null;
  }

  private Reservation findReservationById(String reservationId) {
    if (reservationId == null) return null;
    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && reservationId.equalsIgnoreCase(r.getReservationId())) return r;
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
