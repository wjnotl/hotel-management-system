package control.frontdesk;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.Billing;
import entity.Guest;
import entity.Reservation;
import java.util.Comparator;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.VipReservationRepo;
import util.ConsoleUtil;
import view.frontdesk.GuestInformationView;

public class GuestInformationController {
  private static final int PAGE_SIZE = 10;

  private final GuestInformationView guestInformationView = new GuestInformationView();
  private final GuestRepo guestRepo = new GuestRepo();
  private final VipReservationRepo reservationRepo;
  private final BillingRepo billingRepo = new BillingRepo();

  public GuestInformationController(VipReservationRepo reservationRepo) {
    this.reservationRepo = reservationRepo;
  }

  // Convenience constructor for callers that don't need to share a VipReservationRepo instance.
  public GuestInformationController() {
    this(new VipReservationRepo());
  }

  public void start() {
    while (true) {
      try {
        String rawId = guestInformationView.promptGuestIdInput();

        if (rawId == null || rawId.trim().isEmpty() || "C".equalsIgnoreCase(rawId.trim())) {
          return;
        }

        String guestId = rawId.trim();
        // O(1) lookup - GuestRepo indexes Guest ID -> Guest in a HashMap internally.
        Guest guest = guestRepo.findById(guestId);

        if (guest == null) {
          guestInformationView.displayGuestNotFound(guestId);
          continue;
        }

        boolean reEnterGuestId = handleGuestActionSubmenu(guest);
        if (reEnterGuestId) {
          // "5. ReEnter Guest ID" was chosen - loop back to the guest ID prompt.
          continue;
        }
        // "6. Back to Front Desk Menu" was chosen.
        return;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns true if the guest chose to re-enter a guest ID (option 5),
  // or false if they chose to go back to the Front Desk Menu (option 6).
  private boolean handleGuestActionSubmenu(Guest guest) {
    while (true) {
      try {
        int action = guestInformationView.displayGuestActionSubmenu(guest);

        if (action == 1) {
          Reservation latestReservation = findLatestReservationForGuest(guest.getGuestId());
          Billing latestBilling = findLatestBillingForGuest(guest.getGuestId());
          guestInformationView.displayGuestDetails(guest, latestReservation, latestBilling);
        } else if (action == 2) {
          handleViewBillingHistory(guest);
        } else if (action == 3) {
          handleViewAssignedRoomHistory(guest);
        } else if (action == 4) {
          handleViewReservationHistory(guest);
        } else if (action == 5) {
          return true;
        } else if (action == 6) {
          return false;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleViewBillingHistory(Guest guest) {
    ListInterface<Billing> billingHistory = getGuestStayHistoryNewToOld(guest.getGuestId());
    int currentPage = 1;

    while (true) {
      try {
        int total = billingHistory.getNumberOfEntries();
        ConsoleUtil.GetMenuInputResult result =
            guestInformationView.displayBillingHistory(guest, billingHistory, currentPage, PAGE_SIZE);

        if ("C".equalsIgnoreCase(result.input)) {
          return;
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
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
          int index = Integer.parseInt(result.input);
          Billing selected = billingHistory.getEntry(index);
          guestInformationView.displayReceipt(guest, selected);
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleViewAssignedRoomHistory(Guest guest) {
    ListInterface<Billing> roomHistory = getGuestStayHistoryNewToOld(guest.getGuestId());
    int currentPage = 1;

    while (true) {
      try {
        int total = roomHistory.getNumberOfEntries();
        ConsoleUtil.GetMenuInputResult result =
            guestInformationView.displayAssignedRoomHistory(guest, roomHistory, currentPage, PAGE_SIZE);

        if ("C".equalsIgnoreCase(result.input)) {
          return;
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
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
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleViewReservationHistory(Guest guest) {
    ListInterface<Reservation> reservationHistory =
        getGuestReservationHistoryNewToOld(guest.getGuestId());
    int currentPage = 1;

    while (true) {
      try {
        int total = reservationHistory.getNumberOfEntries();
        ConsoleUtil.GetMenuInputResult result =
            guestInformationView.displayReservationHistory(
                guest, reservationHistory, currentPage, PAGE_SIZE);

        if ("C".equalsIgnoreCase(result.input)) {
          return;
        } else if ("N".equalsIgnoreCase(result.input)) {
          int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
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
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // --- LOOKUP HELPERS ---
  private Reservation findLatestReservationForGuest(String guestId) {
    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    Reservation latest = null;

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && guestId.equalsIgnoreCase(r.getGuestId())) {
        if (latest == null || r.getReservationTime().isAfter(latest.getReservationTime())) {
          latest = r;
        }
      }
    }
    return latest;
  }

  private Billing findLatestBillingForGuest(String guestId) {
    ListInterface<Billing> matches = billingRepo.findByGuestId(guestId);
    Billing latest = null;

    for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
      Billing b = matches.getEntry(i);
      if (latest == null || b.getCreatedAt().isAfter(latest.getCreatedAt())) {
        latest = b;
      }
    }
    return latest;
  }

  // --- STACK-BASED HISTORY BUILDERS (NEW -> OLD) ---
  private ListInterface<Billing> getGuestStayHistoryNewToOld(String guestId) {
    ListInterface<Billing> matches = billingRepo.findByGuestId(guestId);
    matches.sort(Comparator.comparing(b -> b.getCheckInDate(), nullsFirstComparator()));

    StackInterface<Billing> historyStack = new LinkedStack<>();
    for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
      historyStack.push(matches.getEntry(i));
    }

    ListInterface<Billing> newestToOldest = new ArrayList<>();
    while (!historyStack.isEmpty()) {
      newestToOldest.add(historyStack.pop());
    }
    return newestToOldest;
  }

  private ListInterface<Reservation> getGuestReservationHistoryNewToOld(String guestId) {
    ListInterface<Reservation> all = reservationRepo.getAllReservations();
    ListInterface<Reservation> matches = new ArrayList<>();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r != null && guestId.equalsIgnoreCase(r.getGuestId())) {
        matches.add(r);
      }
    }
    matches.sort(Comparator.comparing(r -> r.getReservationTime(), nullsFirstComparator()));

    StackInterface<Reservation> historyStack = new LinkedStack<>();
    for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
      historyStack.push(matches.getEntry(i));
    }

    ListInterface<Reservation> newestToOldest = new ArrayList<>();
    while (!historyStack.isEmpty()) {
      newestToOldest.add(historyStack.pop());
    }
    return newestToOldest;
  }

  private static <T extends Comparable<T>> Comparator<T> nullsFirstComparator() {
    return Comparator.nullsFirst(Comparator.naturalOrder());
  }
}
