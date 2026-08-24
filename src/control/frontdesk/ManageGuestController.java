package control.frontdesk;

import adt.ArrayList;
import adt.ListInterface;
import entity.Billing;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import repo.BillingRepo;
import repo.GuestRepo;
import repo.MemberRepo;
import repo.ReservationRepo;
import util.ConsoleUtil;
import view.frontdesk.ManageGuestView;

public class ManageGuestController {

  private static final int PAGE_SIZE = 10;

  private final ManageGuestView manageGuestView = new ManageGuestView();
  private final GuestRepo guestRepo;
  private final ReservationRepo reservationRepo;
  private final BillingRepo billingRepo;
  private final MemberRepo memberRepo;

  public ManageGuestController(
      ReservationRepo reservationRepo,
      GuestRepo guestRepo,
      BillingRepo billingRepo,
      MemberRepo memberRepo) {
    this.reservationRepo = reservationRepo;
    this.guestRepo = guestRepo;
    this.billingRepo = billingRepo;
    this.memberRepo = memberRepo;
  }

  // entry point
  public void start() {
    int currentPage = 1;
    String searchQuery = null;
    String memberLevelFilter = null;
    String sortCriteria = "NAME (A -> Z)";

    while (true) {
      try {
        ListInterface<Guest> allGuests = guestRepo.getGuestList();
        ListInterface<Guest> filtered =
            filterAndSortGuests(allGuests, searchQuery, memberLevelFilter, sortCriteria);

        int total = filtered.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        ManageGuestView.GuestRowDTO[] pageRows =
            buildGuestRowPage(filtered, currentPage, PAGE_SIZE);

        ConsoleUtil.GetMenuInputResult result =
            manageGuestView.renderGuestTable(
                pageRows,
                total,
                searchQuery,
                memberLevelFilter,
                sortCriteria,
                currentPage,
                PAGE_SIZE);

        String raw = result.input.trim();

        if ("E".equalsIgnoreCase(raw)) {
          return;
        } else if ("R".equalsIgnoreCase(raw)) {
          // Refresh
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
          String[] filters = handleFilterMenu(searchQuery, memberLevelFilter);
          searchQuery = filters[0];
          memberLevelFilter = filters[1];
          currentPage = 1;
        } else if ("O".equalsIgnoreCase(raw)) {
          String newSort = handleSortMenu(sortCriteria);
          if (newSort != null) {
            sortCriteria = newSort;
            currentPage = 1;
          }
        } else if (result.isNumber) {
          int indexOnPage = result.getAsInt();
          int actualIndex = (currentPage - 1) * PAGE_SIZE + indexOnPage;
          if (actualIndex < 1 || actualIndex > total) {
            ConsoleUtil.printError("Invalid row number. Enter a number shown in the table.");
            continue;
          }
          Guest selected = filtered.getEntry(actualIndex);
          if (selected == null) {
            ConsoleUtil.printError("Guest not found.");
            continue;
          }
          boolean reEnter = handleGuestActionSubmenu(selected);
          if (!reEnter) return;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // guest action menu
  private boolean handleGuestActionSubmenu(Guest guest) {
    while (true) {
      try {
        int action = manageGuestView.displayGuestActionSubmenu(guest);

        if (action == 1) {
          handleViewGuestDetails(guest);
        } else if (action == 2) {
          handleViewBillingHistory(guest);
        } else if (action == 3) {
          handleViewReservationAndRoomHistory(guest);
        } else if (action == 4) {
          return true; // Back to Manage Guest list
        } else if (action == 5) {
          return false; // Back to Front Desk Menu
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // action 1 — guest details
  private void handleViewGuestDetails(Guest guest) {
    Member member = (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;
    Reservation latestReservation = findLatestReservationForGuest(guest.getGuestId());
    Billing latestBilling = findLatestBillingForGuest(guest.getGuestId());
    int totalBookings = findAllReservationsForGuest(guest.getGuestId()).getNumberOfEntries();
    manageGuestView.displayGuestDetails(
        guest, member, latestReservation, latestBilling, totalBookings);
  }

  // action 2 - billing history
  private void handleViewBillingHistory(Guest guest) {
    int currentPage = 1;
    LocalDate fromDate = null;
    LocalDate toDate = null;

    while (true) {
      try {
        ListInterface<Billing> billingHistory =
            getGuestBillingSortedNewToOld(guest.getGuestId(), fromDate, toDate);
        int total = billingHistory.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        Billing[] pageRows = toBillingPageArray(billingHistory, currentPage, PAGE_SIZE);

        ConsoleUtil.GetMenuInputResult result =
            manageGuestView.displayBillingHistory(
                guest, pageRows, total, fromDate, toDate, currentPage, PAGE_SIZE);

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
        } else if ("F".equalsIgnoreCase(raw)) {
          LocalDate[] dates = handleBillingDateFilter(fromDate, toDate);
          fromDate = dates[0];
          toDate = dates[1];
          currentPage = 1;
        } else if (result.isNumber) {
          int indexOnPage = result.getAsInt();
          int actualIndex = (currentPage - 1) * PAGE_SIZE + indexOnPage;
          if (actualIndex >= 1 && actualIndex <= total) {
            Billing selected = billingHistory.getEntry(actualIndex);
            if (selected != null) {
              manageGuestView.displayReceipt(guest, selected);
            }
          } else {
            ConsoleUtil.printError("Invalid selection.");
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // action 3 — reservation and room history
  private void handleViewReservationAndRoomHistory(Guest guest) {
    int currentPage = 1;

    while (true) {
      try {
        ListInterface<Reservation> reservationHistory =
            getGuestReservationsSortedNewToOld(guest.getGuestId());
        int total = reservationHistory.getNumberOfEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        Reservation[] pageRows = toReservationPageArray(reservationHistory, currentPage, PAGE_SIZE);

        ConsoleUtil.GetMenuInputResult result =
            manageGuestView.displayReservationHistory(
                guest, pageRows, total, currentPage, PAGE_SIZE);

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
        } else if (result.isNumber) {
          int indexOnPage = result.getAsInt();
          int actualIndex = (currentPage - 1) * PAGE_SIZE + indexOnPage;
          if (actualIndex >= 1 && actualIndex <= total) {
            Reservation selected = reservationHistory.getEntry(actualIndex);
            if (selected != null) {
              Billing linkedBilling = findBillingByReservationId(selected.getReservationId());
              manageGuestView.displayAssignedRoomDetail(guest, selected, linkedBilling);
            }
          } else {
            ConsoleUtil.printError("Invalid selection.");
          }
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  //  filter  menus
  private String[] handleFilterMenu(String currentQuery, String currentMemberLevel) {
    String query = currentQuery;
    String level = currentMemberLevel;

    while (true) {
      try {
        int choice = manageGuestView.displayFilterMenu(query, level);
        if (choice == 1) {
          String input = manageGuestView.promptSearchQuery(query);
          query = (input == null || input.trim().isEmpty()) ? null : input.trim();
        } else if (choice == 2) {
          level = handleMemberLevelSubmenu(level);
        } else if (choice == 3) {
          query = null;
          level = null; // Clear all
        } else if (choice == 4) {
          return new String[] {query, level}; // Apply & back
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private String handleMemberLevelSubmenu(String current) {
    while (true) {
      try {
        int choice = manageGuestView.displayMemberLevelSubmenu(current);
        if (choice == 1) return "DIAMOND";
        if (choice == 2) return "GOLD";
        if (choice == 3) return "SILVER";
        if (choice == 4) return "NON-MEMBER";
        if (choice == 5) return null; // All
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // sort menu
  private String handleSortMenu(String currentSort) {
    while (true) {
      try {
        return manageGuestView.displaySortMenu(currentSort);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private LocalDate[] handleBillingDateFilter(LocalDate currentFrom, LocalDate currentTo) {
    while (true) {
      try {
        String[] raw = manageGuestView.promptDateFilterRaw(currentFrom, currentTo);
        LocalDate from = parseDateOrKeep(raw[0], currentFrom);
        LocalDate to = parseDateOrKeep(raw[1], currentTo);
        return new LocalDate[] {from, to};
      } catch (IllegalArgumentException e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // if - clears it, otherwise parses as YYYY-MM-DD
  private LocalDate parseDateOrKeep(String raw, LocalDate current) {
    if (raw == null || raw.trim().isEmpty()) return current;
    if ("-".equals(raw.trim())) return null;
    try {
      return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD.");
    }
  }

  // filter and sort
  private ListInterface<Guest> filterAndSortGuests(
      ListInterface<Guest> source, String query, String memberLevel, String sort) {
    if (source == null) return new ArrayList<>();

    ListInterface<Guest> filtered;
    if (query == null || query.trim().isEmpty()) {
      filtered = source;
    } else {
      String q = query.trim().toLowerCase();
      filtered =
          source.filter(
              g ->
                  g != null
                      && (containsIgnoreCase(g.getGuestId(), q)
                          || containsIgnoreCase(g.getName(), q)
                          || containsIgnoreCase(g.getIcNumber(), q)
                          || containsIgnoreCase(g.getPassportNumber(), q)
                          || containsIgnoreCase(g.getPhoneNumber(), q)));
    }

    // Apply member level filter
    if (memberLevel != null) {
      String lvl = memberLevel;
      filtered =
          filtered.filter(
              g -> {
                if (g == null) return false;
                if ("NON-MEMBER".equalsIgnoreCase(lvl)) return g.getMemberId() == null;
                entity.Member m = memberRepo.findById(g.getMemberId());
                return m != null && m.getTier() != null && lvl.equalsIgnoreCase(m.getTier().name());
              });
    }

    if ("NAME (Z -> A)".equalsIgnoreCase(sort)) {
      filtered.sort((g1, g2) -> nullSafeCompare(g2.getName(), g1.getName()));
    } else if ("GUEST ID (LOW -> HIGH)".equalsIgnoreCase(sort)) {
      filtered.sort((g1, g2) -> nullSafeCompare(g1.getGuestId(), g2.getGuestId()));
    } else if ("GUEST ID (HIGH -> LOW)".equalsIgnoreCase(sort)) {
      filtered.sort((g1, g2) -> nullSafeCompare(g2.getGuestId(), g1.getGuestId()));
    } else if ("PHONE (A -> Z)".equalsIgnoreCase(sort)) {
      filtered.sort((g1, g2) -> nullSafeCompare(g1.getPhoneNumber(), g2.getPhoneNumber()));
    } else {
      // Default: NAME (A -> Z)
      filtered.sort((g1, g2) -> nullSafeCompare(g1.getName(), g2.getName()));
    }

    return filtered;
  }

  // guest row dto
  private ManageGuestView.GuestRowDTO[] buildGuestRowPage(
      ListInterface<Guest> guests, int currentPage, int pageSize) {
    if (guests == null) return new ManageGuestView.GuestRowDTO[0];

    int total = guests.getNumberOfEntries();
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    int count = Math.max(0, endIndex - startIndex + 1);

    ManageGuestView.GuestRowDTO[] page = new ManageGuestView.GuestRowDTO[count];
    for (int i = 0; i < count; i++) {
      Guest g = guests.getEntry(startIndex + i);
      if (g == null) continue;
      Member member = (g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;
      String memberLevel =
          (member != null && member.getTier() != null) ? member.getTier().name() : "NON-MEMBER";
      String icOrPassport =
          (g.getIcNumber() != null
                  && !g.getIcNumber().trim().isEmpty()
                  && !"N/A".equalsIgnoreCase(g.getIcNumber().trim()))
              ? g.getIcNumber()
              : (g.getPassportNumber() != null && !g.getPassportNumber().trim().isEmpty()
                  ? g.getPassportNumber()
                  : "N/A");
      page[i] =
          new ManageGuestView.GuestRowDTO(
              g.getGuestId(), g.getName(), icOrPassport, g.getPhoneNumber(), memberLevel);
    }
    return page;
  }

  //  billing sorted newest → oldest
  private ListInterface<Billing> getGuestBillingSortedNewToOld(
      String guestId, LocalDate fromDate, LocalDate toDate) {
    ListInterface<Billing> matches =
        billingRepo
            .findByGuestId(guestId)
            .filter(
                b -> {
                  if (fromDate != null
                      && b.getCheckInDate() != null
                      && b.getCheckInDate().isBefore(fromDate)) return false;
                  if (toDate != null
                      && b.getCheckInDate() != null
                      && b.getCheckInDate().isAfter(toDate)) return false;
                  return true;
                });

    matches.sort(
        (b1, b2) -> {
          LocalDate d1 = (b1 != null) ? b1.getCheckInDate() : null;
          LocalDate d2 = (b2 != null) ? b2.getCheckInDate() : null;
          if (d1 == null && d2 == null) return 0;
          if (d1 == null) return 1;
          if (d2 == null) return -1;
          return d2.compareTo(d1); // newest first
        });
    return matches;
  }

  // reservations sorted newest → oldest
  private ListInterface<Reservation> getGuestReservationsSortedNewToOld(String guestId) {
    ListInterface<Reservation> matches =
        reservationRepo
            .getAllReservations()
            .filter(r -> r != null && guestId.equalsIgnoreCase(r.getGuestId()));

    matches.sort(
        (r1, r2) -> {
          LocalDateTime t1 = (r1 != null) ? r1.getReservationTime() : null;
          LocalDateTime t2 = (r2 != null) ? r2.getReservationTime() : null;
          if (t1 == null && t2 == null) return 0;
          if (t1 == null) return 1;
          if (t2 == null) return -1;
          return t2.compareTo(t1); // newest first
        });
    return matches;
  }

  private ListInterface<Reservation> findAllReservationsForGuest(String guestId) {
    return reservationRepo
        .getAllReservations()
        .filter(r -> r != null && guestId.equalsIgnoreCase(r.getGuestId()));
  }

  private Reservation findLatestReservationForGuest(String guestId) {
    return reservationRepo
        .getAllReservations()
        .filter(r -> r != null && guestId.equalsIgnoreCase(r.getGuestId()))
        .reduce(
            null,
            (acc, r) ->
                (acc == null || r.getReservationTime().isAfter(acc.getReservationTime()))
                    ? r
                    : acc);
  }

  private Billing findLatestBillingForGuest(String guestId) {
    return billingRepo
        .findByGuestId(guestId)
        .reduce(
            null,
            (acc, b) -> (acc == null || b.getCreatedAt().isAfter(acc.getCreatedAt())) ? b : acc);
  }

  private Billing findBillingByReservationId(String reservationId) {
    if (reservationId == null) return null;
    return billingRepo
        .getBillingList()
        .find(b -> b != null && reservationId.equalsIgnoreCase(b.getReservationId()));
  }

  // adt list -> plain array
  private Billing[] toBillingPageArray(
      ListInterface<Billing> source, int currentPage, int pageSize) {
    int total = source.getNumberOfEntries();
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    int count = Math.max(0, endIndex - startIndex + 1);
    Billing[] page = new Billing[count];
    for (int i = 0; i < count; i++) page[i] = source.getEntry(startIndex + i);
    return page;
  }

  private Reservation[] toReservationPageArray(
      ListInterface<Reservation> source, int currentPage, int pageSize) {
    int total = source.getNumberOfEntries();
    int startIndex = (currentPage - 1) * pageSize + 1;
    int endIndex = Math.min(startIndex + pageSize - 1, total);
    int count = Math.max(0, endIndex - startIndex + 1);
    Reservation[] page = new Reservation[count];
    for (int i = 0; i < count; i++) page[i] = source.getEntry(startIndex + i);
    return page;
  }

  // uli
  private boolean containsIgnoreCase(String field, String query) {
    return field != null && field.toLowerCase().contains(query);
  }

  private int nullSafeCompare(String a, String b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareToIgnoreCase(b);
  }
}
