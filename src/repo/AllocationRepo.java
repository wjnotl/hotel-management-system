package repo;

import adt.ArrayList;
import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import util.BinaryFileUtil;
import util.TaskSchedulerUtil;

public class AllocationRepo {
  private final BinaryFileUtil<ListInterface<AllocationEntry>> fileUtil;
  private ListInterface<AllocationEntry> allocationList;
  private QueueInterface<AllocationEntry> allocationQueue;

  public AllocationRepo() {
    this.fileUtil = new BinaryFileUtil<>("allocations.dat");
    load();
  }

  private void load() {
    this.allocationList = fileUtil.retrieveFromFile();
    if (this.allocationList == null) {
      this.allocationList = new ArrayList<>();
    }
    rebuildQueue();
  }

  private void rebuildQueue() {
    this.allocationQueue = new LinkedQueue<>();
    if (this.allocationList != null) {
      for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
        AllocationEntry entry = allocationList.getEntry(i);
        if (entry != null) {
          this.allocationQueue.enqueue(entry);
        }
      }
    }
  }

  private void save() {
    fileUtil.saveToFile(allocationList);
  }

  public void addAllocationEntry(
      AllocationEntry entry,
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (entry == null) return;

    allocationList.add(entry);
    allocationQueue.enqueue(entry);
    save();

    // Re-arm scheduler when new entry is added
    scheduleNextAutoExpirationTask(roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);
  }

  public boolean removeAllocationEntry(
      AllocationEntry entry,
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    if (entry == null || allocationList == null) return false;

    boolean removed = false;
    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null && current.equals(entry)) {
        allocationList.removeAt(i);
        removed = true;
        break;
      }
    }

    if (removed) {
      rebuildQueue();
      save();

      // Re-arm scheduler when an entry is removed early
      scheduleNextAutoExpirationTask(
          roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);
    }
    return removed;
  }

  public ListInterface<AllocationEntry> getAllocationList() {
    return allocationList;
  }

  public void scheduleNextAutoExpirationTask(
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {

    // 1. Check if the Queue is empty using Queue ADT
    if (allocationQueue == null || allocationQueue.isEmpty()) return;

    // 2. Queue ADT peek(): The front of the queue is ALWAYS the earliest expiring entry!
    AllocationEntry earliestEntry = allocationQueue.peek();
    if (earliestEntry == null) return;

    // 3. Calculate remaining delay for the top queue item
    long now = System.currentTimeMillis();
    long remainingMs = earliestEntry.getExpirationTimestamp() - now;
    long delayMinutes = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(remainingMs));

    // 4. Schedule ONCE using TaskSchedulerUtil
    TaskSchedulerUtil.scheduleOnce(
        delayMinutes,
        () -> {
          try {
            // 5. DEQUEUE directly from your Queue ADT!
            if (!allocationQueue.isEmpty() && allocationQueue.peek().equals(earliestEntry)) {
              AllocationEntry expiredEntry = allocationQueue.dequeue(); // Queue DEQUEUE!

              // Remove from underlying list storage
              removeExpiredEntryInternal(expiredEntry);

              // Free Room
              Room room = roomRepo.findByRoomNumber(expiredEntry.getAssignedRoomNumber());
              Room.RoomType roomType = (room != null) ? room.getRoomType() : null;

              if (room != null) {
                room.setStatus(Room.Status.VACANT_CLEAN);
                room.setReservationConfirmationNumber(null);
                roomRepo.updateRoom(room);
              }

              // Issue Strike & Handle Reservation
              Reservation res = vipReservationRepo.findById(expiredEntry.getReservationId());
              if (res != null) {
                Guest guest = guestRepo.findById(res.getGuestId());
                if (guest != null) {
                  guest.setStrikeCount(guest.getStrikeCount() + 1);
                  guestRepo.updateGuest(guest);

                  Member member =
                      (guest.getMemberId() != null)
                          ? memberRepo.findById(guest.getMemberId())
                          : null;

                  VipSystemConfig config = configRepo.getConfig();
                  int maxStrikes =
                      (member != null && member.getTier() == Member.LoyaltyTier.DIAMOND)
                          ? config.getDiamondMaxStrikes()
                          : (member != null && member.getTier() == Member.LoyaltyTier.GOLD)
                              ? config.getGoldMaxStrikes()
                              : config.getSilverMaxStrikes();

                  if (guest.getStrikeCount() >= maxStrikes) {
                    res.setStatus(Reservation.Status.NO_SHOW);
                  } else {
                    int newScore = calculateDynamicPriorityScore(guest, member);
                    res.setStatus(Reservation.Status.WAITING);
                    res.setPriorityScore(newScore);
                    res.setQueueArrivalTime(LocalDateTime.now());
                    vipReservationRepo.addReservation(res, newScore);
                  }
                  vipReservationRepo.updateReservation(res);
                }
              }

              // Dequeue next VIP from waitlist queue into freed room
              if (roomType != null && room != null) {
                autoAssignNextWaitingVip(
                    roomType,
                    room,
                    vipReservationRepo,
                    roomRepo,
                    guestRepo,
                    memberRepo,
                    configRepo);
              }
            }
          } catch (Exception ignored) {
          } finally {
            // 6. CHAIN: Re-arm for whatever item is now at the front of the queue!
            scheduleNextAutoExpirationTask(
                roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);
          }
        });
  }

  private void removeExpiredEntryInternal(AllocationEntry entry) {
    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null && current.equals(entry)) {
        allocationList.removeAt(i);
        break;
      }
    }
    rebuildQueue();
    save();
  }

  private void autoAssignNextWaitingVip(
      Room.RoomType roomType,
      Room vacantRoom,
      VipReservationRepo vipReservationRepo,
      RoomRepo roomRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {

    ListInterface<Reservation> queueList = vipReservationRepo.getListByRoomType(roomType);
    if (queueList == null || queueList.isEmpty()) return;

    Reservation topVip = queueList.getEntry(1);
    if (topVip == null) return;

    Guest guest = (guestRepo != null) ? guestRepo.findById(topVip.getGuestId()) : null;
    Member member =
        (guest != null && guest.getMemberId() != null && memberRepo != null)
            ? memberRepo.findById(guest.getMemberId())
            : null;

    VipSystemConfig config = configRepo.getConfig();
    Member.LoyaltyTier tier = (member != null) ? member.getTier() : null;
    int graceMins =
        (tier == Member.LoyaltyTier.DIAMOND)
            ? config.getDiamondGraceWindowMins()
            : (tier == Member.LoyaltyTier.GOLD)
                ? config.getGoldGraceWindowMins()
                : config.getSilverGraceWindowMins();

    long holdDurationMs = graceMins * 60 * 1000L;

    AllocationEntry newHold =
        new AllocationEntry(
            topVip.getReservationId(),
            vacantRoom.getRoomNumber(),
            System.currentTimeMillis() + holdDurationMs);

    allocationList.add(newHold);
    allocationQueue.enqueue(newHold);
    save();

    vacantRoom.setStatus(Room.Status.OCCUPIED);
    vacantRoom.setReservationConfirmationNumber(topVip.getConfirmationNumber());
    roomRepo.updateRoom(vacantRoom);

    topVip.setStatus(Reservation.Status.ALLOCATED);
    vipReservationRepo.updateReservation(topVip);
    vipReservationRepo.cancelReservation(topVip);
  }

  private int calculateDynamicPriorityScore(Guest guest, Member member) {
    if (member == null || member.getTier() == null) return 1000;
    int tierBase;
    switch (member.getTier()) {
      case DIAMOND:
        tierBase = 9000;
        break;
      case GOLD:
        tierBase = 7000;
        break;
      case SILVER:
        tierBase = 5000;
        break;
      default:
        tierBase = 1000;
        break;
    }
    int pointsBonus = Math.min(member.getPoints() / 10, 800);
    int strikePenalty = (guest != null) ? (guest.getStrikeCount() * 500) : 0;
    return Math.max(1000, tierBase + pointsBonus - strikePenalty);
  }
}
