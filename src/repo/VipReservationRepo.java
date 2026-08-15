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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import util.BinaryFileUtil;
import util.TaskSchedulerUtil;

public class VipReservationRepo {
  private final BinaryFileUtil<ListInterface<Reservation>> fileUtil;
  private ListInterface<Reservation> masterList;

  // 3 Dedicated Lists for O(1) queue separation
  private ListInterface<Reservation> luxuryList;
  private ListInterface<Reservation> suiteList;
  private ListInterface<Reservation> standardList;

  // 3 Dedicated Priority Queues (Max Heaps)
  private PriorityQueueInterface<Reservation> luxuryVipQueue;
  private PriorityQueueInterface<Reservation> suiteVipQueue;
  private PriorityQueueInterface<Reservation> standardVipQueue;

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

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
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

  private void save() {
    fileUtil.saveToFile(masterList);
  }

  public void addReservation(
      Reservation reservation,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (reservation == null || reservation.getRoomType() == null) return;

    if (!masterList.contains(reservation)) {
      masterList.add(reservation);
    } else {
      updateReservation(reservation);
    }

    if (reservation.getStatus() == Reservation.Status.WAITING) {
      getListByRoomType(reservation.getRoomType()).add(reservation);
      getHeapByRoomType(reservation.getRoomType()).enqueue(reservation);
    }

    save();
    scheduleNextBoilingTask(guestRepo, memberRepo, configRepo);
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
    int graceMins =
        (tier == Member.LoyaltyTier.DIAMOND)
            ? (config != null ? config.getDiamondGraceWindowMins() : 45)
            : (tier == Member.LoyaltyTier.GOLD)
                ? (config != null ? config.getGoldGraceWindowMins() : 30)
                : (config != null ? config.getSilverGraceWindowMins() : 15);

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

    // Re-arm boiling task for the new top reservation in line
    scheduleNextBoilingTask(guestRepo, memberRepo, configRepo);

    return heapRemoved || listRemoved || updatedInMaster;
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

  public boolean cancelReservation(
      Reservation reservation,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (reservation == null || reservation.getRoomType() == null) return false;

    Room.RoomType type = reservation.getRoomType();

    // Remove from active queue & heap
    boolean heapRemoved = getHeapByRoomType(type).remove(reservation);
    boolean listRemoved = getListByRoomType(type).remove(reservation);

    // Mark status as CANCELLED in masterList
    reservation.setStatus(Reservation.Status.CANCELLED);
    boolean updatedInMaster = updateReservation(reservation);

    scheduleNextBoilingTask(guestRepo, memberRepo, configRepo);
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
              double boilingLimit =
                  (tier == Member.LoyaltyTier.DIAMOND)
                      ? config.getDiamondBoilingLimitMins()
                      : (tier == Member.LoyaltyTier.GOLD)
                          ? config.getGoldBoilingLimitMins()
                          : config.getSilverBoilingLimitMins();
              boolean isBoiling = wait >= boilingLimit;
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

  public int applySettingsToQueue(
      VipSystemConfig config,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      boolean evictOverStrikes,
      boolean forceBoilingCheck,
      VipSystemConfigRepo configRepo) {

    if (masterList == null || config == null) return 0;
    int affectedCount = 0;

    luxuryList.clear();
    suiteList.clear();
    standardList.clear();

    luxuryVipQueue.clear();
    suiteVipQueue.clear();
    standardVipQueue.clear();

    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);

      if (r != null && r.getStatus() == Reservation.Status.WAITING) {
        Guest guest = (guestRepo != null) ? guestRepo.findById(r.getGuestId()) : null;
        Member member =
            (memberRepo != null && guest != null) ? memberRepo.findById(guest.getMemberId()) : null;

        Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
        int maxStrikes =
            (tier == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondMaxStrikes()
                : (tier == Member.LoyaltyTier.GOLD)
                    ? config.getGoldMaxStrikes()
                    : config.getSilverMaxStrikes();

        if (evictOverStrikes && guest != null && guest.getStrikeCount() >= maxStrikes) {
          r.setStatus(Reservation.Status.NO_SHOW);
          updateReservation(r);
          affectedCount++;
          continue;
        }

        if (forceBoilingCheck && r.getQueueArrivalTime() != null) {
          double waitMins =
              java.time.Duration.between(r.getQueueArrivalTime(), LocalDateTime.now()).toMinutes();
          double boilingLimit =
              (tier == Member.LoyaltyTier.DIAMOND)
                  ? config.getDiamondBoilingLimitMins()
                  : (tier == Member.LoyaltyTier.GOLD)
                      ? config.getGoldBoilingLimitMins()
                      : config.getSilverBoilingLimitMins();
          r.setBoiling(waitMins >= boilingLimit);
        }

        int newScore = calculatePriorityScore(r, guest, member, config);
        r.setPriorityScore(newScore);

        getListByRoomType(r.getRoomType()).add(r);
        getHeapByRoomType(r.getRoomType()).enqueue(r);
        affectedCount++;
      }
    }

    save();
    scheduleNextBoilingTask(guestRepo, memberRepo, configRepo);
    return affectedCount;
  }

  private void scheduleNextBoilingTask(
      GuestRepo guestRepo, MemberRepo memberRepo, VipSystemConfigRepo configRepo) {
    VipSystemConfig config = configRepo.getConfig();
    if (config == null) return;

    // 1. Find the top guest and its heap using a clean ternary or helper approach
    // so they are effectively final for the lambda expression!
    final Reservation topRes;
    final PriorityQueueInterface<Reservation> activeHeap;

    if (!luxuryVipQueue.isEmpty()) {
      topRes = luxuryVipQueue.peek();
      activeHeap = luxuryVipQueue;
    } else if (!suiteVipQueue.isEmpty()) {
      topRes = suiteVipQueue.peek();
      activeHeap = suiteVipQueue;
    } else if (!standardVipQueue.isEmpty()) {
      topRes = standardVipQueue.peek();
      activeHeap = standardVipQueue;
    } else {
      topRes = null;
      activeHeap = null;
    }

    if (topRes == null || topRes.getIsBoiling() || topRes.getQueueArrivalTime() == null) {
      return;
    }

    // 2. Resolve tier-specific boiling limit
    Guest g = guestRepo.findById(topRes.getGuestId());
    Member m = (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;
    Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;

    int boilingLimitMins =
        (tier == Member.LoyaltyTier.DIAMOND)
            ? config.getDiamondBoilingLimitMins()
            : (tier == Member.LoyaltyTier.GOLD)
                ? config.getGoldBoilingLimitMins()
                : config.getSilverBoilingLimitMins();

    // 3. Calculate exact time left until this specific tier's limit is hit
    LocalDateTime boilingTargetTime = topRes.getQueueArrivalTime().plusMinutes(boilingLimitMins);
    long targetMs = java.sql.Timestamp.valueOf(boilingTargetTime).getTime();
    long delayMs = targetMs - System.currentTimeMillis();
    long delayMinutes = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(delayMs));

    // 4. Schedule ONCE for the top guest only (topRes and activeHeap are now effectively final!)
    TaskSchedulerUtil.scheduleOnce(
        delayMinutes,
        () -> {
          try {
            if (topRes.getStatus() == Reservation.Status.WAITING && !topRes.getIsBoiling()) {
              topRes.setBoiling(true);
              int newScore = calculatePriorityScore(topRes, g, m, config);
              topRes.setPriorityScore(newScore);
              updateReservation(topRes);

              // Re-sort the heap automatically
              if (activeHeap != null) {
                activeHeap.updatePriority(topRes);
              }
            }
          } catch (Exception ignored) {
          }
        });
  }

  public String generateReservationId() {
    int maxId = 10000;
    if (masterList != null) {
      for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
        Reservation r = masterList.getEntry(i);
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
      if (findByActiveConfirmationNumber(code) == null) {
        return code;
      }
    }
  }

  public Reservation findByActiveConfirmationNumber(String confirmationNumber) {
    if (confirmationNumber == null || masterList == null) return null;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null
          && confirmationNumber.equalsIgnoreCase(r.getConfirmationNumber())
          && (r.getStatus() == Reservation.Status.WAITING
              || r.getStatus() == Reservation.Status.ALLOCATED
              || r.getStatus() == Reservation.Status.CHECKED_IN)) {
        return r;
      }
    }
    return null;
  }

  public Reservation findByConfirmationNumber(String confirmationNumber) {
    if (confirmationNumber == null || masterList == null) return null;
    for (int i = 1; i <= masterList.getNumberOfEntries(); i++) {
      Reservation r = masterList.getEntry(i);
      if (r != null && confirmationNumber.equalsIgnoreCase(r.getConfirmationNumber())) {
        return r;
      }
    }
    return null;
  }
}
