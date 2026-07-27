package entity;

import java.time.LocalDateTime;

public class Reservation {
  public static enum Status {
    RESERVED,
    WAITING,
    ALLOCATED,
    CHECKED_IN,
    NO_SHOW,
    CANCELLED
  };

  private String guestId; // Links to Guest.guestId
  private String confirmationNumber; // Unique 8-digit trip ID (Primary lookup key)
  private Status status; // Enforce typed status state
  private boolean isBoiling; // Patience threshold flag
  private int priorityScore; // Calculated Max-Heap score
  private LocalDateTime reservationTime; // When the booking was made
  private LocalDateTime queueArrivalTime; // Exact timestamp they arrived in the lobby queue

  public Reservation(
      String guestId,
      String confirmationNumber,
      Status status,
      boolean isBoiling,
      int priorityScore,
      LocalDateTime reservationTime,
      LocalDateTime queueArrivalTime) {
    this.guestId = guestId;
    this.confirmationNumber = confirmationNumber;
    this.status = status;
    this.isBoiling = isBoiling;
    this.priorityScore = priorityScore;
    this.reservationTime = reservationTime;
    this.queueArrivalTime = queueArrivalTime;
  }

  public String getGuestId() {
    return guestId;
  }

  public String getConfirmationNumber() {
    return confirmationNumber;
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

  public void setGuestId(String guestId) {
    this.guestId = guestId;
  }

  public void setConfirmationNumber(String confirmationNumber) {
    this.confirmationNumber = confirmationNumber;
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
}
