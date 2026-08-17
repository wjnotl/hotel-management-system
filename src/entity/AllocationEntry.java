package entity;

import java.io.Serializable;

public class AllocationEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  private String reservationId; // References Reservation.reservationId
  private String assignedRoomNumber; // References Room.roomNumber
  private long expirationTimestamp; // Millisecond target timestamp

  public AllocationEntry(
      String reservationId, String assignedRoomNumber, long expirationTimestamp) {
    this.reservationId = reservationId;
    this.assignedRoomNumber = assignedRoomNumber;
    this.expirationTimestamp = expirationTimestamp;
  }

  public String getReservationId() {
    return reservationId;
  }

  public String getAssignedRoomNumber() {
    return assignedRoomNumber;
  }

  public long getExpirationTimestamp() {
    return expirationTimestamp;
  }

  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public void setAssignedRoomNumber(String assignedRoomNumber) {
    this.assignedRoomNumber = assignedRoomNumber;
  }

  public void setExpirationTimestamp(long expirationTimestamp) {
    this.expirationTimestamp = expirationTimestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    AllocationEntry other = (AllocationEntry) obj;
    boolean sameRes = reservationId != null && reservationId.equalsIgnoreCase(other.reservationId);
    boolean sameRoom =
        assignedRoomNumber != null && assignedRoomNumber.equals(other.assignedRoomNumber);
    return sameRes && sameRoom;
  }

  @Override
  public int hashCode() {
    int result = reservationId != null ? reservationId.toLowerCase().hashCode() : 0;
    result = 31 * result + (assignedRoomNumber != null ? assignedRoomNumber.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AllocationEntry{"
        + "reservationId='"
        + reservationId
        + "'"
        + ", assignedRoomNumber='"
        + assignedRoomNumber
        + "'"
        + ", expirationTimestamp="
        + expirationTimestamp
        + "}";
  }
}
