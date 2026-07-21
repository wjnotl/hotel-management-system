package entity;

public class Room {
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
}
