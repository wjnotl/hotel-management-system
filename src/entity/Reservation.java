package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Serializable, Comparable<Reservation> {
  private static final long serialVersionUID = 1L;

  public static enum Status {
    RESERVED,
    WAITING,
    ALLOCATED,
    CHECKED_IN,
    CHECKED_OUT,
    NO_SHOW,
    CANCELLED
  }

  private String reservationId; // Unique internal ID (e.g. "RES-10001")
  private String guestId; // References Guest.guestId
  private String confirmationNumber; // References booking trip ID
  private String roomNumber; // Assigned room designation (e.g., "801")
  private Room.RoomType roomType; // LUXURY, SUITE, STANDARD
  private Status status;
  private boolean isBoiling;
  private int priorityScore; // For vip guests only
  private Integer stayDays;
  private LocalDateTime reservationTime; // Time when the reservation was created
  private LocalDateTime queueArrivalTime; // Time when the reservation was placed in the waiting queue
  private LocalDateTime allocatedTime; // Time when the reservation was allocated to a room
  private Integer allocatedGraceMins; // Snapshot of grace limit set during allocation
  private LocalDateTime checkOutTime; // Time when the reservation was checked out
  private boolean isVip; // Check if the guest is a member

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

  public String getRoomNumber() {
    return roomNumber;
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

  public Integer getStayDays() {
    return stayDays;
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

  public LocalDateTime getCheckOutTime() {
    return checkOutTime;
  }

  public boolean getIsVip() {
    return isVip;
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

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
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

  public void setStayDays(Integer stayDays) {
    this.stayDays = stayDays;
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

  public void setAllocatedGraceMins(Integer allocatedGraceMins) {
    this.allocatedGraceMins = allocatedGraceMins;
  }

  public void setCheckOutTime(LocalDateTime checkOutTime) {
    this.checkOutTime = checkOutTime;
  }

  public void setIsVip(boolean isVip) {
    this.isVip = isVip;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    Reservation other = (Reservation) obj;
    return reservationId != null && reservationId.equalsIgnoreCase(other.reservationId);
  }

  @Override
  public int hashCode() {
    return reservationId != null ? reservationId.toLowerCase().hashCode() : 0;
  }

  @Override
  public int compareTo(Reservation other) {
    if (other == null)
      return 1;

    int priorityCompare = Integer.compare(other.priorityScore, this.priorityScore);
    if (priorityCompare != 0)
      return priorityCompare;

    // tie breaker
    if (this.queueArrivalTime == null && other.queueArrivalTime == null)
      return 0;
    if (this.queueArrivalTime == null)
      return -1;
    if (other.queueArrivalTime == null)
      return 1;

    return this.queueArrivalTime.compareTo(other.queueArrivalTime);
  }

  @Override
  public String toString() {
    return "Reservation{"
        + "reservationId='" + reservationId + "'"
        + ", guestId='" + guestId + "'"
        + ", confirmationNumber='" + confirmationNumber + "'"
        + ", roomNumber='" + roomNumber + "'"
        + ", roomType=" + roomType
        + ", status=" + status
        + ", isVip=" + isVip
        + ", isBoiling=" + isBoiling
        + ", priorityScore=" + priorityScore
        + "}";
  }
}
