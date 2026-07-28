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

    // Initialize 3 separate lists
    this.luxuryList = new ArrayList<>();
    this.suiteList = new ArrayList<>();
    this.standardList = new ArrayList<>();

    // Initialize 3 separate max heaps
    this.luxuryHeap = new BinaryHeapPriorityQueue<>(true);
    this.suiteHeap = new BinaryHeapPriorityQueue<>(true);
    this.standardHeap = new BinaryHeapPriorityQueue<>(true);

    // Populate dedicated lists and heaps from master storage
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && r.getRoomType() != null) {
        getListByRoomType(r.getRoomType()).add(r);
        getHeapByRoomType(r.getRoomType()).enqueue(r, r.getPriorityScore());
      }
    }
  }

  // --- ROOM QUEUE HELPER RESOLVERS ---

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

  // --- CRUD OPERATIONS ---

  public void addReservation(Reservation reservation, int priority) {
    if (reservation == null || reservation.getRoomType() == null) return;

    masterList.add(reservation);
    getListByRoomType(reservation.getRoomType()).add(reservation);
    getHeapByRoomType(reservation.getRoomType()).enqueue(reservation, priority);

    save();
  }

  public Reservation dequeueNextVip(Room.RoomType roomType) {
    PriorityQueueInterface<Reservation> heap = getHeapByRoomType(roomType);
    ListInterface<Reservation> list = getListByRoomType(roomType);

    Reservation top = heap.dequeue();

    if (top != null) {
      masterList.remove(top);
      list.remove(top);
      save();
    }
    return top;
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    Room.RoomType type = reservation.getRoomType();
    boolean heapRemoved = getHeapByRoomType(type).remove(reservation);
    boolean listRemoved = getListByRoomType(type).remove(reservation);
    boolean masterRemoved = masterList.remove(reservation);

    if (heapRemoved || listRemoved || masterRemoved) {
      save();
      return true;
    }
    return false;
  }

  public ListInterface<Reservation> getAllReservations() {
    return masterList;
  }
}
