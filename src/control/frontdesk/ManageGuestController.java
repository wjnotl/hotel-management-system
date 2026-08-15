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
import view.frontdesk.ManageGuestView;

public class ManageGuestController {
  private static final int PAGE_SIZE = 10;

  private final ManageGuestView manageGuestView = new ManageGuestView();
  private final GuestRepo guestRepo = new GuestRepo();
  private final VipReservationRepo reservationRepo;
  private final BillingRepo billingRepo = new BillingRepo();

  public ManageGuestController(VipReservationRepo reservationRepo) {
    this.reservationRepo = reservationRepo;
  }

  // Convenience constructor for callers that don't need to share a VipReservationRepo instance.
  public ManageGuestController() {
    this(new VipReservationRepo());
  }

  public void start() {
    ListInterface<Guest> allGuests = guestRepo.getGuestList();
    String searchQuery = "";
    int currentPage = 1;

    while (true) {
      try {
        ListInterface<Guest> filtered = applyFilter(allGuests, searchQuery);
        int total = filtered.getNumberOfEntries();
        int totalPages = (total == 0) ? 1 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (currentPage > totalPages) currentPage = totalPages;

        ConsoleUtil.GetMenuInputResult result =
            manageGuestView.displayGuestTable(filtered, searchQuery, currentPage, PAGE_SIZE);

        String raw = result.input.trim();

        if ("C".equalsIgnoreCase(raw)) {
          return;
        } else if ("N".equalsIgnoreCase(raw)) {
          if (currentPage < totalPages) {
            currentPage++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
        } else if ("P".equalsIgnoreCase(raw)) {
          if (currentPage > 1) {
            currentPage--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
        } else if ("S".equalsIgnoreCase(raw)) {
          String newQuery = manageGuestView.promptSearchQuery();
          if (newQuery != null) {
            searchQuery = newQuery.trim();
            currentPage = 1;
          }
        } else if (result.isNumber) {
          int globalIndex = Integer.parseInt(raw);
          if (globalIndex < 1 || globalIndex > total) {
            ConsoleUtil.printError("Invalid row number. Enter a number shown in the table.");
            continue;
          }
          Guest selected = filtered.getEntry(globalIndex);
          if (selected == null) {
            ConsoleUtil.printError("Guest not found.");
            continue;
          }
          boolean reEnter = handleGuestActionSubmenu(selected);
          if (!reEnter) {
            // "Back to Front Desk Menu" chosen — propagate upward.
            return;
          }
          // "Re-Select Guest" chosen — stay in the table loop.
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns true if the guest chose to re-select a guest (option 5),
  // or false if they chose to go back to the Front Desk Menu (option 6).
  private boolean handleGuestActionSubmenu(Guest guest) {
    while (true) {
      try {
        int action = manageGuestView.displayGuestActionSubmenu(guest);

        if (action == 1) {
          Reservation latestReservation = findLatestReservationForGuest(guest.getGuestId());
          Billing latestBilling = findLatestBillingForGuest(guest.getGuestId());
          manageGuestView.displayGuestDetails(guest, latestReservation, latestBilling);
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
            manageGuestView.displayBillingHistory(guest, billingHistory, currentPage, PAGE_SIZE);

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
          manageGuestView.displayReceipt(guest, selected);
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
            manageGuestView.displayAssignedRoomHistory(guest, roomHistory, currentPage, PAGE_SIZE);

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
            manageGuestView.displayReservationHistory(
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

  // --- FILTER HELPER ---

  // Case-insensitive match against Guest ID or Guest Name.
  private ListInterface<Guest> applyFilter(ListInterface<Guest> all, String query) {
    if (query == null || query.isEmpty()) {
      return all;
    }
    String lower = query.toLowerCase();
    ListInterface<Guest> result = new ArrayList<>();
    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Guest g = all.getEntry(i);
      if (g == null) continue;
      if (g.getGuestId().toLowerCase().contains(lower)
          || g.getName().toLowerCase().contains(lower)) {
        result.add(g);
      }
    }
    return result;
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
