package repo;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.ListInterface;
import adt.MapInterface;
import entity.Guest;
import java.time.LocalDate;
import util.BinaryFileUtil;

public class GuestRepo {
  private final BinaryFileUtil<ListInterface<Guest>> fileUtil;
  private ListInterface<Guest> guestList;

  private final MapInterface<String, Guest> guestLruCache =
      new DoublyLinkedHashMap<>(16, 0.75, 50, true);

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
    if (guest.getGuestId() != null) {
      guestLruCache.put(guest.getGuestId().toLowerCase(), guest);
    }
    save();
  }

  public boolean updateGuest(Guest updatedGuest) {
    if (updatedGuest == null || guestList == null) return false;

    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest existing = guestList.getEntry(i);
      if (existing != null && existing.equals(updatedGuest)) {
        guestList.replace(i, updatedGuest);
        if (updatedGuest.getGuestId() != null) {
          guestLruCache.put(updatedGuest.getGuestId().toLowerCase(), updatedGuest);
        }
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

    // O(1) Fast LRU Cache Hit
    Guest cached = guestLruCache.get(guestId.toLowerCase());
    if (cached != null) {
      return cached;
    }

    // Cache Miss: Scan list & populate LRU cache
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) {
        guestLruCache.put(guestId.toLowerCase(), g);
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

  // The field names the desk screens offer. Searching one field at a time is what stops a partial
  // phone number from also dragging in every guest whose IC happens to contain the same digits,
  // which is the collision that made the old combined search unreadable.
  public static final String FIELD_ALL = "ALL FIELDS";
  public static final String FIELD_GUEST_ID = "GUEST ID";
  public static final String FIELD_NAME = "NAME";
  public static final String FIELD_IC = "IC NUMBER";
  public static final String FIELD_PASSPORT = "PASSPORT NO";
  public static final String FIELD_PHONE = "PHONE NUMBER";
  public static final String FIELD_EMAIL = "EMAIL ADDRESS";

  public ListInterface<Guest> searchGuests(String query) {
    return searchGuests(query, FIELD_ALL, false);
  }

  public ListInterface<Guest> searchGuests(String query, String field, boolean exactMatch) {
    ListInterface<Guest> matches = new ArrayList<>();
    if (query == null || query.trim().isEmpty() || guestList == null) {
      return matches;
    }

    String q = query.trim().toLowerCase();
    String target = (field == null) ? FIELD_ALL : field;

    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g == null) continue;

      if (matchesField(g, target, q, exactMatch)) {
        matches.add(g);
      }
    }

    return matches;
  }

  private boolean matchesField(Guest g, String field, String query, boolean exactMatch) {
    if (FIELD_GUEST_ID.equals(field)) return hit(g.getGuestId(), query, exactMatch);
    if (FIELD_NAME.equals(field)) return hit(g.getName(), query, exactMatch);
    if (FIELD_IC.equals(field)) return hit(g.getIcNumber(), query, exactMatch);
    if (FIELD_PASSPORT.equals(field)) return hit(g.getPassportNumber(), query, exactMatch);
    if (FIELD_PHONE.equals(field)) return hit(g.getPhoneNumber(), query, exactMatch);
    if (FIELD_EMAIL.equals(field)) return hit(g.getEmail(), query, exactMatch);

    return hit(g.getGuestId(), query, exactMatch)
        || hit(g.getName(), query, exactMatch)
        || hit(g.getIcNumber(), query, exactMatch)
        || hit(g.getPassportNumber(), query, exactMatch)
        || hit(g.getPhoneNumber(), query, exactMatch)
        || hit(g.getEmail(), query, exactMatch)
        || hit(g.getMemberId(), query, exactMatch);
  }

  private boolean hit(String value, String query, boolean exactMatch) {
    if (value == null) return false;
    String candidate = value.toLowerCase();
    return exactMatch ? candidate.equals(query) : candidate.contains(query);
  }

  public ListInterface<Guest> getGuestList() {
    return guestList;
  }

  public void resetAllGuestStrikes(VipSystemConfigRepo configRepo) {
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

    if (configRepo != null) {
      entity.VipSystemConfig config = configRepo.getConfig();
      if (config != null) {
        config.setLastStrikeResetDate(LocalDate.now().toString());
        configRepo.updateConfig(config);
      }
    }
  }
}
