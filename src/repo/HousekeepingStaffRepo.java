package repo;

import adt.ArrayList;
import adt.HashMap;
import adt.ListInterface;
import adt.MapInterface;
import entity.HousekeepingStaff;
import util.BinaryFileUtil;

public class HousekeepingStaffRepo {
  private final BinaryFileUtil<ListInterface<HousekeepingStaff>> fileUtil;
  private ListInterface<HousekeepingStaff> staffList;
  private MapInterface<String, HousekeepingStaff> staffById;

  public HousekeepingStaffRepo() {
    this.fileUtil = new BinaryFileUtil<>("housekeeping_staff.dat");
    load();
  }

  private void load() {
    this.staffList = fileUtil.retrieveFromFile();
    if (this.staffList == null) {
      this.staffList = new ArrayList<>();
    }

    // Rebuild the lookup map from the stored list.
    this.staffById = new HashMap<>();
    for (int i = 1; i <= staffList.getNumberOfEntries(); i++) {
      HousekeepingStaff s = staffList.getEntry(i);
      if (s != null) {
        staffById.put(s.getStaffId(), s);
      }
    }
  }

  private void save() {
    fileUtil.saveToFile(staffList);
  }

  public void addStaff(HousekeepingStaff staff) {
    staffList.add(staff);
    staffById.put(staff.getStaffId(), staff);
    save();
  }

  public HousekeepingStaff findById(String staffId) {
    if (staffId == null) return null;
    return staffById.get(staffId);
  }

  public boolean setAvailability(
      HousekeepingStaff staff, HousekeepingStaff.Availability availability) {
    if (staff == null) return false;

    staff.setAvailability(availability);
    save();
    return true;
  }

  public ListInterface<HousekeepingStaff> getStaffList() {
    return staffList;
  }
}
