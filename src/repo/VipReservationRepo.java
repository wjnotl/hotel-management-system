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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Function;

public class VipReservationRepo {
  private ReservationRepo reservationRepo;

  // 3 Dedicated Lists for O(1) queue separation
  private ListInterface<Reservation> luxuryList;
  private ListInterface<Reservation> suiteList;
  private ListInterface<Reservation> standardList;

  // 3 Dedicated Priority Queues (Max Heaps)
  private PriorityQueueInterface<Reservation> luxuryVipQueue;
  private PriorityQueueInterface<Reservation> suiteVipQueue;
  private PriorityQueueInterface<Reservation> standardVipQueue;

  public VipReservationRepo(ReservationRepo reservationRepo) {
    this.reservationRepo = reservationRepo;
    load();
  }

  private void load() {
    this.luxuryList = new ArrayList<>();
    this.suiteList = new ArrayList<>();
    this.standardList = new ArrayList<>();

    Comparator<Reservation> vipReservationComparator =
        new Comparator<>() {
          @Override
          public int compare(Reservation a, Reservation b) {
            if (a == b) return 0;
            if (a == null) return -1;
            if (b == null) return 1;

            // Higher score returns positive -> a gets dequeued before b
            int scoreComp = Integer.compare(a.getPriorityScore(), b.getPriorityScore());
            if (scoreComp != 0) {
              return scoreComp;
            }

            // Tie-breaker: earlier arrival time gets dequeued first
            // b.compareTo(a) returns positive when a's timestamp is earlier than b's
            return b.getQueueArrivalTime().compareTo(a.getQueueArrivalTime());
          }
        };

    this.luxuryVipQueue = new BinaryHeapPriorityQueue<>(vipReservationComparator);
    this.suiteVipQueue = new BinaryHeapPriorityQueue<>(vipReservationComparator);
    this.standardVipQueue = new BinaryHeapPriorityQueue<>(vipReservationComparator);

    var vipReservationList = reservationRepo.getAllReservations().filter(r -> r.getIsVip());

    for (int i = 1; i <= vipReservationList.getNumberOfEntries(); i++) {
      Reservation r = vipReservationList.getEntry(i);
      if (r != null && r.getRoomType() != null && r.getStatus() == Reservation.Status.WAITING) {
        getListByRoomType(r.getRoomType()).add(r);
        getHeapByRoomType(r.getRoomType()).enqueue(r);
      }
    }
  }

  public ListInterface<Reservation> getListByRoomType(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryList;
    if (roomType == Room.RoomType.SUITE) return suiteList;
    return standardList;
  }

  public PriorityQueueInterface<Reservation> getHeapByRoomType(Room.RoomType roomType) {
    if (roomType == Room.RoomType.LUXURY) return luxuryVipQueue;
    if (roomType == Room.RoomType.SUITE) return suiteVipQueue;
    return standardVipQueue;
  }

  public void addReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return;

    if (!reservationRepo.getAllReservations().contains(reservation)) {
      reservationRepo.getAllReservations().add(reservation);
    } else {
      updateReservation(reservation);
    }

    if (reservation.getStatus() == Reservation.Status.WAITING) {
      getListByRoomType(reservation.getRoomType()).add(reservation);
      getHeapByRoomType(reservation.getRoomType()).enqueue(reservation);
    }

