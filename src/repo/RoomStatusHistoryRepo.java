package repo;

import adt.ArrayList;
import adt.HashMap;
import adt.LinkedStack;
import adt.ListInterface;
import adt.MapInterface;
import adt.StackInterface;
import entity.Room;
import entity.RoomStatusLogEntry;
import java.time.LocalDateTime;
import util.BinaryFileUtil;

public class RoomStatusHistoryRepo {
  private final BinaryFileUtil<ListInterface<RoomStatusLogEntry>> fileUtil;
  private ListInterface<RoomStatusLogEntry> logList;
  private MapInterface<String, StackInterface<Room.Status>> historyByRoom;

  public RoomStatusHistoryRepo() {
    this.fileUtil = new BinaryFileUtil<>("room_status_history.dat");
    load();
  }

  private void load() {
    this.logList = fileUtil.retrieveFromFile();
    if (this.logList == null) {
      this.logList = new ArrayList<>();
    }

    // Rebuild each room's undo stack by replaying its transitions in original order.
    // Note-only entries (fromStatus == null) don't represent a real transition, so they're
    // skipped here — nothing to undo back to.
    this.historyByRoom = new HashMap<>();
    for (int i = 1; i <= logList.getNumberOfEntries(); i++) {
      RoomStatusLogEntry entry = logList.getEntry(i);
      if (entry == null || entry.getFromStatus() == null) continue;

      StackInterface<Room.Status> stack = getOrCreateStack(entry.getRoomNumber());
      stack.push(entry.getFromStatus());
    }
  }

  private void save() {
    fileUtil.saveToFile(logList);
  }

  private StackInterface<Room.Status> getOrCreateStack(String roomNumber) {
    StackInterface<Room.Status> stack = historyByRoom.get(roomNumber);
    if (stack == null) {
      stack = new LinkedStack<>();
      historyByRoom.put(roomNumber, stack);
    }
    return stack;
  }

  // Records a real status transition: pushes the OLD status so it can be undone later.
  public void recordStatusChange(String roomNumber, Room.Status fromStatus, Room.Status toStatus) {
    getOrCreateStack(roomNumber).push(fromStatus);
    logList.add(
        new RoomStatusLogEntry(roomNumber, fromStatus, toStatus, null, LocalDateTime.now()));
    save();
  }

  // Records a note-only event (maintenance flag, inspection request) — no status change,
  // nothing pushed onto the undo stack.
  public void recordNote(String roomNumber, String note) {
    logList.add(new RoomStatusLogEntry(roomNumber, null, null, note, LocalDateTime.now()));
    save();
  }

  // Pops the last real status change for this room, returning what to revert TO (or null if
  // there's nothing left to undo).
  public Room.Status undoLastChange(String roomNumber) {
    StackInterface<Room.Status> stack = historyByRoom.get(roomNumber);
    if (stack == null || stack.isEmpty()) return null;

    Room.Status revertTo = stack.pop();
    logList.add(new RoomStatusLogEntry(roomNumber, null, revertTo, "Undo", LocalDateTime.now()));
    save();
    return revertTo;
  }

  // Returns up to maxCount most-recent log entries for this room, most recent first.
  public ListInterface<RoomStatusLogEntry> getRecentHistory(String roomNumber, int maxCount) {
    ListInterface<RoomStatusLogEntry> result = new ArrayList<>();
    if (roomNumber == null) return result;

    for (int i = logList.getNumberOfEntries();
        i >= 1 && result.getNumberOfEntries() < maxCount;
        i--) {
      RoomStatusLogEntry entry = logList.getEntry(i);
      if (entry != null && roomNumber.equalsIgnoreCase(entry.getRoomNumber())) {
        result.add(entry);
      }
    }
    return result;
  }
}
