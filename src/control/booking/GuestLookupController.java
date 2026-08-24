package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import repo.GuestRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.GuestLookupView;

// Every booking flow starts by naming the person at the counter, so it lives here once.
public class GuestLookupController {
  private static final int MATCH_PAGE_SIZE = 10;

  private final GuestLookupView lookupView = new GuestLookupView();
  private final GuestRepo guestRepo;
  private final StandardReservationRepo standardReservationRepo;

  public GuestLookupController(
      GuestRepo guestRepo, StandardReservationRepo standardReservationRepo) {
    this.guestRepo = guestRepo;
    this.standardReservationRepo = standardReservationRepo;
  }

  /**
   * Runs the register browser until a guest is picked.
   *
   * @return the chosen guest, or null when the clerk backed out
   */
  public Guest findGuest(String title) {
    int searchField = GuestLookupView.FIELD_ALL;
    String searchTerm = null;
    boolean exactMatch = false;
    int page = 1;

    while (true) {
      try {
        // No term means the whole register, so a visible guest needs no invented search.
        ListInterface<Guest> matches =
            (searchTerm == null)
                ? guestRepo.getGuestList()
                : search(searchField, searchTerm, exactMatch);

        int total = matches.getNumberOfEntries();
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / MATCH_PAGE_SIZE);
        page = Math.min(Math.max(page, 1), Math.max(totalPages, 1));

        ConsoleUtil.GetMenuInputResult result =
            lookupView.renderRegisterScreen(
                title,
                buildGuestRowDTO(matches),
                labelFor(searchField),
                searchTerm,
                exactMatch ? "EXACT" : "CONTAINS",
                page,
                MATCH_PAGE_SIZE);

        if ("E".equalsIgnoreCase(result.input)) {
          return null;
        }

        if ("S".equalsIgnoreCase(result.input)) {
          Object[] filters = handleFilterMenu(searchField, searchTerm, exactMatch);
          searchField = (Integer) filters[0];
          searchTerm = (String) filters[1];
          exactMatch = (Boolean) filters[2];
          page = 1;
          continue;
        }

        if ("N".equalsIgnoreCase(result.input)) {
          if (page < totalPages) {
            page++;
          } else {
            ConsoleUtil.printError("Already on the last page!");
          }
          continue;
        }

        if ("P".equalsIgnoreCase(result.input)) {
          if (page > 1) {
            page--;
          } else {
            ConsoleUtil.printError("Already on the first page!");
          }
          continue;
        }

        if (result.isNumber) {
          // The table renumbers per page, so the page origin is added back to the row read.
          Guest picked = matches.getEntry((page - 1) * MATCH_PAGE_SIZE + result.getAsInt());
          if (picked != null) return picked;
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns {field, term, exactMatch}, unchanged when the clerk discards.
  private Object[] handleFilterMenu(int currentField, String currentTerm, boolean currentExact) {
    int field = currentField;
    String term = currentTerm;
    boolean exactMatch = currentExact;

    while (true) {
      try {
        int choice =
            lookupView.displayFilterMainMenu(
                labelFor(field), term, exactMatch ? "EXACT" : "CONTAINS");

        if (choice == 1) {
          int picked = promptSearchField(labelFor(field));
          if (picked != GuestLookupView.BACK) field = picked;
        } else if (choice == 2) {
          String typed = promptSearchTerm(labelFor(field), term);
          if (!"E".equalsIgnoreCase(typed)) {
            term = "-".equals(typed) ? null : typed;
          }
        } else if (choice == 3) {
          exactMatch = !exactMatch;
        } else if (choice == 4) {
          field = GuestLookupView.FIELD_ALL;
          term = null;
          exactMatch = false;
        } else if (choice == 5) {
          return new Object[] {field, term, exactMatch};
        } else {
          return new Object[] {currentField, currentTerm, currentExact};
        }
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptSearchField(String current) {
    while (true) {
      try {
        return lookupView.displaySearchFieldSubmenu(current);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Blank redraws instead of clearing, so a stray Enter never silently widens the search.
  private String promptSearchTerm(String fieldLabel, String current) {
    while (true) {
      try {
        String typed = lookupView.promptSearchTerm(fieldLabel, current);
        if (typed == null || typed.trim().isEmpty()) continue;
        return typed.trim();
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private ListInterface<GuestLookupView.GuestRowDTO> buildGuestRowDTO(
      ListInterface<Guest> matches) {
    ListInterface<GuestLookupView.GuestRowDTO> rows = new ArrayList<>();

    for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
      Guest g = matches.getEntry(i);
      if (g == null) continue;

      rows.add(
          new GuestLookupView.GuestRowDTO(
              g.getGuestId(),
              g.getName(),
              (g.getIcNumber() != null) ? g.getIcNumber() : blankToNa(g.getPassportNumber()),
              blankToNa(g.getPhoneNumber()),
              blankToNa(g.getEmail())));
    }
    return rows;
  }

  private ListInterface<Guest> search(int field, String term, boolean exactMatch) {
    if (field == GuestLookupView.FIELD_BOOKING_CODE) {
      return searchByBookingCode(term, exactMatch);
    }
    if (field == GuestLookupView.FIELD_PHONE) {
      return searchByPhoneDigits(term, exactMatch);
    }
    return guestRepo.searchGuests(term, repoFieldFor(field), exactMatch);
  }

  // Both stored and typed numbers carry stray hyphens, so both reduce to digits.
  private ListInterface<Guest> searchByPhoneDigits(String term, boolean exactMatch) {
    ListInterface<Guest> matches = new ArrayList<>();
    String query = digitsOnly(term);
    if (query.isEmpty()) return matches;

    ListInterface<Guest> guests = guestRepo.getGuestList();
    for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
      Guest g = guests.getEntry(i);
      if (g == null || g.getPhoneNumber() == null) continue;

      String stored = digitsOnly(g.getPhoneNumber());
      if (exactMatch ? stored.equals(query) : stored.contains(query)) {
        matches.add(g);
      }
    }
    return matches;
  }

  private String digitsOnly(String value) {
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isDigit(c)) {
        digits.append(c);
      }
    }
    return digits.toString();
  }

  // A booking code names the guest too, so it resolves through the reservation.
  private ListInterface<Guest> searchByBookingCode(String term, boolean exactMatch) {
    ListInterface<Guest> matches = new ArrayList<>();
    ListInterface<Reservation> all = standardReservationRepo.getAllReservations();
    String query = term.toLowerCase();

    for (int i = 1; i <= all.getNumberOfEntries(); i++) {
      Reservation r = all.getEntry(i);
      if (r == null) continue;

      if (!hit(r.getReservationId(), query, exactMatch)
          && !hit(r.getConfirmationNumber(), query, exactMatch)) {
        continue;
      }

      Guest g = guestRepo.findById(r.getGuestId());
      if (g != null && !matches.contains(g)) {
        matches.add(g);
      }
    }

    return matches;
  }

  private boolean hit(String value, String query, boolean exactMatch) {
    if (value == null) return false;
    String candidate = value.toLowerCase();
    return exactMatch ? candidate.equals(query) : candidate.contains(query);
  }

  private String repoFieldFor(int field) {
    if (field == GuestLookupView.FIELD_GUEST_ID) return GuestRepo.FIELD_GUEST_ID;
    if (field == GuestLookupView.FIELD_NAME) return GuestRepo.FIELD_NAME;
    if (field == GuestLookupView.FIELD_IC) return GuestRepo.FIELD_IC;
    if (field == GuestLookupView.FIELD_PASSPORT) return GuestRepo.FIELD_PASSPORT;
    if (field == GuestLookupView.FIELD_PHONE) return GuestRepo.FIELD_PHONE;
    if (field == GuestLookupView.FIELD_EMAIL) return GuestRepo.FIELD_EMAIL;
    return GuestRepo.FIELD_ALL;
  }

  private String labelFor(int field) {
    if (field == GuestLookupView.FIELD_GUEST_ID) return "Guest ID";
    if (field == GuestLookupView.FIELD_NAME) return "Full Name";
    if (field == GuestLookupView.FIELD_IC) return "IC Number";
    if (field == GuestLookupView.FIELD_PASSPORT) return "Passport Number";
    if (field == GuestLookupView.FIELD_PHONE) return "Phone Number";
    if (field == GuestLookupView.FIELD_EMAIL) return "Email Address";
    if (field == GuestLookupView.FIELD_BOOKING_CODE) return "Reservation ID Or Code";
    return "All Fields";
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }
}
