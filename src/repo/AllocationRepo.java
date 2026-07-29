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

  public void load() {
    this.allocationList = fileUtil.retrieveFromFile();
    if (this.allocationList == null) {
      this.allocationList = new ArrayList<>();
    }
  }

  public void save() {
    fileUtil.saveToFile(allocationList);
  }

  public void addAllocationEntry(AllocationEntry entry) {
    if (entry == null) return;
    allocationList.add(entry);
    save();
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

  public ListInterface<AllocationEntry> getAllocationList() {
    load(); // Always fetch fresh disk state
    return allocationList;
  }
}
