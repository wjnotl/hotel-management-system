package entity;

public class AllocationEntry {
  private String reservationConfirmationNumber; // References Reservation.confirmationNumber
  private String assignedRoomNumber; // References Room.roomNumber
  private long expirationTimestamp; // Millisecond target timestamp
}
