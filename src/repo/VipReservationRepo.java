package repo;

import adt.BinaryHeapPriorityQueue;
import adt.ListInterface;
import entity.Reservation;
import util.BinaryFileUtil;

public class VipReservationRepo {
  private BinaryFileUtil<ListInterface<Reservation>> binaryFileUtil;
  private ListInterface<Reservation> vipReservationList;
  private BinaryHeapPriorityQueue<Reservation> vipReservationQueue;

  public VipReservationRepo() {
    binaryFileUtil = new BinaryFileUtil<>("vip_reservations.dat");
    load();
  }

  private void load() {
    this.vipReservationList = binaryFileUtil.retrieveFromFile();
  }

  private void save() {
    this.vipReservationList = binaryFileUtil.retrieveFromFile();
  }

  public void addReservation(Reservation reservation, int priority) {
    vipReservationQueue.enqueue(reservation, priority);
    vipReservationList.add(reservation);
    save();
  }

  public void removeReservation(Reservation reservation) {
    vipReservationList.remove(0);
    vipReservationQueue.remove(reservation);
    save();
  }

  public void removeLatestReservation() {
    vipReservationQueue.dequeue();
  }

  public void updateReservation(Reservation reservation) {
    vipReservationList.replace(reservation);
    save();
  }

  public ListInterface<Reservation> getReservationList() {
    return vipReservationList;
  }

  public QueueInterface<Reservation> getReservationQueue() {
    return vipReservationQueue;
  }
}
