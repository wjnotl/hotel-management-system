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
import view.FrontDeskView;

public class GuestInformationController {
  private static final int PAGE_SIZE = 10;

  private final FrontDeskView frontDeskView = new FrontDeskView();
  private final GuestRepo guestRepo = new GuestRepo();
  private final VipReservationRepo reservationRepo = new VipReservationRepo();
  private final BillingRepo billingRepo = new BillingRepo();

  public void start() {
    while (true) {
      try {
        String rawId = frontDeskView.promptGuestIdInput();

        if (rawId == null || rawId.trim().isEmpty() || "C".equalsIgnoreCase(rawId.trim())) {
          return;
        }

        String guestId = rawId.trim();
        // O(1) lookup - GuestRepo indexes Guest ID -> Guest in a HashMap internally.
        Guest guest = guestRepo.findById(guestId);

        if (guest == null) {
          frontDeskView.displayGuestNotFound(guestId);
          continue;
        }

        handleGuestActionSubmenu(guest);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleGuestActionSubmenu(Guest guest) {
    while (true) {
      try {
        int action = frontDeskView.displayGuestActionSubmenu(guest);

        if (action == 1) {
          Reservation latestReservation = findLatestReservationForGuest(guest.getGuestId());
          Billing latestBilling = findLatestBillingForGuest(guest.getGuestId());
          frontDeskView.displayGuestDetails(guest, latestReservation, latestBilling);
        } else if (action == 2) {
          handleViewBillingHistory(guest);
        } else if (action == 3) {
          handleViewAssignedRoomHistory(guest);
        } else if (action == 4) {
          handleViewReservationHistory(guest);
        } else if (action == 5) {
          return;
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
      int total = billingHistory.getNumberOfEntries();
      ConsoleUtil.GetMenuInputResult result =
          frontDeskView.displayBillingHistory(guest, billingHistory, currentPage, PAGE_SIZE);

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
        frontDeskView.displayReceipt(guest, selected);
        return;
      }
    }
  }

  private void handleViewAssignedRoomHistory(Guest guest) {
    ListInterface<Billing> roomHistory = getGuestStayHistoryNewToOld(guest.getGuestId());
    int currentPage = 1;

    while (true) {
      int total = roomHistory.getNumberOfEntries();
      ConsoleUtil.GetMenuInputResult result =
          frontDeskView.displayAssignedRoomHistory(guest, roomHistory, currentPage, PAGE_SIZE);

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
    }
  }

  private void handleViewReservationHistory(Guest guest) {
    ListInterface<Reservation> reservationHistory =
        getGuestReservationHistoryNewToOld(guest.getGuestId());
    int currentPage = 1;

    while (true) {
      int total = reservationHistory.getNumberOfEntries();
      ConsoleUtil.GetMenuInputResult result =
          frontDeskView.displayReservationHistory(
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

  // Both "Billing History" and "Assigned Room History" pull from the same stay
  // records (a Billing IS a completed/ongoing room stay) - just rendered with
  // different columns. There's no separate room-assignment log for past stays.
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
