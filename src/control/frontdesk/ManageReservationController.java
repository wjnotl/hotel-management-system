package control.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import adt.LinkedList;
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
    String searchQuery      = null;
    String roomTypeFilter   = null;
    String paymentStatusFilter = null;
    String stayStatusFilter = null;
    String sortCriteria     = "GUEST NAME (A -> Z)";

    while (true) {
      try {
        ArrayList<ManageReservationView.ReservationRowDTO> allDtos = buildAllReservationDTOs();
        ArrayList<ManageReservationView.ReservationRowDTO> filtered =
            filterAndSortDTOs(allDtos, searchQuery, roomTypeFilter,
                paymentStatusFilter, stayStatusFilter, sortCriteria);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ConsoleUtil.GetMenuInputResult result =
            view.renderReservationScreen(
                filtered, searchQuery, roomTypeFilter, paymentStatusFilter,
                stayStatusFilter, sortCriteria, currentPage, PAGE_SIZE);

        String raw = result.input.trim();

        if ("E".equalsIgnoreCase(raw)) {
          return;
        } else if ("R".equalsIgnoreCase(raw)) {
        } else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) currentPage++;
          else ConsoleUtil.printError("Already on the last page!");
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) currentPage--;
          else ConsoleUtil.printError("Already on the first page!");
        } else if ("S".equalsIgnoreCase(raw)) {
          String[] filters = handleFilterMenu(searchQuery, roomTypeFilter,
              paymentStatusFilter, stayStatusFilter);
          searchQuery         = filters[0];
          roomTypeFilter      = filters[1];
          paymentStatusFilter = filters[2];
          stayStatusFilter    = filters[3];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) { sortCriteria = newSort; currentPage = 1; }
        } else if (result.isNumber) {
          int actualIndex = (currentPage - 1) * PAGE_SIZE + result.getAsInt();
          if (actualIndex < 1 || actualIndex > total) {
            ConsoleUtil.printError("Invalid selection.");
            continue;
          }
          ManageReservationView.ReservationRowDTO dto = filtered.getEntry(actualIndex);
          if (dto != null) handleGuestActions(dto);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // =========================================================================
  // GUEST ACTIONS SUBMENU
  // =========================================================================

  private void handleGuestActions(ManageReservationView.ReservationRowDTO dto) {
    while (true) {
      try {
        // Always find the billing fresh — after creation dto.billingId may be stale
        Billing billing = findBillingById(dto.billingId);

        // If not found by ID, try finding by room number (covers post-creation case)
        if (billing == null) {
          billing = findActiveBillingByRoom(dto.roomNumber);
        }

        Guest guest = guestRepo.findById(dto.guestId);

        // No billing record — offer to create one
        if (billing == null) {
          int choice = view.displayNoBillingNotice(dto);
          if (choice == 1) {
            Billing created = handleCreateBilling(dto);
            if (created != null) {
              // Update billingId in dto so next loop finds it directly
              dto = new ManageReservationView.ReservationRowDTO(
                  created.getBillingId(), dto.guestId, dto.guestName,
                  dto.roomNumber, dto.roomType, dto.reservationId,
                  dto.confirmationNumber, dto.checkInDate, dto.checkOutDate,
                  created.getStatus().name(), dto.stayStatus);
              continue;
            }
          }
          return;
        }

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
    if (billing.getStatus() == Billing.Status.PAID) {
      boolean confirm = ConsoleUtil.showConfirmMessage(
          "This stay has already been marked as PAID. Extending will reset payment to UNPAID"
              + " as the total amount has changed. Continue?");
      if (!confirm) return;
    }

    Integer extraDays = view.promptStayExtension(guest, billing);
    if (extraDays == null) return;

    billing.setCheckOutDate(billing.getCheckOutDate().plusDays(extraDays));

    // Reset to UNPAID — the new total is higher and must be re-settled
    billing.setStatus(Billing.Status.UNPAID);

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

    boolean confirmed = ConsoleUtil.showConfirmMessage(
        "Confirm check-out for "
            + (guest != null ? guest.getName() : "guest")
            + " in room " + billing.getRoomNumber() + "?");
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
            HousekeepingTask.TaskType.DEEP_CLEAN,
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
  // CREATE BILLING — for guests with no billing record
  // =========================================================================

  private Billing handleCreateBilling(ManageReservationView.ReservationRowDTO dto) {
    Room room = roomRepo.findByRoomNumber(dto.roomNumber);
    double rate = (room != null) ? room.getPrice() : 0.0;

    ListInterface<Reservation> allRes = reservationRepo.getAllReservations();
    Reservation res = findReservationById(allRes, dto.reservationId);

    LocalDate defaultCheckIn  = LocalDate.now();
    LocalDate defaultCheckOut = LocalDate.now().plusDays(1);
    if (res != null && res.getAllocatedTime() != null) {
      defaultCheckIn = res.getAllocatedTime().toLocalDate();
      if (res.getStayDays() != null) {
        defaultCheckOut = defaultCheckIn.plusDays(res.getStayDays());
      }
    }

    ManageReservationView.CreateBillingInputDTO input =
        view.promptCreateBilling(dto, defaultCheckIn, defaultCheckOut, rate);
    if (input == null) return null;

    String billingId = generateBillingId();
    Billing billing = new Billing(
        billingId,
        dto.guestId,
        "N/A".equals(dto.reservationId) ? null : dto.reservationId,
        dto.roomNumber,
        room != null ? room.getRoomType() : null,
        input.checkInDate,
        input.checkOutDate,
        rate,
        Billing.Status.UNPAID,
        LocalDateTime.now());

    billingRepo.addBilling(billing);
    view.displayBillingCreated(billing);
    return billing;
  }

  /** Generates the next BILL-XXXX id by scanning existing billings. */
  private String generateBillingId() {
    ListInterface<Billing> all = billingRepo.getBillingList();
    int max = 1000;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b == null || b.getBillingId() == null) continue;
      String id = b.getBillingId().toUpperCase();
      if (id.startsWith("BILL-")) {
        try {
          int num = Integer.parseInt(id.substring(5));
          if (num >= max) max = num + 1;
        } catch (NumberFormatException ignored) {}
      }
    }
    return "BILL-" + max;
  }

  // =========================================================================
  // FILTER MENU
  // =========================================================================

  private String[] handleFilterMenu(String search, String roomType,
      String paymentStatus, String stayStatus) {
    String s  = search;
    String rt = roomType;
    String ps = paymentStatus;
    String ss = stayStatus;

    while (true) {
      try {
        int choice = view.displayFilterMenu(s, rt, ps, ss);
        if (choice == 1) {
          String input = view.promptSearchInput(s);
          s = (input == null || input.trim().isEmpty()) ? null : input.trim();
        } else if (choice == 2) {
          rt = handleRoomTypeSubmenu(rt);
        } else if (choice == 3) {
          ps = handlePaymentStatusSubmenu(ps);
        } else if (choice == 4) {
          ss = handleStayStatusSubmenu(ss);
        } else if (choice == 5) {
          return new String[]{null, null, null, null};
        } else if (choice == 6) {
          return new String[]{s, rt, ps, ss};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleStayStatusSubmenu(String current) {
    while (true) {
      try {
        int choice = view.displayStayStatusSubmenu(current);
        if (choice == 1) return "CHECKED_IN";
        if (choice == 2) return "CHECKED_OUT";
        if (choice == 3) return "PENDING";
        if (choice == 4) return null;
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
  // DATA PROCESSING — builds ALL reservation/billing records (active + history)
  // =========================================================================

  private ArrayList<ManageReservationView.ReservationRowDTO> buildAllReservationDTOs() {

    ListInterface<Room>        allRooms        = roomRepo.getRoomList();
    ListInterface<Billing>     allBillings     = billingRepo.getBillingList();
    ListInterface<Reservation> allReservations = reservationRepo.getAllReservations();
    java.time.LocalDate today = java.time.LocalDate.now();

    LinkedList<ManageReservationView.ReservationRowDTO> dtoBuffer = new LinkedList<>();

    // Track billing IDs already added to avoid duplicates
    LinkedList<String> addedBillingIds = new LinkedList<>();

    // PRIMARY: OCCUPIED rooms (active stays)
    for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
      Room room = allRooms.getEntry(i);
      if (room == null || !room.getIsOccupied()) continue;

      Billing best = null;
      for (int j = 1; j <= allBillings.getNumberOfEntries(); j++) {
        Billing b = allBillings.getEntry(j);
        if (b == null || !room.getRoomNumber().equalsIgnoreCase(b.getRoomNumber())) continue;
        if (best == null) { best = b; continue; }
        boolean bActive    = b.getCheckOutDate()    != null && !b.getCheckOutDate().isBefore(today);
        boolean bestActive = best.getCheckOutDate() != null && !best.getCheckOutDate().isBefore(today);
        if (bActive && !bestActive) { best = b; }
        else if (bActive == bestActive && b.getCreatedAt() != null
            && best.getCreatedAt() != null && b.getCreatedAt().isAfter(best.getCreatedAt())) {
          best = b;
        }
      }

      Reservation res = (best != null)
          ? findReservationById(allReservations, best.getReservationId())
          : findActiveReservationForRoom(allReservations, room.getRoomNumber());

      String guestId = (best != null && best.getGuestId() != null)
          ? best.getGuestId() : (res != null ? res.getGuestId() : null);
      Guest guest = guestRepo.findById(guestId);

      String stayStatus = deriveStayStatus(res, room);
      String billingId  = (best != null) ? best.getBillingId() : room.getRoomNumber();

      dtoBuffer.add(buildDTO(billingId, guestId, guest, room.getRoomNumber(),
          best != null ? best.getRoomType() : room.getRoomType(),
          res, best, stayStatus));

      if (best != null) addedBillingIds.add(best.getBillingId());
    }

    // SECONDARY: all billings not yet added — includes history (CHECKED_OUT) and
    // active billings whose room is not flagged OCCUPIED
    for (int i = 1; i <= allBillings.getNumberOfEntries(); i++) {
      Billing b = allBillings.getEntry(i);
      if (b == null || b.getRoomNumber() == null || b.getBillingId() == null) continue;

      // Skip if already added from PRIMARY pass
      boolean alreadyAdded = false;
      for (int k = 1; k <= addedBillingIds.getNumberOfEntries(); k++) {
        if (b.getBillingId().equalsIgnoreCase(addedBillingIds.getEntry(k))) {
          alreadyAdded = true;
          break;
        }
      }
      if (alreadyAdded) continue;

      Reservation res = findReservationById(allReservations, b.getReservationId());
      Guest guest     = guestRepo.findById(b.getGuestId());
      String stayStatus = deriveStayStatusFromBillingRes(b, res, today);

      dtoBuffer.add(buildDTO(b.getBillingId(), b.getGuestId(), guest,
          b.getRoomNumber(), b.getRoomType(), res, b, stayStatus));
    }

    ArrayList<ManageReservationView.ReservationRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= dtoBuffer.getNumberOfEntries(); i++) result.add(dtoBuffer.getEntry(i));
    return result;
  }

  /** Derive stay status from room + reservation for OCCUPIED room entries. */
  private String deriveStayStatus(Reservation res, Room room) {
    if (res != null) {
      if (res.getStatus() == Reservation.Status.CHECKED_OUT) return "CHECKED_OUT";
      if (res.getStatus() == Reservation.Status.CHECKED_IN)  return "CHECKED_IN";
      if (res.getStatus() == Reservation.Status.ALLOCATED)   return "CHECKED_IN";
    }
    return room != null && room.getIsOccupied() ? "CHECKED_IN" : "PENDING";
  }

  /** Derive stay status from billing + reservation for historical entries. */
  private String deriveStayStatusFromBillingRes(
      Billing b, Reservation res, java.time.LocalDate today) {
    if (res != null && res.getStatus() == Reservation.Status.CHECKED_OUT) return "CHECKED_OUT";
    if (b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today)) return "CHECKED_OUT";
    if (res != null && (res.getStatus() == Reservation.Status.CHECKED_IN
        || res.getStatus() == Reservation.Status.ALLOCATED)) return "CHECKED_IN";
    return "PENDING";
  }

  /** Build a DTO row from raw data. */
  private ManageReservationView.ReservationRowDTO buildDTO(
      String billingId, String guestId, Guest guest, String roomNumber,
      Room.RoomType roomTypeEnum, Reservation res, Billing billing, String stayStatus) {

    String guestName = (guest != null) ? guest.getName() : "N/A";
    String roomType  = (roomTypeEnum != null) ? roomTypeEnum.name() : "N/A";
    String resId     = (res != null) ? res.getReservationId()
                       : (billing != null && billing.getReservationId() != null
                           ? billing.getReservationId() : "N/A");
    String confNum   = (res != null) ? res.getConfirmationNumber() : "N/A";
    String checkIn   = "N/A", checkOut = "N/A";
    if (billing != null && billing.getCheckInDate() != null) {
      checkIn = billing.getCheckInDate().toString();
    } else if (res != null && res.getAllocatedTime() != null) {
      checkIn = res.getAllocatedTime().toLocalDate().toString();
    }
    if (billing != null && billing.getCheckOutDate() != null) {
      checkOut = billing.getCheckOutDate().toString();
    } else if (res != null && res.getAllocatedTime() != null && res.getStayDays() != null) {
      checkOut = res.getAllocatedTime().toLocalDate().plusDays(res.getStayDays()).toString();
    }

    String payment;
    if (billing != null) {
      payment = billing.getStatus().name();
    } else if (res != null) {
      payment = "PENDING";
    } else {
      payment = "PENDING";
    }

    return new ManageReservationView.ReservationRowDTO(
        billingId,
        guestId != null ? guestId : "N/A",
        guestName, roomNumber, roomType, resId, confNum,
        checkIn, checkOut, payment, stayStatus);
  }

  /** Finds a CHECKED_IN or ALLOCATED reservation for a given room number. */
  private Reservation findActiveReservationForRoom(
      ListInterface<Reservation> all, String roomNumber) {
    if (roomNumber == null || all == null) return null;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null || !roomNumber.equalsIgnoreCase(r.getRoomNumber())) continue;
      if (r.getStatus() == Reservation.Status.CHECKED_IN
          || r.getStatus() == Reservation.Status.ALLOCATED) return r;
    }
    return null;
  }

  private Reservation findReservationById(
      ListInterface<Reservation> all, String reservationId) {
    if (reservationId == null || all == null) return null;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && reservationId.equalsIgnoreCase(r.getReservationId())) return r;
    }
    return null;
  }

  private ArrayList<ManageReservationView.ReservationRowDTO> filterAndSortDTOs(
      ArrayList<ManageReservationView.ReservationRowDTO> source,
      String search, String roomType, String paymentStatus,
      String stayStatus, String sort) {

    LinkedList<ManageReservationView.ReservationRowDTO> filteredBuffer = new LinkedList<>();

    if (source != null) {
      for (int i = 1; i <= source.getNumberOfEntries(); i++) {
        ManageReservationView.ReservationRowDTO dto = source.getEntry(i);
        if (dto == null) continue;

        boolean matchesSearch = search == null || search.trim().isEmpty()
            || containsIgnoreCase(dto.guestId,            search)
            || containsIgnoreCase(dto.guestName,          search)
            || containsIgnoreCase(dto.roomNumber,         search)
            || containsIgnoreCase(dto.reservationId,      search)
            || containsIgnoreCase(dto.confirmationNumber, search);

        boolean matchesRoomType   = roomType == null || roomType.equalsIgnoreCase(dto.roomType);
        boolean matchesPayment    = paymentStatus == null || paymentStatus.equalsIgnoreCase(dto.paymentStatus);
        boolean matchesStayStatus = stayStatus == null || stayStatus.equalsIgnoreCase(dto.stayStatus);

        if (matchesSearch && matchesRoomType && matchesPayment && matchesStayStatus) {
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

  private Billing findActiveBillingByRoom(String roomNumber) {
    if (roomNumber == null) return null;
    LocalDate today = LocalDate.now();
    ListInterface<Billing> all = billingRepo.getBillingList();
    Billing best = null;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b == null || !roomNumber.equalsIgnoreCase(b.getRoomNumber())) continue;
      if (b.getCheckOutDate() == null || b.getCheckOutDate().isBefore(today)) continue;
      if (best == null || b.getCreatedAt().isAfter(best.getCreatedAt())) best = b;
    }
    return best;
  }

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
