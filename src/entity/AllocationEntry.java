package entity;

import java.io.Serializable;

public class AllocationEntry implements Serializable {
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
}
