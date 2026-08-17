package control.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.HousekeepingTask;
import entity.Room;
import java.time.LocalDate;
import java.time.LocalDateTime;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.HousekeepingTaskRepo;
import repo.RoomRepo;
import repo.RoomStatusHistoryRepo;
import util.ConsoleUtil;
import util.NumberUtil;
import view.frontdesk.ManageGuestCheckOutView;

public class ManageGuestCheckOutController {
  private static final int PAGE_SIZE = 10;

  private final ManageGuestCheckOutView manageCheckOutView = new ManageGuestCheckOutView();
  private final GuestRepo guestRepo;
  private final BillingRepo billingRepo;
  private final RoomRepo roomRepo;
  private final RoomStatusHistoryRepo roomStatusHistoryRepo;
  private final HousekeepingTaskRepo housekeepingTaskRepo;

  public ManageGuestCheckOutController(
      GuestRepo guestRepo,
      BillingRepo billingRepo,
      RoomRepo roomRepo,
      RoomStatusHistoryRepo roomStatusHistoryRepo,
      HousekeepingTaskRepo housekeepingTaskRepo) {
    this.guestRepo = guestRepo;
    this.billingRepo = billingRepo;
    this.roomRepo = roomRepo;
    this.roomStatusHistoryRepo = roomStatusHistoryRepo;
    this.housekeepingTaskRepo = housekeepingTaskRepo;
  }

