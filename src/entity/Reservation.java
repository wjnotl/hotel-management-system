package entity;

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
  private int strikeCount; // Daily no-show counter
  private boolean isBoiling; // Patience threshold flag
  private double priorityScore; // Calculated Max-Heap score

  public Reservation(
      String guestId,
      String confirmationNumber,
      Status status,
      int strikeCount,
      boolean isBoiling,
      double priorityScore) {
    this.guestId = guestId;
    this.confirmationNumber = confirmationNumber;
    this.status = status;
    this.strikeCount = strikeCount;
    this.isBoiling = isBoiling;
    this.priorityScore = priorityScore;
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

  public int getStrikeCount() {
    return strikeCount;
  }

  public boolean getIsBoiling() {
    return isBoiling;
  }

  public double getPriorityScore() {
    return priorityScore;
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

  public void setStrikeCount(int strikeCount) {
    this.strikeCount = strikeCount;
  }

  public void setBoiling(boolean isBoiling) {
    this.isBoiling = isBoiling;
  }

  public void setPriorityScore(double priorityScore) {
    this.priorityScore = priorityScore;
  }
}
