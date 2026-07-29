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
          ListInterface<Billing> roomHistory = getGuestBillingHistoryNewToOld(guest.getGuestId());
          frontDeskView.displayAssignedRoomHistory(guest, roomHistory);
        } else if (action == 4) {
          ListInterface<Reservation> reservationHistory =
              getGuestReservationHistoryNewToOld(guest.getGuestId());
          frontDeskView.displayReservationHistory(guest, reservationHistory);
        } else if (action == 5) {
          return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private void handleViewBillingHistory(Guest guest) {
    ListInterface<Billing> billingHistory = getGuestBillingHistoryNewToOld(guest.getGuestId());
    String choice = frontDeskView.displayBillingHistory(guest, billingHistory);

    if (choice == null || "C".equalsIgnoreCase(choice)) {
      return;
    }

    try {
      int index = Integer.parseInt(choice);
      if (index >= 1 && index <= billingHistory.getNumberOfEntries()) {
        Billing selected = billingHistory.getEntry(index);
        frontDeskView.displayReceipt(guest, selected);
      }
    } catch (NumberFormatException ignored) {
      // The view already restricts input to a valid index or 'C', so this should
      // never actually happen - guard kept for safety.
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
  //
  // Both histories are built the same way: collect the guest's matching records,
  // sort them oldest-first, then PUSH them onto a Stack ADT in that order. Because
  // a stack is LIFO, the most recently pushed (i.e. most recent) record ends up on
  // top. Popping the stack until it is empty then naturally yields the records in
  // newest-to-oldest order, which is what the front desk view displays.

  private ListInterface<Billing> getGuestBillingHistoryNewToOld(String guestId) {
    ListInterface<Billing> matches = billingRepo.findByGuestId(guestId);
    matches.sort(Comparator.comparing(Billing::getCheckInDate, nullsFirstComparator()));

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
    matches.sort(Comparator.comparing(Reservation::getReservationTime, nullsFirstComparator()));

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
