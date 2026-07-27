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
    guestList.add(guest);
    save();
  }

  public Guest findById(String guestId) {
    if (guestId == null) return null;
    for (int i = 1; i <= guestList.getNumberOfEntries(); i++) {
      Guest g = guestList.getEntry(i);
      if (g != null && guestId.equalsIgnoreCase(g.getGuestId())) {
        return g;
      }
    }
    return null;
  }

  public ListInterface<Guest> getGuestList() {
    return guestList;
  }
}
