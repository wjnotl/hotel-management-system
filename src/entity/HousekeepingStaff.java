package entity;

import adt.ArrayList;
import adt.ListInterface;
import java.io.Serializable;

public class HousekeepingStaff implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum Shift {
    MORNING,
    AFTERNOON,
    NIGHT
  }

  public static enum Availability {
    AVAILABLE,
    ON_TASK,
    OFF_DUTY
  }

  private String staffId; // Unique staff ID (e.g., "HK-004")
  private String name;
  private Shift shift;
  private Availability availability;
  private ListInterface<String> assignedRoomNumbers; // Rooms currently assigned to this staff

  public HousekeepingStaff(String staffId, String name, Shift shift, Availability availability) {
    this.staffId = staffId;
    this.name = name;
    this.shift = shift;
    this.availability = availability;
    this.assignedRoomNumbers = new ArrayList<>();
  }

  public String getStaffId() {
    return staffId;
  }

  public String getName() {
    return name;
  }

  public Shift getShift() {
    return shift;
  }

  public Availability getAvailability() {
    return availability;
  }

  public ListInterface<String> getAssignedRoomNumbers() {
    return assignedRoomNumbers;
  }

  public void setStaffId(String staffId) {
    this.staffId = staffId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setShift(Shift shift) {
    this.shift = shift;
  }

  public void setAvailability(Availability availability) {
    this.availability = availability;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    HousekeepingStaff other = (HousekeepingStaff) obj;
    return staffId != null && staffId.equals(other.staffId);
  }

  @Override
  public int hashCode() {
    return staffId != null ? staffId.hashCode() : 0;
  }
}
