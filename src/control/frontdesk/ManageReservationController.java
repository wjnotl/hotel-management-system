package control.frontdesk;

import adt.ArrayList;
import adt.LinkedList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Reservation;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
  private static final int MIN_EXTENSION_DAYS = 1;
  private static final int MAX_EXTENSION_DAYS = 30;
  private static final DateTimeFormatter DISPLAY_DATE_FMT =
      DateTimeFormatter.ofPattern("dd MMM yyyy");

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

  // entry
  public void start() {
    int currentPage = 1;
    String searchQuery = null;
    String roomTypeFilter = null;
    String paymentStatusFilter = null;
    String stayStatusFilter = null;
    String sortCriteria = "GUEST NAME (A -> Z)";

    while (true) {
      try {
        ArrayList<ManageReservationView.ReservationRowDTO> allDtos = buildAllReservationDTOs();
        ArrayList<ManageReservationView.ReservationRowDTO> filtered =
            filterAndSortDTOs(
                allDtos,
                searchQuery,
                roomTypeFilter,
                paymentStatusFilter,
                stayStatusFilter,
                sortCriteria);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ManageReservationView.ReservationRowDTO[] pageRows =
            toPageArray(filtered, currentPage, PAGE_SIZE);

        ConsoleUtil.GetMenuInputResult result =
            view.renderReservationScreen(
                pageRows,
                total,
                searchQuery,
                roomTypeFilter,
                paymentStatusFilter,
                stayStatusFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

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
          String[] filters =
              handleFilterMenu(searchQuery, roomTypeFilter, paymentStatusFilter, stayStatusFilter);
          searchQuery = filters[0];
          roomTypeFilter = filters[1];
          paymentStatusFilter = filters[2];
          stayStatusFilter = filters[3];
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
          if (dto != null) handleGuestActions(dto);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // guest action
  private void handleGuestActions(ManageReservationView.ReservationRowDTO dto) {
    while (true) {
      try {
        // find billing by id
        Billing billing = findBillingById(dto.billingId);

        // If not found by ID, try finding by room number
        if (billing == null) {
          billing = findActiveBillingByRoom(dto.roomNumber);
        }

        Guest guest = guestRepo.findById(dto.guestId);

        // No billing record, offer to create one
        if (billing == null) {
          int choice = view.displayNoBillingNotice(dto);
          if (choice == 1) {
            Billing created = handleCreateBilling(dto);
            if (created != null) {
              // update billing id in dto
              dto =
                  new ManageReservationView.ReservationRowDTO(
                      created.getBillingId(),
                      dto.guestId,
                      dto.guestName,
                      dto.roomNumber,
                      dto.roomType,
                      dto.reservationId,
                      dto.confirmationNumber,
                      dto.checkInDate,
                      dto.checkOutDate,
                      created.getStatus().name(),
                      dto.stayStatus);
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

  // action 1 - calculate room charges
  private void handleCalculateRoomCharges(Billing billing) {
    LocalDate today = LocalDate.now();
    if (billing.getCheckOutDate() == null || billing.getCheckOutDate().isBefore(today)) {
      billing.setCheckOutDate(today);
      billingRepo.updateBilling(billing);
    }
    Guest guest = guestRepo.findById(billing.getGuestId());
    view.displayRoomCharges(guest, billing);
  }

  // action 2 - record payment
  private void handleRecordPayment(Billing billing) {
    if (billing.getStatus() == Billing.Status.PAID) {
      ConsoleUtil.printError("Payment has already been recorded for this stay!");
      return;
    }
    double amountDue = billing.getOutstandingTotal();
    billing.setPaidNights(billing.getPaidNights() + billing.getOutstandingNights());
    billing.setStatus(Billing.Status.PAID);
    billingRepo.updateBilling(billing);
    view.displayPaymentRecorded(billing, amountDue);
  }

  //  action 4 - stay extension
  private void handleStayExtension(Billing billing, Guest guest) {
    Reservation reservation = findReservationById(billing.getReservationId());
    if (reservation != null && reservation.getStatus() == Reservation.Status.CHECKED_OUT) {
      ConsoleUtil.printError("This guest has already checked out. Stay cannot be extended.");
      return;
    }
    if (billing.getCheckOutDate() != null && billing.getCheckOutDate().isBefore(LocalDate.now())) {
      ConsoleUtil.printError("Check-out date has already passed. Stay cannot be extended.");
      return;
    }

    if (billing.getStatus() == Billing.Status.PAID) {
      boolean confirm =
          ConsoleUtil.showConfirmMessage(
              "This stay has already been marked as PAID. Extending will reset"
                  + " payment to UNPAID as the total amount has changed. Continue?");
      if (!confirm) return;
    }

    String input = view.promptStayExtensionInput(guest, billing);
    if (input == null || "C".equalsIgnoreCase(input.trim()) || "0".equals(input.trim())) {
      return;
    }

    int extraDays;
    try {
      extraDays = Integer.parseInt(input.trim());
    } catch (NumberFormatException e) {
      ConsoleUtil.printError("Invalid input. Extension cancelled.");
      return;
    }

    if (extraDays < MIN_EXTENSION_DAYS || extraDays > MAX_EXTENSION_DAYS) {
      ConsoleUtil.printError(
          "Please enter a value between "
              + MIN_EXTENSION_DAYS
              + " and "
              + MAX_EXTENSION_DAYS
              + ".");
      return;
    }

    LocalDate newCheckOut =
        billing.getCheckOutDate() != null ? billing.getCheckOutDate().plusDays(extraDays) : null;
    if (!view.confirmStayExtension(extraDays, newCheckOut)) return;

    billing.setCheckOutDate(newCheckOut);
    billing.setStatus(Billing.Status.UNPAID);

    billingRepo.updateBilling(billing);
    view.displayStayExtensionSuccess(guest, billing, extraDays);
  }

  // action 5 - complete check-out
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
      room.setIsOccupied(false);
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

  // create billing - for guests with no billing record
  private Billing handleCreateBilling(ManageReservationView.ReservationRowDTO dto) {
    Room room = roomRepo.findByRoomNumber(dto.roomNumber);
    double rate = (room != null) ? room.getPrice() : 0.0;

    // A reservation may only ever have one billing record — block creating a second
    Billing existingBilling =
        billingRepo
            .getBillingList()
            .find(
                b ->
                    b != null
                        && dto.reservationId != null
                        && !"N/A".equalsIgnoreCase(dto.reservationId)
                        && dto.reservationId.equalsIgnoreCase(b.getReservationId()));

    if (existingBilling != null) {
      ConsoleUtil.printError(
          "A billing ("
              + existingBilling.getBillingId()
              + ") already exists for this reservation.\n"
              + "Room: "
              + existingBilling.getRoomNumber()
              + " — use Stay Extension to modify the current stay instead.");
      return null;
    }

    ListInterface<Reservation> allRes = reservationRepo.getAllReservations();
    Reservation res = findReservationById(allRes, dto.reservationId);

    LocalDate defaultCheckIn = LocalDate.now();
    LocalDate defaultCheckOut = LocalDate.now().plusDays(1);
    if (res != null && res.getAllocatedTime() != null) {
      defaultCheckIn = res.getAllocatedTime().toLocalDate();
      if (res.getStayDays() != null) {
        defaultCheckOut = defaultCheckIn.plusDays(res.getStayDays());
      } else {
        defaultCheckOut = defaultCheckIn.plusDays(1);
      }
    }

    LocalDate[] dates = promptCreateBillingDates(dto, defaultCheckIn, defaultCheckOut, rate);

    if (dates == null) return null;
    LocalDate checkIn = dates[0];
    LocalDate checkOut = dates[1];

    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    double total = nights * rate * (1 + Billing.SST_RATE);
    if (!view.confirmCreateBilling(nights, rate, total)) return null;

    String billingId = generateBillingId();
    Billing billing =
        new Billing(
            billingId,
            dto.guestId,
            "N/A".equals(dto.reservationId) ? null : dto.reservationId,
            dto.roomNumber,
            room != null ? room.getRoomType() : null,
            checkIn,
            checkOut,
            rate,
            Billing.Status.UNPAID,
            LocalDateTime.now());

    billingRepo.addBilling(billing);
    view.displayBillingCreated(billing);
    return billing;
  }

  // if - cancels, otherwise parses as YYYY-MM-DD
  private LocalDate[] promptCreateBillingDates(
      ManageReservationView.ReservationRowDTO dto,
      LocalDate defaultCheckIn,
      LocalDate defaultCheckOut,
      double rate) {
    while (true) {
      try {
        String[] raw = view.promptCreateBillingDatesRaw(dto, defaultCheckIn, defaultCheckOut, rate);
        if (isCancel(raw[0]) || isCancel(raw[1])) return null;

        LocalDate checkIn = parseDateOrDefault(raw[0], defaultCheckIn);
        LocalDate checkOut = parseDateOrDefault(raw[1], defaultCheckOut);
        if (!checkOut.isAfter(checkIn)) {
          throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        return new LocalDate[] {checkIn, checkOut};
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private boolean isCancel(String raw) {
    return raw != null && "C".equalsIgnoreCase(raw.trim());
  }

  // blank uses the suggested default, otherwise parses as YYYY-MM-DD
  private LocalDate parseDateOrDefault(String raw, LocalDate defaultVal) {
    if (raw == null || raw.trim().isEmpty()) return defaultVal;
    try {
      return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD.");
    }
  }

  // generate next billing id
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
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return "BILL-" + max;
  }

  // filter menu
  private String[] handleFilterMenu(
      String search, String roomType, String paymentStatus, String stayStatus) {
    String s = search;
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
          return new String[] {null, null, null, null};
        } else if (choice == 6) {
          return new String[] {s, rt, ps, ss};
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

  // data processing
  private ArrayList<ManageReservationView.ReservationRowDTO> buildAllReservationDTOs() {

    ListInterface<Room> allRooms = roomRepo.getRoomList();
    ListInterface<Billing> allBillings = billingRepo.getBillingList();
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
        if (best == null) {
          best = b;
          continue;
        }
        boolean bActive = b.getCheckOutDate() != null && !b.getCheckOutDate().isBefore(today);
        boolean bestActive =
            best.getCheckOutDate() != null && !best.getCheckOutDate().isBefore(today);
        if (bActive && !bestActive) {
          best = b;
        } else if (bActive == bestActive
            && b.getCreatedAt() != null
            && best.getCreatedAt() != null
            && b.getCreatedAt().isAfter(best.getCreatedAt())) {
          best = b;
        }
      }

      Reservation res =
          (best != null)
              ? findReservationById(allReservations, best.getReservationId())
              : findActiveReservationForRoom(allReservations, room.getRoomNumber());

      String guestId =
          (best != null && best.getGuestId() != null)
              ? best.getGuestId()
              : (res != null ? res.getGuestId() : null);
      Guest guest = guestRepo.findById(guestId);

      String stayStatus = deriveStayStatus(res, room);
      String billingId = (best != null) ? best.getBillingId() : room.getRoomNumber();

      dtoBuffer.add(
          buildDTO(
              billingId,
              guestId,
              guest,
              room.getRoomNumber(),
              best != null ? best.getRoomType() : room.getRoomType(),
              res,
              best,
              stayStatus));

      if (best != null) addedBillingIds.add(best.getBillingId());
    }

    // secondary: all billings not yet added
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
      Guest guest = guestRepo.findById(b.getGuestId());
      String stayStatus = deriveStayStatusFromBillingRes(b, res, today);

      dtoBuffer.add(
          buildDTO(
              b.getBillingId(),
              b.getGuestId(),
              guest,
              b.getRoomNumber(),
              b.getRoomType(),
              res,
              b,
              stayStatus));
    }

    // tertiary: checked_in / allocated reservations with no billing record yet
    LinkedList<String> addedResIds = new LinkedList<>();
    for (int i = 1; i <= dtoBuffer.getNumberOfEntries(); i++) {
      ManageReservationView.ReservationRowDTO d = dtoBuffer.getEntry(i);
      if (d != null && d.reservationId != null && !"N/A".equalsIgnoreCase(d.reservationId)) {
        addedResIds.add(d.reservationId);
      }
    }

    for (int i = 1; i <= allReservations.getNumberOfEntries(); i++) {
      Reservation res = allReservations.getEntry(i);
      if (res == null || res.getRoomNumber() == null) continue;
      if (res.getStatus() != Reservation.Status.CHECKED_IN
          && res.getStatus() != Reservation.Status.ALLOCATED) continue;

      Room roomCheck = roomRepo.findByRoomNumber(res.getRoomNumber());
      if (res.getStatus() == Reservation.Status.ALLOCATED
          && (roomCheck == null || !roomCheck.getIsOccupied())) continue;

      // Skip if this reservation is already represented
      boolean seen = false;
      for (int k = 1; k <= addedResIds.getNumberOfEntries(); k++) {
        if (res.getReservationId().equalsIgnoreCase(addedResIds.getEntry(k))) {
          seen = true;
          break;
        }
      }
      if (seen) continue;

      Guest guest = guestRepo.findById(res.getGuestId());
      Room room = roomRepo.findByRoomNumber(res.getRoomNumber());
      dtoBuffer.add(
          buildDTO(
              res.getReservationId(),
              res.getGuestId(),
              guest,
              res.getRoomNumber(),
              room != null ? room.getRoomType() : null,
              res,
              null,
              "CHECKED_IN"));
    }

    ArrayList<ManageReservationView.ReservationRowDTO> result = new ArrayList<>();
    for (int i = 1; i <= dtoBuffer.getNumberOfEntries(); i++) result.add(dtoBuffer.getEntry(i));
    return result;
  }

  // derive stay status from room + reservation for occupied room entries
  private String deriveStayStatus(Reservation res, Room room) {
    if (res != null) {
      if (res.getStatus() == Reservation.Status.CHECKED_OUT) return "CHECKED_OUT";
      if (res.getStatus() == Reservation.Status.CHECKED_IN) return "CHECKED_IN";
      if (res.getStatus() == Reservation.Status.ALLOCATED) return "CHECKED_IN";
    }
    return room != null && room.getIsOccupied() ? "CHECKED_IN" : "PENDING";
  }

  // derive stay status from billing + reservation for historical entries
  private String deriveStayStatusFromBillingRes(
      Billing b, Reservation res, java.time.LocalDate today) {
    if (res != null && res.getStatus() == Reservation.Status.CHECKED_OUT) return "CHECKED_OUT";
    if (b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today)) return "CHECKED_OUT";
    if (res != null
        && (res.getStatus() == Reservation.Status.CHECKED_IN
            || res.getStatus() == Reservation.Status.ALLOCATED)) return "CHECKED_IN";
    return "PENDING";
  }

  // build a dto row from raw data
  private ManageReservationView.ReservationRowDTO buildDTO(
      String billingId,
      String guestId,
      Guest guest,
      String roomNumber,
      Room.RoomType roomTypeEnum,
      Reservation res,
      Billing billing,
      String stayStatus) {

    String guestName = (guest != null) ? guest.getName() : "N/A";
    String roomType = (roomTypeEnum != null) ? roomTypeEnum.name() : "N/A";
    String resId =
        (res != null)
            ? res.getReservationId()
            : (billing != null && billing.getReservationId() != null
                ? billing.getReservationId()
                : "N/A");
    String confNum = (res != null) ? res.getConfirmationNumber() : "N/A";
    String checkIn = "N/A", checkOut = "N/A";
    if (billing != null && billing.getCheckInDate() != null) {
      checkIn = billing.getCheckInDate().format(DISPLAY_DATE_FMT);
    } else if (res != null && res.getAllocatedTime() != null) {
      checkIn = res.getAllocatedTime().toLocalDate().format(DISPLAY_DATE_FMT);
    }
    if (billing != null && billing.getCheckOutDate() != null) {
      checkOut = billing.getCheckOutDate().format(DISPLAY_DATE_FMT);
    } else if (res != null && res.getAllocatedTime() != null && res.getStayDays() != null) {
      checkOut =
          res.getAllocatedTime().toLocalDate().plusDays(res.getStayDays()).format(DISPLAY_DATE_FMT);
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
        guestName,
        roomNumber,
        roomType,
        resId,
        confNum,
        checkIn,
        checkOut,
        payment,
        stayStatus);
  }

  // finds a checked_in or allocated reservation for a given room number
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

  private Reservation findReservationById(ListInterface<Reservation> all, String reservationId) {
    if (reservationId == null || all == null) return null;
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && reservationId.equalsIgnoreCase(r.getReservationId())) return r;
    }
    return null;
  }

  private ArrayList<ManageReservationView.ReservationRowDTO> filterAndSortDTOs(
      ArrayList<ManageReservationView.ReservationRowDTO> source,
      String search,
      String roomType,
      String paymentStatus,
      String stayStatus,
      String sort) {

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
                || containsIgnoreCase(dto.reservationId, search)
                || containsIgnoreCase(dto.confirmationNumber, search);

        boolean matchesRoomType = roomType == null || roomType.equalsIgnoreCase(dto.roomType);
        boolean matchesPayment =
            paymentStatus == null || paymentStatus.equalsIgnoreCase(dto.paymentStatus);
        boolean matchesStayStatus =
            stayStatus == null || stayStatus.equalsIgnoreCase(dto.stayStatus);

        boolean hasCheckOut = !"N/A".equalsIgnoreCase(dto.checkOutDate);

        if (matchesSearch
            && matchesRoomType
            && matchesPayment
            && matchesStayStatus
            && hasCheckOut) {
          filteredBuffer.add(dto);
        }
      }
    }

    // sort
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

  // adt list -> plain array
  private ManageReservationView.ReservationRowDTO[] toPageArray(
      ArrayList<ManageReservationView.ReservationRowDTO> source, int currentPage, int pageSize) {
    int total = source.getNumberOfEntries();
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    int count = Math.max(0, endIndex - startIndex + 1);
    ManageReservationView.ReservationRowDTO[] page =
        new ManageReservationView.ReservationRowDTO[count];
    for (int i = 0; i < count; i++) page[i] = source.getEntry(startIndex + i);
    return page;
  }

  // find active billing by room number
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

  // uli
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
