package entity;

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

  private static final int DEFAULT_ROOM_CAPACITY = 4;

  private String staffId;
  private String name;
  private Shift shift;
  private Availability availability;
  private String[] assignedRoomNumbers;
  private int roomCount;

  public HousekeepingStaff(String staffId, String name, Shift shift, Availability availability) {
    this.staffId = staffId;
    this.name = name;
    this.shift = shift;
    this.availability = availability;
    this.assignedRoomNumbers = new String[DEFAULT_ROOM_CAPACITY];
    this.roomCount = 0;
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

  // Adds a room to this staff member's assignment list (if it isn't already present),
  // growing the backing array as needed. Returns false if the room was already assigned.
  public boolean addRoom(String roomNumber) {
    if (roomNumber == null || containsRoom(roomNumber)) return false;

    if (roomCount == assignedRoomNumbers.length) {
      // Create a new array double the size
      String[] expandedArray = new String[assignedRoomNumbers.length * 2];

      for (int i = 0; i < assignedRoomNumbers.length; i++) {
        expandedArray[i] = assignedRoomNumbers[i];
      }

      assignedRoomNumbers = expandedArray;
    }
    assignedRoomNumbers[roomCount++] = roomNumber;
    return true;
  }

  // Removes a room from this staff member's assignment list, shifting later entries down
  // to close the gap. Returns false if the room wasn't assigned to begin with.
  public boolean removeRoom(String roomNumber) {
    if (roomNumber == null) return false;

    for (int i = 0; i < roomCount; i++) {
      if (assignedRoomNumbers[i].equals(roomNumber)) {
        for (int j = i; j < roomCount - 1; j++) {
          assignedRoomNumbers[j] = assignedRoomNumbers[j + 1];
        }
        assignedRoomNumbers[--roomCount] = null;
        return true;
      }
    }
    return false;
  }

  // Checks whether a given room is currently assigned to this staff member.
  public boolean containsRoom(String roomNumber) {
    if (roomNumber == null) return false;

    for (int i = 0; i < roomCount; i++) {
      if (assignedRoomNumbers[i].equals(roomNumber)) return true;
    }
    return false;
  }

  // True if this staff member currently has no rooms assigned.
  public boolean hasNoRooms() {
    return roomCount == 0;
  }

  // Current number of rooms assigned to this staff member.
  public int getRoomCount() {
    return roomCount;
  }

  // Returns a right-sized copy of the assigned rooms (no trailing nulls / unused capacity).
  public String[] getAssignedRoomNumbersArray() {
    String[] currentRooms = new String[roomCount];

    for (int i = 0; i < roomCount; i++) {
      currentRooms[i] = assignedRoomNumbers[i];
    }

    return currentRooms;
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

  @Override
  public String toString() {
    return "HousekeepingStaff{"
        + "staffId='"
        + staffId
        + "'"
        + ", name='"
        + name
        + "'"
        + ", shift="
        + shift
        + ", availability="
        + availability
        + "}";
  }
}
