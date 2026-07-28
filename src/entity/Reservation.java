package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum Status {
    RESERVED,
    WAITING,
    ALLOCATED,
    CHECKED_IN,
    NO_SHOW,
    CANCELLED
  }

  private String reservationId; // Dedicated Unique ID (e.g., "RES-10001")
  private String guestId; // Links to Guest.guestId
  private String confirmationNumber; // Outer booking reference/trip ID
  private Room.RoomType roomType; // LUXURY, SUITE, STANDARD
  private Status status;
  private boolean isBoiling;
  private int priorityScore;
  private LocalDateTime reservationTime;
  private LocalDateTime queueArrivalTime;
  private LocalDateTime allocatedTime; // For future analytics reporting

  public Reservation(
      String reservationId,
      String guestId,
      String confirmationNumber,
      Room.RoomType roomType,
      Status status,
      boolean isBoiling,
      int priorityScore,
      LocalDateTime reservationTime,
      LocalDateTime queueArrivalTime) {
    this.reservationId = reservationId;
    this.guestId = guestId;
    this.confirmationNumber = confirmationNumber;
    this.roomType = roomType;
    this.status = status;
    this.isBoiling = isBoiling;
    this.priorityScore = priorityScore;
    this.reservationTime = reservationTime;
    this.queueArrivalTime = queueArrivalTime;
  }

  // --- GETTERS ---
  public String getReservationId() {
    return reservationId;
  }

  public String getGuestId() {
    return guestId;
  }

  public String getConfirmationNumber() {
    return confirmationNumber;
  }

  public Room.RoomType getRoomType() {
    return roomType;
  }

  public Status getStatus() {
    return status;
  }

  public boolean getIsBoiling() {
    return isBoiling;
  }

  public int getPriorityScore() {
    return priorityScore;
  }

  public LocalDateTime getReservationTime() {
    return reservationTime;
  }

  public LocalDateTime getQueueArrivalTime() {
    return queueArrivalTime;
  }

  public LocalDateTime getAllocatedTime() {
    return allocatedTime;
  }

  // --- SETTERS ---
  public void setReservationId(String reservationId) {
    this.reservationId = reservationId;
  }

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public void setConfirmationNumber(String confirmationNumber) {
    this.confirmationNumber = confirmationNumber;
  }

  public void setRoomType(Room.RoomType roomType) {
    this.roomType = roomType;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setBoiling(boolean isBoiling) {
    this.isBoiling = isBoiling;
  }

  public void setPriorityScore(int priorityScore) {
    this.priorityScore = priorityScore;
  }

  public void setReservationTime(LocalDateTime reservationTime) {
    this.reservationTime = reservationTime;
  }

  public void setQueueArrivalTime(LocalDateTime queueArrivalTime) {
    this.queueArrivalTime = queueArrivalTime;
  }

  public void setAllocatedTime(LocalDateTime allocatedTime) {
    this.allocatedTime = allocatedTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Reservation other = (Reservation) obj;
    return reservationId != null && reservationId.equalsIgnoreCase(other.reservationId);
  }

  @Override
  public int hashCode() {
    return reservationId != null ? reservationId.toLowerCase().hashCode() : 0;
  }
}
