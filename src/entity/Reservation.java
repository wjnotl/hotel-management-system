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

  private String confirmationNumber; // Unique 8-digit trip ID (Primary lookup key)
  private String guestId; // Links to Guest.guestId
  private Status status; // Enforce typed status state
  private int strikeCount; // Daily no-show counter
  private boolean isBoiling; // Patience threshold flag
  private double priorityScore; // Calculated Max-Heap score
}
