package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.AllocationEntry;
import util.BinaryFileUtil;

public class AllocationRepo {
  private final BinaryFileUtil<ListInterface<AllocationEntry>> fileUtil;
  private ListInterface<AllocationEntry> allocationList;

  public AllocationRepo() {
    this.fileUtil = new BinaryFileUtil<>("allocations.dat");
    load();
  }

  private void load() {
    this.allocationList = fileUtil.retrieveFromFile();
    if (this.allocationList == null) {
      this.allocationList = new ArrayList<>();
    }
  }

  public void save() {
    fileUtil.saveToFile(allocationList);
  }

  public void addAllocationEntry(AllocationEntry entry) {
    if (entry != null) {
      allocationList.add(entry);
      save();
    }
  }

  public boolean removeAllocationEntry(AllocationEntry entry) {
    if (entry == null || allocationList == null) return false;

    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null && current.equals(entry)) {
        allocationList.removeAt(i);
        save();
        return true;
      }
    }
    return false;
  }

  public AllocationEntry findByConfirmationNumber(String confNum) {
    if (confNum == null || allocationList == null) return null;
    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry entry = allocationList.getEntry(i);
      if (entry != null && confNum.equalsIgnoreCase(entry.getReservationConfirmationNumber())) {
        return entry;
      }
    }
    return null;
  }

  public ListInterface<AllocationEntry> getAllocationList() {
    // Automatically clean expired entries whenever accessed
    cleanExpiredEntries();
    return allocationList;
  }

  public void cleanExpiredEntries() {
    if (allocationList == null) return;
    long currentMs = System.currentTimeMillis();
    boolean changed = false;

    for (int i = allocationList.getNumberOfEntries(); i >= 1; i--) {
      AllocationEntry entry = allocationList.getEntry(i);
      if (entry != null && currentMs > entry.getExpirationTimestamp()) {
        allocationList.removeAt(i);
        changed = true;
      }
    }

    if (changed) {
      save();
    }
  }
}
