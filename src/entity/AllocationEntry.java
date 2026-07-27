package entity;

import java.io.Serializable;

public class AllocationEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  private String reservationConfirmationNumber; // References Reservation.confirmationNumber
  private String assignedRoomNumber; // References Room.roomNumber
  private long expirationTimestamp; // Millisecond target timestamp

  public AllocationEntry(
      String reservationConfirmationNumber, String assignedRoomNumber, long expirationTimestamp) {
    this.reservationConfirmationNumber = reservationConfirmationNumber;
    this.assignedRoomNumber = assignedRoomNumber;
    this.expirationTimestamp = expirationTimestamp;
  }

  public String getReservationConfirmationNumber() {
    return reservationConfirmationNumber;
  }

  public String getAssignedRoomNumber() {
    return assignedRoomNumber;
  }

  public long getExpirationTimestamp() {
    return expirationTimestamp;
  }

  public void setReservationConfirmationNumber(String reservationConfirmationNumber) {
    this.reservationConfirmationNumber = reservationConfirmationNumber;
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
    boolean sameConf =
        reservationConfirmationNumber != null
            && reservationConfirmationNumber.equals(other.reservationConfirmationNumber);
    boolean sameRoom =
        assignedRoomNumber != null && assignedRoomNumber.equals(other.assignedRoomNumber);
    return sameConf && sameRoom;
  }

  @Override
  public int hashCode() {
    int result =
        reservationConfirmationNumber != null ? reservationConfirmationNumber.hashCode() : 0;
    result = 31 * result + (assignedRoomNumber != null ? assignedRoomNumber.hashCode() : 0);
    return result;
  }
}
