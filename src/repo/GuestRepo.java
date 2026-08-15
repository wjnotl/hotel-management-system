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

  public Guest findByName(String name) {
    if (name == null || guestList == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && name.equalsIgnoreCase(g.getName())) {
        return g;
      }
    }
    return null;
  }

  // One scan covers both documents because a guest carries whichever one they presented, and
  // the desk must not be able to open a second file for a person already on record.
  public Guest findByIdentityDocument(String document) {
    if (document == null || guestList == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g == null) continue;
      if (document.equalsIgnoreCase(g.getIcNumber())
          || document.equalsIgnoreCase(g.getPassportNumber())) {
        return g;
      }
    }
    return null;
  }

  public Guest findByPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || guestList == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && phoneNumber.equalsIgnoreCase(g.getPhoneNumber())) {
        return g;
      }
    }
    return null;
  }

  // Ids are minted here because this repository owns the list that decides which suffix is
  // still free. Guest.equals compares the id alone, so a reused number would make findById
  // and updateGuest resolve to whichever record happened to be stored first.
  public String generateGuestId() {
    int maxId = 100;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && g.getGuestId() != null && g.getGuestId().startsWith("G-")) {
        try {
          int num = Integer.parseInt(g.getGuestId().substring(2));
          if (num > maxId) {
            maxId = num;
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return "G-" + (maxId + 1);
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
