package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Room implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum Status {
    DIRTY,
    VACANT_CLEAN,
  }

  public static enum RoomType {
    LUXURY,
    SUITE,
    STANDARD
  }

  private boolean isOccupied;

  private String roomNumber; // Room designation (e.g., "801")
  private RoomType roomType; // Category: LUXURY, SUITE, STANDARD
  private Status status; // Operational cleaning/occupancy state
  private double price; // Room price per night (RM), stored on the room itself
  private LocalDateTime dirtyTime;

  public Room(String roomNumber, RoomType roomType, Status status, double price) {
    this.roomNumber = roomNumber;
    this.roomType = roomType;
    this.status = status;
    this.price = price;
  }

  public double getPrice() {
    return price;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public Status getStatus() {
    return status;
  }

  public boolean getIsOccupied() {
    return isOccupied;
  }

  public LocalDateTime getDirtyTime() {
    return dirtyTime;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public void setRoomType(RoomType roomType) {
    this.roomType = roomType;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public void setIsOccupied(boolean isOccupied) {
    this.isOccupied = isOccupied;
  }

  public void setDirtyTime(LocalDateTime dirtyTime) {
    this.dirtyTime = dirtyTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Room other = (Room) obj;
    return roomNumber != null && roomNumber.equals(other.roomNumber);
  }

  @Override
  public int hashCode() {
    return roomNumber != null ? roomNumber.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Room{"
        + "roomNumber='"
        + roomNumber
        + "'"
        + ", roomType="
        + roomType
        + ", status="
        + status
        + ", price="
        + price
        + ", isOccupied="
        + isOccupied
        + ", dirtyTime="
        + dirtyTime
        + "}";
  }
}
