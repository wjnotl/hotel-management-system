package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RoomStatusLogEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  private String roomNumber;
  private Room.Status fromStatus; // null for a note-only entry (maintenance flag, inspection, etc.)
  private Room.Status toStatus; // null for a note-only entry
  private String note; // e.g. "Flagged for maintenance", "Inspection requested", "Undo"
  private LocalDateTime changedAt;

  public RoomStatusLogEntry(
      String roomNumber,
      Room.Status fromStatus,
      Room.Status toStatus,
      String note,
      LocalDateTime changedAt) {
    this.roomNumber = roomNumber;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.note = note;
    this.changedAt = changedAt;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public Room.Status getFromStatus() {
    return fromStatus;
  }

  public Room.Status getToStatus() {
    return toStatus;
  }

  public String getNote() {
    return note;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }
}