  public void start() {
    int currentPage = 1;

    String guestIdFilter = null;
    String guestNameFilter = null;
    String roomNumberFilter = null;
    String paymentStatusFilter = null;
    String sortCriteria = "GUEST NAME (A -> Z)";

    while (true) {
      try {
        ListInterface<Billing> activeStays = getActiveStayList();
        ListInterface<Billing> filteredList =
            filterAndSortActiveStays(
                activeStays,
                guestIdFilter,
                guestNameFilter,
                roomNumberFilter,
                paymentStatusFilter,
                sortCriteria);

        ConsoleUtil.GetMenuInputResult result =
            manageCheckOutView.renderCheckOutScreen(
                filteredList,
                guestRepo.getGuestList(),
                guestIdFilter,
                guestNameFilter,
                roomNumberFilter,
                paymentStatusFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return;
        } else if ("S".equalsIgnoreCase(result.input)) {
          String[] filters =
              handleFilterMenu(
                  guestIdFilter, guestNameFilter, roomNumberFilter, paymentStatusFilter);
          guestIdFilter = filters[0];
          guestNameFilter = filters[1];
          roomNumberFilter = filters[2];
          paymentStatusFilter = filters[3];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(result.input)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if ("R".equalsIgnoreCase(result.input)) {
          // Table refreshes on loop - active stays are re-derived from room status each
          // pass.
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
          handleGuestActions(filteredList, result.getAsInt(), currentPage, PAGE_SIZE);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleGuestActions(
      ListInterface<Billing> list, int indexOnPage, int page, int pageSize) {
    int actualIndex = (page - 1) * pageSize + indexOnPage;
    if (actualIndex < 1 || actualIndex > list.getNumberOfEntries()) {
      ConsoleUtil.printError("Invalid guest row selection!");
      return;
    }

    Billing billing = list.getEntry(actualIndex);
    if (billing == null) return;

    while (true) {
      try {
        Guest guest = guestRepo.findById(billing.getGuestId());
        int action = manageCheckOutView.displayGuestCheckOutSubmenu(guest, billing);

        if (action == 1) {
          handleCalculateRoomCharges(billing, guest);
        } else if (action == 2) {
          handleRecordPayment(billing);
        } else if (action == 3) {
          if (guest == null) {
            ConsoleUtil.printError("Guest record not found for this billing entry!");
          } else {
            manageCheckOutView.displayReceipt(guest, billing);
          }
        } else if (action == 4) {
          boolean completed = handleCompleteCheckOut(billing, guest);
          if (completed) return; // Back to guest list - room is no longer OCCUPIED
        } else if (action == 5) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleCalculateRoomCharges(Billing billing, Guest guest) {
    LocalDate today = LocalDate.now();
    if (!today.equals(billing.getCheckOutDate())) {
      billing.setCheckOutDate(today);
      billingRepo.updateBilling(billing);
    }
    manageCheckOutView.displayRoomCharges(guest, billing);
  }

  private void handleRecordPayment(Billing billing) {
    if (billing.getStatus() == Billing.Status.PAID) {
      ConsoleUtil.printError("Payment has already been recorded for this stay!");
      return;
    }

    billing.setStatus(Billing.Status.PAID);
    billingRepo.updateBilling(billing);
    manageCheckOutView.displayPaymentRecorded(billing);
  }

  private boolean handleCompleteCheckOut(Billing billing, Guest guest) {
    if (billing.getStatus() != Billing.Status.PAID) {
      ConsoleUtil.printError("Guest must settle payment before check-out can be completed!");
      return false;
    }

    boolean confirmed =
        ConsoleUtil.showConfirmMessage(
            "Confirm check-out for room " + billing.getRoomNumber() + "? This will free the room.");
    if (!confirmed) return false;

    Room room = roomRepo.findByRoomNumber(billing.getRoomNumber());
    if (room != null) {
      Room.Status previousStatus = room.getStatus();
      room.setStatus(Room.Status.DIRTY);
      roomRepo.updateRoom(room);
      roomStatusHistoryRepo.recordStatusChange(
          room.getRoomNumber(), previousStatus, Room.Status.DIRTY);

      HousekeepingTask turnoverTask =
          new HousekeepingTask(
              NumberUtil.generateFormattedId("T-", 1000, 9999, 4),
              room.getRoomNumber(),
              HousekeepingTask.TaskType.TURNOVER,
              HousekeepingTask.Status.PENDING,
              null,
              false,
              LocalDateTime.now());
      housekeepingTaskRepo.enqueueTask(turnoverTask);
    }

    manageCheckOutView.displayCheckOutSuccess(guest, billing, room);
    return true;
  }

  // --- ACTIVE STAY COLLECTION ---
  private ListInterface<Billing> getActiveStayList() {
    ListInterface<Billing> activeStays = new ArrayList<>();
    ListInterface<Room> rooms = roomRepo.getRoomList();

    for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
      Room room = rooms.getEntry(i);
      if (room == null || room.getStatus() != Room.Status.OCCUPIED) continue;

      Billing current = findLatestBillingForRoom(room.getRoomNumber());
      if (current != null) {
        activeStays.add(current);
      }
    }
    return activeStays;
  }

  private Billing findLatestBillingForRoom(String roomNumber) {
    ListInterface<Billing> all = billingRepo.getBillingList();
    Billing latest = null;

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Billing b = all.getEntry(i);
      if (b == null || !roomNumber.equalsIgnoreCase(b.getRoomNumber())) continue;
      if (latest == null || b.getCreatedAt().isAfter(latest.getCreatedAt())) {
        latest = b;
      }
    }
    return latest;
  }

  private String[] handleFilterMenu(
      String guestId, String guestName, String roomNumber, String paymentStatus) {
    String gId = guestId;
    String gName = guestName;
    String rNo = roomNumber;
    String pStatus = paymentStatus;

    while (true) {
      try {
        int choice = manageCheckOutView.displayFilterMainMenu(gId, gName, rNo, pStatus);

        if (choice == 1) {
          gId = normalizeFilter(manageCheckOutView.promptGuestIdFilterInput());
        } else if (choice == 2) {
          gName = normalizeFilter(manageCheckOutView.promptGuestNameFilterInput());
        } else if (choice == 3) {
          rNo = normalizeFilter(manageCheckOutView.promptRoomNumberFilterInput());
        } else if (choice == 4) {
          pStatus = handlePaymentStatusSubmenu(pStatus);
        } else if (choice == 5) {
          return new String[] {null, null, null, null};
        } else if (choice == 6) {
          return new String[] {gId, gName, rNo, pStatus};
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

  private String handlePaymentStatusSubmenu(String current) {
    while (true) {
      try {
        int choice = manageCheckOutView.displayPaymentStatusSubmenu(current);
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
        String selectedSort = manageCheckOutView.displaySortMenu();
        return (selectedSort == null) ? currentSort : selectedSort;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<Billing> filterAndSortActiveStays(
      ListInterface<Billing> source,
      String guestId,
      String guestName,
      String roomNumber,
      String paymentStatus,
      String sort) {
    ListInterface<Billing> filtered = new ArrayList<>();
    if (source == null || source.isEmpty()) return filtered;

    for (int i = 1; i <= source.getNumberOfEntries(); i++) {
      Billing b = source.getEntry(i);
      if (b == null) continue;
      Guest g = guestRepo.findById(b.getGuestId());

      boolean matchesGuestId =
          guestId == null
              || (b.getGuestId() != null
                  && b.getGuestId().toLowerCase().contains(guestId.toLowerCase()));
      boolean matchesGuestName =
          guestName == null
              || (g != null
                  && g.getName() != null
                  && g.getName().toLowerCase().contains(guestName.toLowerCase()));
      boolean matchesRoom =
          roomNumber == null
              || (b.getRoomNumber() != null
                  && b.getRoomNumber().toLowerCase().contains(roomNumber.toLowerCase()));
      boolean matchesPayment =
          paymentStatus == null || paymentStatus.equalsIgnoreCase(b.getStatus().name());

      if (matchesGuestId && matchesGuestName && matchesRoom && matchesPayment) {
        filtered.add(b);
      }
    }

    if ("ROOM NUMBER (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort((b1, b2) -> b1.getRoomNumber().compareToIgnoreCase(b2.getRoomNumber()));
    } else {
      // Default: GUEST NAME (A -> Z)
      filtered.sort(
          (b1, b2) -> {
            Guest g1 = guestRepo.findById(b1.getGuestId());
            Guest g2 = guestRepo.findById(b2.getGuestId());
            String n1 = (g1 != null && g1.getName() != null) ? g1.getName() : "";
            String n2 = (g2 != null && g2.getName() != null) ? g2.getName() : "";
            return n1.compareToIgnoreCase(n2);
          });
    }

    return filtered;
  }
}
