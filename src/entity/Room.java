package entity;

import java.io.Serializable;

public class Room implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum Status {
    DIRTY,
    CLEANING,
    INSPECTED,
    VACANT_CLEAN,
    OCCUPIED
  }

  private String roomNumber; // Room designation (e.g., "101")
  private Status status; // Operational cleaning/occupancy state
  private String reservationConfirmationNumber; // Links to Reservation.confirmationNumber

  public Room(String roomNumber, Status status, String reservationConfirmationNumber) {
    this.roomNumber = roomNumber;
    this.status = status;
    this.reservationConfirmationNumber = reservationConfirmationNumber;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public Status getStatus() {
    return status;
  }

  public String getReservationConfirmationNumber() {
    return reservationConfirmationNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setReservationConfirmationNumber(String reservationConfirmationNumber) {
    this.reservationConfirmationNumber = reservationConfirmationNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Room other = (Room) obj;
    return roomNumber != null && roomNumber.equals(other.roomNumber);
  }

  @Override
  public int hashCode() {
    return roomNumber != null ? roomNumber.hashCode() : 0;
  }
}
