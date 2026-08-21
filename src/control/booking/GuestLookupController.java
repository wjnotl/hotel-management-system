package control.booking;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import entity.Reservation;
import repo.GuestRepo;
import repo.StandardReservationRepo;
import util.ConsoleUtil;
import view.booking.GuestLookupView;

// Putting a name to the person at the counter is the first thing every booking flow does, so it is
// written once here rather than three times with three sets of rules.
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
   * Runs the whole search loop.
   *
   * @return the chosen guest, or null when the clerk backed out
   */
  public Guest findGuest(String title) {
    boolean exactMatch = false;

    while (true) {
      try {
        String matchMode = exactMatch ? "EXACT" : "CONTAINS";
        int choice = lookupView.displayFieldMenu(title, matchMode);

        if (choice == GuestLookupView.BACK) {
          return null;
        }

        if (choice == GuestLookupView.TOGGLE_MATCH_MODE) {
          exactMatch = !exactMatch;
          continue;
        }

        Guest picked = runSearch(choice, exactMatch);
        if (picked != null) return picked;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  // Returns the guest once one is settled on, or null to fall back to the field menu.
  private Guest runSearch(int field, boolean exactMatch) {
    String fieldLabel = labelFor(field);
    String matchMode = exactMatch ? "EXACT" : "CONTAINS";

    while (true) {
      try {
        String term = lookupView.promptSearchTerm(fieldLabel, matchMode);

        if ("E".equalsIgnoreCase(term.trim())) {
          return null;
        }

        term = term.trim();
        ListInterface<Guest> matches = search(field, term, exactMatch);

        if (matches.isEmpty()) {
          if (promptNotFound(fieldLabel, term) == GuestLookupView.BACK) {
            return null;
          }
          continue;
        }

        // A single hit is not a choice, so the clerk is not made to confirm a list of one.
        if (matches.getNumberOfEntries() == 1) {
          return matches.getEntry(1);
        }

        Guest picked = pickFromMatches(fieldLabel, term, matchMode, matches);
        if (picked != null) return picked;
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private int promptNotFound(String fieldLabel, String term) {
    while (true) {
      try {
        return lookupView.displayNotFoundScreen(fieldLabel, term);
      } catch (Exception e) {
        ConsoleUtil.printError(e.getMessage());
      }
    }
  }

  private Guest pickFromMatches(
      String fieldLabel, String term, String matchMode, ListInterface<Guest> matches) {

    ListInterface<GuestLookupView.GuestRowDTO> rows = buildGuestRowDTO(matches);
    int page = 1;

    while (true) {
      try {
        ConsoleUtil.GetMenuInputResult result =
            lookupView.displayMatches(fieldLabel, term, matchMode, rows, page, MATCH_PAGE_SIZE);

        int totalPages = (int) Math.ceil((double) matches.getNumberOfEntries() / MATCH_PAGE_SIZE);

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
          if (result.getAsInt() == GuestLookupView.BACK) {
            return null;
          }

          // The table renumbers from 1 on every page, so the row read off the screen is an offset
          // into the page and the page origin has to be added back.
          int index = (page - 1) * MATCH_PAGE_SIZE + result.getAsInt();
          Guest picked = matches.getEntry(index);
          if (picked != null) return picked;
        }
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
    return guestRepo.searchGuests(term, repoFieldFor(field), exactMatch);
  }

  // A guest who quotes a booking code rather than a document is still the guest this flow needs, so
  // the code is resolved to its reservation first and the reservation back to its guest file.
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
    return "Any Field";
  }

  private String blankToNa(String value) {
    return (value == null || value.isEmpty()) ? "N/A" : value;
  }
}
