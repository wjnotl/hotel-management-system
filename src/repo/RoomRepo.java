package repo;

import adt.ArrayList;
import adt.ListInterface;
import entity.Room;
import util.BinaryFileUtil;

public class RoomRepo {
  private final BinaryFileUtil<ListInterface<Room>> fileUtil;
  private ListInterface<Room> roomList;
  
  public RoomRepo() {
    this.fileUtil = new BinaryFileUtil<>("rooms.dat");
    load();
  }

  private void load() {
    this.roomList = fileUtil.retrieveFromFile();
    if (this.roomList == null) {
      this.roomList = new ArrayList<>();
    }
  }

  private void save() {
    fileUtil.saveToFile(roomList);
  }

  public void addRoom(Room room) {
    if (room == null) return;
    roomList.add(room);
    save();
  }

  public boolean updateRoom(Room updatedRoom) {
    if (updatedRoom == null || roomList == null) return false;

    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room existing = roomList.getEntry(i);
      if (existing != null && existing.equals(updatedRoom)) {
        roomList.replace(i, updatedRoom);
        save();
        return true;
      }
    }
    return false;
  }

  public Room findByRoomNumber(String roomNumber) {
    if (roomNumber == null || roomList == null) return null;
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room r = roomList.getEntry(i);
      if (r != null && roomNumber.equalsIgnoreCase(r.getRoomNumber())) {
        return r;
      }
    }
    return null;
  }

  public Room findVacantCleanRoom(Room.RoomType roomType) {
    if (roomType == null || roomList == null) return null;
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room r = roomList.getEntry(i);
      if (r != null && r.getRoomType() == roomType && r.getStatus() == Room.Status.VACANT_CLEAN) {
        return r;
      }
    }
    return null;
  }

  public ListInterface<Room> getRoomList() {
    return roomList;
  }
}
