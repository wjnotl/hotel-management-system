package entity;

import java.io.Serializable;

public class Room implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum Status {
    DIRTY,
    CLEANING,
    INSPECTED,
    VACANT_CLEAN,
    OCCUPIED
  }

  public static enum RoomType {
    LUXURY,
    SUITE,
    STANDARD
  }

  private String roomNumber; // Room designation (e.g., "801")
  private RoomType roomType; // Category: LUXURY, SUITE, STANDARD
  private Status status; // Operational cleaning/occupancy state
  private double price; // Room price per night (RM), stored on the room itself

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
}
