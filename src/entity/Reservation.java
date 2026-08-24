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
  private LocalDateTime
      queueArrivalTime; // Time when the reservation was placed in the waiting queue
  private LocalDateTime allocatedTime; // Time when the reservation was allocated to a room
  private Integer allocatedGraceMins; // Snapshot of grace limit set during allocation
  private LocalDateTime checkInTime; // Time when the guest actually took the room
  private LocalDateTime checkOutTime; // Time when the reservation was checked out
  private boolean isVip; // Check if the guest is a member
  private Integer strikeCountSnapshot; // Snapshot of guest strike count when penalty event occurred

  // When an advance booking says the guest will turn up. A walk-in leaves this null because it is
  // already standing at the counter. Together with stayDays it is what lets the module answer
  // "is a room of this type still free on that date", instead of only "is one free right now".
  private LocalDateTime expectedArrivalTime;

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

  public LocalDateTime getCheckInTime() {
    return checkInTime;
  }

  public LocalDateTime getCheckOutTime() {
    return checkOutTime;
  }

  public LocalDateTime getExpectedArrivalTime() {
    return expectedArrivalTime;
  }

  public boolean getIsVip() {
    return isVip;
  }

  public Integer getStrikeCountSnapshot() {
    return strikeCountSnapshot;
  }

  // The first night this booking occupies a room. An advance booking is pinned to the date it was
  // promised; anything already in a room is pinned to the day it took the key; a hold that has
  // been called but not yet checked in is occupying a room today. Null means the record consumes
  // no room at all, which is true of a guest still standing in the line.
  public java.time.LocalDate getOccupancyStartDate() {
    if (status == Status.RESERVED) {
      return (expectedArrivalTime != null) ? expectedArrivalTime.toLocalDate() : null;
    }
    if (status == Status.CHECKED_IN) {
      if (checkInTime != null) return checkInTime.toLocalDate();
      if (allocatedTime != null) return allocatedTime.toLocalDate();
      return (reservationTime != null) ? reservationTime.toLocalDate() : null;
    }
    if (status == Status.ALLOCATED) {
      return (allocatedTime != null) ? allocatedTime.toLocalDate() : null;
    }
    return null;
  }

  // The first night the room is free again. Checkout day is a turnover day: the guest leaves in
  // the morning and the next arrival takes the same room that afternoon, so the interval is half
  // open and a departure on the 5th does not block an arrival on the 5th.
  public java.time.LocalDate getOccupancyEndDate() {
    java.time.LocalDate start = getOccupancyStartDate();
    if (start == null) return null;

    if (status == Status.CHECKED_OUT || status == Status.NO_SHOW || status == Status.CANCELLED) {
      return start;
    }

    // A hold is not a stay yet, so it books out today only until the guest checks in and states
    // how many nights they are staying.
    if (status == Status.ALLOCATED) {
      return start.plusDays(1);
    }

    int nights = (stayDays != null && stayDays > 0) ? stayDays : 1;
    return start.plusDays(nights);
  }

  // True when this booking has a room to itself for at least part of [from, to).
  public boolean occupiesRoomBetween(java.time.LocalDate from, java.time.LocalDate to) {
    if (from == null || to == null || !from.isBefore(to)) return false;

    java.time.LocalDate start = getOccupancyStartDate();
    java.time.LocalDate end = getOccupancyEndDate();
    if (start == null || end == null || !start.isBefore(end)) return false;

    return start.isBefore(to) && from.isBefore(end);
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

  public void setCheckInTime(LocalDateTime checkInTime) {
    this.checkInTime = checkInTime;
  }

  public void setCheckOutTime(LocalDateTime checkOutTime) {
    this.checkOutTime = checkOutTime;
  }

  public void setExpectedArrivalTime(LocalDateTime expectedArrivalTime) {
    this.expectedArrivalTime = expectedArrivalTime;
  }

  public void setIsVip(boolean isVip) {
    this.isVip = isVip;
  }

  public void setStrikeCountSnapshot(Integer strikeCountSnapshot) {
    this.strikeCountSnapshot = strikeCountSnapshot;
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

  @Override
  public int compareTo(Reservation other) {
    if (other == null) return 1;

    int priorityCompare = Integer.compare(other.priorityScore, this.priorityScore);
    if (priorityCompare != 0) return priorityCompare;

    // tie breaker
    if (this.queueArrivalTime == null && other.queueArrivalTime == null) return 0;
    if (this.queueArrivalTime == null) return -1;
    if (other.queueArrivalTime == null) return 1;

    return this.queueArrivalTime.compareTo(other.queueArrivalTime);
  }

  @Override
  public String toString() {
    return "Reservation{"
        + "reservationId='"
        + reservationId
        + "'"
        + ", guestId='"
        + guestId
        + "'"
        + ", confirmationNumber='"
        + confirmationNumber
        + "'"
        + ", roomNumber='"
        + roomNumber
        + "'"
        + ", roomType="
        + roomType
        + ", status="
        + status
        + ", isVip="
        + isVip
        + ", isBoiling="
        + isBoiling
        + ", priorityScore="
        + priorityScore
        + ", strikeCountSnapshot="
        + strikeCountSnapshot
        + "}";
  }
}
