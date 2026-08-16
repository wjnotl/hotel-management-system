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

  private String reservationId; // Unique internal ID (e.g. "RES-10001")
  private String guestId; // References Guest.guestId
  private String confirmationNumber; // References booking trip ID
  private Room.RoomType roomType; // LUXURY, SUITE, STANDARD
  private Status status;
  private boolean isBoiling;
  private int priorityScore;
  private Integer stayDays;
  private LocalDateTime reservationTime;
  private LocalDateTime queueArrivalTime;
  private LocalDateTime allocatedTime; // Useful for completed history & analytics
  private Integer allocatedGraceMins; // Snapshot of grace limit set during allocation
  private LocalDateTime checkOutTime;
  private boolean isVip;

  public Reservation(
      String reservationId,
      String guestId,
      String confirmationNumber,
      Room.RoomType roomType,
      Status status,
      boolean isBoiling,
      int priorityScore,
      LocalDateTime reservationTime,
      LocalDateTime queueArrivalTime,
      boolean isVip) {
    this.reservationId = reservationId;
    this.guestId = guestId;
    this.confirmationNumber = confirmationNumber;
    this.roomType = roomType;
    this.status = status;
    this.isBoiling = isBoiling;
    this.priorityScore = priorityScore;
    this.reservationTime = reservationTime;
    this.queueArrivalTime = queueArrivalTime;
    this.isVip = isVip;
  }

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

  public Integer getAllocatedGraceMins() {
    return allocatedGraceMins;
  }

  public void setAllocatedGraceMins(Integer allocatedGraceMins) {
    this.allocatedGraceMins = allocatedGraceMins;
  }

  public Integer getStayDays() {
    return stayDays;
  }

  public LocalDateTime getCheckOutTime() {
    return checkOutTime;
  }

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

  public void setStayDays(Integer stayDays) {
    this.stayDays = stayDays;
  }

  public void setCheckOutTime(LocalDateTime checkOutTime) {
    this.checkOutTime = checkOutTime;
  }

  public boolean getIsVip() {
    return isVip;
  }

  public void setIsVip(boolean isVip) {
    this.isVip = isVip;
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
