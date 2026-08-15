package repo;

import adt.ArrayList;
import adt.ListInterface;
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

  public AllocationRepo() {
    this.fileUtil = new BinaryFileUtil<>("allocations.dat");
    load();
  }

  private void load() {
    this.allocationList = fileUtil.retrieveFromFile();
    if (this.allocationList == null) {
      this.allocationList = new ArrayList<>();
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
    save();

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

  private void scheduleNextAutoExpirationTask(
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {

    if (allocationList == null || allocationList.getNumberOfEntries() == 0) return;

    AllocationEntry earliestEntry = null;
    long earliestTime = Long.MAX_VALUE;

    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null && current.getExpirationTimestamp() < earliestTime) {
        earliestTime = current.getExpirationTimestamp();
        earliestEntry = current;
      }
    }

    if (earliestEntry == null) return;

    // Create a final reference copy for the lambda scope
    final AllocationEntry targetEntry = earliestEntry;

    // Calculate remaining delay
    long now = System.currentTimeMillis();
    long remainingMs = targetEntry.getExpirationTimestamp() - now;
    long delayMinutes = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(remainingMs));

    TaskSchedulerUtil.scheduleOnce(
        delayMinutes,
        () -> {
          try {
            // Use targetEntry instead of earliestEntry inside the lambda
            if (allocationList.contains(targetEntry)) {
              removeExpiredEntryInternal(targetEntry);

              Room room = roomRepo.findByRoomNumber(targetEntry.getAssignedRoomNumber());
              Room.RoomType roomType = (room != null) ? room.getRoomType() : null;

              if (room != null) {
                room.setStatus(Room.Status.VACANT_CLEAN);
                room.setReservationConfirmationNumber(null);
                roomRepo.updateRoom(room);
              }

              Reservation res = vipReservationRepo.findById(targetEntry.getReservationId());
              if (res != null) {
                Guest guest = guestRepo.findById(res.getGuestId());
                if (guest != null) {
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

                  // Check if already at the limit?
                  if (guest.getStrikeCount() >= maxStrikes) {
                    res.setStatus(Reservation.Status.NO_SHOW);
                    vipReservationRepo.updateReservation(res);
                  } else {
                    // Under limit: mark current as NO_SHOW, increment strike, create new WAITING reservation
                    res.setStatus(Reservation.Status.NO_SHOW);
                    vipReservationRepo.updateReservation(res);

                    guest.setStrikeCount(guest.getStrikeCount() + 1);
                    guestRepo.updateGuest(guest);

                    int newScore =
                        vipReservationRepo.calculatePriorityScore(
                            res, guest, member, configRepo.getConfig());

                    String newResId = vipReservationRepo.generateReservationId();
                    String newConfNum = vipReservationRepo.generateConfirmationNumber();
                    Reservation newRes = new Reservation(
                        newResId,
                        guest.getGuestId(),
                        newConfNum,
                        res.getRoomType(),
                        Reservation.Status.WAITING,
                        res.getIsBoiling(),
                        newScore,
                        LocalDateTime.now(),
                        LocalDateTime.now());

                    vipReservationRepo.addReservation(
                        newRes, newScore, guestRepo, memberRepo, configRepo);
                  }
                }
              }

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
    save();

    vacantRoom.setStatus(Room.Status.OCCUPIED);
    vacantRoom.setReservationConfirmationNumber(topVip.getConfirmationNumber());
    roomRepo.updateRoom(vacantRoom);

    vipReservationRepo.allocateReservation(topVip, guestRepo, memberRepo, configRepo);
  }

  public int recalculateActiveGraceTimers(
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    VipSystemConfig config = configRepo.getConfig();
    if (allocationList == null || allocationList.isEmpty()) return 0;

    int updatedCount = 0;
    long now = System.currentTimeMillis();

    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry entry = allocationList.getEntry(i);
      if (entry != null) {
        Reservation r = vipReservationRepo.findById(entry.getReservationId());
        Guest g = (r != null) ? guestRepo.findById(r.getGuestId()) : null;
        Member m =
            (g != null && g.getMemberId() != null) ? memberRepo.findById(g.getMemberId()) : null;

        Member.LoyaltyTier tier = (m != null) ? m.getTier() : null;
        int newGraceMins =
            (tier == Member.LoyaltyTier.DIAMOND)
                ? config.getDiamondGraceWindowMins()
                : (tier == Member.LoyaltyTier.GOLD)
                    ? config.getGoldGraceWindowMins()
                    : config.getSilverGraceWindowMins();

        // Reset expiration timestamp based on new grace minutes from current time
        entry.setExpirationTimestamp(now + (newGraceMins * 60 * 1000L));
        updatedCount++;
      }
    }

    save();

    // Re-arm auto-expiration scheduler with updated top item
    scheduleNextAutoExpirationTask(roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);

    return updatedCount;
  }
}
