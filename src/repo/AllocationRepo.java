package repo;

import adt.ArrayList;
import adt.ListInterface;
import control.vip.VipController;
import entity.AllocationEntry;
import entity.Guest;
import entity.Member;
import entity.Reservation;
import entity.Room;
import entity.VipSystemConfig;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    long now = System.currentTimeMillis();

    // 1. Process any overdue expired allocations synchronously (where expirationTimestamp <= now)
    boolean processedOverdue = false;
    for (int i = allocationList.getNumberOfEntries(); i >= 1; i--) {
      AllocationEntry entry = allocationList.getEntry(i);
      if (entry != null && entry.getExpirationTimestamp() <= now) {
        allocationList.removeAt(i);
        processedOverdue = true;

        Room room = roomRepo.findByRoomNumber(entry.getAssignedRoomNumber());
        if (room != null) {
          room.setStatus(Room.Status.VACANT_CLEAN);
          room.setReservationConfirmationNumber(null);
          roomRepo.updateRoom(room);
        }

        Reservation res = vipReservationRepo.findById(entry.getReservationId());
        if (res != null) {
          Guest guest = guestRepo.findById(res.getGuestId());
          if (guest != null) {
            Member member =
                (guest.getMemberId() != null) ? memberRepo.findById(guest.getMemberId()) : null;

            VipSystemConfig config = configRepo.getConfig();
            int maxStrikes =
                (member != null && member.getTier() == Member.LoyaltyTier.DIAMOND)
                    ? config.getDiamondMaxStrikes()
                    : (member != null && member.getTier() == Member.LoyaltyTier.GOLD)
                        ? config.getGoldMaxStrikes()
                        : config.getSilverMaxStrikes();

            guest.setStrikeCount(guest.getStrikeCount() + 1);
            guestRepo.updateGuest(guest);

            res.setStatus(Reservation.Status.NO_SHOW);
            vipReservationRepo.updateReservation(res);

            if (guest.getStrikeCount() <= maxStrikes) {
              int newScore = vipReservationRepo.calculatePriorityScore(res, guest, member, config);

              String newResId = vipReservationRepo.generateReservationId();
              Reservation newRes =
                  new Reservation(
                      newResId,
                      guest.getGuestId(),
                      null,
                      res.getRoomType(),
                      Reservation.Status.WAITING,
                      res.getIsBoiling(),
                      newScore,
                      LocalDateTime.now(),
                      LocalDateTime.now(),
                      true);

              vipReservationRepo.addReservation(newRes);
              VipController.scheduleNextBoilingTask(
                  vipReservationRepo, guestRepo, memberRepo, configRepo);
            }
          }
        }
      }
    }

    if (processedOverdue) {
      save();
    }

    // 2. Find the earliest FUTURE expiration target (where expirationTimestamp > now)
    AllocationEntry earliestFutureEntry = null;
    long earliestFutureTime = Long.MAX_VALUE;

    for (int i = 1; i <= allocationList.getNumberOfEntries(); i++) {
      AllocationEntry current = allocationList.getEntry(i);
      if (current != null
          && current.getExpirationTimestamp() > now
          && current.getExpirationTimestamp() < earliestFutureTime) {
        earliestFutureTime = current.getExpirationTimestamp();
        earliestFutureEntry = current;
      }
    }

    if (earliestFutureEntry == null) return;

    final AllocationEntry targetEntry = earliestFutureEntry;
    long remainingMs = Math.max(1, earliestFutureTime - now);

    TaskSchedulerUtil.scheduleOnce(
        remainingMs,
        () -> {
          try {
            if (allocationList.contains(targetEntry)) {
              removeExpiredEntryInternal(targetEntry);

              Room room = roomRepo.findByRoomNumber(targetEntry.getAssignedRoomNumber());

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

                  guest.setStrikeCount(guest.getStrikeCount() + 1);
                  guestRepo.updateGuest(guest);

                  res.setStatus(Reservation.Status.NO_SHOW);
                  vipReservationRepo.updateReservation(res);

                  if (guest.getStrikeCount() <= maxStrikes) {
                    int newScore =
                        vipReservationRepo.calculatePriorityScore(
                            res, guest, member, configRepo.getConfig());

                    String newResId = vipReservationRepo.generateReservationId();
                    Reservation newRes =
                        new Reservation(
                            newResId,
                            guest.getGuestId(),
                            null,
                            res.getRoomType(),
                            Reservation.Status.WAITING,
                            res.getIsBoiling(),
                            newScore,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            true);

                    vipReservationRepo.addReservation(newRes);
                    VipController.scheduleNextBoilingTask(
                        vipReservationRepo, guestRepo, memberRepo, configRepo);
                  }
                }
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

  public void processExpiredAllocationsOnStartup(
      RoomRepo roomRepo,
      VipReservationRepo vipReservationRepo,
      GuestRepo guestRepo,
      MemberRepo memberRepo,
      VipSystemConfigRepo configRepo) {
    // Delegates directly to scheduleNextAutoExpirationTask which processes overdue holds
    // synchronously and arms the timer for future expirations.
    scheduleNextAutoExpirationTask(roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);
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
    long now =
        System.currentTimeMillis(); // Current system time as raw milliseconds since 1 Jan 1970 UTC

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

        // Calculate new expiration target from the original allocation timestamp
        long newExpiration;
        if (r != null && r.getAllocatedTime() != null) {
          long startMs =
              r.getAllocatedTime()
                  .atZone(ZoneId.systemDefault()) // Attach local timezone (UTC+8)
                  .toInstant() // Converts to UTC Instant
                  .toEpochMilli(); // Converts to raw long ms
          newExpiration = startMs + (newGraceMins * 60 * 1000L);
          r.setAllocatedGraceMins(newGraceMins);
          vipReservationRepo.updateReservation(r);
        } else {
          newExpiration = now + (newGraceMins * 60 * 1000L);
        }

        // If hold has already exceeded the new grace window, expire it immediately
        entry.setExpirationTimestamp(Math.min(newExpiration, now));
        updatedCount++;
      }
    }

    save();

    // Re-arm auto-expiration scheduler with updated top item
    scheduleNextAutoExpirationTask(roomRepo, vipReservationRepo, guestRepo, memberRepo, configRepo);

    return updatedCount;
  }
}
