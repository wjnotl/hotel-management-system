package repo;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
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
    DoublyLinkedHashMap<String, HousekeepingStaff> lruMap = new DoublyLinkedHashMap<>();
    lruMap.setLruEnabled(true);
    this.staffById = lruMap;
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

  public boolean setShift(HousekeepingStaff staff, HousekeepingStaff.Shift shift) {
    if (staff == null) return false;

    staff.setShift(shift);
    save();
    return true;
  }

  // Called when a task is assigned to this staff member: adds the room to their assigned list
  // (if not already there) and flips them to ON_TASK so the roster reflects they're occupied.
  public boolean assignRoomToStaff(HousekeepingStaff staff, String roomNumber) {
    if (staff == null || roomNumber == null) return false;

    if (!staff.getAssignedRoomNumbers().contains(roomNumber)) {
      staff.getAssignedRoomNumbers().add(roomNumber);
    }
    staff.setAvailability(HousekeepingStaff.Availability.ON_TASK);
    save();
    return true;
  }

  // Called when a task assigned to this staff member reaches a terminal status (Completed/
  // Skipped): drops the room from their assigned list. Only flips them back to AVAILABLE if
  // they have no other rooms left — an OFF_DUTY staff member (e.g. sent home mid-shift) stays
  // OFF_DUTY rather than being silently reset to AVAILABLE.
  public boolean releaseRoomFromStaff(HousekeepingStaff staff, String roomNumber) {
    if (staff == null || roomNumber == null) return false;

    staff.getAssignedRoomNumbers().remove(roomNumber);
    if (staff.getAssignedRoomNumbers().isEmpty()
        && staff.getAvailability() == HousekeepingStaff.Availability.ON_TASK) {
      staff.setAvailability(HousekeepingStaff.Availability.AVAILABLE);
    }
    save();
    return true;
  }

  public String generateStaffId() {
    int maxId = 1000;
    if (staffList != null) {
      for (int i = 1; i <= staffList.getNumberOfEntries(); i++) {
        HousekeepingStaff s = staffList.getEntry(i);
        if (s != null && s.getStaffId() != null && s.getStaffId().startsWith("HK-")) {
          try {
            int num = Integer.parseInt(s.getStaffId().substring(3));
            if (num > maxId) {
              maxId = num;
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
    return "HK-" + (maxId + 1);
  }

  public ListInterface<HousekeepingStaff> getStaffList() {
    return staffList;
  }
}
