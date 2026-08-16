package repo;

import adt.ArrayList;
import adt.DoublyLinkedHashMap;
import adt.ListInterface;
import adt.MapInterface;
import entity.Reservation;
import util.BinaryFileUtil;

public class ReservationRepo {
  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> reservationList;

  // Bounded LRU Cache (Capacity: 100 active reservation entries)
  private final MapInterface<String, Reservation> reservationLruCache =
      new DoublyLinkedHashMap<>(16, 0.75, 100, true);

  public ReservationRepo() {
    this.fileUtil = new BinaryFileUtil<>("reservations.dat");
    load();
  }

  private void load() {
    this.reservationList = fileUtil.retrieveFromFile();
    if (this.reservationList == null) {
      this.reservationList = new ArrayList<>(25, true);
    }
  }

  public void save() {
    fileUtil.saveToFile(reservationList);
  }

  public ListInterface<Reservation> getAllReservations() {
    return reservationList;
  }

  public boolean updateReservation(Reservation updatedRes) {
    if (updatedRes == null || reservationList == null) return false;

    for (int i = 1; i <= reservationList.getNumberOfEntries(); i++) {
      Reservation existing = reservationList.getEntry(i);
      if (existing != null && existing.equals(updatedRes)) {
        reservationList.replace(i, updatedRes);
        if (updatedRes.getReservationId() != null) {
          reservationLruCache.put(updatedRes.getReservationId().toLowerCase(), updatedRes);
        }
        save();
        return true;
      }
    }
    return false;
  }

  public Reservation findById(String reservationId) {
    if (reservationId == null || reservationList == null) return null;

    // 1. O(1) Fast LRU Cache Hit
    Reservation cached = reservationLruCache.get(reservationId.toLowerCase());
    if (cached != null) {
      return cached;
    }

    // 2. Cache Miss: Scan list & populate LRU cache
    Reservation res =
        reservationList.find(r -> reservationId.equalsIgnoreCase(r.getReservationId()));
    if (res != null) {
      reservationLruCache.put(reservationId.toLowerCase(), res);
    }
    return res;
  }

  public String generateReservationId() {
    int maxId = 10000;
    if (reservationList != null) {
      for (int i = 1; i <= reservationList.getNumberOfEntries(); i++) {
        Reservation r = reservationList.getEntry(i);
        if (r != null && r.getReservationId() != null && r.getReservationId().startsWith("RES-")) {
          try {
            int num = Integer.parseInt(r.getReservationId().substring(4));
            if (num > maxId) {
              maxId = num;
            }
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
    return "RES-" + (maxId + 1);
  }

  public String generateConfirmationNumber() {
    while (true) {
      String code = util.NumberUtil.generateDigitPin(8);
      if (findByConfirmationNumber(code) == null) {
        return code;
      }
    }
  }

  public Reservation findByConfirmationNumber(String confirmationNumber) {
    if (confirmationNumber == null || reservationList == null) return null;
    return reservationList.find(
        r ->
            r.getConfirmationNumber().equalsIgnoreCase(confirmationNumber)
                && r.getStatus() == Reservation.Status.CHECKED_IN);
  }
}
