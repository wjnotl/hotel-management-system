package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;
import util.BinaryFileUtil;

public class GuestRepo {
  private final BinaryFileUtil<ListInterface<Guest>> fileUtil;
  private ListInterface<Guest> guestList;

  public GuestRepo() {
    this.fileUtil = new BinaryFileUtil<>("guests.dat");
    load();
  }

  private void load() {
    this.guestList = fileUtil.retrieveFromFile();
    if (this.guestList == null) {
      this.guestList = new ArrayList<>();
    }
  }

  private void save() {
    fileUtil.saveToFile(guestList);
  }

  public void addGuest(Guest guest) {
    if (guest == null) return;
    guestList.add(guest);
    save();
  }

  public boolean updateGuest(Guest updatedGuest) {
    if (updatedGuest == null || guestList == null) return false;

    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest existing = guestList.getEntry(i);
      if (existing != null && existing.equals(updatedGuest)) {
        guestList.replace(i, updatedGuest);
        save();
        return true;
      }
    }
    return false;
  }

  public void addOrUpdateGuest(Guest guest) {
    if (!updateGuest(guest)) {
      addGuest(guest);
    }
  }

  public Guest findById(String guestId) {
    if (guestId == null || guestList == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) {
        return g;
      }
    }
    return null;
  }

  public ListInterface<Guest> searchGuests(String query) {
    ListInterface<Guest> matches = new ArrayList<>();
    if (query == null || query.trim().isEmpty() || guestList == null) {
      return matches;
    }

    String q = query.trim().toLowerCase();

    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g == null) continue;

      boolean matchId = g.getGuestId() != null && g.getGuestId().toLowerCase().contains(q);
      boolean matchName = g.getName() != null && g.getName().toLowerCase().contains(q);
      boolean matchIc = g.getIcNumber() != null && g.getIcNumber().toLowerCase().contains(q);
      boolean matchPassport =
          g.getPassportNumber() != null && g.getPassportNumber().toLowerCase().contains(q);
      boolean matchPhone =
          g.getPhoneNumber() != null && g.getPhoneNumber().toLowerCase().contains(q);
      boolean matchMember = g.getMemberId() != null && g.getMemberId().toLowerCase().contains(q);

      if (matchId || matchName || matchIc || matchPassport || matchPhone || matchMember) {
        matches.add(g);
      }
    }

    return matches;
  }

  public ListInterface<Guest> getGuestList() {
    return guestList;
  }

  public void resetAllGuestStrikes() {
    if (guestList == null || guestList.isEmpty()) return;

    boolean needSave = false;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && g.getStrikeCount() > 0) {
        g.setStrikeCount(0);
        if (!needSave) {
          needSave = true;
        }
      }
    }

    if (needSave) {
      save();
    }
  }
}