    reservationRepo.save();
  }

  public boolean allocateReservation(
      Reservation reservation,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    Room.RoomType type = reservation.getRoomType();

    // Determine grace minutes snapshot for this allocation session
    Guest guest = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
    Member member =
        (guest != null && guest.getMemberId() != null && memberRepo != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;

    VipSystemConfig config = (configRepo != null) ? configRepo.getConfig() : null;
    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
    int graceMins = (config != null) ? config.getGraceWindowMins(tier) : 15;

    if (reservation.getAllocatedTime() == null) {
      reservation.setAllocatedTime(LocalDateTime.now());
    }
    reservation.setAllocatedGraceMins(graceMins);

    // Remove from active waitlist queue & heap
    boolean heapRemoved = getHeapByRoomType(type).remove(reservation);
    boolean listRemoved = getListByRoomType(type).remove(reservation);

    // Mark status as ALLOCATED in masterList
    reservation.setStatus(Reservation.Status.ALLOCATED);
    boolean updatedInMaster = updateReservation(reservation);

    return heapRemoved || listRemoved || updatedInMaster;
  }

  public boolean updateReservation(Reservation updatedRes) {
    return reservationRepo.updateReservation(updatedRes);
  }

  public boolean cancelReservation(Reservation reservation) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    Room.RoomType type = reservation.getRoomType();

    // Remove from active queue & heap
    boolean heapRemoved = getHeapByRoomType(type).remove(reservation);
    boolean listRemoved = getListByRoomType(type).remove(reservation);

    // Mark status as CANCELLED in masterList
    reservation.setStatus(Reservation.Status.CANCELLED);
    boolean updatedInMaster = updateReservation(reservation);

    return heapRemoved || listRemoved || updatedInMaster;
  }

  public int calculatePriorityScore(
      Reservation reservation, Guest guest, Member member, VipSystemConfig config) {

    if (config == null) return 1000;

    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;

    // Resolve variable names to dynamic values
    Function<String, Double> resolver =
        (var) -> {
          switch (var.toUpperCase()) {
            case "TIER":
              return (double) config.getBaseValue(tier);

            case "STRIKES":
              return (guest != null) ? (double) guest.getStrikeCount() : 0.0;

            case "W_STRIKE":
              return config.getStrikePenalty(tier);

            case "BOILING":
              double wait =
                  (reservation != null && reservation.getQueueArrivalTime() != null)
                      ? Duration.between(reservation.getQueueArrivalTime(), LocalDateTime.now())
                          .toMinutes()
                      : 0.0;
              double boilingLimit = config.getBoilingLimitMins(tier);
              boolean isBoiling = wait >= boilingLimit;
              if (reservation != null) reservation.setBoiling(isBoiling);
              return isBoiling ? 1.0 : 0.0;

            case "W_BOILING":
              return config.getBoilingBoost(tier);

            default:
              return 0.0;
          }
        };

    double result =
        util.ExpressionEvaluator.evaluateInfix(config.getActiveFormulaInfix(), resolver);
    return (int) Math.max(0, Math.round(result));
  }

  public int applySettingsToQueue(
      VipSystemConfig config,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      boolean evictOverStrikes,
      boolean forceBoilingCheck) {

    var vipReservationList = reservationRepo.getAllReservations().filter(r -> r.getIsVip());
    if (vipReservationList == null || config == null) return 0;
    int affectedCount = 0;

    luxuryList.clear();
    suiteList.clear();
    standardList.clear();

    luxuryVipQueue.clear();
    suiteVipQueue.clear();
    standardVipQueue.clear();

    for (int i = 1; i <= vipReservationList.getNumberOfEntries(); i++) {
      Reservation reservation = vipReservationList.getEntry(i);

      if (reservation != null && reservation.getStatus() == Reservation.Status.WAITING) {
        Guest guest = (guestRepo != null) ? guestRepo.findById(reservation.getGuestId()) : null;
        Member member =
            (memberRepo != null && guest != null) ? memberRepo.findById(guest.getMemberId()) : null;

        Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
        int maxStrikes = config.getMaxStrikes(tier);

        if (evictOverStrikes && guest != null && guest.getStrikeCount() > maxStrikes) {
          reservation.setStatus(Reservation.Status.NO_SHOW);
          updateReservation(reservation);
          affectedCount++;
          continue;
        }

        boolean affected = false;

        if (forceBoilingCheck && reservation.getQueueArrivalTime() != null) {
          double waitMins =
              Duration.between(reservation.getQueueArrivalTime(), LocalDateTime.now()).toMinutes();
          double boilingLimit = config.getBoilingLimitMins(tier);
          reservation.setBoiling(waitMins >= boilingLimit);
          affected = true;
        }

        int newScore = calculatePriorityScore(reservation, guest, member, config);
        if (reservation.getPriorityScore() != newScore) {
          reservation.setPriorityScore(newScore);
          affected = true;
        }

        getListByRoomType(reservation.getRoomType()).add(reservation);
        getHeapByRoomType(reservation.getRoomType()).enqueue(reservation);

        if (affected) {
          updateReservation(reservation);
          affectedCount++;
        }
      }
    }

    return affectedCount;
  }

  public Reservation findById(String reservationId) {
    return reservationRepo.findById(reservationId);
  }

  public String generateReservationId() {
    return reservationRepo.generateReservationId();
  }

  public String generateConfirmationNumber() {
    return reservationRepo.generateConfirmationNumber();
  }

  public ListInterface<Reservation> getAllReservations() {
    return reservationRepo.getAllReservations().filter(r -> r.getIsVip());
  }
}
