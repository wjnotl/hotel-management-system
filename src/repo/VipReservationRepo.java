package repo;

import adt.ArrayList;
import adt.BinaryHeapPriorityQueue;
import adt.ListInterface;
import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
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

  public int calculatePriorityScore(
      Reservation reservation, Guest guest, Member member, VipSystemConfig config) {

    if (config == null) return 1000;

    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

    // Resolve variable names to dynamic values
    java.util.function.Function<String, Double> resolver =
        (var) -> {
          switch (var.toUpperCase()) {
            case "TIER":
              return (tier == Member.LoyaltyTier.DIAMOND)
                  ? (double) config.getDiamondBaseValue()
                  : (tier == Member.LoyaltyTier.GOLD)
                      ? (double) config.getGoldBaseValue()
                      : (tier == Member.LoyaltyTier.SILVER)
                          ? (double) config.getSilverBaseValue()
                          : 1000.0;
            case "W_TIER":
              return 1.0;
            case "WAIT":
              if (reservation == null || reservation.getQueueArrivalTime() == null) return 0.0;
              return (double)
                  java.time.Duration.between(
                          reservation.getQueueArrivalTime(), java.time.LocalDateTime.now())
                      .toMinutes();
            case "W_TIME":
              return (tier == Member.LoyaltyTier.DIAMOND)
                  ? config.getDiamondTimeWeight()
                  : (tier == Member.LoyaltyTier.GOLD)
                      ? config.getGoldTimeWeight()
                      : config.getSilverTimeWeight();
            case "STRIKES":
              return (guest != null) ? (double) guest.getStrikeCount() : 0.0;
            case "W_STRIKE":
              return (tier == Member.LoyaltyTier.DIAMOND)
                  ? config.getDiamondStrikePenalty()
                  : (tier == Member.LoyaltyTier.GOLD)
                      ? config.getGoldStrikePenalty()
                      : config.getSilverStrikePenalty();
            case "BOILING":
              double wait =
                  (reservation != null && reservation.getQueueArrivalTime() != null)
                      ? java.time.Duration.between(
                              reservation.getQueueArrivalTime(), java.time.LocalDateTime.now())
                          .toMinutes()
                      : 0.0;
              double patienceLimit =
                  (tier == Member.LoyaltyTier.DIAMOND)
                      ? config.getDiamondPatienceLimitMins()
                      : (tier == Member.LoyaltyTier.GOLD)
                          ? config.getGoldPatienceLimitMins()
                          : config.getSilverPatienceLimitMins();
              boolean isBoiling = wait >= patienceLimit;
              if (reservation != null) reservation.setBoiling(isBoiling);
              return isBoiling ? 1.0 : 0.0;
            case "W_BOILING":
              return (tier == Member.LoyaltyTier.DIAMOND)
                  ? config.getDiamondBoilingBoost()
                  : (tier == Member.LoyaltyTier.GOLD)
                      ? config.getGoldBoilingBoost()
                      : config.getSilverBoilingBoost();
            default:
              return 0.0;
          }
        };

    double result =
        util.ExpressionEvaluator.evaluateInfix(config.getActiveFormulaInfix(), resolver);
    return (int) Math.max(1000, Math.round(result));
  }
}
