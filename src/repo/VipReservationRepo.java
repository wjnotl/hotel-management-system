package repo;

import adt.ArrayList;
import adt.BinaryHeapPriorityQueue;
import adt.ListInterface;
import adt.PriorityQueueInterface;
import entity.Reservation;
import util.BinaryFileUtil;

public class VipReservationRepo {
  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> reservationList;
  private PriorityQueueInterface<Reservation> heap;

  public VipReservationRepo() {
    this.fileUtil = new BinaryFileUtil<>("vip_reservations.dat");
    load();
  }

  private void load() {
    this.reservationList = fileUtil.retrieveFromFile();
    if (this.reservationList == null) {
      this.reservationList = new ArrayList<>(25, true);
    }

    // Rebuild priority queue from stored list
    this.heap = new BinaryHeapPriorityQueue<>(true); // Max Heap
    for (int i = 1; i <= reservationList.getNumberOfEntries(); i++) {
      Reservation r = reservationList.getEntry(i);
      if (r != null) {
        heap.enqueue(r, r.getPriorityScore());
      }
    }
  }

  private void save() {
    fileUtil.saveToFile(reservationList);
  }

  public void addReservation(Reservation reservation, int priority) {
    reservationList.add(reservation);
    heap.enqueue(reservation, priority);
    save();
  }

  public Reservation dequeueNextVip() {
    Reservation top = heap.dequeue();

    if (top != null) {
      // 1. Remove from backing reservation list
      boolean removed = reservationList.remove(top);

      // 2. Fallback: If equals() failed, remove manually by matching confirmation number
      if (!removed) {
        for (int i = 1; i <= reservationList.getNumberOfEntries(); i++) {
          Reservation r = reservationList.getEntry(i);
          if (r != null
              && top.getConfirmationNumber().equalsIgnoreCase(r.getConfirmationNumber())) {
            reservationList.removeAt(i);
            break;
          }
        }
      }

      // 3. Save updated list back to dat file
      save();
    }

    return top;
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null) return false;

    boolean heapRemoved = heap.remove(reservation);
    boolean listRemoved = reservationList.remove(reservation);

    if (heapRemoved || listRemoved) {
      save();
      return true;
    }
    return false;
  }

  public ListInterface<Reservation> getReservationList() {
    return reservationList;
  }
}
