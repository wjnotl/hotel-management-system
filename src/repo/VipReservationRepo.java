package repo;

import adt.ArrayList;
import adt.BinaryHeapPriorityQueue;
import adt.ListInterface;
import adt.PriorityQueueInterface;
import entity.Reservation;
import util.BinaryFileUtil;

public class VipReservationRepo {
  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> vipReservationList;
  private PriorityQueueInterface<Reservation> vipReservationQueue;

  public VipReservationRepo() {
    this.fileUtil = new BinaryFileUtil<>("vip_reservations.dat");
    load();
  }

  private void load() {
    // 1. Read persistent list from disk
    this.vipReservationList = fileUtil.retrieveFromFile();
    if (this.vipReservationList == null) {
      this.vipReservationList = new ArrayList<>();
    }

    // 2. Build in-memory priority queue from loaded list
    this.vipReservationQueue = new BinaryHeapPriorityQueue<>();
    for (int i = 1; i <= vipReservationList.getNumberOfEntries(); i++) {
      Reservation r = vipReservationList.getEntry(i);
      vipReservationQueue.enqueue(r, r.getPriorityScore());
    }
  }

  private void save() {
    fileUtil.saveToFile(vipReservationList);
  }

  public void addReservation(Reservation reservation, int priority) {
    vipReservationList.add(reservation);
    vipReservationQueue.enqueue(reservation, priority);
    save();
  }

  public Reservation dequeueNextVip() {
    Reservation next = vipReservationQueue.dequeue();
    if (next != null) {
      vipReservationList.remove(next);
      save();
    }
    return next;
  }

  public Reservation peekNextVip() {
    return vipReservationQueue.peek();
  }

  public boolean cancelReservation(Reservation reservation) {
    boolean removedList = vipReservationList.remove(reservation);
    boolean removedQueue = vipReservationQueue.remove(reservation);

    if (removedList || removedQueue) {
      save();
      return true;
    }
    return false;
  }

  public boolean updatePriority(Reservation reservation, int newPriority) {
    boolean updatedQueue = vipReservationQueue.changePriority(reservation, newPriority);
    if (updatedQueue) {
      reservation.setPriorityScore(newPriority);
      save();
      return true;
    }
    return false;
  }

  public ListInterface<Reservation> getReservationList() {
    return vipReservationList;
  }

  public int getVipCount() {
    return vipReservationQueue.getNumberOfEntries();
  }
}
