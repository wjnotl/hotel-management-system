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
}
