package repo;

import adt.ArrayList;
import adt.BinaryHeapPriorityQueue;
import adt.ListInterface;
import adt.PriorityQueueInterface;
import entity.Reservation;
import entity.Room;
import util.BinaryFileUtil;

public class VipReservationRepo {
  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> masterList;

  // 3 Dedicated Lists for O(1) queue separation
  private ListInterface<Reservation> luxuryList;
  private ListInterface<Reservation> suiteList;
  private ListInterface<Reservation> standardList;

  // 3 Dedicated Priority Queues (Max Heaps)
  private PriorityQueueInterface<Reservation> luxuryHeap;
  private PriorityQueueInterface<Reservation> suiteHeap;
  private PriorityQueueInterface<Reservation> standardHeap;

  public VipReservationRepo() {
    this.fileUtil = new BinaryFileUtil<>("vip_reservations.dat");
    load();
  }

  private void load() {
    this.masterList = fileUtil.retrieveFromFile();
    if (this.masterList == null) {
      this.masterList = new ArrayList<>(25, true);
    }

    this.luxuryList = new ArrayList<>();
    this.suiteList = new ArrayList<>();
    this.standardList = new ArrayList<>();

    this.luxuryHeap = new BinaryHeapPriorityQueue<>(true);
    this.suiteHeap = new BinaryHeapPriorityQueue<>(true);
    this.standardHeap = new BinaryHeapPriorityQueue<>(true);

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && r.getRoomType() != null && r.getStatus() == Reservation.Status.WAITING) {
        getListByRoomType(r.getRoomType()).add(r);
        getHeapByRoomType(r.getRoomType()).enqueue(r, r.getPriorityScore());
      }
    }
  }

  public ListInterface<Reservation> getListByRoomType(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryList;
    if (roomType == Room.RoomType.SUITE) return suiteList;
    return standardList;
  }

  public PriorityQueueInterface<Reservation> getHeapByRoomType(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryHeap;
    if (roomType == Room.RoomType.SUITE) return suiteHeap;
    return standardHeap;
  }

  private void save() {
    fileUtil.saveToFile(masterList);
  }

  public void addReservation(Reservation reservation, int priority) {
    if (reservation == null || reservation.getRoomType() == null) return;

    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
    } else {
      updateReservation(reservation);
    }

    if (reservation.getStatus() == Reservation.Status.WAITING) {
      getListByRoomType(reservation.getRoomType()).add(reservation);
      getHeapByRoomType(reservation.getRoomType()).enqueue(reservation, priority);
    }

    save();
  }

  public boolean updateReservation(Reservation updatedRes) {
    if (updatedRes == null || masterList == null) return false;

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation existing = masterList.getEntry(i);
      if (existing != null && existing.equals(updatedRes)) {
        masterList.replace(i, updatedRes);
        save();
        return true;
      }
    }
    return false;
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    Room.RoomType type = reservation.getRoomType();

    // Remove from active queue & heap
    boolean heapRemoved = getHeapByRoomType(type).remove(reservation);
    boolean listRemoved = getListByRoomType(type).remove(reservation);

    // Mark status as CANCELLED in masterList (do NOT delete it from masterList!)
    reservation.setStatus(Reservation.Status.CANCELLED);
    boolean updatedInMaster = updateReservation(reservation);

    return heapRemoved || listRemoved || updatedInMaster;
  }

  public Reservation findById(String reservationId) {
    if (reservationId == null || masterList == null) return null;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && reservationId.equalsIgnoreCase(r.getReservationId())) {
        return r;
      }
    }
    return null;
  }

  public ListInterface<Reservation> getAllReservations() {
    return masterList;
  }
}
